
package com.example.ruwia

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.example.ruwia.domain.UserRole
import com.example.ruwia.presentation.AdminViewModel
import com.example.ruwia.presentation.AuthViewModel
import com.example.ruwia.presentation.EmployeeViewModel
import com.example.ruwia.ui.AdminDashboardScreen
import com.example.ruwia.ui.EmployeeDashboardScreen
import com.example.ruwia.ui.ForgotPasswordScreen
import com.example.ruwia.ui.LoginScreen
import com.example.ruwia.ui.SplashScreen
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
        val authVm = koinInject<AuthViewModel>()
        val authState by authVm.state.collectAsState()

        // Global guard: directly collect the StateFlow (bypasses collectAsState's one-frame
        // delay) so that any loggedIn→false transition navigates to Login immediately,
        // regardless of what triggered it (logout button, token expiry, etc.).
        LaunchedEffect(authVm) {
            authVm.state.collect { state ->
                if (!state.loggedIn &&
                    screen != Screen.Splash &&
                    screen != Screen.Login &&
                    screen != Screen.ForgotPassword
                ) {
                    screen = Screen.Login
                }
            }
        }

        when (screen) {
            Screen.Splash -> {
                var minTimeDone by remember { mutableStateOf(false) }

                // Start session check + minimum display timer in parallel
                LaunchedEffect(Unit) {
                    authVm.checkSession()
                    kotlinx.coroutines.delay(2500L)
                    minTimeDone = true
                }

                // Navigate only when BOTH: 2.5 s elapsed AND session check finished
                LaunchedEffect(minTimeDone, authState.loading) {
                    if (minTimeDone && !authState.loading) {
                        screen = when {
                            authState.loggedIn -> when (authState.role) {
                                UserRole.admin    -> Screen.AdminHome
                                UserRole.employee -> Screen.EmployeeHome
                                else              -> Screen.UserHome
                            }
                            else -> Screen.Login
                        }
                    }
                }

                // Splash renders normally; navigation is driven by the LaunchedEffect above
                SplashScreen(onTimeout = {})
            }

            Screen.Login -> {
                LoginScreen(
                    vm = authVm,
                    onForgotPassword = { screen = Screen.ForgotPassword },
                    onLoggedIn = { role ->
                        screen = when (role) {
                            UserRole.admin    -> Screen.AdminHome
                            UserRole.employee -> Screen.EmployeeHome
                            else              -> Screen.UserHome
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
                // Role came back null (no profile row yet, network hiccup, etc.)
                // Default to the employee dashboard so a staff member is never
                // accidentally shown the owner/admin UI.
                val vm = koinInject<EmployeeViewModel>()
                EmployeeDashboardScreen(
                    vm = vm,
                    onLogout = { authVm.logout(); screen = Screen.Login }
                )
            }

            Screen.AdminHome -> {
                val vm = koinInject<AdminViewModel>()
                AdminDashboardScreen(
                    vm = vm,
                    onLogout = { authVm.logout(); screen = Screen.Login }
                )
            }

            Screen.EmployeeHome -> {
                val vm = koinInject<EmployeeViewModel>()
                EmployeeDashboardScreen(
                    vm = vm,
                    onLogout = { authVm.logout(); screen = Screen.Login }
                )
            }
        }
    }
}
