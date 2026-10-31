package com.example.ruwia.ui

import com.example.ruwia.domain.DeliveryTask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeliveryTaskTest {

    @Test
    fun taskClosureOnlyCoversCompletedAndCancelledStatuses() {
        assertTrue(DeliveryTask(status = "completed").isTaskClosed())
        assertTrue(DeliveryTask(status = "DONE").isTaskClosed())
        assertTrue(DeliveryTask(status = "cancelled").isTaskClosed())
        assertFalse(DeliveryTask(status = "pending").isTaskClosed())
        assertFalse(DeliveryTask(status = "").isTaskClosed())
    }

    @Test
    fun nextScheduledTaskIgnoresClosedAndSortsByDateThenTime() {
        val tasks = listOf(
            DeliveryTask(id = "tomorrow-late", scheduledDate = "2026-10-29", etaText = "9:00 AM"),
            DeliveryTask(id = "tomorrow-early", scheduledDate = "2026-10-29", etaText = "8:30 AM"),
            DeliveryTask(id = "today-night", scheduledDate = "2026-10-28", etaText = "11:00 PM"),
            DeliveryTask(id = "already-done", scheduledDate = "2026-10-27", status = "completed"),
            DeliveryTask(id = "cancelled", scheduledDate = "2026-10-27", status = "cancelled"),
        )

        assertEquals("today-night", tasks.nextScheduledTask()?.id)
    }

    @Test
    fun nextScheduledTaskIsNullWhenNothingIsOpen() {
        val tasks = listOf(
            DeliveryTask(id = "done", status = "completed"),
        )

        assertNull(tasks.nextScheduledTask())
    }

    @Test
    fun scheduleLabelCombinesDisplayDateAndStoredTime() {
        val task = DeliveryTask(scheduledDate = "2026-10-28", etaText = "3:05 PM")

        assertEquals("28/10/2026 · 3:05 PM", deliveryTaskScheduleLabel(task))
    }

    @Test
    fun scheduleLabelFallsBackToDeliveryDateAndHandlesMissingTime() {
        val task = DeliveryTask(deliveryDate = "2026-11-02T00:00:00Z")

        assertEquals("02/11/2026", deliveryTaskScheduleLabel(task))
    }

    @Test
    fun productLabelReadsQuantityAndProductName() {
        val task = DeliveryTask(canQty = 2, productName = "20L Bottle")

        assertEquals("2 Cases · 20L Bottle", deliveryTaskProductLabel(task))
        assertEquals("1 Case", deliveryTaskProductLabel(DeliveryTask(canQty = 1)))
    }

    @Test
    fun deliveryTaskSupportsTimelinePoints() {
        val points = listOf(
            com.example.ruwia.domain.TaskTimelinePoint(
                label = "Start Point",
                latitude = 11.0168,
                longitude = 76.9558,
                placeName = "RS Puram, Coimbatore",
                recordedAt = "10:15 AM",
                status = "accepted",
            ),
            com.example.ruwia.domain.TaskTimelinePoint(
                label = "Second Point",
                latitude = 11.0250,
                longitude = 76.9610,
                placeName = "Mettupalayam Road",
                recordedAt = "10:25 AM",
                status = "in_transit",
            ),
        )
        val task = DeliveryTask(id = "task-1", status = "in_transit", timeline = points)
        assertEquals(2, task.timeline.size)
        assertEquals("RS Puram, Coimbatore", task.timeline.first().placeName)
        assertEquals("Second Point", task.timeline[1].label)
    }

    @Test
    fun deliveryTaskStoresDestinationAndLiveTrackingDetails() {
        val task = DeliveryTask(
            id = "task-dest",
            destName = "Green City Grocers",
            destLatitude = 12.9716,
            destLongitude = 77.5946,
            mapsLink = "https://maps.google.com/?q=12.9716,77.5946",
            currentLatitude = 12.9500,
            currentLongitude = 77.5800,
            currentLocationName = "Lalbagh West Gate",
            journeyStartTime = "10:00 AM",
            lastLocationUpdateTime = "10:18 AM",
            distanceRemainingMeters = 2400.0,
            liveEta = "~10 mins (10:28 AM)",
        )

        assertEquals("Green City Grocers", task.destName)
        assertEquals(12.9716, task.destLatitude)
        assertEquals(77.5946, task.destLongitude)
        assertEquals(2400.0, task.distanceRemainingMeters)
        assertEquals("~10 mins (10:28 AM)", task.liveEta)
    }
}
