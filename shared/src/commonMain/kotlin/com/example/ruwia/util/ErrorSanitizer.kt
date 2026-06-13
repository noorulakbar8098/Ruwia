package com.example.ruwia.util

/**
 * Turns a raw backend/SDK exception message into something safe and readable to
 * show a user.
 *
 * Supabase/Ktor exception messages embed the full HTTP request dump — including
 * `Authorization: Bearer <JWT>` and `apikey=<key>` headers. Showing that raw
 * message on screen leaks secrets (the user's session token, or the service-role
 * key). ALWAYS run backend error text through this before displaying or storing
 * it in UI state.
 */
fun sanitizeError(raw: String?): String {
    if (raw == null) return "Something went wrong. Please try again."

    var msg: String = raw
    // Cut everything from the first technical marker onward (URL / Headers dump).
    listOf("URL:", "Headers:", "Authorization=", "apikey=", "Http Method:").forEach { marker ->
        val idx = msg.indexOf(marker)
        if (idx >= 0) msg = msg.substring(0, idx)
    }
    // Defensively redact any bearer token / key that slipped through.
    msg = Regex("Bearer\\s+[A-Za-z0-9._\\-]+").replace(msg, "Bearer ***")
    msg = msg.trim().trim('(', '|', ' ', '\n', '\t', '-')

    // Friendlier, actionable copy for the most common backend errors.
    return when {
        msg.contains("schema cache", ignoreCase = true) ||
            msg.contains("find the table", ignoreCase = true) ->
            "The database isn't set up yet. Run supabase_schema.sql in your " +
                "Supabase project's SQL Editor, then try again."

        msg.contains("Invalid API key", ignoreCase = true) ||
            msg.contains("Unauthorized", ignoreCase = true) ->
            "Server rejected the request key. The Supabase service-role key is " +
                "missing or invalid — set SUPABASE_SERVICE_KEY in SupabaseClient.kt."

        msg.contains("row-level security", ignoreCase = true) ||
            msg.contains("violates row-level", ignoreCase = true) ->
            "You don't have permission to do this. Your account may not be an admin."

        msg.isBlank() -> "Couldn't reach the server. Please try again."
        else -> msg
    }
}
