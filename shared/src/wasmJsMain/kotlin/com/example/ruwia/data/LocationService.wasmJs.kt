package com.example.ruwia.data

class WasmJsLocationService : LocationService {
    override suspend fun getCurrentLocation(): GeoPoint? = null

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): String {
        val latStr = ((latitude * 10000).toLong() / 10000.0).toString()
        val lonStr = ((longitude * 10000).toLong() / 10000.0).toString()
        return "Point ($latStr, $lonStr)"
    }
}

actual fun getLocationService(): LocationService = WasmJsLocationService()
