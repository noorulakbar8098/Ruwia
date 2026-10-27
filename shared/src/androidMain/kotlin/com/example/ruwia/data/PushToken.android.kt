package com.example.ruwia.data

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual fun refreshPushToken() {
    try {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result ?: return@addOnCompleteListener
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { NotificationRepository().upsertDeviceToken(token) }
            }
        }
    } catch (_: Exception) {
        // Firebase not configured on this build (no google-services.json):
        // outside-the-app push simply stays off.
    }
}
