package com.example.ruwia

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.example.ruwia.domain.UserRole
import com.example.ruwia.presentation.AuthViewModel
import com.example.ruwia.presentation.UserViewModel
import com.example.ruwia.ui.LoginScreen
import com.example.ruwia.ui.PlaceOrderScreen
import com.example.ruwia.ui.UserHomeScreen
import org.koin.compose.koinInject

private sealed class Screen {
    data object Login : Screen()
    data object UserHome : Screen()
    data object AdminHome : Screen()
}

@Composable
fun App() {
    MaterialTheme {
        var screen by remember { mutableStateOf<Screen>(Screen.Login) }

        when (screen) {
            Screen.Login -> {
                val vm = koinInject<AuthViewModel>()
                LoginScreen(vm) { role ->
                    screen = if (role == UserRole.admin) Screen.AdminHome else Screen.UserHome
                }
            }

            Screen.UserHome -> {
                val vm = koinInject<UserViewModel>()
                var showPlaceOrder by remember { mutableStateOf(false) }
                if (showPlaceOrder) {
                    PlaceOrderScreen(vm, onBack = { showPlaceOrder = false })
                } else {
                    UserHomeScreen(
                        vm = vm,
                        onPlaceOrder = { showPlaceOrder = true },
                        onLogout = { screen = Screen.Login },
                    )
                }
            }

            Screen.AdminHome -> {
                // Milestone 3 — admin screens go here
                Text("Admin — coming soon")
            }
        }
    }
}
