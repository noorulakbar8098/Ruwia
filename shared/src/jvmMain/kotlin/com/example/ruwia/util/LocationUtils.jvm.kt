package com.example.ruwia.util

import java.awt.Desktop
import java.net.URI

actual fun openMapNavigation(latitude: Double, longitude: Double, label: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"))
        }
    } catch (_: Exception) {}
}
