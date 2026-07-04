
package com.example.ruwia

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ruwia.domain.UserRole
import com.example.ruwia.presentation.AdminViewModel
import com.example.ruwia.presentation.AuthViewModel
import com.example.ruwia.presentation.EmployeeViewModel
import com.example.ruwia.ui.AdminDashboardScreen
import com.example.ruwia.ui.EmployeeDashboardScreen
import com.example.ruwia.ui.ForgotPasswordScreen
import com.example.ruwia.ui.LoginScreen
import com.example.ruwia.ui.SplashScreen
import com.example.ruwia.ui.NoInternetScreen
import com.example.ruwia.ui.dashboard.NTTheme
import com.example.ruwia.theme.RuwiaTheme
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
        val adminVm = koinInject<AdminViewModel>()
        val employeeVm = koinInject<EmployeeViewModel>()
        val authState by authVm.state.collectAsState()

        val lifecycleState by rememberLifecycleObserver()

        LaunchedEffect(lifecycleState) {
            if (lifecycleState == LifecycleState.ON_RESUME) {
                authVm.checkSession()
                adminVm.loadData()
                employeeVm.startPeriodicRefresh()
            }
        }

        val connectivityObserver = remember { getConnectivityObserver() }
        val isConnected by connectivityObserver.isConnected.collectAsState()
        var showNoInternet by remember { mutableStateOf(false) }

        LaunchedEffect(isConnected) {
            if (!isConnected) {
                showNoInternet = true
            }
        }

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

        Box(modifier = Modifier.fillMaxSize()) {
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
                    NTTheme {
                        RuwiaTheme {
                            EmployeeDashboardScreen(
                                vm           = employeeVm,
                                onLogout     = {
                                    adminVm.clearState()
                                    employeeVm.clearState()
                                    authVm.logout()
                                    screen = Screen.Login
                                },
                                employeeName = authState.displayName.ifEmpty { "User" },
                                userEmail    = authState.email,
                            )
                        }
                    }
                }

                Screen.AdminHome -> {
                    NTTheme {
                        AdminDashboardScreen(
                            vm           = adminVm,
                            onLogout     = {
                                adminVm.clearState()
                                employeeVm.clearState()
                                authVm.logout()
                                screen = Screen.Login
                            },
                            adminName    = authState.displayName.ifEmpty { "Admin" },
                            adminEmail   = authState.email,
                        )
                    }
                }

                Screen.EmployeeHome -> {
                    NTTheme {
                        RuwiaTheme {
                            EmployeeDashboardScreen(
                                vm           = employeeVm,
                                onLogout     = {
                                    adminVm.clearState()
                                    employeeVm.clearState()
                                    authVm.logout()
                                    screen = Screen.Login
                                },
                                employeeName = authState.displayName.ifEmpty { "Employee" },
                                userEmail    = authState.email,
                            )
                        }
                    }
                }
            }

            if (showNoInternet) {
                NoInternetScreen(
                    onRetry = {
                        if (isConnected) {
                            showNoInternet = false
                        }
                    },
                    onDismiss = {
                        showNoInternet = false
                    }
                )
            }
        }
    }
}