package com.example.ruwia.data

import kotlinx.coroutines.flow.StateFlow

interface DeliveryTrackingManager {
    val isTracking: StateFlow<Boolean>
    val activeTaskId: StateFlow<String?>
    fun startTracking(
        taskId: String,
        employeeId: String,
        destLat: Double?,
        destLon: Double?,
        destName: String?,
    )
    fun stopTracking()
}

expect fun getDeliveryTrackingManager(): DeliveryTrackingManager
