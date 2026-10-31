package com.example.ruwia.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
actual fun rememberLocationPermissionController(
    onPermissionResult: (Boolean) -> Unit
): LocationPermissionController {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val sharedPrefs = remember(context) {
        context.getSharedPreferences("ruwia_location_prefs", Context.MODE_PRIVATE)
    }

    fun checkStatus(): LocationPermissionStatus {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            return LocationPermissionStatus.GRANTED
        }

        val requestedBefore = sharedPrefs.getBoolean("location_requested_before", false)
        if (activity != null && requestedBefore) {
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (!shouldShowRationale) {
                return LocationPermissionStatus.PERMANENTLY_DENIED
            }
            return LocationPermissionStatus.DENIED
        }

        return LocationPermissionStatus.NOT_DETERMINED
    }

    var currentStatus by remember { mutableStateOf(checkStatus()) }

    val backgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Background permission callback for Android 10+
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        sharedPrefs.edit().putBoolean("location_requested_before", true).apply()
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val granted = fine || coarse

        if (granted) {
            currentStatus = LocationPermissionStatus.GRANTED
            onPermissionResult(true)
            // On Android 10+ (Q+), prompt for background permission if not granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasBg = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasBg) {
                    try {
                        backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } catch (_: Exception) {}
                }
            }
        } else {
            currentStatus = checkStatus()
            onPermissionResult(false)
        }
    }

    return remember(currentStatus) {
        object : LocationPermissionController {
            override val status: LocationPermissionStatus
                get() = currentStatus

            override fun requestPermission() {
                sharedPrefs.edit().putBoolean("location_requested_before", true).apply()
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }

            override fun openAppSettings() {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallback)
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
