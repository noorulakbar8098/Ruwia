package com.example.ruwia.data

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun refreshPushToken(token: String?) {
    try {
        if (!token.isNullOrBlank()) {
            registerToken(token)
        } else {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener
                val refreshedToken = task.result ?: return@addOnCompleteListener
                registerToken(refreshedToken)
            }
        }
    } catch (_: Exception) {
        // Firebase not configured on this build (no google-services.json):
        // outside-the-app push simply stays off.
    }
}

actual suspend fun unregisterPushToken() {
    val token = try {
        suspendCancellableCoroutine<String?> { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (continuation.isActive) {
                    val tokenResult: String? = if (task.isSuccessful) task.result else null
                    continuation.resume(tokenResult)
                }
            }
        }
    } catch (_: Exception) {
        null
    }
    if (!token.isNullOrBlank()) {
        NotificationRepository().removeDeviceToken(token)
    }
}

private fun registerToken(token: String) {
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        runCatching { NotificationRepository().upsertDeviceToken(token) }
    }
}
