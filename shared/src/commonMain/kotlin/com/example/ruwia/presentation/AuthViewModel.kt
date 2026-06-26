package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.AuthRepository
import com.example.ruwia.data.RolePreferences
import com.example.ruwia.data.supabase
import com.example.ruwia.domain.UserRole
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Job
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
    val displayName: String = "",
    val email: String = "",
)

class AuthViewModel(
    private val repo: AuthRepository,
    private val rolePrefs: RolePreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    // Tracks the async signOut job so that a follow-up login() can join() it
    // and wait for the on-disk session to be fully cleared before signing in
    // again. Without this synchronization, the old user's session can remain
    // on disk and be auto-restored on the next cold-start of the app.
    private var logoutJob: Job? = null

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
            // Clean up any stale role cache from a previous session.
            rolePrefs.clear()
            _state.value = AuthState()
            return@launch
        }

        // ── Step 3: token is valid — resolve role ──────────────────────────
        // Prefer the locally cached role so cold-start is instant and works
        // even when the network is flaky. The actual `profiles` row is the
        // source of truth, so we still refresh in the background and update
        // state if the role changed (e.g. a user was promoted to admin).
        val cachedRole = rolePrefs.getRole()

        if (cachedRole != null) {
            // Navigate to the correct dashboard immediately using the cache…
            val cachedEmail = repo.currentUserEmail()
            _state.value = AuthState(
                loggedIn    = true,
                role        = cachedRole,
                email       = cachedEmail,
            )
            // …then refresh name + role from the server in the background.
            launch { refreshRoleFromServer() }
        } else {
            // No cached role — must fetch from network. Retry a few times to
            // ride out transient failures right after app launch.
            val role = fetchRoleWithRetry()
            if (role != null) rolePrefs.saveRole(role)
            val profile = runCatching { repo.myProfile() }.getOrNull()
            _state.value = AuthState(
                loggedIn    = true,
                role        = role,
                displayName = profile?.fullName ?: "",
                email       = repo.currentUserEmail(),
            )
        }
    }

    /** Best-effort role refresh that updates state + cache if the role changed. */
    private suspend fun refreshRoleFromServer() {
        val profile   = runCatching { repo.myProfile() }.getOrNull() ?: return
        val freshRole = profile.role
        if (freshRole != _state.value.role || _state.value.displayName.isEmpty()) {
            if (freshRole != _state.value.role) rolePrefs.saveRole(freshRole)
            _state.value = _state.value.copy(
                role        = freshRole,
                displayName = profile.fullName ?: _state.value.displayName,
                email       = repo.currentUserEmail().ifEmpty { _state.value.email },
            )
        }
    }

    /** Retries [AuthRepository.myProfile] up to [attempts] times with backoff. */
    private suspend fun fetchRoleWithRetry(attempts: Int = 3): UserRole? {
        repeat(attempts) { i ->
            val role = runCatching { repo.myProfile()?.role }.getOrNull()
            if (role != null) return role
            if (i < attempts - 1) kotlinx.coroutines.delay(400L * (i + 1))
        }
        return null
    }

    /** Signs the user out of Supabase and clears local auth state. */
    fun logout() {
        // Clear in-memory state SYNCHRONOUSLY first so the Login screen
        // never sees loggedIn=true and immediately bounces back to the dashboard.
        _state.value = AuthState()
        // Wipe the cached role IMMEDIATELY so the next cold-start can never
        // restore the previous user's role from disk.
        rolePrefs.clear()
        logoutJob = viewModelScope.launch {
            // repo.logout() runs inside NonCancellable internally, so the
            // local session is guaranteed to be wiped from disk even if this
            // job is later joined/awaited by a fast follow-up login.
            runCatching { repo.logout() }
        }
    }

    fun login(email: String, password: String, roleHint: UserRole? = null) = viewModelScope.launch {
        // ── Wait for any in-flight logout to FULLY complete ──────────────
        // Critical: do NOT cancel the previous logout — cancelling skips the
        // local-storage wipe inside Supabase's signOut(), leaving the OLD
        // user's session on disk. On the next cold-start, autoLoadFromStorage
        // restores that stale session and the app lands on the wrong dashboard.
        //
        // Joining instead guarantees: (1) old session is fully removed from
        // disk, then (2) signInWith() writes the NEW session as the only one
        // in storage.
        logoutJob?.join()
        logoutJob = null

        // Belt-and-suspenders: clear any stale cached role before the new
        // login resolves its own role.
        rolePrefs.clear()

        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            repo.login(email, password)
            // Use the role stored in the profiles table; if the row is missing or
            // the fetch fails, fall back to whatever the user selected on the login screen.
            Pair(repo.myProfile(), roleHint)
        }.onSuccess { (profile, hint) ->
            val resolvedRole = profile?.role ?: hint
            // Cache the resolved role so the next cold-start is instant and
            // independent of network availability.
            rolePrefs.saveRole(resolvedRole)
            _state.value = AuthState(
                loggedIn    = true,
                role        = resolvedRole,
                displayName = profile?.fullName ?: "",
                email       = repo.currentUserEmail(),
            )
        }.onFailure {
            _state.value = AuthState(error = it.message ?: "Login failed")
        }
    }

    fun signUp(email: String, password: String, name: String, phone: String) =
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                repo.signUp(email, password, name, phone)
                repo.login(email, password)
                val uid = repo.currentUserId() ?: throw Exception("User ID not found after registration")
                repo.promoteToAdmin(uid)
                repo.myProfile()
            }.onSuccess { profile ->
                val resolvedRole = profile?.role ?: UserRole.admin
                rolePrefs.saveRole(resolvedRole)
                _state.value = AuthState(
                    loggedIn    = true,
                    role        = resolvedRole,
                    displayName = profile?.fullName ?: "",
                    email       = repo.currentUserEmail(),
                )
            }.onFailure {
                _state.value = AuthState(error = it.message ?: "Signup failed")
            }
        }
}
