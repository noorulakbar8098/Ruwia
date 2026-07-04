package com.example.ruwia.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.theme.RuwiaColor

@Composable
fun SaaSLoadingOverlay(
    message: String = "Syncing Database"
) {
    val infiniteTransition = rememberInfiniteTransition()

    // 1. Slow, elegant outer circle rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // 2. Fast, snappy inner icon rotation (opposite direction)
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // 3. Gentle scaling pulse for the central icon
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 4. Shimmering dots animation
    val dotCount by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 4,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val dots = ".".repeat(dotCount).padEnd(3, ' ')

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)) // Blocking dark overlay
            .clickable(
                enabled = false,
                onClick = {}
            ), // Prevent any clicks underneath
        contentAlignment = Alignment.Center
    ) {
        // Glowing SaaS card container
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = RuwiaColor.Surface,
            tonalElevation = 12.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 300.dp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Premium rotating indicator
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer progress circle
                    CircularProgressIndicator(
                        color = RuwiaColor.TealPrimary,
                        trackColor = RuwiaColor.TealPrimary.copy(alpha = 0.15f),
                        strokeWidth = 3.5.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(outerRotation)
                    )
                    
                    // Secondary inner circle rotating in reverse
                    CircularProgressIndicator(
                        color = RuwiaColor.TealPrimary.copy(alpha = 0.4f),
                        trackColor = Color.Transparent,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(52.dp)
                            .rotate(innerRotation)
                    )

                    // Central pulsing sync icon
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        tint = RuwiaColor.TealPrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(iconScale)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title with shimmering dots
                Text(
                    text = "$message$dots",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = RuwiaColor.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = "Please wait, syncing in real-time",
                    fontSize = 12.sp,
                    color = RuwiaColor.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
