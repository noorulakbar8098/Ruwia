package com.example.ruwia.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    valueFormatter: ((Double) -> String)? = null,
    /** When non-null, a small arrow button is shown on the right that invokes it. */
    onViewAllClick: (() -> Unit)? = null,
    viewAllLabel: String = "View all",
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        border = BorderStroke(1.dp, NTColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
            // Explicit meaning: +X% = growth, -X% = decline, 0% = flat.
            // The sign is never dropped — a decline must not read as growth.
            val badgeText = when {
                isPos -> "+${abs(growthPercent).toInt()}%"
                isNeg -> "-${abs(growthPercent).toInt()}%"
                else  -> "0%"
            }
            val badgeDesc = when {
                isPos -> "Growing, up ${abs(growthPercent).toInt()} percent"
                isNeg -> "Declining, down ${abs(growthPercent).toInt()} percent"
                else  -> "No change"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1
                    )
                }
                // Right side — arrow on top, growth badge below it.
                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    if (onViewAllClick != null) {
                        GlossyArrowButton(
                            onClick = onViewAllClick,
                            contentDescription = viewAllLabel,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(NTDp.radFull))
                            .background(badgeBgColor)
                            .semantics(mergeDescendants = true) { contentDescription = badgeDesc }
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
                            text = badgeText,
                            color = badgeTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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

// ── Professional bar-chart analytics card ─────────────────────
//  Premium minimal: white card, subtle border, clean bars, minimal grid.

@Composable
fun NTAnalyticsBarCard(
    title: String,
    valueLabel: String,
    growthPercent: Double?,
    vsLabel: String = "vs last month",
    rawValues: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NTColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val isPos = (growthPercent ?: 0.0) >= 0.0
            val isNeg = (growthPercent ?: 0.0) <= -0.5
            val badgeBg = when {
                growthPercent == null -> NTColors.SurfaceVar
                isPos -> NTColors.SuccessLight
                else -> NTColors.ErrorLight
            }
            val badgeFg = when {
                growthPercent == null -> NTColors.TextSecondary
                isPos -> NTColors.SuccessText
                else -> NTColors.ErrorText
            }
            val badgeIcon = when {
                growthPercent == null -> Icons.Rounded.Remove
                isPos -> Icons.Rounded.TrendingUp
                else -> Icons.Rounded.TrendingDown
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = NTColors.TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = valueLabel,
                        color = NTColors.TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = vsLabel,
                        color = NTColors.TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formatBarDelta(growthPercent),
                        color = badgeFg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (rawValues.all { it == 0.0 }) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No revenue data yet",
                        color = NTColors.TextTertiary, fontSize = 13.sp
                    )
                }
            } else {
                NTRevenueBars(
                    values = rawValues,
                    labels = labels,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun formatBarDelta(pct: Double?): String {
    if (pct == null) return "—"
    val rounded = (abs(pct) * 10).toLong() / 10.0
    val str = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    return if (pct >= 0) "+$str%" else "-$str%"
}

@Composable
private fun NTRevenueBars(
    values: List<Double>,
    labels: List<String>,
    height: Dp = 150.dp,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxWidth().height(height)
        ) {
            val w = size.width
            val h = size.height
            val bottomPad = 8.dp.toPx()
            val topPad = 8.dp.toPx()
            val drawH = h - topPad - bottomPad
            val maxV = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0

            // Minimal grid — 3 faint lines
            listOf(0.25f, 0.5f, 0.75f).forEach { r ->
                val y = topPad + drawH * (1f - r)
                drawLine(
                    color = Color(0xFFE3ECEB),
                    start = Offset(0f, y), end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val n = values.size
            if (n == 0) return@Canvas
            val gap = if (n > 20) 3.dp.toPx() else if (n > 10) 6.dp.toPx() else 10.dp.toPx()
            val totalGap = gap * (n - 1)
            val barW = ((w - totalGap) / n).coerceAtLeast(4.dp.toPx())
            val barColor = Color(0xFF0F9D8A)
            val barColorSoft = Color(0xFF0F9D8A).copy(alpha = 0.22f)
            val maxIdx = values.indexOf(maxV)

            values.forEachIndexed { i, v ->
                val frac = (v / maxV).toFloat().coerceIn(0f, 1f)
                val barH = (drawH * frac).coerceAtLeast(if (v > 0) 4.dp.toPx() else 2.dp.toPx())
                val x = i * (barW + gap)
                val y = topPad + drawH - barH
                drawRoundRect(
                    color = if (i == maxIdx) barColor else if (frac < 0.02f) barColorSoft.copy(alpha = 0.4f) else barColor.copy(alpha = 0.78f),
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X labels — evenly spaced, max ~5
        val displayLabels = downsampleLabels(labels, values.size, 5)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            displayLabels.forEach { lbl ->
                Text(
                    text = lbl,
                    color = NTColors.TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun downsampleLabels(source: List<String>, pointCount: Int, max: Int): List<String> {
    if (source.isEmpty()) return List(pointCount.coerceAtLeast(1).coerceAtMost(max)) { "" }
    if (source.size <= max) return source
    val step = (source.size - 1).toDouble() / (max - 1)
    return (0 until max).map { i -> source[(i * step).toInt().coerceAtMost(source.size - 1)] }
}
