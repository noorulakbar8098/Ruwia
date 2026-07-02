package com.example.ruwia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
actual fun rememberLifecycleObserver(): State<LifecycleState> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState = remember { mutableStateOf(LifecycleState.ON_CREATE) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycleState.value = when (event) {
                Lifecycle.Event.ON_CREATE -> LifecycleState.ON_CREATE
                Lifecycle.Event.ON_START -> LifecycleState.ON_START
                Lifecycle.Event.ON_RESUME -> LifecycleState.ON_RESUME
                Lifecycle.Event.ON_PAUSE -> LifecycleState.ON_PAUSE
                Lifecycle.Event.ON_STOP -> LifecycleState.ON_STOP
                Lifecycle.Event.ON_DESTROY -> LifecycleState.ON_DESTROY
                else -> lifecycleState.value
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return lifecycleState
}