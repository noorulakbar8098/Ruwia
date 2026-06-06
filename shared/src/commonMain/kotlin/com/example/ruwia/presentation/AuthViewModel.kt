package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.AuthRepository
import com.example.ruwia.domain.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun login(email: String, password: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            repo.login(email, password)
            repo.myProfile()?.role
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
