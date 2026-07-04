package com.example.ruwia

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JvmConnectivityObserver : ConnectivityObserver {
    override val isConnected: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
}

actual fun getConnectivityObserver(): ConnectivityObserver = JvmConnectivityObserver()
