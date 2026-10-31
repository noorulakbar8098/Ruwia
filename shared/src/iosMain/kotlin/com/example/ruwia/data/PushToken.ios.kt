package com.example.ruwia.data

// System push on iOS needs APNs wiring (certificates + native delegate),
// which is not set up for this project yet. In-app notifications work as-is.
actual fun refreshPushToken(token: String?) {
}

actual suspend fun unregisterPushToken() {
}
