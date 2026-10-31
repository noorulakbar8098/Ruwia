package com.example.ruwia.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JsDeliveryTrackingManager : DeliveryTrackingManager {
    private val _isTracking = MutableStateFlow(false)
    private val _activeTaskId = MutableStateFlow<String?>(null)

    override val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    override val activeTaskId: StateFlow<String?> = _activeTaskId.asStateFlow()

    override fun startTracking(taskId: String, employeeId: String, destLat: Double?, destLon: Double?, destName: String?) {
        _isTracking.value = true
        _activeTaskId.value = taskId
    }

    override fun stopTracking() {
        _isTracking.value = false
        _activeTaskId.value = null
    }
}

actual fun getDeliveryTrackingManager(): DeliveryTrackingManager = JsDeliveryTrackingManager()
