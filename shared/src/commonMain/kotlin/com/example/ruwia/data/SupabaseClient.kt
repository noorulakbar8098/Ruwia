package com.example.ruwia.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

// TODO: paste your values from Supabase → Project Settings → API
private const val SUPABASE_URL = "https://srvertclbjcvlfdectsl.supabase.co"
private const val SUPABASE_ANON_KEY = "sb_publishable_N3oENikhDxobWX5dcEJoMw_FMbth7CN"

val supabase: SupabaseClient = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_ANON_KEY,
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
}
