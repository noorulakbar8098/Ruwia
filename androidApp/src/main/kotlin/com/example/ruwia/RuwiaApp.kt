package com.example.ruwia

import android.app.Application
import com.example.ruwia.data.initKoinAndroid
import com.example.ruwia.data.refreshPushToken

class RuwiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Lets shared UI read the real APK version (PackageManager) for the
        // version label shown at the bottom of both settings screens.
        AppContextHolder.context = this
        // Create the push channel before FCM can display a background push;
        // otherwise Android routes it to the generic fallback channel.
        createRuwiaNotificationChannels(this)
        // Initialize Koin here so all singletons (including Supabase client)
        // are created AFTER the Application context is available.
        // AndroidX Startup (SupabaseInitializer) runs before this — via ContentProviders —
        // so appContext is guaranteed to be set when Supabase initializes.
        initKoinAndroid(this)
        // Registers this device for outside-the-app push when a session
        // exists (restored login). No-ops without google-services.json.
        refreshPushToken()
    }
}

