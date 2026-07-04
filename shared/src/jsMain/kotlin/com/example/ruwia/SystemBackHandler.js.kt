package com.example.ruwia

import androidx.compose.runtime.Composable

@Composable
actual fun SystemBackHandler(onBack: () -> Unit) {
    // Web — browser history API handles back; no override needed here
}
