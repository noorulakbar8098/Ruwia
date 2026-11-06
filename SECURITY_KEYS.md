# Key & Signing Security

This file is the checklist for creating, rotating, and storing every credential
used by Ruwia. It intentionally contains **no secret values**.

## 1. Key inventory

| Credential | Sensitivity | Where it lives | Where it is used |
| --- | --- | --- | --- |
| Supabase project URL | Public | `shared/src/commonMain/kotlin/com/example/ruwia/data/SupabaseClient.kt` (`URL_PARTS`) | Android app |
| Supabase publishable key (`sb_publishable_…`) | Public (RLS protects data) | `SupabaseClient.kt` (`ANON_PARTS`), gitignored `local.properties` | Android app |
| Supabase secret key (`sb_secret_…`) | **Server-only** | Supabase Edge Function secret `RUWIA_SERVICE_KEY` | `admin-users`, `send-push` |
| Legacy `SUPABASE_SERVICE_ROLE_KEY` | **Server-only** | Injected automatically by Supabase | Fallback only in Edge Functions |
| FCM service account JSON | **Server-only** | Function secret `FCM_SERVICE_ACCOUNT` | `send-push` |
| FCM project id | Server config | Function secret `FCM_PROJECT_ID` | `send-push` |
| Webhook shared secret | **Server-only** | Function secret `PUSH_WEBHOOK_SECRET` | `send-push` |
| Release signing keystore | **Secret / irreplaceable** | `androidApp/ruwia-release-2026.jks` (gitignored) | `androidApp/build.gradle.kts` |
| Keystore passwords / alias | **Secret** | Gitignored `local.properties` or `RUWIA_*` environment variables | Gradle signing config |
| Firebase `google-services.json` | Contains a public API key | `androidApp/google-services.json` (gitignored) | Android build |

**Rule:** anything shipped in an APK is public. Encryption or XOR masking inside
the app only slows down extraction. The only real protection is to keep a secret
on a server and enforce authorization there. The publishable key is safe to ship
because every table is protected by row-level security (RLS).

## 2. Files that must exist and stay correct

Client:
- `shared/src/commonMain/kotlin/com/example/ruwia/data/SupabaseClient.kt`
  — URL and publishable key; installs `Auth`, `Postgrest`, `Realtime`, `Functions`.
- `shared/src/commonMain/kotlin/com/example/ruwia/data/AdminFunctions.kt`
  — calls the privileged `admin-users` function with the signed-in user's JWT.
- `androidApp/build.gradle.kts`
  — reads signing values from `RUWIA_*` environment variables, then
    `local.properties`; contains no Supabase secret and no `BuildConfig` keys.
- `local.properties` — **never commit**. Holds `signing.*` and the publishable
  key used by local builds.
- `androidApp/google-services.json` — **never commit**. Replace it when the
  Firebase package name changes.

Server:
- `supabase/functions/admin-users/index.ts` — verifies caller JWT + admin role,
  then creates/deactivates employees. Reads `RUWIA_SERVICE_KEY`.
- `supabase/functions/send-push/index.ts` — verifies `PUSH_WEBHOOK_SECRET`,
  then sends FCM pushes. Reads `RUWIA_SERVICE_KEY`.
- `supabase_migration_key_hardening.sql` — signup trigger ignores client
  `role`/`admin_id`; clients cannot change `profiles.role` or `profiles.admin_id`.
- `supabase_migration_notification_security.sql` — recipient-scoped RLS for
  notifications and device tokens.
- `supabase_migration_notifications.sql` / `supabase_schema.sql` — schema + RLS.

## 3. Creating / rotating keys

### Supabase secret key (server)

1. Supabase Dashboard → Project Settings → API Keys → create a **secret** key.
   Name it for the environment and date, e.g. `ruwia_server_2026_11`.
2. Store it only as a function secret:
   ```sh
   supabase secrets set --env-file ./functions.env --project-ref <ref>
   ```
   where `functions.env` contains `RUWIA_SERVICE_KEY=sb_secret_…` and is deleted
   immediately afterwards.
3. Redeploy both functions:
   ```sh
   supabase functions deploy admin-users --project-ref <ref>
   supabase functions deploy send-push --no-verify-jwt --project-ref <ref>
   ```
4. Verify the key works against PostgREST with `apikey` + `Authorization`.
5. Delete the previous secret key in the dashboard/API only after the new key is
   confirmed working.

### Supabase publishable key (client)

1. Create a new **publishable** key in the dashboard.
2. Re-encode it into `ANON_PARTS` in `SupabaseClient.kt` using the existing
   `KEY_MASK` (XOR each character, split into chunks).
3. Update the gitignored `local.properties` value.
4. Rebuild and publish the APK.
5. Keep the old publishable key active until existing installs have migrated,
   then revoke it.

### Release signing keystore

The keystore is the app's permanent identity. **Back it up offline** (password
manager + encrypted backup). Losing it means you can never update the installed
app; changing it forces every user to uninstall and reinstall.

```sh
keytool -genkeypair -v \
  -keystore androidApp/ruwia-release-<year>.jks \
  -storetype JKS -alias ruwia -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Ruwia, OU=Mobile, O=Neer Thuli, C=IN"
```

Then set these in gitignored `local.properties` (or CI secrets):
```
signing.storeFile=ruwia-release-<year>.jks
signing.storePassword=…
signing.keyAlias=ruwia
signing.keyPassword=…
```

CI/environment equivalents: `RUWIA_STORE_FILE`, `RUWIA_STORE_PASSWORD`,
`RUWIA_KEY_ALIAS`, `RUWIA_KEY_PASSWORD`.

Certificate SHA-256 (public fingerprint) is printed by:
```sh
apksigner verify --print-certs androidApp/build/outputs/apk/release/androidApp-release.apk
```

## 4. Never commit

- `local.properties`, `*.jks`, `*.keystore`, `*.p12`, `.env*`
- `androidApp/google-services.json`
- Supabase secret keys, FCM service-account JSON, webhook secrets
- APKs/AABs or website build output (`website/.next`, `website/out`)

`.gitignore` enforces these paths. If a secret is ever committed, rotate it
immediately — removing the file from the latest commit is not enough because it
remains in Git history.

## 5. Mandatory release checks

```sh
./gradlew :shared:jvmTest :androidApp:assembleRelease
unzip -p androidApp/build/outputs/apk/release/androidApp-release.apk 'classes*.dex' \
  | strings | grep -c sb_secret        # must be 0
apksigner verify --print-certs androidApp/build/outputs/apk/release/androidApp-release.apk
shasum -a 256 androidApp/build/outputs/apk/release/androidApp-release.apk
```

Only publish the APK after all four checks pass. Update the website release
version, checksum, size, and download URL in
`website/src/config/app.config.ts` at the same time.
