package com.example.ruwia.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// ── Canvas-based smooth Bezier line chart ──────────────────────────────────

@Composable
fun NTLineChartCard(
    title: String,
    valueLabel: String,
    growthPercent: Double,
    points: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(NTDp.cardPad)) {
            val isPos = growthPercent >= 0
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
                        .background(if (isPos) NTColors.SuccessLight else NTColors.ErrorLight)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isPos) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = if (isPos) NTColors.Success else NTColors.Error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${if (isPos) "+" else ""}${abs(growthPercent).toInt()}%",
                        color = if (isPos) NTColors.SuccessText else NTColors.ErrorText,
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
                NTLineChart(points = points, labels = labels)
            }
        }
    }
}

@Composable
fun NTLineChart(
    points: List<Float>,
    labels: List<String>,
    height: Dp = NTDp.chartH,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
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
