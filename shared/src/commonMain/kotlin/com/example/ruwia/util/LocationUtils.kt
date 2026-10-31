package com.example.ruwia.util

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object LocationUtils {

    /**
     * Parses latitude and longitude from Google Maps URLs, share links, or coordinate text.
     * Supported formats:
     * - "12.9716, 77.5946" or "12.9716,77.5946"
     * - DMS: 12°58'17.8"N 77°35'40.4"E
     * - "https://maps.google.com/?q=12.9716,77.5946" (including %2C encoding)
     * - "https://www.google.com/maps/@12.9715987,77.5945627,15z"
     * - "https://www.google.com/maps/place/.../@12.9715987,77.5945627,..."
     * - "!3d12.9715987!4d77.5945627" (Google Maps place URLs)
     * - "destination=12.9716,77.5946" / "daddr=12.9716,77.5946" / "center=12.9716,77.5946"
     */
    fun parseGoogleMapsCoordinates(input: String?): Pair<Double, Double>? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()
            .replace("%2C", ",")
            .replace("%2c", ",")
            .replace("%20", " ")

        // 1. Check for "!3d... !4d..." in Google Maps place links
        val placeMatch = Regex("""!3d([-+]?\d{1,2}\.\d+)!4d([-+]?\d{1,3}\.\d+)""").find(trimmed)
        if (placeMatch != null) {
            val lat = placeMatch.groupValues[1].toDoubleOrNull()
            val lon = placeMatch.groupValues[2].toDoubleOrNull()
            if (isValidLatLng(lat, lon)) return Pair(lat!!, lon!!)
        }

        // 2. Check for query parameter q=lat,lon or destination=lat,lon or daddr=lat,lon or ll=lat,lon or center=lat,lon
        val paramMatch = Regex("""(?:[?&](?:q|destination|daddr|ll|center)=)([-+]?\d{1,2}\.\d+)[, ]+([-+]?\d{1,3}\.\d+)""").find(trimmed)
        if (paramMatch != null) {
            val lat = paramMatch.groupValues[1].toDoubleOrNull()
            val lon = paramMatch.groupValues[2].toDoubleOrNull()
            if (isValidLatLng(lat, lon)) return Pair(lat!!, lon!!)
        }

        // 3. Check for @lat,lon in Google Maps URLs
        val atMatch = Regex("""@([-+]?\d{1,2}\.\d+),([-+]?\d{1,3}\.\d+)""").find(trimmed)
        if (atMatch != null) {
            val lat = atMatch.groupValues[1].toDoubleOrNull()
            val lon = atMatch.groupValues[2].toDoubleOrNull()
            if (isValidLatLng(lat, lon)) return Pair(lat!!, lon!!)
        }

        // 4. Check for DMS (Degrees Minutes Seconds) e.g. 12°58'17.8"N 77°35'40.4"E
        val dmsMatch = Regex("""(\d{1,2})[°\s]+(\d{1,2})['′\s]+(\d{1,2}(?:\.\d+)?)["″\s]*([NSns])[, ]+(\d{1,3})[°\s]+(\d{1,2})['′\s]+(\d{1,2}(?:\.\d+)?)["″\s]*([EWew])""").find(trimmed)
        if (dmsMatch != null) {
            val latDeg = dmsMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val latMin = dmsMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            val latSec = dmsMatch.groupValues[3].toDoubleOrNull() ?: 0.0
            val latDir = dmsMatch.groupValues[4].uppercase()
            var lat = latDeg + (latMin / 60.0) + (latSec / 3600.0)
            if (latDir == "S") lat = -lat

            val lonDeg = dmsMatch.groupValues[5].toDoubleOrNull() ?: 0.0
            val lonMin = dmsMatch.groupValues[6].toDoubleOrNull() ?: 0.0
            val lonSec = dmsMatch.groupValues[7].toDoubleOrNull() ?: 0.0
            val lonDir = dmsMatch.groupValues[8].uppercase()
            var lon = lonDeg + (lonMin / 60.0) + (lonSec / 3600.0)
            if (lonDir == "W") lon = -lon

            if (isValidLatLng(lat, lon)) return Pair(lat, lon)
        }

        // 5. Raw coordinate string e.g. "12.971598, 77.594562"
        val rawMatch = Regex("""^[-+]?\d{1,2}\.\d+[, ]+[-+]?\d{1,3}\.\d+$""").find(trimmed)
        if (rawMatch != null) {
            val parts = trimmed.split(Regex("[, ]+")).filter { it.isNotBlank() }
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull()
                val lon = parts[1].toDoubleOrNull()
                if (isValidLatLng(lat, lon)) return Pair(lat!!, lon!!)
            }
        }

        // 6. Fallback regex search anywhere in the string for coordinates
        val generalMatch = Regex("""([-+]?\d{1,2}\.\d{3,})[, ]+([-+]?\d{1,3}\.\d{3,})""").find(trimmed)
        if (generalMatch != null) {
            val lat = generalMatch.groupValues[1].toDoubleOrNull()
            val lon = generalMatch.groupValues[2].toDoubleOrNull()
            if (isValidLatLng(lat, lon)) return Pair(lat!!, lon!!)
        }

        return null
    }

    /** Extracts the first HTTP/HTTPS URL from a string (such as a shared Maps message). */
    fun extractUrl(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val match = Regex("""https?://[^\s]+""").find(input)
        return match?.value?.trimEnd('.', ',', ';', ')')
    }

    /** Returns true if input contains a Google Maps link (including short links like maps.app.goo.gl). */
    fun isGoogleMapsUrl(input: String?): Boolean {
        val url = extractUrl(input) ?: return false
        return url.contains("maps.app.goo.gl", ignoreCase = true) ||
                url.contains("goo.gl/maps", ignoreCase = true) ||
                url.contains("google.com/maps", ignoreCase = true) ||
                url.contains("maps.google.com", ignoreCase = true)
    }

    private fun isValidLatLng(lat: Double?, lon: Double?): Boolean {
        if (lat == null || lon == null) return false
        return lat in -90.0..90.0 && lon in -180.0..180.0
    }

    /**
     * Calculate haversine distance in meters between two GPS coordinates.
     */
    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = (lat2 - lat1) * (PI / 180.0)
        val dLon = (lon2 - lon1) * (PI / 180.0)
        val a = sin(dLat / 2).pow(2) +
                cos(lat1 * (PI / 180.0)) * cos(lat2 * (PI / 180.0)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Format distance in meters into human readable text (e.g. "250 m", "1.4 km").
     */
    fun formatDistance(meters: Double?): String {
        if (meters == null || meters < 0) return "—"
        return if (meters < 1000.0) {
            "${meters.toInt()} m"
        } else {
            val km = meters / 1000.0
            val rounded = (km * 10).toInt() / 10.0
            "$rounded km"
        }
    }

    /**
     * Calculate estimated arrival time (ETA) based on remaining distance and current speed.
     * Uses 25 km/h (~6.94 m/s) urban delivery estimate if current speed is negligible.
     */
    fun calculateEta(distanceMeters: Double, speedMps: Double? = null): String {
        if (distanceMeters <= 300.0) {
            return "Arriving now"
        }
        // Urban delivery traffic average speed: 25 km/h = 6.94 m/s
        val effectiveSpeed = if (speedMps != null && speedMps >= 2.0) speedMps else 6.94
        val durationSeconds = (distanceMeters / effectiveSpeed).toLong().coerceAtLeast(60)

        val etaInstant = Clock.System.now().plus(durationSeconds.seconds)
        val local = etaInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        val h = local.hour
        val hh = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        val ampm = if (h >= 12) "PM" else "AM"
        val mm = local.minute.toString().padStart(2, '0')
        val timeStr = "$hh:$mm $ampm"

        val minutesRemaining = (durationSeconds / 60).toInt()
        return if (minutesRemaining < 60) {
            "~$minutesRemaining min ($timeStr)"
        } else {
            val hrs = minutesRemaining / 60
            val remMins = minutesRemaining % 60
            "~${hrs}h ${remMins}m ($timeStr)"
        }
    }

    /**
     * Label for timeline index (0-based) up to 10 points.
     */
    fun timelineLabelForIndex(index: Int, isCompleted: Boolean = false): String {
        if (isCompleted) return "Delivery Completed"
        return when (index) {
            0 -> "Start Point"
            1 -> "Second Point"
            2 -> "Third Point"
            3 -> "Fourth Point"
            4 -> "Fifth Point"
            5 -> "Sixth Point"
            6 -> "Seventh Point"
            7 -> "Eighth Point"
            8 -> "Ninth Point"
            9 -> "Tenth Point"
            else -> "Point ${index + 1}"
        }
    }

    /**
     * Keeps a dynamic window of up to [maxPoints] points (default 10):
     * Always preserves the journey's initial Start Point (first checkpoint) and keeps
     * the latest (maxPoints - 1) dynamic checkpoints.
     * If total points <= maxPoints, all points are kept.
     */
    fun rollTimelinePoints(
        existing: List<com.example.ruwia.domain.TaskTimelinePoint>,
        newPoint: com.example.ruwia.domain.TaskTimelinePoint,
        maxPoints: Int = 10,
    ): List<com.example.ruwia.domain.TaskTimelinePoint> {
        if (existing.isEmpty()) {
            val startLabel = if (newPoint.label.isNotBlank() && !newPoint.label.startsWith("Point")) newPoint.label else "Start Point"
            return listOf(newPoint.copy(label = startLabel))
        }
        val combined = existing + newPoint
        val trimmed = if (combined.size <= maxPoints) {
            combined
        } else {
            val start = combined.first()
            val recent = combined.drop(1).takeLast(maxPoints - 1)
            listOf(start) + recent
        }

        return trimmed.mapIndexed { idx, pt ->
            val isFirst = idx == 0
            val isLast = idx == trimmed.lastIndex
            val cleanLabel = when {
                isFirst -> if (pt.label.isNotBlank() && !pt.label.startsWith("Point")) pt.label else "Start Point"
                isLast && (pt.status.equals("completed", ignoreCase = true) || pt.status.equals("arrived", ignoreCase = true)) -> {
                    if (pt.label.isNotBlank() && !pt.label.startsWith("Point")) pt.label else "Arrived at Destination"
                }
                else -> timelineLabelForIndex(idx)
            }
            pt.copy(label = cleanLabel)
        }
    }
}

/**
 * Platform-specific navigation launcher.
 */
expect fun openMapNavigation(latitude: Double, longitude: Double, label: String = "")
