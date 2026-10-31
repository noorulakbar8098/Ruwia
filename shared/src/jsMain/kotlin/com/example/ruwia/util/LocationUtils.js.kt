package com.example.ruwia.util

import kotlinx.browser.window

actual fun openMapNavigation(latitude: Double, longitude: Double, label: String) {
    try {
        window.open("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude", "_blank")
    } catch (_: Exception) {}
}
