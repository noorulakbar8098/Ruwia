package com.example.ruwia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.data.getLocationService
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InteractiveMapPickerCard(
    latitude: Double,
    longitude: Double,
    address: String,
    onLocationChanged: (latitude: Double, longitude: Double, address: String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
    onExpandFullScreen: (() -> Unit)? = null,
) {
    val locationService = remember { getLocationService() }
    val coroutineScope = rememberCoroutineScope()

    var isReverseGeocoding by remember { mutableStateOf(false) }
    var isFetchingGps by remember { mutableStateOf(false) }
    var displayAddress by remember { mutableStateOf(address) }

    // When the external address changes (e.g. from typing / suggestion selection), update display
    LaunchedEffect(address) {
        if (address.isNotBlank()) {
            displayAddress = address
        }
    }

    var reverseGeocodeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Debounce reverse geocode when user manually drags the map
    fun onMapMovedInternal(lat: Double, lng: Double) {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = coroutineScope.launch {
            delay(500)
            isReverseGeocoding = true
            try {
                val addr = locationService.reverseGeocode(lat, lng)
                val finalAddr = if (addr.isNotBlank() && !addr.startsWith("Point")) {
                    addr
                } else {
                    val latStr = ((lat * 10000).toLong() / 10000.0).toString()
                    val lonStr = ((lng * 10000).toLong() / 10000.0).toString()
                    "$latStr, $lonStr"
                }
                displayAddress = finalAddr
                onLocationChanged(lat, lng, finalAddr)
            } catch (_: Exception) {
            } finally {
                isReverseGeocoding = false
            }
        }
    }

    fun fetchDeviceGps() {
        coroutineScope.launch {
            isFetchingGps = true
            try {
                val loc = locationService.getCurrentLocation()
                if (loc != null) {
                    val addr = locationService.reverseGeocode(loc.latitude, loc.longitude)
                    val finalAddr = if (addr.isNotBlank() && !addr.startsWith("Point")) addr else "${loc.latitude}, ${loc.longitude}"
                    displayAddress = finalAddr
                    onLocationChanged(loc.latitude, loc.longitude, finalAddr)
                }
            } catch (_: Exception) {
            } finally {
                isFetchingGps = false
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, NTColors.Border, RoundedCornerShape(16.dp)),
        color = NTColors.Surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = NTDp.cardElevation,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Interactive Map area with centered Red Pin
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                PlatformMapView(
                    modifier = Modifier.fillMaxSize(),
                    latitude = latitude,
                    longitude = longitude,
                    zoom = 16,
                    onMapMoved = { lat, lng ->
                        onMapMovedInternal(lat, lng)
                    }
                )

                // Top Badge: Drag hint
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📍 Place red point on the delivery spot",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Top Right Action Buttons: GPS and Fullscreen
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Current GPS Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(NTColors.Surface)
                            .clickable(enabled = !isFetchingGps) { fetchDeviceGps() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFetchingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = NTColors.Primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MyLocation,
                                contentDescription = "Use GPS",
                                tint = NTColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Fullscreen Button
                    if (onExpandFullScreen != null) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(NTColors.Surface)
                                .clickable { onExpandFullScreen() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "Expand Map",
                                tint = NTColors.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Center Red Pin / Red Point Marker
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-18).dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(8.dp, CircleShape)
                            .background(Color(0xFFEF4444), CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Bottom point/needle of the pin
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .offset(y = (-2).dp)
                            .background(Color(0xFFEF4444), CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                    )
                }
            }

            // Bottom Info Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NTColors.SurfaceVar)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val latStr = ((latitude * 10000).toLong() / 10000.0).toString()
                val lonStr = ((longitude * 10000).toLong() / 10000.0).toString()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NTColors.Primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "📍 $latStr, $lonStr",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NTColors.Primary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = displayAddress.ifBlank { "Moving to location…" },
                        fontSize = 12.sp,
                        color = NTColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isReverseGeocoding) {
                    Spacer(Modifier.width(6.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = NTColors.TextTertiary
                    )
                }
            }
        }
    }
}
