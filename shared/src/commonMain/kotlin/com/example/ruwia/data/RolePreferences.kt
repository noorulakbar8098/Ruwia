package com.example.ruwia.data

import com.example.ruwia.domain.UserRole
import com.russhwolf.settings.Settings

/**
 * Persists the authenticated user's role on disk so cold-starts don't have to
 * re-fetch it from the network.
 *
 * Without this cache, [com.example.ruwia.presentation.AuthViewModel.checkSession]
 * has to make a network call to the `profiles` table on every app launch. If
 * that call is slow or fails, [com.example.ruwia.presentation.AuthState.role]
 * stays `null` and the app falls back to the wrong dashboard (e.g. an admin
 * sees the employee UI). Caching makes the role available instantly and
 * survives a transient network outage.
 *
 * Backed by [Settings] which maps to:
 *   • Android  → SharedPreferences
 *   • iOS      → NSUserDefaults
 *   • JVM      → java.util.prefs
 *   • JS/Wasm  → window.localStorage
 */
class RolePreferences(private val settings: Settings) {

    fun saveRole(role: UserRole?) {
        if (role == null) {
            settings.remove(KEY_ROLE)
        } else {
            settings.putString(KEY_ROLE, role.name)
        }
    }

    fun getRole(): UserRole? {
        val raw = settings.getStringOrNull(KEY_ROLE) ?: return null
        return runCatching { UserRole.valueOf(raw) }.getOrNull()
    }

    fun clear() {
        settings.remove(KEY_ROLE)
    }

    private companion object {
        const val KEY_ROLE = "auth.cached_role"
    }
}

