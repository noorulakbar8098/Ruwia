package com.example.ruwia

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.example.ruwia.domain.UserRole
import com.example.ruwia.presentation.AdminViewModel
import com.example.ruwia.presentation.AuthViewModel
import com.example.ruwia.presentation.EmployeeViewModel
import com.example.ruwia.presentation.UserViewModel
import com.example.ruwia.ui.AdminDashboardScreen
import com.example.ruwia.ui.EmployeeDashboardScreen
import com.example.ruwia.ui.ForgotPasswordScreen
import com.example.ruwia.ui.LoginScreen
import com.example.ruwia.ui.PlaceOrderScreen
import com.example.ruwia.ui.SplashScreen
import com.example.ruwia.ui.UserHomeScreen
import org.koin.compose.koinInject

private sealed class Screen {
    data object Splash : Screen()
    data object Login : Screen()
    data object ForgotPassword : Screen()
    data object UserHome : Screen()
    data object AdminHome : Screen()
    data object EmployeeHome : Screen()
}

@Composable
fun App() {
    MaterialTheme {
        var screen by remember { mutableStateOf<Screen>(Screen.Splash) }

        when (screen) {
            Screen.Splash -> {
                SplashScreen(
                    onTimeout = { screen = Screen.Login }
                )
            }

            Screen.Login -> {
                val vm = koinInject<AuthViewModel>()
                LoginScreen(
                    vm = vm,
                    onForgotPassword = { screen = Screen.ForgotPassword },
                    onLoggedIn = { role ->
                        screen = when (role) {
                            UserRole.admin -> Screen.AdminHome
                            UserRole.employee -> Screen.EmployeeHome
                            else -> Screen.UserHome
                        }
                    }
                )
            }

            Screen.ForgotPassword -> {
                ForgotPasswordScreen(
                    onBack = { screen = Screen.Login }
                )
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
                val vm = koinInject<AdminViewModel>()
                AdminDashboardScreen(
                    vm = vm,
                    onLogout = { screen = Screen.Login }
                )
            }

            Screen.EmployeeHome -> {
                val vm = koinInject<EmployeeViewModel>()
                EmployeeDashboardScreen(
                    vm = vm,
                    onLogout = { screen = Screen.Login }
                )
            }
        }
    }
}
