# Ruwia Push Notifications — Setup Guide

Two layers ship in this repo:

| Layer | What the user sees | Needs |
|---|---|---|
| **A. In-app feed** (done in code) | Bell badges + Notifications screen in both apps | Run `supabase_migration_notifications.sql` once |
| **B. System push** (needs the steps below) | Banner outside the app, even closed | Firebase project + `google-services.json` + deploy `send-push` + DB webhook |

---

## A. In-app feed (5 minutes)

1. Open Supabase Dashboard → SQL Editor.
2. Paste the contents of `supabase_migration_notifications.sql` → Run. Re-run it for existing installations to enable multiple devices per account.
3. Paste the contents of `supabase_migration_notification_security.sql` → Run. This replaces the old tenant-wide policies with recipient-scoped `notifications` policies, self-only `device_tokens` policies, and the `upsert_device_token` RPC used by the app. Re-running is safe.
3. Events wired in the app:
   - employee records a sale, stock change, customer, or delivery update → admin is notified
   - admin adds/updates/removes a customer or changes stock → employees are notified
   - admin assigns or updates a delivery task → that employee is notified
4. Open the app → bell icon → Notifications screen. Tap a row to mark it read.

## B. System push via FCM (Android)

### 1. Firebase project (you, in the Firebase console)
1. [console.firebase.google.com](https://console.firebase.google.com) → Add project (any name, e.g. `ruwia`).
2. Add app → Android → package name **`com.example.ruwia`** → Download **`google-services.json`**.
3. Copy it to **`androidApp/google-services.json`** (repo root file, git-ignored — never commit it).
4. Rebuild the APK. Without that file the app builds and runs normally, just without outside-the-app push.

### 2. Service account (you, one time)
> The service account **must** come from the same Firebase project as
> `androidApp/google-services.json`. Check `project_id` in that file; a key
> from another project fails with `403 PERMISSION_DENIED — SenderId mismatch`.

1. Firebase console → open the project whose id matches `google-services.json` → Project settings → Service accounts → **Generate new private key** → saves a JSON file.
2. Supabase Dashboard → Edge Functions → Secrets → add:
   - `FCM_PROJECT_ID` = that same project id (e.g. `neerthuli-c8e59`)
   - `FCM_SERVICE_ACCOUNT` = the **entire JSON** from step 1 (single line)
   - `PUSH_WEBHOOK_SECRET` = a long random value (`openssl rand -hex 32`)

### 3. Deploy the sender (terminal, needs Supabase CLI + a personal access token)
```bash
supabase login
supabase link --project-ref <your-project-ref>
supabase secrets set \
  FCM_PROJECT_ID=<project-id> \
  FCM_SERVICE_ACCOUNT="$(cat /path/to/service-account.json)" \
  PUSH_WEBHOOK_SECRET="$(openssl rand -hex 32)"
supabase functions deploy send-push --no-verify-jwt
```

`--no-verify-jwt` is deliberate: the app's public key also passes the
gateway's JWT check, so the function authenticates the webhook with
`PUSH_WEBHOOK_SECRET` instead.

### 4. Fire it on every notification (you, in the Supabase Dashboard)
1. Database → Webhooks → Create webhook:
   - Table: `notifications`, Events: **Insert**
   - Type: **Supabase Edge Functions**, Function: `send-push`
   - HTTP header: `x-webhook-secret: <the same random value>`
2. Save. From now on, every notification row (sale, stock update) fans out to the recipients' devices automatically.

### 5. Test
1. Install the release APK on two phones; log one in as **admin**, one as **employee**.
2. Accept the notification permission prompt (Android 13+).
3. Confirm each device registered after login:
   ```sql
   select user_id, platform, updated_at from device_tokens order by updated_at desc;
   ```
   A signed-in account with no row here will never receive a push — reinstall
   the current build and sign in again.
4. Employee records a sale → admin phone buzzes **with the app closed**.
5. Admin adds stock → employee phone buzzes.

### Troubleshooting
| Symptom | Cause | Fix |
|---|---|---|
| `401 UNAUTHORIZED_NO_AUTH_HEADER` in `net._http_response` | Webhook has no `x-webhook-secret` header | Recreate the webhook with the header (step 4). |
| `Missing FCM_PROJECT_ID or FCM_SERVICE_ACCOUNT secret` | Secrets not set on the function | Add both secrets (step 2). |
| `403 PERMISSION_DENIED — SenderId mismatch` | Service account belongs to a different Firebase project than the app | Generate the key from the project in `google-services.json`. |
| Notification row exists but no push | Recipient has no `device_tokens` row | Sign in on that device with the current build and accept notifications. |
| Background push appears under **Miscellaneous** | The app's channel metadata was missing in an older build | Install the current build; the `ruwia_updates` channel is created at startup and set as the FCM default in `AndroidManifest.xml`. |
| `upsert_device_token` not found | `supabase_migration_notification_security.sql` has not been run | Run it in the SQL Editor. |

### How it fits together
`RuwiaMessagingService` (system tray) ← FCM ← `send-push` edge function ← `notifications` table ← app writes. Device tokens register after login and session restoration, refresh on FCM rotation, and are removed on logout. The migrations support multiple devices per account and restrict each user to their own tokens and only the notifications addressed to them. Tapping a banner opens the app, deep-links to that item in the bell center, and marks it read. When FCM reports a token as `UNREGISTERED` or belonging to another project, `send-push` deletes it so dead devices are not retried.

### iOS note
iOS needs APNs certificates plus a native delegate — not wired in this change. The in-app feed works on iOS as-is.
