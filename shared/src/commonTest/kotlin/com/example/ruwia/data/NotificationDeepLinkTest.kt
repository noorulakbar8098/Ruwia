package com.example.ruwia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationDeepLinkTest {

    @BeforeTest
    fun reset() = NotificationDeepLink.consume()

    @AfterTest
    fun cleanup() = NotificationDeepLink.consume()

    @Test
    fun opensNonBlankNotificationId() {
        NotificationDeepLink.open("notification-1")

        assertEquals("notification-1", NotificationDeepLink.pendingNotificationId.value)
    }

    @Test
    fun ignoresBlankNotificationIds() {
        NotificationDeepLink.open(null)
        NotificationDeepLink.open("")
        NotificationDeepLink.open("   ")

        assertNull(NotificationDeepLink.pendingNotificationId.value)
    }

    @Test
    fun consumeClearsPendingId() {
        NotificationDeepLink.open("notification-1")

        NotificationDeepLink.consume()

        assertNull(NotificationDeepLink.pendingNotificationId.value)
    }
}
