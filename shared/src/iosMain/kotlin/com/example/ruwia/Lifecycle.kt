package com.example.ruwia

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

@Composable
actual fun rememberLifecycleObserver(): State<LifecycleState> {
    val lifecycleState = remember { mutableStateOf(LifecycleState.ON_RESUME) }

    DisposableEffect(Unit) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            UIApplicationDidBecomeActiveNotification,
            null,
            NSOperationQueue.mainQueue
        ) {
            lifecycleState.value = LifecycleState.ON_RESUME
        }

        val observer2 = NSNotificationCenter.defaultCenter.addObserverForName(
            UIApplicationWillResignActiveNotification,
            null,
            NSOperationQueue.mainQueue
        ) {
            lifecycleState.value = LifecycleState.ON_PAUSE
        }

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            NSNotificationCenter.defaultCenter.removeObserver(observer2)
        }
    }

    return lifecycleState
}