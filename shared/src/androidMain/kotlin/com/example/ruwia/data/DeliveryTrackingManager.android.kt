package com.example.ruwia.data

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.ruwia.AppContextHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidDeliveryTrackingManager : DeliveryTrackingManager {
    companion object {
        val _isTracking = MutableStateFlow(false)
        val _activeTaskId = MutableStateFlow<String?>(null)

        fun updateTrackingState(isTracking: Boolean, taskId: String?) {
            _isTracking.value = isTracking
            _activeTaskId.value = taskId
        }
    }

    override val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    override val activeTaskId: StateFlow<String?> = _activeTaskId.asStateFlow()

    override fun startTracking(
        taskId: String,
        employeeId: String,
        destLat: Double?,
        destLon: Double?,
        destName: String?,
    ) {
        val context = AppContextHolder.context ?: return
        _isTracking.value = true
        _activeTaskId.value = taskId

        val intent = Intent().apply {
            setClassName(context.packageName, "com.example.ruwia.service.DeliveryTrackingService")
            action = "ACTION_START_TRACKING"
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_EMPLOYEE_ID", employeeId)
            destLat?.let { putExtra("EXTRA_DEST_LAT", it) }
            destLon?.let { putExtra("EXTRA_DEST_LON", it) }
            destName?.let { putExtra("EXTRA_DEST_NAME", it) }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            println("Failed to start DeliveryTrackingService: ${e.message}")
        }
    }

    override fun stopTracking() {
        val context = AppContextHolder.context ?: return
        _isTracking.value = false
        _activeTaskId.value = null

        val intent = Intent().apply {
            setClassName(context.packageName, "com.example.ruwia.service.DeliveryTrackingService")
            action = "ACTION_STOP_TRACKING"
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {}
    }
}

private val defaultAndroidDeliveryTrackingManager = AndroidDeliveryTrackingManager()

actual fun getDeliveryTrackingManager(): DeliveryTrackingManager = defaultAndroidDeliveryTrackingManager
