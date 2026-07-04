package com.example.ruwia

import androidx.compose.runtime.Composable

@Composable
actual fun SystemBackHandler(onBack: () -> Unit) {
    // iOS uses native swipe-back gesture — no override needed
}
