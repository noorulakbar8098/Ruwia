package com.example.ruwia.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.ruwia.MainActivity
import com.example.ruwia.R
import com.example.ruwia.data.AndroidDeliveryTrackingManager
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.data.GeoPoint
import com.example.ruwia.data.LocationService
import com.example.ruwia.domain.TaskTimelinePoint
import com.example.ruwia.data.getLocationService
import com.example.ruwia.data.supabase
import com.example.ruwia.util.LocationUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.github.jan.supabase.postgrest.from

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Clock

class DeliveryTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager
    private val locationService: LocationService by lazy { getLocationService() }

    private var currentTaskId: String? = null
    private var currentEmployeeId: String? = null
    private var destLatitude: Double? = null
    private var destLongitude: Double? = null
    private var destName: String? = null

    private var lastRecordedPoint: GeoPoint? = null
    private var lastRecordedTimeMs: Long = 0L
    private var consecutiveArrivalReadings = 0
    private var currentTimeline = mutableListOf<TaskTimelinePoint>()

    // Offline queue for updates if network is unavailable
    private val pendingUpdatesQueue = ConcurrentLinkedQueue<TaskUpdatePayload>()

    data class TaskUpdatePayload(
        val taskId: String,
        val status: String,
        val currentLat: Double,
        val currentLon: Double,
        val placeName: String,
        val updateTime: String,
        val distRemaining: Double?,
        val liveEta: String?,
        val arrivalTime: String? = null,
        val newPoint: TaskTimelinePoint? = null,
    )

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            serviceScope.launch {
                handleLocationUpdate(location)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_TRACKING) {
            stopTracking()
            return START_NOT_STICKY
        }

        if (action == ACTION_START_TRACKING) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID)
            val employeeId = intent.getStringExtra(EXTRA_EMPLOYEE_ID)
            if (taskId.isNullOrBlank() || employeeId.isNullOrBlank()) {
                stopSelf()
                return START_NOT_STICKY
            }

            currentTaskId = taskId
            currentEmployeeId = employeeId
            destLatitude = if (intent.hasExtra(EXTRA_DEST_LAT)) intent.getDoubleExtra(EXTRA_DEST_LAT, 0.0) else null
            destLongitude = if (intent.hasExtra(EXTRA_DEST_LON)) intent.getDoubleExtra(EXTRA_DEST_LON, 0.0) else null
            destName = intent.getStringExtra(EXTRA_DEST_NAME)

            startForegroundServiceWithNotification()
            startLocationUpdates()
            fetchInitialTaskState(taskId)
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Delivery Journey Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live progress and ETA for active delivery journeys"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildNotification(
            title = "Active Delivery: ${destName ?: "In Transit"}",
            content = "Recording route and calculating ETA in real-time"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        AndroidDeliveryTrackingManager.updateTrackingState(true, currentTaskId)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.app_icon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = buildNotification(title, content)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
                .setMinUpdateIntervalMillis(5000L)
                .setMinUpdateDistanceMeters(20f)
                .setWaitForAccurateLocation(true)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            println("Location permission not available for DeliveryTrackingService: ${e.message}")
            stopSelf()
        }
    }

    private fun fetchInitialTaskState(taskId: String) {
        serviceScope.launch {
            try {
                val task = supabase.from("route_tasks")
                    .select {
                        filter { eq("id", taskId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<DeliveryTask>()

                if (task != null) {
                    currentTimeline = task.timeline.toMutableList()
                    if (destLatitude == null && task.destLatitude != null) {
                        destLatitude = task.destLatitude
                        destLongitude = task.destLongitude
                        destName = task.destName ?: task.customerName
                    }
                    if (task.timeline.isNotEmpty()) {
                        val last = task.timeline.last()
                        if (last.latitude != null && last.longitude != null) {
                            lastRecordedPoint = GeoPoint(last.latitude!!, last.longitude!!)
                            lastRecordedTimeMs = System.currentTimeMillis()
                        }
                    }
                }
            } catch (e: Exception) {
                println("Could not fetch initial task state: ${e.message}")
            }
        }
    }

    private suspend fun handleLocationUpdate(location: Location) {
        val taskId = currentTaskId ?: return
        val accuracy = location.accuracy.toDouble()

        // 1. Filter out inaccurate readings (> 60m)
        if (accuracy > 60.0) {
            return
        }

        val lat = location.latitude
        val lon = location.longitude
        val speedMps = if (location.hasSpeed()) location.speed.toDouble() else null
        val nowMs = System.currentTimeMillis()
        val timeFormatted = currentFormattedTime()

        // 2. Reverse geocode place name
        val placeName = locationService.reverseGeocode(lat, lon)

        // 3. Compute distance & ETA to destination
        var distMeters: Double? = null
        var eta: String? = null
        val destLat = destLatitude
        val destLon = destLongitude

        if (destLat != null && destLon != null) {
            distMeters = LocationUtils.haversineDistanceMeters(lat, lon, destLat, destLon)
            eta = LocationUtils.calculateEta(distMeters, speedMps)
        }

        // 4. Geofence Arrival Check (300 meters)
        var isArrivalConfirmed = false
        if (distMeters != null && distMeters <= 300.0) {
            consecutiveArrivalReadings++
            if (consecutiveArrivalReadings >= 2) {
                isArrivalConfirmed = true
            }
        } else {
            consecutiveArrivalReadings = 0
        }

        // 5. Determine if a new checkpoint should be recorded (up to 10 points)
        var newCheckpoint: TaskTimelinePoint? = null
        val distFromLast = if (lastRecordedPoint != null) {
            LocationUtils.haversineDistanceMeters(lat, lon, lastRecordedPoint!!.latitude, lastRecordedPoint!!.longitude)
        } else {
            Double.MAX_VALUE
        }

        val elapsedSinceLastMs = nowMs - lastRecordedTimeMs

        val shouldRecord = when {
            currentTimeline.isEmpty() -> true // Start point
            isArrivalConfirmed -> true // Arrival point
            distFromLast >= 200.0 -> true // Distance spaced checkpoint
            elapsedSinceLastMs >= 300_000L && distFromLast >= 100.0 -> true // 5 mins + 100m
            else -> false
        }

        if (shouldRecord) {
            val label = when {
                currentTimeline.isEmpty() -> "Start Point"
                isArrivalConfirmed -> "Arrived at Destination"
                else -> LocationUtils.timelineLabelForIndex(currentTimeline.size)
            }

            newCheckpoint = TaskTimelinePoint(
                label = label,
                latitude = lat,
                longitude = lon,
                placeName = placeName,
                recordedAt = timeFormatted,
                status = if (isArrivalConfirmed) "arrived" else if (currentTimeline.isEmpty()) "accepted" else "in_transit",
                accuracyMeters = accuracy,
            )
            // Dynamically maintain the last 10 points while preserving the journey's start point
            currentTimeline = LocationUtils.rollTimelinePoints(currentTimeline, newCheckpoint).toMutableList()
            lastRecordedPoint = GeoPoint(lat, lon)
            lastRecordedTimeMs = nowMs
        }

        // 6. Update Notification text
        val remainingText = if (distMeters != null) LocationUtils.formatDistance(distMeters) else "In Transit"
        val etaText = eta ?: "Calculating…"
        val notifContent = if (isArrivalConfirmed) {
            "Arrived at destination (${destName ?: "Shop"})"
        } else {
            "Remaining: $remainingText · ETA: $etaText"
        }
        updateNotification("Active Delivery: ${destName ?: "In Transit"}", notifContent)

        // 7. Queue and dispatch update to Supabase
        val updatePayload = TaskUpdatePayload(
            taskId = taskId,
            status = if (isArrivalConfirmed) "arrived" else "in_transit",
            currentLat = lat,
            currentLon = lon,
            placeName = placeName,
            updateTime = timeFormatted,
            distRemaining = distMeters,
            liveEta = eta,
            arrivalTime = if (isArrivalConfirmed) timeFormatted else null,
            newPoint = newCheckpoint,
        )

        pendingUpdatesQueue.add(updatePayload)
        flushPendingUpdates()

        // 8. If arrival confirmed, stop active tracking
        if (isArrivalConfirmed) {
            stopTracking()
        }
    }

    private suspend fun flushPendingUpdates() = withContext(Dispatchers.IO) {
        while (!pendingUpdatesQueue.isEmpty()) {
            val update = pendingUpdatesQueue.peek() ?: break
            try {
                val payload = buildJsonObject {
                    put("status", update.status)
                    put("current_latitude", update.currentLat)
                    put("current_longitude", update.currentLon)
                    put("current_location_name", update.placeName)
                    put("last_location_update_time", update.updateTime)
                    if (update.distRemaining != null) {
                        put("distance_remaining_meters", update.distRemaining)
                    }
                    if (update.liveEta != null) {
                        put("live_eta", update.liveEta)
                    }
                    if (update.arrivalTime != null) {
                        put("arrival_time", update.arrivalTime)
                    }
                    // Sync timeline
                    put("timeline", Json.encodeToJsonElement(ListSerializer(TaskTimelinePoint.serializer()), currentTimeline))
                }

                supabase.from("route_tasks")
                    .update(payload) {
                        select()
                        filter { eq("id", update.taskId) }
                    }

                // Succeeded, remove from queue
                pendingUpdatesQueue.poll()
            } catch (e: Exception) {
                println("Failed to flush location update to Supabase, will retry on next tick: ${e.message}")
                break // Keep in queue for next cycle
            }
        }
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.launch {
            flushPendingUpdates()
            AndroidDeliveryTrackingManager.updateTrackingState(false, null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        AndroidDeliveryTrackingManager.updateTrackingState(false, null)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"

        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_EMPLOYEE_ID = "EXTRA_EMPLOYEE_ID"
        const val EXTRA_DEST_LAT = "EXTRA_DEST_LAT"
        const val EXTRA_DEST_LON = "EXTRA_DEST_LON"
        const val EXTRA_DEST_NAME = "EXTRA_DEST_NAME"

        private const val CHANNEL_ID = "ruwia_delivery_tracking"
        private const val NOTIFICATION_ID = 2001

        private fun currentFormattedTime(): String {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val h = now.hour
            val hh = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
            val ampm = if (h >= 12) "PM" else "AM"
            val mm = now.minute.toString().padStart(2, '0')
            return "$hh:$mm $ampm"
        }
    }
}
