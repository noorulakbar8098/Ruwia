package com.example.ruwia.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberLocationPermissionController(
    onPermissionResult: (Boolean) -> Unit
): LocationPermissionController {
    return remember {
        object : LocationPermissionController {
            override val status: LocationPermissionStatus = LocationPermissionStatus.GRANTED
            override fun requestPermission() { onPermissionResult(true) }
            override fun openAppSettings() {}
        }
    }
}
