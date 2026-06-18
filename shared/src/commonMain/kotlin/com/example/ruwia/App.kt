
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
                LaunchedEffect(minTimeDone, authState.loading, authState.role, authState.loggedIn) {
                    if (minTimeDone && !authState.loading) {
                        screen = when {
                            authState.loggedIn -> when (authState.role) {
                                UserRole.admin    -> Screen.AdminHome
                                UserRole.employee -> Screen.EmployeeHome
                                UserRole.user     -> Screen.UserHome
                                // Role is unknown (network failure & no cache).
                                // Stay on splash — `checkSession()` will keep
                                // refreshing the role in the background; the
                                // moment it resolves, this LaunchedEffect re-fires
                                // and routes to the correct dashboard. We must
                                // NOT silently default to UserHome/Employee
                                // because that would show the wrong UI to admins.
                                null              -> Screen.Splash
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
                            UserRole.user     -> Screen.UserHome
                            // Defensive fallback — login should always resolve
                            // a role, but if it doesn't, stay on Login rather
                            // than showing the wrong dashboard.
                            null              -> Screen.Login
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
                // End-customer dashboard (UserRole.user). Currently reuses the
                // employee dashboard until a dedicated customer UI is built.
                val vm = koinInject<EmployeeViewModel>()
                EmployeeDashboardScreen(
                    vm           = vm,
                    onLogout     = { authVm.logout(); screen = Screen.Login },
                    employeeName = authState.displayName.ifEmpty { "User" },
                    userEmail    = authState.email,
                )
            }

            Screen.AdminHome -> {
                val vm = koinInject<AdminViewModel>()
                AdminDashboardScreen(
                    vm           = vm,
                    onLogout     = { authVm.logout(); screen = Screen.Login },
                    adminName    = authState.displayName.ifEmpty { "Admin" },
                    adminEmail   = authState.email,
                )
            }

            Screen.EmployeeHome -> {
                val vm = koinInject<EmployeeViewModel>()
                EmployeeDashboardScreen(
                    vm           = vm,
                    onLogout     = { authVm.logout(); screen = Screen.Login },
                    employeeName = authState.displayName.ifEmpty { "Employee" },
                    userEmail    = authState.email,
                )
            }
        }
    }
}
