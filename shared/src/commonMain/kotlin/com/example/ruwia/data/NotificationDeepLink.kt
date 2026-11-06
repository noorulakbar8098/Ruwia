package com.example.ruwia.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Carries the notification id from a push tap into shared UI.
 *
 * Android puts FCM `data` keys into the launch intent's extras. [MainActivity]
 * reads the id from there (and from our own foreground tap intent) and calls
 * [open]. The role dashboard observes [pendingNotificationId], opens the
 * notification center, marks the item read, and calls [consume].
 */
object NotificationDeepLink {
    /** Extra key used by FCM data payloads and by our own tap PendingIntent. */
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    private val _pendingNotificationId = MutableStateFlow<String?>(null)
    val pendingNotificationId: StateFlow<String?> = _pendingNotificationId.asStateFlow()

    fun open(notificationId: String?) {
        if (!notificationId.isNullOrBlank()) {
            _pendingNotificationId.value = notificationId
        }
    }

    fun consume() {
        _pendingNotificationId.value = null
    }
}
