package com.example.ruwia

import android.app.Application
import com.example.ruwia.data.initKoinAndroid
import com.example.ruwia.data.initSupabaseClient

class RuwiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize the Supabase Client with our active flavor's keys from BuildConfig
        initSupabaseClient(
            url = BuildConfig.SUPABASE_URL,
            anonKey = BuildConfig.SUPABASE_ANON_KEY,
            serviceKey = BuildConfig.SUPABASE_SERVICE_KEY
        )
        // Initialize Koin here so all singletons (including Supabase client)
        // are created AFTER the Application context is available.
        // AndroidX Startup (SupabaseInitializer) runs before this — via ContentProviders —
        // so appContext is guaranteed to be set when Supabase initializes.
        initKoinAndroid(this)
    }
}

