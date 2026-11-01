package com.example.ruwia.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.ruwia.AppContextHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import com.example.ruwia.util.LocationUtils

class AndroidLocationService(private val explicitContext: Context? = null) : LocationService {
    private fun getContext(): Context? {
        return explicitContext ?: AppContextHolder.context ?: runCatching {
            org.koin.core.context.GlobalContext.getOrNull()?.get<Context>()
        }.getOrNull()
    }

    override suspend fun getCurrentLocation(): GeoPoint? {
        val context = getContext() ?: return null
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return null

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        // Try FusedLocationProviderClient first
        val fusedLoc = withTimeoutOrNull(4000) {
            suspendCancellableCoroutine<Location?> { cont ->
                try {
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { loc ->
                            if (loc != null) {
                                cont.resume(loc)
                            } else {
                                fusedClient.lastLocation
                                    .addOnSuccessListener { last -> cont.resume(last) }
                                    .addOnFailureListener { cont.resume(null) }
                            }
                        }
                        .addOnFailureListener { cont.resume(null) }
                } catch (_: SecurityException) {
                    cont.resume(null)
                }
            }
        }
        if (fusedLoc != null) {
            return GeoPoint(fusedLoc.latitude, fusedLoc.longitude)
        }

        val mgr = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val lastGps = runCatching { mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
        val lastNetwork = runCatching { mgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
        val bestLast = when {
            lastGps != null && lastNetwork != null -> if (lastGps.time >= lastNetwork.time) lastGps else lastNetwork
            else -> lastGps ?: lastNetwork
        }

        val now = System.currentTimeMillis()
        if (bestLast != null && (now - bestLast.time) < 30_000) {
            return GeoPoint(bestLast.latitude, bestLast.longitude)
        }

        val fresh = withTimeoutOrNull(3000) {
            suspendCancellableCoroutine<Location?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        cont.resume(loc)
                        runCatching { mgr.removeUpdates(this) }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                val provider = when {
                    mgr.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    mgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> null
                }

                if (provider == null) {
                    cont.resume(bestLast)
                    return@suspendCancellableCoroutine
                }

                runCatching {
                    mgr.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }.onFailure {
                    cont.resume(bestLast)
                }

                cont.invokeOnCancellation {
                    runCatching { mgr.removeUpdates(listener) }
                }
            }
        } ?: bestLast

        return fresh?.let { GeoPoint(it.latitude, it.longitude) }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        val context = getContext()
        if (context != null) {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val list = geocoder.getFromLocation(latitude, longitude, 1)
                    val address = list?.firstOrNull()
                    if (address != null) {
                        val line = address.getAddressLine(0)
                        if (!line.isNullOrBlank()) return@withContext line
                        val subLocality = address.subLocality ?: address.featureName
                        val locality = address.locality ?: address.subAdminArea
                        val adminArea = address.adminArea
                        val parts = listOfNotNull(subLocality, locality, adminArea).distinct().filter { it.isNotBlank() }
                        if (parts.isNotEmpty()) return@withContext parts.joinToString(", ")
                    }
                }
            } catch (_: Exception) {}
        }

        // Fallback to client reverse geocoding API
        try {
            val url = URL("https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$latitude&longitude=$longitude&localityLanguage=en")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = org.json.JSONObject(body)
                val locality = obj.optString("locality", "")
                val city = obj.optString("city", "").ifBlank { obj.optString("principalSubdivision", "") }
                val country = obj.optString("countryName", "")
                val parts = listOfNotNull(locality.ifBlank { null }, city.ifBlank { null }, country.ifBlank { null }).distinct()
                if (parts.isNotEmpty()) return@withContext parts.joinToString(", ")
            }
        } catch (_: Exception) {}

        val latStr = ((latitude * 10000).toLong() / 10000.0).toString()
        val lonStr = ((longitude * 10000).toLong() / 10000.0).toString()
        "Point ($latStr, $lonStr)"
    }

    override suspend fun searchAddressSuggestions(query: String): List<AddressSuggestion> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val results = mutableListOf<AddressSuggestion>()

        // 1. Android native Geocoder
        val context = getContext()
        if (context != null) {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val list = geocoder.getFromLocationName(cleanQuery, 6)
                    if (!list.isNullOrEmpty()) {
                        for (addr in list) {
                            val line = addr.getAddressLine(0)
                            val title = addr.featureName ?: addr.subLocality ?: addr.locality ?: cleanQuery
                            val parts = listOfNotNull(addr.thoroughfare, addr.subLocality, addr.locality, addr.adminArea)
                                .distinct()
                                .filter { it.isNotBlank() && it != title }
                            val subtitle = parts.joinToString(", ")
                            val full = line ?: listOfNotNull(title, subtitle).filter { it.isNotBlank() }.joinToString(", ")
                            results.add(
                                AddressSuggestion(
                                    title = title,
                                    subtitle = subtitle,
                                    fullAddress = full,
                                    latitude = addr.latitude,
                                    longitude = addr.longitude,
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (results.isNotEmpty()) {
            return@withContext results
        }

        // 2. Fallback to Photon geocoding search (OpenStreetMap data, unblocked open API)
        try {
            val encoded = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
            val url = URL("https://photon.komoot.io/api/?q=$encoded&limit=6")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = org.json.JSONObject(body)
                val features = root.optJSONArray("features") ?: org.json.JSONArray()
                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val geom = feature.optJSONObject("geometry")
                    val coords = geom?.optJSONArray("coordinates")
                    val lon = coords?.optDouble(0, 0.0) ?: 0.0
                    val lat = coords?.optDouble(1, 0.0) ?: 0.0
                    val props = feature.optJSONObject("properties")
                    val name = props?.optString("name", "")?.ifBlank { null }
                        ?: props?.optString("street", "")?.ifBlank { null }
                        ?: cleanQuery
                    val subtitleParts = listOfNotNull(
                        props?.optString("street", "")?.takeIf { it.isNotBlank() && it != name },
                        props?.optString("district", "")?.takeIf { it.isNotBlank() },
                        props?.optString("city", "")?.takeIf { it.isNotBlank() },
                        props?.optString("state", "")?.takeIf { it.isNotBlank() },
                    ).distinct()
                    val subtitle = subtitleParts.joinToString(", ")
                    val full = listOfNotNull(name, subtitle.takeIf { it.isNotBlank() }).joinToString(", ")
                    if (lat != 0.0 && lon != 0.0) {
                        results.add(
                            AddressSuggestion(
                                title = name,
                                subtitle = subtitle,
                                fullAddress = full,
                                latitude = lat,
                                longitude = lon,
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        results
    }

    override suspend fun resolveCoordinatesFromInput(input: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (input.isBlank()) return@withContext null

        // 1. Direct synchronous coordinate parse (works for @lat,lon, !3dlat!4dlon, q=lat,lon, raw coords, DMS)
        val direct = LocationUtils.parseGoogleMapsCoordinates(input)
        if (direct != null) return@withContext direct

        // 2. Check if input contains a URL (especially Google Maps share links: maps.app.goo.gl or goo.gl/maps)
        val url = LocationUtils.extractUrl(input)
        if (url != null) {
            val resolvedFromUrl = resolveCoordinatesFromUrl(url)
            if (resolvedFromUrl != null) return@withContext resolvedFromUrl
        }

        // 3. Fallback: try resolving place name / address via Android Geocoder
        val context = getContext()
        if (context != null && Geocoder.isPresent()) {
            try {
                val cleanText = input.replace(Regex("""https?://[^\s]+"""), "").trim()
                    .trim(',', '-', ':', ' ')
                if (cleanText.length >= 3) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val results = geocoder.getFromLocationName(cleanText, 1)
                    val first = results?.firstOrNull()
                    if (first != null) {
                        return@withContext Pair(first.latitude, first.longitude)
                    }
                }
            } catch (_: Exception) {}
        }

        null
    }

    private fun resolveCoordinatesFromUrl(startUrl: String): Pair<Double, Double>? {
        try {
            var currentUrl = startUrl
            var redirects = 0
            while (redirects < 6) {
                val coords = LocationUtils.parseGoogleMapsCoordinates(currentUrl)
                if (coords != null) return coords

                val u = URL(currentUrl)
                val conn = (u.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                    setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                }

                val code = conn.responseCode
                val locationHeader = conn.getHeaderField("Location")

                if (locationHeader != null && (code in 300..399)) {
                    val nextUrl = if (locationHeader.startsWith("/")) {
                        "${u.protocol}://${u.host}$locationHeader"
                    } else {
                        locationHeader
                    }
                    val redirectedCoords = LocationUtils.parseGoogleMapsCoordinates(nextUrl)
                    if (redirectedCoords != null) return redirectedCoords

                    currentUrl = nextUrl
                    redirects++
                    continue
                }

                if (code == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }

                    val metaMatch = Regex("""content=["'](https?://[^"']+)["']\s+itemprop=["']url["']""").find(body)
                        ?: Regex("""itemprop=["']url["']\s+content=["'](https?://[^"']+)["']""").find(body)
                    if (metaMatch != null) {
                        val metaCoords = LocationUtils.parseGoogleMapsCoordinates(metaMatch.groupValues[1])
                        if (metaCoords != null) return metaCoords
                    }

                    val centerMatch = Regex("""center=([-+]?\d{1,2}\.\d+)(?:%2C|,)([-+]?\d{1,3}\.\d+)""").find(body)
                    if (centerMatch != null) {
                        val lat = centerMatch.groupValues[1].toDoubleOrNull()
                        val lon = centerMatch.groupValues[2].toDoubleOrNull()
                        if (lat != null && lon != null) return Pair(lat, lon)
                    }

                    val initStateMatch = Regex("""\[null,null,([-+]?\d{1,2}\.\d{4,}),([-+]?\d{1,3}\.\d{4,})\]""").find(body)
                    if (initStateMatch != null) {
                        val lat = initStateMatch.groupValues[1].toDoubleOrNull()
                        val lon = initStateMatch.groupValues[2].toDoubleOrNull()
                        if (lat != null && lon != null) return Pair(lat, lon)
                    }

                    val atBodyMatch = Regex("""@([-+]?\d{1,2}\.\d{4,}),([-+]?\d{1,3}\.\d{4,})""").find(body)
                    if (atBodyMatch != null) {
                        val lat = atBodyMatch.groupValues[1].toDoubleOrNull()
                        val lon = atBodyMatch.groupValues[2].toDoubleOrNull()
                        if (lat != null && lon != null) return Pair(lat, lon)
                    }

                    val dMatch = Regex("""!3d([-+]?\d{1,2}\.\d+)!4d([-+]?\d{1,3}\.\d+)""").find(body)
                    if (dMatch != null) {
                        val lat = dMatch.groupValues[1].toDoubleOrNull()
                        val lon = dMatch.groupValues[2].toDoubleOrNull()
                        if (lat != null && lon != null) return Pair(lat, lon)
                    }
                }
                break
            }
        } catch (_: Exception) {}
        return null
    }
}

private val defaultAndroidLocationService = AndroidLocationService()

actual fun getLocationService(): LocationService = defaultAndroidLocationService
