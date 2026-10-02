package com.example.ruwia

import android.app.Application
import com.example.ruwia.data.initKoinAndroid
import com.example.ruwia.data.initSupabaseClient
import com.example.ruwia.data.reportEffectiveServiceKey

class RuwiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Lets shared UI read the real APK version (PackageManager) for the
        // version label shown at the bottom of both settings screens.
        AppContextHolder.context = this
        // Single source of truth on Android: the active flavor's keys from
        // BuildConfig (populated from local.properties, never committed).
        // This pre-empts the shared defaults, so diagnostics must report
        // THESE values — not the bundled fallback.
        initSupabaseClient(
            url = BuildConfig.SUPABASE_URL,
            anonKey = BuildConfig.SUPABASE_ANON_KEY,
            serviceKey = BuildConfig.SUPABASE_SERVICE_KEY
        )
        reportEffectiveServiceKey(
            BuildConfig.SUPABASE_SERVICE_KEY.length,
            BuildConfig.SUPABASE_SERVICE_KEY.takeLast(4),
        )
        // Initialize Koin here so all singletons (including Supabase client)
        // are created AFTER the Application context is available.
        // AndroidX Startup (SupabaseInitializer) runs before this — via ContentProviders —
        // so appContext is guaranteed to be set when Supabase initializes.
        initKoinAndroid(this)
    }
}

