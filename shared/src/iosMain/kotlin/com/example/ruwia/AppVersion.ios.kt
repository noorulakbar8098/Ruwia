package com.example.ruwia

import platform.Foundation.NSBundle

actual fun appVersionLabel(): String {
    val info = NSBundle.mainBundle.infoDictionary
    val name = info?.get("CFBundleShortVersionString") as? String
    return if (name != null) {
        val code = info?.get("CFBundleVersion") as? String
        "v$name (${code ?: "?"})"
    } else {
        appVersionFallbackLabel
    }
}
