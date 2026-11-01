package com.example.ruwia.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ruwia.data.getLocationService
import com.example.ruwia.ui.dashboard.NTColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InteractiveMapLocationDialog(
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    initialAddress: String = "",
    onConfirmLocation: (latitude: Double, longitude: Double, address: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val locationService = remember { getLocationService() }
    val coroutineScope = rememberCoroutineScope()

    // Default to Bengaluru/Chennai or provided coords
    var currentLat by remember { mutableStateOf(initialLatitude ?: 12.9716) }
    var currentLng by remember { mutableStateOf(initialLongitude ?: 77.5946) }
    var resolvedAddress by remember { mutableStateOf(initialAddress) }
    var isReverseGeocoding by remember { mutableStateOf(false) }
    var isFetchingGps by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }

    // If initial coordinates weren't supplied, try fetching device GPS location
    LaunchedEffect(Unit) {
        if (initialLatitude == null || initialLongitude == null) {
            isFetchingGps = true
            try {
                val loc = locationService.getCurrentLocation()
                if (loc != null) {
                    currentLat = loc.latitude
                    currentLng = loc.longitude
                    val addr = locationService.reverseGeocode(loc.latitude, loc.longitude)
                    if (addr.isNotBlank() && !addr.startsWith("Point")) {
                        resolvedAddress = addr
                    }
                }
            } catch (_: Exception) {
            } finally {
                isFetchingGps = false
            }
        } else if (initialAddress.isBlank()) {
            isReverseGeocoding = true
            try {
                val addr = locationService.reverseGeocode(currentLat, currentLng)
                if (addr.isNotBlank() && !addr.startsWith("Point")) {
                    resolvedAddress = addr
                }
            } catch (_: Exception) {
            } finally {
                isReverseGeocoding = false
            }
        }
    }

    // Debounced reverse geocoding when coordinates change from dragging the map
    LaunchedEffect(currentLat, currentLng) {
        delay(400) // 400ms debounce
        isReverseGeocoding = true
        try {
            val addr = locationService.reverseGeocode(currentLat, currentLng)
            if (addr.isNotBlank() && !addr.startsWith("Point")) {
                resolvedAddress = addr
            } else {
                val latStr = ((currentLat * 10000).toLong() / 10000.0).toString()
                val lonStr = ((currentLng * 10000).toLong() / 10000.0).toString()
                resolvedAddress = "$latStr, $lonStr"
            }
        } catch (_: Exception) {
        } finally {
            isReverseGeocoding = false
        }
    }

    fun fetchCurrentGpsLocation() {
        coroutineScope.launch {
            isFetchingGps = true
            try {
                val loc = locationService.getCurrentLocation()
                if (loc != null) {
                    currentLat = loc.latitude
                    currentLng = loc.longitude
                }
            } catch (_: Exception) {
            } finally {
                isFetchingGps = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(22.dp),
                color = NTColors.Surface,
                shadowElevation = 16.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, NTColors.Border)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFEE2E2), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Select Exact Location",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NTColors.TextPrimary
                                )
                                Text(
                                    text = "Place the red point on the delivery spot",
                                    fontSize = 11.sp,
                                    color = NTColors.TextTertiary
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = NTColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 2. Search Bar for jumping map directly to a place
                    Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                        AddressAutoCompleteField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = "Search area / street to jump on map…",
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = NTColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onSuggestionSelected = { suggestion ->
                                searchInput = suggestion.title
                                currentLat = suggestion.latitude
                                currentLng = suggestion.longitude
                                resolvedAddress = suggestion.fullAddress
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // 3. Map Container with centered Red Pin Marker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        PlatformMapView(
                            modifier = Modifier.fillMaxSize(),
                            latitude = currentLat,
                            longitude = currentLng,
                            zoom = 17,
                            onMapMoved = { lat, lng ->
                                currentLat = lat
                                currentLng = lng
                            }
                        )

                        // Floating Hint at top of map
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "📍 Drag map to position the red point",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Floating GPS Button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .shadow(6.dp, CircleShape)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NTColors.Surface)
                                .clickable(enabled = !isFetchingGps) { fetchCurrentGpsLocation() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFetchingGps) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = NTColors.Primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.MyLocation,
                                    contentDescription = "My Location",
                                    tint = NTColors.Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Center Red Pin / Red Point Marker
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-20).dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .shadow(8.dp, CircleShape)
                                    .background(Color(0xFFEF4444), CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            // Bottom needle / point of the pin
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .offset(y = (-2).dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                    }

                    // 4. Bottom Details Card & Confirm Button
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = NTColors.Surface,
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NTColors.Border.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val latFormatted = ((currentLat * 10000).toLong() / 10000.0).toString()
                                    val lonFormatted = ((currentLng * 10000).toLong() / 10000.0).toString()
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NTColors.Primary.copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "📍 $latFormatted, $lonFormatted",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NTColors.Primary
                                        )
                                    }
                                }

                                if (isReverseGeocoding) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.5.dp,
                                            color = NTColors.TextTertiary
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Finding address…",
                                            fontSize = 11.sp,
                                            color = NTColors.TextTertiary
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = resolvedAddress.ifBlank { "Moving to location…" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NTColors.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel", color = NTColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        val finalAddr = resolvedAddress.trim().ifEmpty {
                                            val latStr = ((currentLat * 10000).toLong() / 10000.0).toString()
                                            val lonStr = ((currentLng * 10000).toLong() / 10000.0).toString()
                                            "$latStr, $lonStr"
                                        }
                                        onConfirmLocation(currentLat, currentLng, finalAddr)
                                    },
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary)
                                ) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Confirm Location",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
