package com.example.ruwia.util

import android.content.Intent
import android.net.Uri
import com.example.ruwia.AppContextHolder

actual fun openMapNavigation(latitude: Double, longitude: Double, label: String) {
    val context = AppContextHolder.context ?: return
    val hasCoords = (latitude != 0.0 || longitude != 0.0)

    if (hasCoords) {
        try {
            // Try Google Maps Navigation URI first
            val gmapsUri = Uri.parse("google.navigation:q=$latitude,$longitude")
            val intent = Intent(Intent.ACTION_VIEW, gmapsUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {}
    }

    try {
        // Try geo URI
        val geoUri = if (hasCoords) {
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label.ifBlank { "Destination" })})")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(label.ifBlank { "Location" })}")
        }
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (geoIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(geoIntent)
            return
        }
    } catch (_: Exception) {}

    try {
        // Fallback to web browser Google Maps
        val webUri = if (hasCoords) {
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude")
        } else {
            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(label)}")
        }
        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    } catch (_: Exception) {}
}
