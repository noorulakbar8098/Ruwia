package com.example.ruwia

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Holds the Application context so [appVersionLabel] can query PackageManager. */
object AppContextHolder {
    @SuppressLint("StaticFieldLeak")
    var context: Context? = null
}

actual fun appVersionLabel(): String {
    val ctx = AppContextHolder.context ?: return appVersionFallbackLabel
    return try {
        val pkg = ctx.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val info = ctx.packageManager.getPackageInfo(
                pkg,
                PackageManager.PackageInfoFlags.of(0),
            )
            "v${info.versionName} (${info.longVersionCode})"
        } else {
            @Suppress("DEPRECATION")
            val info = ctx.packageManager.getPackageInfo(pkg, 0)
            @Suppress("DEPRECATION")
            "v${info.versionName} (${info.versionCode})"
        }
    } catch (_: Exception) {
        appVersionFallbackLabel
    }
}
