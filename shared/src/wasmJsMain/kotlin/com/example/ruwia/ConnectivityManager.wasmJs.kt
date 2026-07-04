package com.example.ruwia

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WasmConnectivityObserver : ConnectivityObserver {
    override val isConnected: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
}

actual fun getConnectivityObserver(): ConnectivityObserver = WasmConnectivityObserver()
