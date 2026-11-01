package com.example.ruwia.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.domain.TaskTimelinePoint

@Composable
fun DeliveryTimelineView(
    task: DeliveryTask,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = task.timeline.isNotEmpty(),
) {
    DeliveryTimelineView(
        timeline = task.timeline,
        status = task.status,
        modifier = modifier,
        initialExpanded = initialExpanded,
        destinationAddress = task.address.ifBlank { task.customerAddress.orEmpty() },
        destinationName = task.destName?.takeIf { it.isNotBlank() } ?: task.customerName,
        destLatitude = task.destLatitude,
        destLongitude = task.destLongitude,
        arrivalTime = task.arrivalTime,
    )
}

@Composable
fun DeliveryTimelineView(
    timeline: List<TaskTimelinePoint>,
    status: String,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = true,
    destinationAddress: String = "",
    destinationName: String = "",
    destLatitude: Double? = null,
    destLongitude: Double? = null,
    arrivalTime: String? = null,
) {
    var expanded by remember(timeline.isNotEmpty()) { mutableStateOf(initialExpanded) }
    val isCompleted = status.equals("completed", ignoreCase = true) ||
        status.equals("done", ignoreCase = true) ||
        status.equals("delivered", ignoreCase = true)
    val isArrived = status.equals("arrived", ignoreCase = true)
    val isCancelled = status.equals("cancelled", ignoreCase = true) ||
        status.equals("canceled", ignoreCase = true)
    val isInTransit = status.equals("in_transit", ignoreCase = true) ||
        status.equals("accepted", ignoreCase = true)
    val accent = if (isCompleted || isArrived) TimelinePalette.Green else TimelinePalette.Orange
    val countLabel = when {
        timeline.isEmpty() -> "No updates"
        timeline.size == 1 -> "1 update"
        else -> "${timeline.size} updates"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(18.dp))
            .background(TimelinePalette.Surface)
            .border(1.dp, TimelinePalette.Border, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(TimelinePalette.SoftNeutral),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Journey activity",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TimelinePalette.Ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        isCompleted -> "Delivery completed"
                        isArrived -> "Arrived at destination"
                        isCancelled -> "Route cancelled"
                        isInTransit -> "Location updates from the route"
                        else -> "Updates will appear once the driver starts the route"
                    },
                    fontSize = 11.sp,
                    color = TimelinePalette.SecondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                countLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TimelinePalette.SecondaryInk,
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse journey activity" else "Expand journey activity",
                tint = TimelinePalette.SecondaryInk,
                modifier = Modifier.padding(start = 4.dp).size(20.dp),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                val hasTimeline = timeline.isNotEmpty()
                val hasDestination = destinationAddress.isNotBlank() || destinationName.isNotBlank() || destLatitude != null

                if (!hasTimeline) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TimelinePalette.SoftNeutral)
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                    ) {
                        Text(
                            if (isCompleted) {
                                "The route finished without saved location updates."
                            } else if (isArrived) {
                                "The delivery person has reached the destination."
                            } else if (isCancelled) {
                                "This delivery will not continue."
                            } else if (isInTransit) {
                                "Waiting for the next location update."
                            } else {
                                "Updates will show here once the driver starts the route."
                            },
                            fontSize = 12.sp,
                            color = TimelinePalette.SecondaryInk,
                        )
                    }
                    if (hasDestination) {
                        Spacer(Modifier.height(12.dp))
                        DestinationTimelinePointRow(
                            address = destinationAddress,
                            destinationName = destinationName,
                            destLatitude = destLatitude,
                            destLongitude = destLongitude,
                            completionTime = arrivalTime.orEmpty(),
                            isCompleted = isCompleted || isArrived,
                        )
                    }
                } else {
                    val lastPoint = timeline.last()
                    val isLastPointCompletion = lastPoint.status.equals("completed", ignoreCase = true) ||
                        lastPoint.status.equals("arrived", ignoreCase = true) ||
                        (isCompleted && timeline.size > 1 && lastPoint.label.contains("completed", ignoreCase = true))

                    val startPoint = timeline.first()
                    val intermediatePoints = if (isLastPointCompletion && timeline.size > 1) {
                        timeline.subList(1, timeline.lastIndex)
                    } else {
                        timeline.subList(1, timeline.size)
                    }
                    val completionPoint = if (isLastPointCompletion) lastPoint else null

                    // 1. Start point: Green point, bold address, small remaining text font
                    StartTimelinePointRow(
                        point = startPoint,
                        hasNext = true,
                    )

                    // 2. Intermediate points: Green tick marks, normal address, small remaining text font
                    intermediatePoints.forEach { checkpoint ->
                        CheckpointTimelinePointRow(
                            point = checkpoint,
                            hasNext = true,
                        )
                    }

                    // 3. Destination point: Red point at the last, red destination address, small remaining font
                    val finalDestAddress = destinationAddress.ifBlank {
                        completionPoint?.placeName.orEmpty().ifBlank { "Destination" }
                    }
                    val finalCompletionTime = completionPoint?.recordedAt?.takeIf { it.isNotBlank() } ?: arrivalTime.orEmpty()
                    val finalLat = destLatitude ?: completionPoint?.latitude
                    val finalLon = destLongitude ?: completionPoint?.longitude

                    DestinationTimelinePointRow(
                        address = finalDestAddress,
                        destinationName = destinationName,
                        destLatitude = finalLat,
                        destLongitude = finalLon,
                        completionTime = finalCompletionTime,
                        isCompleted = isCompleted || isArrived || completionPoint != null,
                    )
                }
            }
        }
    }
}

@Composable
private fun StartTimelinePointRow(
    point: TaskTimelinePoint,
    hasNext: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Column(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(TimelinePalette.Green),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
            if (hasNext) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(TimelinePalette.Green),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 7.dp, bottom = if (hasNext) 12.dp else 2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = point.placeName.ifBlank { "Starting location" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TimelinePalette.Ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (point.recordedAt.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = point.recordedAt,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TimelinePalette.SecondaryInk,
                        maxLines = 1,
                    )
                }
            }
            if (point.latitude != null && point.longitude != null) {
                val latitude = ((point.latitude * 10000).toLong() / 10000.0).toString()
                val longitude = ((point.longitude * 10000).toLong() / 10000.0).toString()
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Coordinates: $latitude, $longitude",
                    fontSize = 10.sp,
                    color = TimelinePalette.SecondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            point.accuracyMeters?.let { accuracy ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "GPS accuracy: ±${accuracy.toInt()} m",
                    fontSize = 10.sp,
                    color = TimelinePalette.SecondaryInk,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CheckpointTimelinePointRow(
    point: TaskTimelinePoint,
    hasNext: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Column(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(TimelinePalette.Green),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Reached checkpoint",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
            }
            if (hasNext) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(TimelinePalette.Green),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 7.dp, bottom = if (hasNext) 12.dp else 2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = point.placeName.ifBlank { "Location captured" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TimelinePalette.Ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (point.recordedAt.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = point.recordedAt,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TimelinePalette.SecondaryInk,
                        maxLines = 1,
                    )
                }
            }
            if (point.latitude != null && point.longitude != null) {
                val latitude = ((point.latitude * 10000).toLong() / 10000.0).toString()
                val longitude = ((point.longitude * 10000).toLong() / 10000.0).toString()
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Coordinates: $latitude, $longitude",
                    fontSize = 10.sp,
                    color = TimelinePalette.SecondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            point.accuracyMeters?.let { accuracy ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "GPS accuracy: ±${accuracy.toInt()} m",
                    fontSize = 10.sp,
                    color = TimelinePalette.SecondaryInk,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DestinationTimelinePointRow(
    address: String,
    destinationName: String,
    destLatitude: Double?,
    destLongitude: Double?,
    completionTime: String,
    isCompleted: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Column(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(TimelinePalette.Red),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 7.dp, bottom = 2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = address.ifBlank { "Destination location" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TimelinePalette.Red,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (completionTime.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = completionTime,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TimelinePalette.SecondaryInk,
                        maxLines = 1,
                    )
                } else if (isCompleted) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Arrived",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TimelinePalette.Green,
                        maxLines = 1,
                    )
                }
            }
            if (destinationName.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = destinationName,
                    fontSize = 10.sp,
                    color = TimelinePalette.SecondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (destLatitude != null && destLongitude != null) {
                val latitude = ((destLatitude * 10000).toLong() / 10000.0).toString()
                val longitude = ((destLongitude * 10000).toLong() / 10000.0).toString()
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Coordinates: $latitude, $longitude",
                    fontSize = 10.sp,
                    color = TimelinePalette.SecondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private object TimelinePalette {
    val Surface = Color.White
    val SoftNeutral = Color(0xFFF7F5F2)
    val Border = Color(0xFFECE8E3)
    val Ink = Color(0xFF252321)
    val SecondaryInk = Color(0xFF77716B)
    val Orange = Color(0xFFE86D2F)
    val Green = Color(0xFF26834A)
    val Red = Color(0xFFDC2626)
    val TimelineLine = Color(0xFFB8DCC2)
}
