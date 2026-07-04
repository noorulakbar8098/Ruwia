package com.example.ruwia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

@Composable
expect fun rememberLifecycleObserver(): State<LifecycleState>

enum class LifecycleState {
    ON_CREATE,
    ON_START,
    ON_RESUME,
    ON_PAUSE,
    ON_STOP,
    ON_DESTROY
}