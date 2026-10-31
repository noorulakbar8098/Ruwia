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
1. Firebase console → Project settings → Service accounts → **Generate new private key** → saves a JSON file.
2. Supabase Dashboard → Edge Functions → Secrets → add:
   - `FCM_PROJECT_ID` = your Firebase project id
   - `FCM_SERVICE_ACCOUNT` = the **entire JSON** from step 1 (single line)

### 3. Deploy the sender (terminal, needs Supabase CLI + a personal access token)
```bash
supabase login
supabase link --project-ref <your-project-ref>
supabase functions deploy send-push
```

### 4. Fire it on every notification (you, in the Supabase Dashboard)
1. Database → Webhooks → Create webhook:
   - Table: `notifications`, Events: **Insert**
   - Type: **Supabase Edge Functions**, Function: `send-push`
2. Save. From now on, every notification row (sale, stock update) fans out to the recipients' devices automatically.

### 5. Test
1. Install the release APK on two phones; log one in as **admin**, one as **employee**.
2. Accept the notification permission prompt (Android 13+).
3. Employee records a sale → admin phone buzzes **with the app closed**.
4. Admin adds stock → employee phone buzzes.

### How it fits together
`RuwiaMessagingService` (system tray) ← FCM ← `send-push` edge function ← `notifications` table ← app writes. Device tokens register after login and session restoration, refresh on FCM rotation, and are removed on logout. The migration supports multiple devices per account. Tapping a banner opens the app, where the same item waits in the bell center.

### iOS note
iOS needs APNs certificates plus a native delegate — not wired in this change. The in-app feed works on iOS as-is.
