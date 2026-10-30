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
}
