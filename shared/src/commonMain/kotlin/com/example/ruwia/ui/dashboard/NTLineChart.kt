package com.example.ruwia.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

// ── Canvas-based smooth Bezier line chart ──────────────────────────────────

@Composable
fun NTLineChartCard(
    title: String,
    valueLabel: String,
    growthPercent: Double,
    points: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    rawValues: List<Double>? = null,
    xRawLabels: List<String>? = null,
    valueFormatter: ((Double) -> String)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(NTDp.cardPad)) {
            val isPos = growthPercent >= 0.5
            val isNeg = growthPercent <= -0.5
            val badgeBgColor = when {
                isPos -> NTColors.SuccessLight
                isNeg -> NTColors.ErrorLight
                else -> NTColors.SurfaceVar
            }
            val badgeTextColor = when {
                isPos -> NTColors.SuccessText
                isNeg -> NTColors.ErrorText
                else -> NTColors.TextSecondary
            }
            val badgeIconColor = when {
                isPos -> NTColors.Success
                isNeg -> NTColors.Error
                else -> NTColors.TextTertiary
            }
            val badgeIcon = when {
                isPos -> Icons.Rounded.TrendingUp
                isNeg -> Icons.Rounded.TrendingDown
                else -> Icons.Rounded.Remove
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        color = NTColors.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = valueLabel,
                        color = NTColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Growth badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(NTDp.radFull))
                        .background(badgeBgColor)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeIconColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${if (isPos) "+" else ""}${growthPercent.toInt()}%",
                        color = badgeTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(NTDp.md))

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(NTDp.chartH),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No chart data", color = NTColors.TextTertiary, fontSize = 13.sp)
                }
            } else {
                NTLineChart(
                    points = points,
                    labels = labels,
                    rawValues = rawValues,
                    xRawLabels = xRawLabels,
                    valueFormatter = valueFormatter
                )
            }
        }
    }
}

@Composable
fun NTLineChart(
    points: List<Float>,
    labels: List<String>,
    height: Dp = NTDp.chartH,
    modifier: Modifier = Modifier,
    rawValues: List<Double>? = null,
    xRawLabels: List<String>? = null,
    valueFormatter: ((Double) -> String)? = null
) {
    val textMeasurer = rememberTextMeasurer()
    var activeIndex by remember { mutableStateOf<Int?>(null) }
    
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(points) {
                    detectTapGestures(
                        onPress = { offset ->
                            val n = points.size
                            if (n > 0) {
                                val w = size.width
                                val i = if (n > 1) {
                                    ((offset.x / w) * (n - 1)).roundToInt().coerceIn(0, n - 1)
                                } else {
                                    0
                                }
                                activeIndex = i
                                val released = tryAwaitRelease()
                                if (released) {
                                    delay(2000)
                                    if (activeIndex == i) {
                                        activeIndex = null
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val padTop = 16f
            val drawH = h - padTop
            val n = points.size

            val pts = points.mapIndexed { i, v ->
                Offset(
                    x = if (n > 1) i.toFloat() / (n - 1) * w else w / 2f,
                    y = padTop + drawH - v.coerceIn(0f, 1f) * drawH
                )
            }

            // Gradient fill under the curve
            val fillPath = smoothPath(pts)
            fillPath.lineTo(pts.last().x, h)
            fillPath.lineTo(pts.first().x, h)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(NTColors.ChartFillTop, NTColors.ChartFillBot),
                    startY = padTop,
                    endY = h
                )
            )

            // Curve line
            drawPath(
                path = smoothPath(pts),
                color = NTColors.ChartLine,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Highlight dot on last point
            val last = pts.last()
            drawCircle(color = NTColors.ChartLine, radius = 5.dp.toPx(), center = last)
            drawCircle(color = Color.White,        radius = 3.dp.toPx(), center = last)

            // Tooltip drawing
            if (activeIndex != null && activeIndex!! in points.indices) {
                val idx = activeIndex!!
                val pt = pts[idx]
                val rawVal = rawValues?.getOrNull(idx)
                val labelVal = xRawLabels?.getOrNull(idx)
                
                val valStr = if (rawVal != null) {
                    if (valueFormatter != null) {
                        valueFormatter(rawVal)
                    } else {
                        if (rawVal >= 1000) "₹${formatValue(rawVal)}" else "₹${rawVal.toInt()}"
                    }
                } else {
                    "${(points[idx] * 100).toInt()}%"
                }
                
                val tooltipText = if (labelVal != null && labelVal.isNotEmpty()) {
                    "$labelVal: $valStr"
                } else {
                    valStr
                }
                
                val tooltipLayoutResult = textMeasurer.measure(
                    text = tooltipText,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                
                val tooltipWidth = tooltipLayoutResult.size.width + 16.dp.toPx()
                val tooltipHeight = tooltipLayoutResult.size.height + 8.dp.toPx()
                
                val tooltipLeft = (pt.x - tooltipWidth / 2f).coerceIn(4.dp.toPx(), w - tooltipWidth - 4.dp.toPx())
                val tooltipTop = (pt.y - tooltipHeight - 12.dp.toPx()).coerceAtLeast(4.dp.toPx())
                
                // Tooltip background
                drawRoundRect(
                    color = Color(0xFF191D30),
                    topLeft = Offset(tooltipLeft, tooltipTop),
                    size = Size(tooltipWidth, tooltipHeight),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
                
                // Tooltip arrow
                val arrowCenterX = pt.x.coerceIn(tooltipLeft + 8.dp.toPx(), tooltipLeft + tooltipWidth - 8.dp.toPx())
                val arrowY = tooltipTop + tooltipHeight
                val arrowPath = Path().apply {
                    moveTo(arrowCenterX - 4.dp.toPx(), arrowY)
                    lineTo(arrowCenterX + 4.dp.toPx(), arrowY)
                    lineTo(arrowCenterX, arrowY + 4.dp.toPx())
                    close()
                }
                drawPath(arrowPath, color = Color(0xFF191D30))
                
                // Tooltip text
                drawText(
                    textLayoutResult = tooltipLayoutResult,
                    topLeft = Offset(
                        x = tooltipLeft + 8.dp.toPx(),
                        y = tooltipTop + 4.dp.toPx()
                    )
                )
                
                // Highlight circle on touched point
                drawCircle(color = NTColors.ChartLine, radius = 6.dp.toPx(), center = pt)
                drawCircle(color = Color.White,        radius = 4.dp.toPx(), center = pt)
            }
        }

        Spacer(modifier = Modifier.height(NTDp.sm))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(text = label, color = NTColors.ChartLabel, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun formatValue(v: Double): String = when {
    v >= 100000 -> "${(v / 100000 * 100).toInt() / 100.0}L"
    v >= 1000   -> "${(v / 1000).toInt()}K"
    else        -> "${v.toInt()}"
}

private fun smoothPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts.first().x, pts.first().y)
    for (i in 1 until pts.size) {
        val prev = pts[i - 1]
        val curr = pts[i]
        val midX = (prev.x + curr.x) / 2f
        path.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
    }
    return path
}
