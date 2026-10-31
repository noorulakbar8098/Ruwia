package com.example.ruwia.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

enum class LocationPermissionStatus {
    GRANTED,
    NOT_DETERMINED,
    DENIED,
    PERMANENTLY_DENIED
}

interface LocationPermissionController {
    val status: LocationPermissionStatus
    fun requestPermission()
    fun openAppSettings()
}

@Composable
expect fun rememberLocationPermissionController(
    onPermissionResult: (Boolean) -> Unit = {}
): LocationPermissionController

@Composable
fun LocationPermissionRationaleDialog(
    status: LocationPermissionStatus,
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val isPermanentlyDenied = status == LocationPermissionStatus.PERMANENTLY_DENIED

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (isPermanentlyDenied) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPermanentlyDenied) Icons.Rounded.Settings else Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = if (isPermanentlyDenied) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = if (isPermanentlyDenied) "Location Permission Required" else "Location Access Needed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isPermanentlyDenied) {
                        "Location permission was permanently denied. Ruwia needs location access while navigating to record journey checkpoints, show live arrival ETA, and detect arrival at the shop. Please open App Settings and allow Location permission."
                    } else {
                        "Ruwia uses your GPS location while you travel for an assigned delivery to record journey checkpoints along your route, update arrival ETA for the admin, and automatically detect when you reach the shop."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = Color(0xFF555555)),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Not Now")
                    }

                    Button(
                        onClick = {
                            if (isPermanentlyDenied) {
                                onOpenSettings()
                            } else {
                                onRequestPermission()
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPermanentlyDenied) Color(0xFF1976D2) else Color(0xFF2E7D32)
                        )
                    ) {
                        Text(if (isPermanentlyDenied) "Open Settings" else "Allow Access")
                    }
                }
            }
        }
    }
}
