package com.example.ruwia

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.browser.window

class JsConnectivityObserver : ConnectivityObserver {
    private val _isConnected = MutableStateFlow(window.navigator.onLine)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        window.addEventListener("online", { _isConnected.value = true })
        window.addEventListener("offline", { _isConnected.value = false })
    }
}

actual fun getConnectivityObserver(): ConnectivityObserver = JsConnectivityObserver()
