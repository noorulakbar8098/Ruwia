package com.example.ruwia.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openMapNavigation(latitude: Double, longitude: Double, label: String) {
    try {
        val url = NSURL.URLWithString("https://maps.apple.com/?daddr=$latitude,$longitude")
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
        }
    } catch (_: Exception) {}
}
