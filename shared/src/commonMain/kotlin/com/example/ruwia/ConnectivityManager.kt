package com.example.ruwia

import kotlinx.coroutines.flow.StateFlow

interface ConnectivityObserver {
    val isConnected: StateFlow<Boolean>
}

expect fun getConnectivityObserver(): ConnectivityObserver
