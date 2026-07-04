package com.example.ruwia

import androidx.compose.runtime.Composable

/**
 * KMP-safe hardware back-button handler.
 * On Android → intercepts the system back press.
 * On iOS / Desktop / Web → no-op (platforms use native swipe-back).
 */
@Composable
expect fun SystemBackHandler(onBack: () -> Unit)
