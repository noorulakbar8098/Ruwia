package com.example.ruwia.data

import com.example.ruwia.presentation.AdminViewModel
import com.example.ruwia.presentation.AuthViewModel
import com.example.ruwia.presentation.EmployeeViewModel
import com.example.ruwia.presentation.UserViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val appModule = module {
    single { AuthRepository() }
    single { CustomerRepository(get()) }
    single { AdminRepository() }
    single { EmployeeRepository() }
    single { AuthViewModel(get()) }
    factory { UserViewModel(get(), get()) }
    factory { AdminViewModel(get()) }
    factory { EmployeeViewModel(get()) }
}

// Guard against double-init (iOS re-entry, Activity recreation on Android)
private var koinStarted = false

fun initKoin(config: KoinAppDeclaration? = null) {
    if (koinStarted) return
    koinStarted = true
    startKoin {
        config?.invoke(this)
        modules(appModule)
    }
}
