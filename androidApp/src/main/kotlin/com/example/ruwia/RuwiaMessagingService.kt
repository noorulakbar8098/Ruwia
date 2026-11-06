package com.example.ruwia

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.example.ruwia.data.NotificationDeepLink
import com.example.ruwia.data.refreshPushToken
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM pushes while the app is closed or backgrounded and posts them
 * as system notifications. Tapping opens the app and deep-links to the
 * in-app notification center item.
 *
 * Token rotation is handled by re-running the shared refresh (same row,
 * keyed by user — last writer wins).
 */
class RuwiaMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        refreshPushToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]?.takeIf { it.isNotBlank() }
            ?: "Ruwia"
        val body = message.notification?.body
            ?: message.data["body"].orEmpty()
        showNotification(
            title = title,
            body = body,
            notificationId = message.data[NotificationDeepLink.EXTRA_NOTIFICATION_ID],
        )
    }

    private fun showNotification(title: String, body: String, notificationId: String?) {
        createRuwiaNotificationChannels(this)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // A distinct request code per notification keeps each tap intent's
        // extras intact when several pushes are on screen at once.
        val requestCode = notificationId?.hashCode() ?: System.currentTimeMillis().toInt()
        val openApp = PendingIntent.getActivity(
            this,
            requestCode,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(NotificationDeepLink.EXTRA_NOTIFICATION_ID, notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, RUWIA_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_neerthuli)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.app_icon))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        manager.notify(requestCode, notification)
    }
}
