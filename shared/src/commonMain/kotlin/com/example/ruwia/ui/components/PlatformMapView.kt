package com.example.ruwia.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformMapView(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Int = 16,
    onMapMoved: (latitude: Double, longitude: Double) -> Unit,
)
