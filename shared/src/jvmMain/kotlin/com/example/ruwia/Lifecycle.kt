package com.example.ruwia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

@Composable
actual fun rememberLifecycleObserver(): State<LifecycleState> {
    return mutableStateOf(LifecycleState.ON_RESUME)
}