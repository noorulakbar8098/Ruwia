package com.example.ruwia.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter

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
internal const val SUPABASE_SERVICE_KEY = "sb_secret_F8KyUwYsXLvzcdM_5TQqRg_a7IHDjEr"  // ← replace this

private var _supabase: SupabaseClient? = null
val supabase: SupabaseClient
    get() {
        if (_supabase == null) {
            initSupabaseClient(SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_KEY)
        }
        return _supabase!!
    }

private var _supabaseAdmin: SupabaseClient? = null
val supabaseAdmin: SupabaseClient
    get() {
        if (_supabaseAdmin == null) {
            initSupabaseClient(SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_KEY)
        }
        return _supabaseAdmin!!
    }

fun initSupabaseClient(url: String, anonKey: String, serviceKey: String) {
    if (_supabase != null) return
    _supabase = createSupabaseClient(
        supabaseUrl = url,
        supabaseKey = anonKey,
    ) {
        install(Auth) {
            autoLoadFromStorage = true
            autoSaveToStorage   = true
        }
        install(Postgrest)
        install(Realtime)
    }

    _supabaseAdmin = createSupabaseClient(
        supabaseUrl = url,
        supabaseKey = serviceKey,
    ) {
        install(Auth)
        install(Postgrest)
    }
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
    check(SUPABASE_SERVICE_KEY != "YOUR_SERVICE_ROLE_KEY_HERE" && SUPABASE_SERVICE_KEY.isNotBlank()) {
        "Supabase service-role key is not configured. Open SupabaseClient.kt and set " +
            "SUPABASE_SERVICE_KEY to your project's service_role key " +
            "(Supabase Dashboard → Project Settings → API → service_role)."
    }
    supabaseAdmin.auth.importAuthToken(SUPABASE_SERVICE_KEY)
}

/** Waits for Supabase Auth to resolve initialization and returns true if authenticated. */
suspend fun awaitAuthentication(): Boolean {
    val status = try {
        supabase.auth.sessionStatus
            .filter { it !is SessionStatus.Initializing }
            .first()
    } catch (_: Exception) {
        return false
    }

    if (status is SessionStatus.Authenticated) {
        return true
    }

    if (status is SessionStatus.RefreshFailure) {
        val success = runCatching { supabase.auth.refreshCurrentSession() }.isSuccess
        if (success) {
            val nextStatus = try {
                supabase.auth.sessionStatus
                    .filter { it !is SessionStatus.Initializing }
                    .first()
            } catch (_: Exception) {
                return false
            }
            return nextStatus is SessionStatus.Authenticated
        }
    }

    return false
}
