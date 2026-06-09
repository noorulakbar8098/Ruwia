
package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.AuthRepository
import com.example.ruwia.data.supabase
import com.example.ruwia.domain.UserRole
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AuthState(
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
    val role: UserRole? = null,
)

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** Called on splash — restores a persisted Supabase session if one exists. */
    fun checkSession() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)

        // ── Step 1: wait for auth module to finish reading from storage ────
        val status = try {
            supabase.auth.sessionStatus
                .filter { it !is SessionStatus.Initializing }
                .first()
        } catch (_: Exception) {
            _state.value = AuthState()
            return@launch
        }

        // ── Step 2: no valid token → show login ────────────────────────────
        if (status !is SessionStatus.Authenticated) {
            _state.value = AuthState()
            return@launch
        }


        // ── Step 3: token is valid — fetch role (best-effort, don't logout on failure) ──
        val role = try {
            repo.myProfile()?.role
        } catch (_: Exception) {
            null  // network hiccup — navigate to dashboard anyway; role loads later
        }

        _state.value = AuthState(loggedIn = true, role = role)
    }

    /** Signs the user out of Supabase and clears local auth state. */
    fun logout() {
        // Clear in-memory state SYNCHRONOUSLY first so the Login screen
        // never sees loggedIn=true and immediately bounces back to the dashboard.
        _state.value = AuthState()
        viewModelScope.launch {
            runCatching { repo.logout() } // async: revoke server-side token
        }
    }

    fun login(email: String, password: String, roleHint: UserRole? = null) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            repo.login(email, password)
            // Use the role stored in the profiles table; if the row is missing or
            // the fetch fails, fall back to whatever the user selected on the login screen.
            repo.myProfile()?.role ?: roleHint
        }.onSuccess { role ->
            _state.value = AuthState(loggedIn = true, role = role)
        }.onFailure {
            _state.value = AuthState(error = it.message ?: "Login failed")
        }
    }

    fun signUp(email: String, password: String, name: String, phone: String) =
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repo.signUp(email, password, name, phone) }
                .onSuccess { login(email, password) }
                .onFailure { _state.value = AuthState(error = it.message ?: "Signup failed") }
        }
}
