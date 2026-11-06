package com.example.ruwia.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter

// ─────────────────────────────────────────────────────────────
//  Supabase clients
//  Values: Supabase Dashboard → Project Settings → API
//
//  SECURITY MODEL
//  • The publishable/anon key is public by design. It is safe to ship:
//    every table is protected by row-level security (RLS).
//  • The service-role key is a server-only secret. It must NEVER be added
//    to this file, BuildConfig, local.properties for the app, or an APK.
//    Privileged operations live in the `admin-users` Edge Function, where
//    Supabase injects the key from its encrypted function environment.
//  • The URL and publishable key below are only masked to limit accidental
//    copy/paste exposure. Obfuscation is not a substitute for RLS.
// ─────────────────────────────────────────────────────────────

// Mask shared by both blobs below.
private val KEY_MASK = intArrayOf(
    236, 207, 113, 157, 214, 11, 25, 141,
    239, 220, 13, 20, 254, 43, 90, 178,
)

private fun unmask(parts: Array<IntArray>): String {
    val flat = parts.fold(mutableListOf<Int>()) { acc, chunk -> acc.apply { addAll(chunk.asIterable()) } }
    val chars = CharArray(flat.size) { i -> (flat[i] xor KEY_MASK[i % KEY_MASK.size]).toChar() }
    val result = chars.concatToString()
    chars.fill('\u0000')
    return result
}

private val URL_PARTS = arrayOf(
    intArrayOf(132, 187, 5, 237, 165, 49, 54, 162, 156, 174, 123, 113, 140, 95),
    intArrayOf(57, 222, 142, 165, 18, 235, 186, 109, 125, 232, 140, 168, 126, 120),
    intArrayOf(208, 88, 47, 194, 141, 173, 16, 238, 179, 37, 122, 226),
)

private val ANON_PARTS = arrayOf(
    intArrayOf(159, 173, 46, 237, 163, 105, 117, 228, 156, 180, 108, 118, 146, 78, 5, 198),
    intArrayOf(168, 249, 67, 169, 144, 71, 111, 196, 140, 234, 60, 112, 206, 124, 29, 216),
    intArrayOf(181, 255, 22, 244, 177, 84, 91, 254, 164, 159, 71, 44, 204, 120),
)

private val SUPABASE_URL: String by lazy { unmask(URL_PARTS) }
private val SUPABASE_ANON_KEY: String by lazy { unmask(ANON_PARTS) }

private var _supabase: SupabaseClient? = null

/** Authenticated app client. Every request carries the signed-in user's JWT. */
val supabase: SupabaseClient
    get() {
        if (_supabase == null) {
            _supabase = createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_ANON_KEY,
            ) {
                install(Auth) {
                    autoLoadFromStorage = true
                    autoSaveToStorage   = true
                }
                install(Postgrest)
                install(Realtime)
                install(Functions)
            }
        }
        return _supabase!!
    }

/**
 * True when a PostgREST failure means the named column does not exist yet
 * (e.g. "Could not find the 'client_key' column … in the schema cache").
 * Write paths use this to fall back to legacy payloads so the app keeps
 * working on databases where the latest supabase_schema.sql migration has
 * not been run yet.
 */
internal fun isUnknownColumnError(e: Exception, column: String): Boolean {
    val msg = e.message ?: return false
    return msg.contains(column, ignoreCase = true) &&
        (msg.contains("column", ignoreCase = true) || msg.contains("does not exist", ignoreCase = true))
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
