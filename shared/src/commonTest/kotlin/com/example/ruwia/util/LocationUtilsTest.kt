package com.example.ruwia.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationUtilsTest {

    @Test
    fun parseGoogleMapsCoordinates_parsesRawCoordinates() {
        val parsed = LocationUtils.parseGoogleMapsCoordinates("12.971598, 77.594562")
        assertNotNull(parsed)
        assertEquals(12.971598, parsed.first, 0.0001)
        assertEquals(77.594562, parsed.second, 0.0001)
    }

    @Test
    fun parseGoogleMapsCoordinates_parsesQueryUrl() {
        val url = "https://maps.google.com/?q=11.016844,76.955832"
        val parsed = LocationUtils.parseGoogleMapsCoordinates(url)
        assertNotNull(parsed)
        assertEquals(11.016844, parsed.first, 0.0001)
        assertEquals(76.955832, parsed.second, 0.0001)
    }

    @Test
    fun parseGoogleMapsCoordinates_parsesAtFormatUrl() {
        val url = "https://www.google.com/maps/@13.082680,80.270718,17z"
        val parsed = LocationUtils.parseGoogleMapsCoordinates(url)
        assertNotNull(parsed)
        assertEquals(13.082680, parsed.first, 0.0001)
        assertEquals(80.270718, parsed.second, 0.0001)
    }

    @Test
    fun parseGoogleMapsCoordinates_parsesPlaceUrlWith3d4d() {
        val url = "https://www.google.com/maps/place/Shop/@11.016844,76.955832,15z/data=!4m2!3m1!1s0x0:0x0!3d11.016844!4d76.955832"
        val parsed = LocationUtils.parseGoogleMapsCoordinates(url)
        assertNotNull(parsed)
        assertEquals(11.016844, parsed.first, 0.0001)
        assertEquals(76.955832, parsed.second, 0.0001)
    }

    @Test
    fun parseGoogleMapsCoordinates_returnsNullForInvalidInput() {
        assertNull(LocationUtils.parseGoogleMapsCoordinates(""))
        assertNull(LocationUtils.parseGoogleMapsCoordinates("not a coordinate or link"))
        assertNull(LocationUtils.parseGoogleMapsCoordinates("120.0, 200.0")) // out of bounds
    }

    @Test
    fun haversineDistanceMeters_calculatesAccurateDistance() {
        // Distance between Bangalore (12.9716, 77.5946) and Mysore (12.2958, 76.6394) is ~128 km
        val distance = LocationUtils.haversineDistanceMeters(12.9716, 77.5946, 12.2958, 76.6394)
        assertTrue(distance in 127_000.0..130_000.0, "Expected ~128km, was $distance")

        // 300m test
        val distClose = LocationUtils.haversineDistanceMeters(11.0000, 76.0000, 11.0010, 76.0000)
        assertTrue(distClose in 100.0..120.0, "Expected ~111m, was $distClose")
    }

    @Test
    fun calculateEta_reportsArrivingNowWithin300Meters() {
        assertEquals("Arriving now", LocationUtils.calculateEta(250.0))
        assertEquals("Arriving now", LocationUtils.calculateEta(50.0))
    }

    @Test
    fun calculateEta_returnsFormattedTimeForLargerDistances() {
        val eta = LocationUtils.calculateEta(3500.0) // 3.5 km
        assertTrue(eta.contains("min"), "Expected minutes in ETA: $eta")
    }

    @Test
    fun rollTimelinePoints_preservesStartPointAndDynamicallyKeepsTenPoints() {
        var timeline = emptyList<com.example.ruwia.domain.TaskTimelinePoint>()

        // 1. Add first 10 points
        for (i in 0 until 10) {
            val pt = com.example.ruwia.domain.TaskTimelinePoint(
                label = "Point $i",
                latitude = 11.0 + (i * 0.01),
                longitude = 76.0 + (i * 0.01),
                placeName = "Place $i",
                recordedAt = "10:${i.toString().padStart(2, '0')} AM",
                status = if (i == 0) "accepted" else "in_transit",
            )
            timeline = LocationUtils.rollTimelinePoints(timeline, pt)
        }

        assertEquals(10, timeline.size)
        assertEquals("Start Point", timeline.first().label)
        assertEquals("Place 0", timeline.first().placeName)
        assertEquals("Tenth Point", timeline.last().label)
        assertEquals("Place 9", timeline.last().placeName)

        // 2. Add 11th dynamic point
        val eleventh = com.example.ruwia.domain.TaskTimelinePoint(
            label = "Checkpoint 11",
            latitude = 11.11,
            longitude = 76.11,
            placeName = "Place 11",
            recordedAt = "10:15 AM",
            status = "in_transit",
        )
        timeline = LocationUtils.rollTimelinePoints(timeline, eleventh)

        assertEquals(10, timeline.size)
        // Original Start Point MUST be preserved
        assertEquals("Start Point", timeline.first().label)
        assertEquals("Place 0", timeline.first().placeName)
        // Last point is the new 11th point
        assertEquals("Place 11", timeline.last().placeName)
        assertEquals("Tenth Point", timeline.last().label)

        // 3. Add arrival point
        val arrived = com.example.ruwia.domain.TaskTimelinePoint(
            label = "Arrived at Destination",
            latitude = 11.20,
            longitude = 76.20,
            placeName = "Final Shop",
            recordedAt = "10:30 AM",
            status = "arrived",
        )
        timeline = LocationUtils.rollTimelinePoints(timeline, arrived)

        assertEquals(10, timeline.size)
        assertEquals("Start Point", timeline.first().label)
        assertEquals("Place 0", timeline.first().placeName)
        assertEquals("Arrived at Destination", timeline.last().label)
        assertEquals("Final Shop", timeline.last().placeName)
    }
}
