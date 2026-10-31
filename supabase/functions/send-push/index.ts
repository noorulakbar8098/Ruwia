// Ruwia send-push — Supabase Edge Function.
//
// Turns an in-app notification row into outside-the-app FCM pushes.
// Trigger: Database Webhook on notifications INSERT (see
// PUSH_NOTIFICATIONS_SETUP.md), or POST {"notification_id": "<uuid>"}.
//
// Env (Dashboard → Edge Functions → Secrets):
//   FCM_PROJECT_ID      Firebase project id
//   FCM_SERVICE_ACCOUNT full service-account JSON (Firebase → Project
//                       settings → Service accounts → Generate new key)
// SUPABASE_URL + SUPABASE_SERVICE_ROLE_KEY are injected automatically.

import { createClient } from "jsr:@supabase/supabase-js@2";
import { SignJWT, importPKCS8 } from "npm:jose@5.10.0";

const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

async function googleAccessToken(serviceAccount: {
  client_email: string;
  private_key: string;
}): Promise<string> {
  const jwt = await new SignJWT({ scope: FCM_SCOPE })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuedAt()
    .setIssuer(serviceAccount.client_email)
    .setSubject(serviceAccount.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setExpirationTime("1h")
    .sign(await importPKCS8(serviceAccount.private_key, "RS256"));
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });
  if (!res.ok) throw new Error(`oauth2 failed: ${await res.text()}`);
  const { access_token } = await res.json();
  return access_token as string;
}

Deno.serve(async (req: Request) => {
  try {
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );
    const projectId = Deno.env.get("FCM_PROJECT_ID")!;
    const serviceAccount = JSON.parse(Deno.env.get("FCM_SERVICE_ACCOUNT")!);

    // Accept a direct id or a Database-Webhook envelope.
    const payload = await req.json().catch(() => ({}));
    let notification = payload.record ?? null;
    if (!notification && payload.notification_id) {
      const { data, error } = await supabase
        .from("notifications")
        .select("*")
        .eq("id", payload.notification_id)
        .single();
      if (error) throw error;
      notification = data;
    }
    if (!notification?.id || !notification?.admin_id) {
      return Response.json({ ok: false, error: "no notification found" }, { status: 400 });
    }

    // Resolve recipient user ids within the tenant.
    let userIds: string[] = [];
    if (notification.to_user_id) {
      const { data, error } = await supabase
        .from("employees")
        .select("id")
        .eq("id", notification.to_user_id)
        .eq("admin_id", notification.admin_id)
        .eq("status", "active")
        .limit(1);
      if (error) throw error;
      userIds = (data ?? []).map((employee: { id: string }) => employee.id);
    } else if (notification.to_role === "employee") {
      const { data, error } = await supabase
        .from("employees")
        .select("id")
        .eq("admin_id", notification.admin_id)
        .eq("status", "active");
      if (error) throw error;
      userIds = (data ?? []).map((e: { id: string }) => e.id);
    } else if (notification.to_role === "admin") {
      userIds = [notification.admin_id];
    }
    if (userIds.length === 0) {
      return Response.json({ ok: true, sent: 0, reason: "no recipients" });
    }

    // Their registered device tokens.
    const { data: tokens, error: tokenError } = await supabase
      .from("device_tokens")
      .select("token")
      .in("user_id", userIds)
      .eq("admin_id", notification.admin_id);
    if (tokenError) throw tokenError;
    const pushTokens = [...new Set((tokens ?? []).map((t: { token: string }) => t.token))];
    if (pushTokens.length === 0) {
      return Response.json({ ok: true, sent: 0, reason: "no device tokens" });
    }

    const accessToken = await googleAccessToken(serviceAccount);
    let sent = 0;
    const failures: string[] = [];
    for (const token of pushTokens) {
      const res = await fetch(
        `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            message: {
              token,
              notification: {
                title: notification.title ?? "Ruwia",
                body: notification.body ?? "",
              },
              data: {
                notification_id: String(notification.id),
                kind: String(notification.kind ?? "general"),
              },
            },
          }),
        },
      );
      if (res.ok) {
        sent++;
      } else {
        failures.push(await res.text());
      }
    }
    return Response.json({ ok: true, sent, failed: failures.length });
  } catch (e) {
    return Response.json({ ok: false, error: String(e) }, { status: 500 });
  }
});
