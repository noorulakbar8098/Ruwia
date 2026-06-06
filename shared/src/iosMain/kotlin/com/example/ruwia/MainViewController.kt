package com.example.ruwia

import androidx.compose.ui.window.ComposeUIViewController
import com.example.ruwia.data.initKoin

fun MainViewController() = ComposeUIViewController {
    initKoin()
    App()
}