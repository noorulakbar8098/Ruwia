// Ruwia admin-users — privileged user management.
//
// The service-role key NEVER ships in the app. This function runs on Supabase,
// reads the key from the encrypted function environment, verifies that the
// caller is a tenant admin, and only then performs the privileged operation.
//
// Deploy with the default JWT verification enabled:
//   supabase functions deploy admin-users
//
// SUPABASE_URL and SUPABASE_ANON_KEY are injected automatically. The privileged
// key is read from the RUWIA_SERVICE_KEY function secret (a server-only
// sb_secret_… key) with the built-in SUPABASE_SERVICE_ROLE_KEY as fallback.
// Neither value is ever present in the repository or the app.

import { createClient } from "jsr:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const EMPLOYEE_ROLES = new Set(["manager", "stock", "cashier", "driver"]);
const MIN_PASSWORD_LENGTH = 8;
const BAN_DURATION = "876000h"; // 100 years — effectively permanent until unbanned.

interface RequestBody {
  action?: string;
  email?: string;
  password?: string;
  name?: string;
  phone?: string;
  role?: string;
  shopName?: string;
  salary?: number;
  employeeId?: string;
}

/** Always return JSON so the client can show a useful message. */
function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
  });
}

function bearerToken(req: Request): string | null {
  const header = req.headers.get("Authorization") ?? "";
  return header.startsWith("Bearer ") ? header.slice(7).trim() : null;
}

function clean(value: unknown): string {
  return typeof value === "string" ? value.trim() : "";
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: CORS_HEADERS });
  }
  if (req.method !== "POST") {
    return json({ ok: false, error: "method not allowed" }, 405);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey =
    Deno.env.get("RUWIA_SERVICE_KEY") ?? Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceKey) {
    return json({ ok: false, error: "server is not configured" }, 500);
  }

  const token = bearerToken(req);
  if (!token) {
    return json({ ok: false, error: "missing session" }, 401);
  }

  // Identify the caller from their own JWT — never trust a user id in the body.
  const userResponse = await fetch(`${supabaseUrl}/auth/v1/user`, {
    headers: { apikey: anonKey, Authorization: `Bearer ${token}` },
  });
  if (!userResponse.ok) {
    return json({ ok: false, error: "invalid session" }, 401);
  }
  const caller = await userResponse.json();
  if (!caller?.id) {
    return json({ ok: false, error: "invalid session" }, 401);
  }

  // Service-role client stays inside the function. It is never sent to a device.
  const admin = createClient(supabaseUrl, serviceKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });

  const { data: callerProfile, error: callerError } = await admin
    .from("profiles")
    .select("role, admin_id")
    .eq("id", caller.id)
    .maybeSingle();
  if (callerError) {
    return json({ ok: false, error: "could not verify your account" }, 500);
  }
  if (callerProfile?.role !== "admin") {
    return json({ ok: false, error: "admin role required" }, 403);
  }
  const tenantId = callerProfile.admin_id ?? caller.id;

  const body = (await req.json().catch(() => null)) as RequestBody | null;
  const action = clean(body?.action);
  if (!action) {
    return json({ ok: false, error: "action is required" }, 400);
  }

  if (action === "createEmployee") {
    const email = clean(body?.email).toLowerCase();
    const password = typeof body?.password === "string" ? body.password : "";
    const name = clean(body?.name);
    const phone = clean(body?.phone);
    const role = clean(body?.role).toLowerCase() || "driver";
    const shopName = clean(body?.shopName);
    const salary = Number(body?.salary ?? 0);

    if (!email.includes("@") || !password || !name) {
      return json({ ok: false, error: "name, email and password are required" }, 400);
    }
    if (password.length < MIN_PASSWORD_LENGTH) {
      return json(
        { ok: false, error: `password must be at least ${MIN_PASSWORD_LENGTH} characters` },
        400,
      );
    }
    if (!EMPLOYEE_ROLES.has(role)) {
      return json({ ok: false, error: "invalid employee role" }, 400);
    }
    if (!Number.isFinite(salary) || salary < 0) {
      return json({ ok: false, error: "invalid salary" }, 400);
    }

    const { data: created, error: createError } = await admin.auth.admin.createUser({
      email,
      password,
      email_confirm: true,
      user_metadata: { full_name: name, phone },
    });
    if (createError || !created?.user) {
      return json({ ok: false, error: createError?.message ?? "could not create user" }, 400);
    }
    const userId = created.user.id;

    // The signup trigger creates every auth user as an independent admin.
    // Move the new account into the caller's tenant with the employee role.
    // Service-role access is required because clients cannot change tenant or
    // role columns.
    const { error: profileError } = await admin
      .from("profiles")
      .update({ role: "employee", admin_id: tenantId, full_name: name, phone })
      .eq("id", userId);
    if (profileError) {
      await admin.auth.admin.deleteUser(userId).catch(() => {});
      return json({ ok: false, error: "could not create employee profile" }, 500);
    }

    const { data: employee, error: employeeError } = await admin
      .from("employees")
      .insert({
        id: userId,
        name,
        phone,
        role,
        shop_name: shopName,
        monthly_salary: salary,
        status: "active",
        admin_id: tenantId,
      })
      .select("id, name, phone, role, shop_name, monthly_salary, status")
      .single();
    if (employeeError) {
      await admin.auth.admin.deleteUser(userId).catch(() => {});
      return json({ ok: false, error: "could not create employee record" }, 500);
    }

    return json({ ok: true, employee });
  }

  if (action === "deactivateEmployee") {
    const employeeId = clean(body?.employeeId);
    if (!employeeId) {
      return json({ ok: false, error: "employeeId is required" }, 400);
    }

    // Tenant check: the target must belong to the caller's tenant.
    const { data: employee, error: employeeError } = await admin
      .from("employees")
      .select("id")
      .eq("id", employeeId)
      .eq("admin_id", tenantId)
      .maybeSingle();
    if (employeeError) {
      return json({ ok: false, error: "could not verify employee" }, 500);
    }
    if (!employee) {
      return json({ ok: false, error: "employee not found" }, 404);
    }

    const { error: updateError } = await admin
      .from("employees")
      .update({ status: "inactive" })
      .eq("id", employeeId)
      .eq("admin_id", tenantId);
    if (updateError) {
      return json({ ok: false, error: "could not deactivate employee" }, 500);
    }

    // Ban the auth user so existing refresh tokens stop working too.
    const { error: banError } = await admin.auth.admin.updateUserById(employeeId, {
      ban_duration: BAN_DURATION,
      user_metadata: { disabled: true },
    });
    if (banError) {
      return json(
        { ok: false, error: "employee deactivated but sign-in could not be revoked" },
        500,
      );
    }

    return json({ ok: true });
  }

  return json({ ok: false, error: "unknown action" }, 400);
});
