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
//
//  OBFUSCATED KEY STORAGE — the URL and keys below are NOT plain
//  literals: each is split into chunks and XOR-masked, decoded only in
//  memory when the clients initialise. This defeats casual extraction
//  (grep / strings on the binary) but is NOT true security — anything
//  shipped inside an app can be recovered by a determined attacker.
//  Rules that still apply:
//   • Never post these values anywhere (the old service secret posted in
//     chat must stay rolled/revoked).
//   • To rotate a key, replace its three PARTS arrays (ask for re-encoded
//     chunks — never paste a live secret into a plain const).
//   • For production → move employee creation to a Supabase Edge Function
//     and remove the service-role key from the client entirely.
// ─────────────────────────────────────────────────────────────

// Mask shared by all three blobs below.
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
    intArrayOf(159, 173, 46, 237, 163, 105, 117, 228, 156, 180, 108, 118, 146, 78, 5, 252),
    intArrayOf(223, 160, 52, 211, 191, 96, 113, 201, 151, 179, 111, 67, 166, 30, 62, 209),
    intArrayOf(169, 133, 30, 208, 161, 84, 95, 192, 141, 168, 101, 35, 189, 101),
)

private val SERVICE_PARTS = arrayOf(
    intArrayOf(159, 173, 46, 238, 179, 104, 107, 232, 155, 131, 74, 114, 177, 77),
    intArrayOf(21, 224, 193, 158, 6, 235, 137, 102, 44, 192, 218, 146, 98, 122),
    intArrayOf(169, 79, 55, 227, 179, 250, 50, 203, 188, 111, 91, 230, 142),
)

private val SUPABASE_URL: String by lazy { unmask(URL_PARTS) }
private val SUPABASE_ANON_KEY: String by lazy { unmask(ANON_PARTS) }
private val SUPABASE_SERVICE_KEY: String by lazy { unmask(SERVICE_PARTS) }

/** Placeholder values that must never reach the server. */
private val SERVICE_KEY_SENTINELS = setOf(
    "YOUR_SERVICE_ROLE_KEY_HERE",
    "REVOKED_SENTINEL_DO_NOT_USE",
)

/** True when the configured key looks like a real service-role secret. */
internal fun isServiceKeyConfigured(): Boolean {
    val key = currentServiceKey().trim()
    return key.isNotBlank() && key !in SERVICE_KEY_SENTINELS && key.length >= 40
}

/** Actionable setup message shown whenever the service key is unusable. */
internal fun serviceKeySetupMessage(): String =
    "Server rejected the service key, so this action cannot continue. " +
        "On Android the key comes from local.properties " +
        "(supabase.servicekey.prod for the prod variant); on other platforms " +
        "it is SUPABASE_SERVICE_KEY in SupabaseClient.kt. Set it to your " +
        "project's full service_role secret (Supabase Dashboard → Project " +
        "Settings → API → service_role), then rebuild the app. " +
        "Installed key: ${serviceKeyFingerprint()}."

/**
 * Fingerprint of the key actually handed to the network clients.
 * The Android Application reports its BuildConfig value here at startup;
 * other platforms fall back to the bundled key below.
 */
private var effectiveServiceKeyNote: String? = null

/** Full service-role key actually in use (Android BuildConfig wins when set). */
private var effectiveServiceKey: String? = null

/** Returns the service key that [supabaseAdmin] was built with, or the bundled fallback. */
internal fun currentServiceKey(): String =
    effectiveServiceKey ?: SUPABASE_SERVICE_KEY.trim()

fun reportEffectiveServiceKey(length: Int, last4: String) {
    effectiveServiceKeyNote = "$length chars ending …$last4"
}

/**
 * Non-sensitive fingerprint of the installed key (length + last 4 chars —
 * insufficient to reconstruct it) so a mismatch between the bundled key
 * and the dashboard key is visible right in the error message.
 */
internal fun serviceKeyFingerprint(): String {
    effectiveServiceKeyNote?.let { return it }
    val key = currentServiceKey().trim()
    if (key.isEmpty()) return "missing"
    return "${key.length} chars ending …${key.takeLast(4)}"
}

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
    if (_supabase != null) {
        // First init wins (on Android this is RuwiaApp with BuildConfig).
        // Still remember the key if nothing was recorded yet.
        if (effectiveServiceKey == null) effectiveServiceKey = serviceKey.trim()
        return
    }
    effectiveServiceKey = serviceKey.trim()
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
    check(isServiceKeyConfigured()) { serviceKeySetupMessage() }
    supabaseAdmin.auth.importAuthToken(currentServiceKey())
}

/**
 * True when a backend failure looks like a rejected service-role key
 * (as opposed to a business error such as a duplicate email, which must
 * keep its original message).
 */
internal fun isServiceKeyRejection(raw: String?): Boolean {
    if (raw == null) return false
    val msg = raw.lowercase()
    if ("already registered" in msg || "already exists" in msg || "duplicate" in msg) return false
    return "invalid api key" in msg || "invalid jwt" in msg ||
        "unauthorized" in msg || "forbidden" in msg ||
        "not allowed" in msg || "must be admin" in msg ||
        "service_role" in msg || "service role" in msg ||
        "apikey" in msg || " 401" in msg || " 403" in msg ||
        "admin api" in msg
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
