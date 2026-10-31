package com.example.ruwia.data

/**
 * Fire-and-forget refresh of this device's push token.
 *
 * Android: fetches the FCM token and registers it in `device_tokens` so the
 * `send-push` edge function can reach this device outside the app. Silently
 * no-ops when Firebase isn't configured (no `google-services.json`) or no
 * user is signed in. Other targets: no-op.
 *
 * Call after login/signup (session user exists) and at app start (restored
 * session) — covered by the Android Application class and AuthViewModel.
 */
expect fun refreshPushToken(token: String?)

fun refreshPushToken() {
    refreshPushToken(null)
}

/** Removes this device's registration before the current account signs out. */
expect suspend fun unregisterPushToken()
