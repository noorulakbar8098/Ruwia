package com.example.ruwia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.TaskTimelinePoint
import com.example.ruwia.ui.dashboard.NTColors

@Composable
fun DeliveryTimelineView(
    timeline: List<TaskTimelinePoint>,
    status: String,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = true,
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    val pointsCount = timeline.size
    val isCompleted = status.equals("completed", ignoreCase = true) || status.equals("delivered", ignoreCase = true)
    val isInTransit = status.equals("in_transit", ignoreCase = true) || status.equals("accepted", ignoreCase = true)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NTColors.SurfaceVar.copy(alpha = 0.5f))
            .border(1.dp, NTColors.Border.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Timeline,
                    contentDescription = null,
                    tint = if (isInTransit) NTColors.Primary else if (isCompleted) NTColors.Success else NTColors.TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Delivery Timeline",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NTColors.TextPrimary,
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(NTColors.PrimaryLight)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (pointsCount < 10) "$pointsCount/10 Points" else "10 Points · Dynamic",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NTColors.Primary,
                    )
                }
            }

            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = NTColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
        }

        if (expanded) {
            Spacer(Modifier.height(10.dp))

            if (timeline.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = if (status.equals("pending", ignoreCase = true)) {
                            "Route not started yet · Waiting for delivery person to accept"
                        } else {
                            "No location timeline points recorded yet"
                        },
                        fontSize = 12.sp,
                        color = NTColors.TextTertiary,
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    timeline.forEachIndexed { index, point ->
                        val isFirst = index == 0
                        val isLast = index == timeline.lastIndex
                        val isDestination = point.status.equals("completed", ignoreCase = true) || isLast && isCompleted

                        TimelineNodeItem(
                            index = index,
                            point = point,
                            isFirst = isFirst,
                            isLast = isLast && (!isInTransit || pointsCount >= 10),
                            isDestination = isDestination,
                        )
                    }

                    // If still traveling and has fewer than 10 points, show the next slot
                    if (isInTransit && pointsCount < 10) {
                        UpcomingTimelineSlot(nextPointIndex = pointsCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineNodeItem(
    index: Int,
    point: TaskTimelinePoint,
    isFirst: Boolean,
    isLast: Boolean,
    isDestination: Boolean,
) {
    val nodeColor = when {
        isDestination -> NTColors.Success
        isFirst -> Color(0xFF0D9488) // Teal
        else -> NTColors.Primary
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Track: Circle dot and connecting vertical line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(nodeColor.copy(alpha = 0.15f))
                    .border(1.5.dp, nodeColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (isDestination) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = nodeColor,
                        modifier = Modifier.size(12.dp)
                    )
                } else if (isFirst) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = nodeColor,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Text(
                        text = (index + 1).toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = nodeColor,
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(38.dp)
                        .background(nodeColor.copy(alpha = 0.35f))
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 2.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = point.label.ifBlank { "Point ${index + 1}" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NTColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (point.recordedAt.isNotBlank()) {
                    Text(
                        text = point.recordedAt,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = NTColors.TextTertiary,
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            // Place name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = nodeColor,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = point.placeName.ifBlank { "Location captured" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (point.placeName.isNotBlank()) NTColors.TextPrimary else NTColors.TextTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Optional Coordinates & Accuracy
            if (point.latitude != null && point.longitude != null) {
                val latFormatted = ((point.latitude * 10000).toLong() / 10000.0).toString()
                val lonFormatted = ((point.longitude * 10000).toLong() / 10000.0).toString()
                val accuracySuffix = if (point.accuracyMeters != null) " · ±${point.accuracyMeters.toInt()}m" else ""
                Text(
                    text = "($latFormatted, $lonFormatted)$accuracySuffix",
                    fontSize = 10.sp,
                    color = NTColors.TextTertiary,
                    modifier = Modifier.padding(start = 17.dp, top = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun UpcomingTimelineSlot(nextPointIndex: Int) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(NTColors.SurfaceVar)
                    .border(1.dp, NTColors.Border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (nextPointIndex + 1).toString(),
                    fontSize = 9.sp,
                    color = NTColors.TextTertiary,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Next Checkpoint (${nextPointIndex + 1}/10) · In Transit",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = NTColors.TextTertiary,
            )
        }
    }
}
