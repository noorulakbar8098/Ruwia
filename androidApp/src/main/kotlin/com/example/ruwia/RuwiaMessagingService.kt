package com.example.ruwia

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ruwia.data.refreshPushToken
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM pushes while the app is closed or backgrounded and posts them
 * as system notifications. Tapping opens the app; the in-app notification
 * center (bell) holds the same items for when the app is open.
 *
 * Token rotation is handled by re-running the shared refresh (same row,
 * keyed by user — last writer wins).
 */
class RuwiaMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        refreshPushToken()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]?.takeIf { it.isNotBlank() }
            ?: "Ruwia"
        val body = message.notification?.body
            ?: message.data["body"].orEmpty()
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Shop updates",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Sales and stock updates from your shop" }
            )
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "ruwia_updates"
    }
}
