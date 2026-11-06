package com.example.ruwia

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Channel used for every shop update push (foreground and background). */
const val RUWIA_UPDATES_CHANNEL_ID = "ruwia_updates"

/**
 * Registers the app's notification channels.
 *
 * Called from [RuwiaApp.onCreate] so the channel exists before FCM displays a
 * background push, and again from the messaging service for safety. Creating
 * an existing channel is a no-op, so repeated calls are safe.
 */
fun createRuwiaNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return
    val channel = NotificationChannel(
        RUWIA_UPDATES_CHANNEL_ID,
        "Shop updates",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Sales and stock updates from your shop"
    }
    manager.createNotificationChannel(channel)
}
