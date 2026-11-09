package com.example.ruwia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.ruwia.data.NotificationDeepLink

class MainActivity : ComponentActivity() {
    // System-push permission (Android 13+). Without it FCM still refreshes
    // the in-app feed, but no outside-the-app banner appears.
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Every screen in the app has a dark teal header, so status bar icons
        // must stay light regardless of the system theme or the in-app theme.
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Koin is started in RuwiaApp.onCreate() — no need to call initKoin() here
        handleNotificationIntent(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    /**
     * Turns a push tap into a deep link to the item in the notification
     * center. FCM puts the data payload into the launch intent's extras; our
     * own foreground notifications add the same extra explicitly.
     *
     * The extra is removed after reading so an activity recreation (rotation)
     * doesn't re-open an already-consumed notification.
     */
    private fun handleNotificationIntent(intent: Intent?) {
        val id = intent?.getStringExtra(NotificationDeepLink.EXTRA_NOTIFICATION_ID) ?: return
        intent.removeExtra(NotificationDeepLink.EXTRA_NOTIFICATION_ID)
        NotificationDeepLink.open(id)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}