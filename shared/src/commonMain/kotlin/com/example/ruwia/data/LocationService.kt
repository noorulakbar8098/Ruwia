package com.example.ruwia.data

import com.example.ruwia.util.LocationUtils
import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class AddressSuggestion(
    val title: String,
    val subtitle: String = "",
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double,
)

interface LocationService {
    suspend fun getCurrentLocation(): GeoPoint?
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String
    suspend fun searchAddressSuggestions(query: String): List<AddressSuggestion> = emptyList()
    suspend fun resolveCoordinatesFromInput(input: String): Pair<Double, Double>? =
        LocationUtils.parseGoogleMapsCoordinates(input)
}

expect fun getLocationService(): LocationService
