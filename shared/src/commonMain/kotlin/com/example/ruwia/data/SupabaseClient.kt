package com.example.ruwia.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

// ─────────────────────────────────────────────────────────────
//  Supabase clients
//  Values: Supabase Dashboard → Project Settings → API
// ─────────────────────────────────────────────────────────────

private const val SUPABASE_URL      = "https://srvertclbjcvlfdectsl.supabase.co"
private const val SUPABASE_ANON_KEY = "sb_publishable_N3oENikhDxobWX5dcEJoMw_FMbth7CN"

// ⚠️  SERVICE ROLE KEY — bypasses Row Level Security.
//     Get it from: Supabase Dashboard → Project Settings → API → service_role (secret).
//     It looks like: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoic2VydmljZV9yb2xlIi...
//     For production → move employee creation to a Supabase Edge Function and
//     remove this key from the client entirely.
internal const val SUPABASE_SERVICE_KEY = "YOUR_SERVICE_ROLE_KEY_HERE"  // ← replace this

/** Regular client — anon key, used for all user-level operations. */
val supabase: SupabaseClient = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_ANON_KEY,
) {
    install(Auth) {
        autoLoadFromStorage = true
        autoSaveToStorage   = true
    }
    install(Postgrest)
    install(Realtime)
}

/**
 * Admin client — service-role key.
 * Used ONLY for admin-initiated operations (e.g. creating employee accounts).
 * All reads/writes bypass Row Level Security.
 */
val supabaseAdmin: SupabaseClient = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_SERVICE_KEY,
) {
    install(Auth)
    install(Postgrest)
}

/**
 * Must be called before any [supabaseAdmin] admin-API call.
 *
 * GoTrue's admin endpoints require `Authorization: Bearer <service-role-jwt>`.
 * Initialising the client with the service key only sets the `apikey` header;
 * [importAuthToken] also loads it as the session's access token so the Bearer
 * header is populated on every subsequent request.
 */
suspend fun initAdminSession() {
    supabaseAdmin.auth.importAuthToken(SUPABASE_SERVICE_KEY)
}
