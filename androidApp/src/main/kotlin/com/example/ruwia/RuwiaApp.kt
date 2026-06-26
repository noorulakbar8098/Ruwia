package com.example.ruwia

import android.app.Application
import com.example.ruwia.data.initKoinAndroid

class RuwiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Koin here so all singletons (including Supabase client)
        // are created AFTER the Application context is available.
        // AndroidX Startup (SupabaseInitializer) runs before this — via ContentProviders —
        // so appContext is guaranteed to be set when Supabase initializes.
        initKoinAndroid(this)
    }
}

