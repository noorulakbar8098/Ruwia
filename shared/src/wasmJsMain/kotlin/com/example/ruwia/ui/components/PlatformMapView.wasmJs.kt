package com.example.ruwia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Int,
    onMapMoved: (latitude: Double, longitude: Double) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color(0xFFE5E3DF)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Map Preview: $latitude, $longitude",
            color = Color.DarkGray,
            fontSize = 12.sp,
        )
    }
}
