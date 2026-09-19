package com.example.ruwia.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.isEmptyCansSource
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

// ─────────────────────────────────────────────────────────────────────────────
//  Water-dispatch analytics: pure data layer (unit-tested) + SaaS chart card.
//  Source of truth is the employee's own stock-movement feed — no extra API.
// ─────────────────────────────────────────────────────────────────────────────

/** Time ranges offered by the analytics card. `chip` mirrors an entries chip. */
enum class DispatchRange(val title: String, val chip: String, val previousLabel: String) {
    TODAY("Today", "Today", "vs yesterday"),
    LAST_7_DAYS("Last 7 Days", "Last 7 Days", "vs previous 7 days"),
    THIS_MONTH("This Month", "This Month", "vs last month"),
    LAST_MONTH("Last Month", "Last Month", "vs previous month"),
}

/** One product variant series (e.g. "1L"). Color is assigned by rank in UI. */
data class DispatchVariant(
    val key: String,
    val label: String,
    val total: Int,
)

/** One chart bucket: per-variant case counts plus the precomputed total. */
data class DispatchBucket(
    /** Tooltip title, e.g. "8 – 10 AM" or "Mon, 15 Sep". */
    val title: String,
    /** X-axis label, null when the tick is intentionally unlabeled. */
    val axisLabel: String?,
    val counts: Map<String, Int>,
    val total: Int,
)

data class DispatchAnalyticsData(
    val range: DispatchRange,
    val buckets: List<DispatchBucket>,
    val variants: List<DispatchVariant>,
    val total: Int,
    /** Total of the immediately previous comparable period, null when unknown. */
    val previousTotal: Int?,
)

/** "1282" -> "1,282" (KMP-safe thousands grouping). */
fun formatCount(n: Int): String {
    if (n < 0) return "-" + formatCount(-n)
    val s = n.toString()
    if (s.length <= 3) return s
    return s.reversed().chunked(3).joinToString(",").reversed()
}

/** Y-axis range: an exact top gridline plus its round tick labels. */
data class YAxisRange(val max: Int, val ticks: List<Int>)

/**
 * Nice-number Y-axis derived ONLY from plotted individual series values —
 * never from aggregate totals. The top gridline always sits above the highest
 * plotted point (≈10–25% headroom) so the peak floats near the top without
 * touching the edge. Examples: 20 → top 25 (0,5,…,25); 100 → top 120;
 * 137 → top 150.
 */
fun calculateYAxisRange(values: List<Int>): YAxisRange {
    val peak = values.maxOrNull()?.coerceAtLeast(0) ?: 0
    if (peak <= 0) return YAxisRange(1, listOf(0, 1))
    var step = niceStepAtLeast(peak / 5.0)
    var top = (peak / step + 1) * step
    var ticks = (0..top step step).toList()
    if (ticks.size > 6) {
        step *= 2
        top = (peak / step + 1) * step
        ticks = (0..top step step).toList()
    }
    return YAxisRange(top, ticks)
}

/** Smallest integer step of the form {1,2,5}×10ⁿ that is ≥ [raw]. */
private fun niceStepAtLeast(raw: Double): Int {
    if (raw <= 1.0) return 1
    var base = 1
    while (raw > base * 10) base *= 10
    return when {
        raw <= base -> base
        raw <= 2 * base -> 2 * base
        raw <= 5 * base -> 5 * base
        else -> 10 * base
    }
}

private data class ParsedDispatch(val date: LocalDate, val hour: Int, val qty: Int, val key: String)

private fun parseMovementDateTime(createdAt: String?, zone: TimeZone): LocalDateTime? {
    if (createdAt.isNullOrBlank()) return null
    runCatching { return Instant.parse(createdAt).toLocalDateTime(zone) }.getOrNull()
    // Fallback: date prefix only (assigns noon) so the entry still counts.
    return runCatching {
        val d = LocalDate.parse(createdAt.take(10))
        LocalDateTime(d.year, d.month, d.dayOfMonth, 12, 0)
    }.getOrNull()
}

private fun hourLabel(h: Int): String {
    val hh = ((h % 24) + 24) % 24
    val suffix = if (hh < 12) "AM" else "PM"
    val h12 = if (hh % 12 == 0) 12 else hh % 12
    return "$h12 $suffix"
}

private fun dayTitle(date: LocalDate): String {
    val dow = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    val mon = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$dow, ${date.dayOfMonth} $mon"
}

private fun daysOfMonth(year: Int, month: Month, maxDayOfMonth: Int? = null): List<LocalDate> {
    val first = LocalDate(year, month, 1)
    return generateSequence(first) { d ->
        val n = d.plus(1, DateTimeUnit.DAY)
        if (n.month == month && (maxDayOfMonth == null || n.dayOfMonth <= maxDayOfMonth)) n else null
    }.toList()
}

/**
 * Aggregates outward (dispatch) movements into chart buckets.
 *
 * - TODAY: twelve 2-hour bins; x labels every other bin.
 * - LAST_7_DAYS: one bin per day, ending today.
 * - THIS_MONTH / LAST_MONTH: one bin per calendar day.
 * - Variants group by product size name (e.g. "1L"); unknown products fold
 *   into "Other". At most the top 3 variants keep their own series — the rest
 *   merge into "Other" so the chart stays readable on small phones.
 */
fun aggregateDispatch(
    movements: List<StockMovement>,
    products: List<ProductCategory>,
    range: DispatchRange,
    today: LocalDate,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): DispatchAnalyticsData {
    val productById = products.associateBy { it.id }
    val labels = mutableMapOf<String, String>()
    val parsed = movements.mapNotNull { m ->
        if (m.type != "outward" || m.source.isEmptyCansSource()) return@mapNotNull null
        if (m.qty <= 0) return@mapNotNull null
        val dt = parseMovementDateTime(m.createdAt, zone) ?: return@mapNotNull null
        val product = m.productId?.let { productById[it] }
        // Group by product so each series carries its real water-variant name
        // (e.g. "Neer thuli - 20L"), never just a size bucket.
        val key = m.productId?.takeIf { it.isNotBlank() } ?: "OTHER"
        labels.getOrPut(key) {
            product?.displayName?.ifBlank { product?.name }?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Other"
        }
        ParsedDispatch(dt.date, dt.hour, m.qty, key)
    }

    // Bucket frames + previous-period window per range.
    val lastMonthFirst = if (today.month == kotlinx.datetime.Month.JANUARY)
        LocalDate(today.year - 1, 12, 1)
    else LocalDate(today.year, today.monthNumber - 1, 1)
    val twoMonthsAgoFirst = if (lastMonthFirst.month == kotlinx.datetime.Month.JANUARY)
        LocalDate(lastMonthFirst.year - 1, 12, 1)
    else LocalDate(lastMonthFirst.year, lastMonthFirst.monthNumber - 1, 1)

    // Each frame: date + bin (hour-bin for TODAY, 0 for daily).
    val frames: List<Pair<LocalDate, Int>>
    val prevDates: Set<LocalDate>
    when (range) {
        DispatchRange.TODAY -> {
            frames = (0..11).map { today to it }
            prevDates = setOf(today.minus(1, DateTimeUnit.DAY))
        }
        DispatchRange.LAST_7_DAYS -> {
            val days = (6 downTo 0).map { today.minus(it, DateTimeUnit.DAY) }
            frames = days.map { it to 0 }
            prevDates = (7..13).map { today.minus(it, DateTimeUnit.DAY) }.toSet()
        }
        DispatchRange.THIS_MONTH -> {
            val days = daysOfMonth(today.year, today.month, today.dayOfMonth)
            frames = days.map { it to 0 }
            prevDates = daysOfMonth(lastMonthFirst.year, lastMonthFirst.month).toSet()
        }
        DispatchRange.LAST_MONTH -> {
            val days = daysOfMonth(lastMonthFirst.year, lastMonthFirst.month)
            frames = days.map { it to 0 }
            prevDates = daysOfMonth(twoMonthsAgoFirst.year, twoMonthsAgoFirst.month).toSet()
        }
    }
    val frameIndexByDay: Map<LocalDate, Int> = when (range) {
        DispatchRange.TODAY -> emptyMap()
        else -> frames.mapIndexed { i, (date, _) -> date to i }.toMap()
    }

    val perBucket = Array(frames.size) { mutableMapOf<String, Int>() }
    parsed.forEach { p ->
        val idx = when (range) {
            DispatchRange.TODAY ->
                if (p.date == today) (p.hour / 2).coerceIn(0, 11) else -1
            else -> frameIndexByDay[p.date] ?: -1
        }
        if (idx >= 0) {
            perBucket[idx][p.key] = (perBucket[idx][p.key] ?: 0) + p.qty
        }
    }

    // Every variant keeps its own series under its real product name —
    // nothing is merged into "Other".
    val totals = mutableMapOf<String, Int>()
    perBucket.forEach { bucket -> bucket.forEach { (k, v) -> totals[k] = (totals[k] ?: 0) + v } }
    val rankedKeys = totals.entries.sortedByDescending { it.value }.map { it.key }

    val buckets = frames.mapIndexed { i, (date, bin) ->
        val raw = perBucket[i]
        val counts = raw.filterValues { it > 0 }.toMap()
        val (title, axis) = when (range) {
            DispatchRange.TODAY -> {
                val h = bin * 2
                "${hourLabel(h)} – ${hourLabel(h + 2)}" to
                    if (bin % 2 == 0) hourLabel(h) else null
            }
            DispatchRange.LAST_7_DAYS -> dayTitle(date) to
                date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
            else -> dayTitle(date) to
                if (date.dayOfMonth == 1 || date.dayOfMonth % 7 == 1) "${date.dayOfMonth}" else null
        }
        DispatchBucket(title, axis, counts.toMap(), counts.values.sum())
    }

    val variants = rankedKeys.map { DispatchVariant(it, labels[it] ?: it, totals[it] ?: 0) }

    val previousTotal = parsed.filter { it.date in prevDates }.sumOf { it.qty }

    return DispatchAnalyticsData(
        range = range,
        buckets = buckets,
        variants = variants,
        total = buckets.sumOf { it.total },
        previousTotal = previousTotal,
    )
}

/** Smooth S-curve through every point (horizontal-tangent cubic segments). */
private fun smoothLine(pts: List<Offset>): Path = Path().apply {
    moveTo(pts[0].x, pts[0].y)
    for (i in 0 until pts.size - 1) {
        val cx = (pts[i].x + pts[i + 1].x) / 2f
        cubicTo(cx, pts[i].y, cx, pts[i + 1].y, pts[i + 1].x, pts[i + 1].y)
    }
}

/** Closes [line] along [baselineY] to form a fillable area path. */
private fun areaUnder(line: Path, pts: List<Offset>, baselineY: Float): Path =
    Path().apply {
        addPath(line)
        lineTo(pts.last().x, baselineY)
        lineTo(pts.first().x, baselineY)
        close()
    }

// ── Card ──────────────────────────────────────────────────────────────────────

/** Series palette by rank, cycling for any number of variants. */
private fun seriesColor(index: Int, key: String): Color = when {
    key == "OTHER" -> NTColors.TextTertiary
    else -> when (index % 6) {
        0 -> NTColors.Primary
        1 -> NTColors.Info
        2 -> Color(0xFF8B5CF6)
        3 -> NTColors.Warning
        4 -> NTColors.Success
        else -> Color(0xFFEC4899)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchAnalyticsCard(
    data: DispatchAnalyticsData,
    range: DispatchRange,
    onRangeChange: (DispatchRange) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dropdownOpen by remember { mutableStateOf(false) }
    var showBreakdown by rememberSaveable { mutableStateOf(false) }
    // Selected bucket index; -1 = none. Reset whenever the range changes.
    var selected by rememberSaveable(range) { mutableStateOf(-1) }
    val selectedBucket = data.buckets.getOrNull(selected)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Divider, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        // Header: title + range dropdown.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Water Dispatch",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NTColors.TextPrimary,
                )
                Text(
                    "Cases dispatched over time",
                    fontSize = 11.sp,
                    color = NTColors.TextTertiary,
                )
            }
            Box {
                Row(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, NTColors.Divider, RoundedCornerShape(12.dp))
                        .clickable(
                            onClick = { dropdownOpen = true },
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .semantics { contentDescription = "Select time range, currently ${range.title}" },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        range.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NTColors.Primary,
                    )
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        tint = NTColors.Primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = dropdownOpen,
                    onDismissRequest = { dropdownOpen = false },
                ) {
                    DispatchRange.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.title) },
                            trailingIcon = {
                                if (option == range) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = NTColors.Primary,
                                    )
                                }
                            },
                            onClick = {
                                dropdownOpen = false
                                if (option != range) onRangeChange(option)
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            isLoading && data.total == 0 && data.buckets.all { it.total == 0 } -> {
                DispatchSkeleton()
            }
            errorMessage != null && data.total == 0 -> {
                DispatchError(message = errorMessage, onRetry = onRetry)
            }
            data.total == 0 -> {
                DispatchEmpty()
            }
            else -> {
                // KPI.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            formatCount(data.total),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = NTColors.TextPrimary,
                        )
                        Text(
                            "Total cases",
                            fontSize = 11.sp,
                            color = NTColors.TextTertiary,
                        )
                    }
                    ChangePill(
                        total = data.total,
                        previous = data.previousTotal,
                        label = range.previousLabel,
                    )
                }

                Spacer(Modifier.height(8.dp))

                DispatchChart(
                    data = data,
                    selected = selected,
                    onSelect = { selected = if (it == selected) -1 else it },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))

                // Legend: product series only (never the Total, never color
                // alone). Wraps to rows of 3 so any catalog fits small phones.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    data.variants.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            row.forEach { variant ->
                                val colorIndex = data.variants.indexOf(variant)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                seriesColor(colorIndex, variant.key),
                                                RoundedCornerShape(4.dp),
                                            ),
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        "${variant.label} · ${formatCount(variant.total)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NTColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { showBreakdown = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                ) {
                    Text(
                        "View case breakdown",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NTColors.Primary,
                    )
                }
            }
        }
    }

    if (showBreakdown && data.total > 0) {
        DispatchBreakdownSheet(
            data = data,
            onDismiss = { showBreakdown = false },
        )
    }
}

// ── Change pill ───────────────────────────────────────────────────────────────

@Composable
private fun ChangePill(total: Int, previous: Int?, label: String) {
    if (total == 0) return
    if (previous == null) return
    if (previous == 0) {
        // Current volume with no baseline is genuinely new — not a percent.
        PillShell(bg = NTColors.PrimaryLight, fg = NTColors.Primary) {
            Text("New", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NTColors.Primary)
        }
        return
    }
    val pct = (total - previous) * 100.0 / previous
    val up = pct >= 0
    PillShell(
        bg = if (up) NTColors.SuccessLight else NTColors.ErrorLight,
        fg = if (up) NTColors.SuccessText else NTColors.ErrorText,
    ) {
        Icon(
            if (up) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
            contentDescription = null,
            tint = if (up) NTColors.SuccessText else NTColors.ErrorText,
            modifier = Modifier.size(11.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            "${if (up) "+" else ""}${"%.1f".format(pct)}% $label",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (up) NTColors.SuccessText else NTColors.ErrorText,
        )
    }
}

@Composable
private fun PillShell(bg: Color, fg: Color, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

// ── Chart ─────────────────────────────────────────────────────────────────────

@Composable
private fun DispatchChart(
    data: DispatchAnalyticsData,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buckets = data.buckets
    val variants = data.variants
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = NTColors.TextTertiary,
    )
    val gridColor = NTColors.Divider
    val selectionColor = NTColors.TextSecondary

    // Dynamic axis: the highest bucket always reaches the top gridline and
    // zero always sits at the bottom, whatever the data max is (15, 25, …).
    // Scale ONLY from individually plotted series points — bucket totals and
    // the KPI total must never influence the axis. Recalculates whenever the
    // buckets/variants change (i.e. on every range switch).
    val plottedValues = remember(buckets, variants) {
        buckets.flatMap { bucket -> variants.map { variant -> bucket.counts[variant.key] ?: 0 } }
    }
    val yAxis = remember(plottedValues) { calculateYAxisRange(plottedValues) }
    val yTop = yAxis.max
    val yVals = yAxis.ticks

    val padL = 34.dp
    val padR = 12.dp
    val padT = 10.dp
    val padB = 26.dp
    val chartH = 210.dp

    val summary = remember(data) {
        "Dispatch chart. Total ${formatCount(data.total)} cases. " +
            variants.joinToString(", ") { "${it.label} ${formatCount(it.total)}" }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartH)
                .semantics { contentDescription = summary }
                .pointerInput(buckets) {
                    detectTapGestures { offset ->
                        val n = buckets.size
                        if (n == 0) return@detectTapGestures
                        val padLPx = padL.toPx()
                        val padRPx = padR.toPx()
                        // Taps outside the plot (axis labels, gutters) dismiss.
                        if (offset.x < padLPx - 24f ||
                            offset.x > size.width - padRPx + 24f ||
                            offset.y > size.height - padB.toPx() + 12f
                        ) {
                            onSelect(-1)
                            return@detectTapGestures
                        }
                        val stepX = (size.width - padLPx - padRPx) /
                            if (n > 1) (n - 1).toFloat() else 1f
                        val idx = ((offset.x - padLPx) / stepX).roundToInt()
                            .coerceIn(0, n - 1)
                        onSelect(idx)
                    }
                },
        ) {
            val n = buckets.size
            if (n == 0) return@Canvas
            val plotW = size.width - padL.toPx() - padR.toPx()
            val plotH = size.height - padT.toPx() - padB.toPx()
            val stepX = if (n > 1) plotW / (n - 1) else 0f
            fun xAt(i: Int) = padL.toPx() + i * stepX
            fun yAt(v: Int) = padT.toPx() + plotH * (1f - (v.toFloat() / yTop).coerceIn(0f, 1f))

            // Grid + y labels.
            yVals.forEach { v ->
                val y = yAt(v)
                drawLine(gridColor, Offset(padL.toPx(), y), Offset(size.width - padR.toPx(), y), 1f)
                val layout = textMeasurer.measure(v.toString(), axisStyle)
                drawText(
                    textMeasurer, v.toString(),
                    topLeft = Offset(padL.toPx() - layout.size.width - 6f, y - layout.size.height / 2f),
                    style = axisStyle,
                )
            }

            // X labels.
            buckets.forEachIndexed { i, bucket ->
                bucket.axisLabel?.let { label ->
                    val layout = textMeasurer.measure(label, axisStyle)
                    drawText(
                        textMeasurer, label,
                        topLeft = Offset(
                            xAt(i) - layout.size.width / 2f,
                            size.height - padB.toPx() + 8f,
                        ),
                        style = axisStyle,
                    )
                }
            }

            // Series: largest area first so the lead series stays on top.
            variants.indices.reversed().forEach { vi ->
                val variant = variants[vi]
                val color = seriesColor(vi, variant.key)
                val pts = buckets.mapIndexed { i, b ->
                    Offset(xAt(i), yAt(b.counts[variant.key] ?: 0))
                }
                if (pts.size == 1) {
                    drawCircle(color, 8f, pts[0])
                } else {
                    drawPath(smoothLine(pts), color, style = Stroke(5f, cap = StrokeCap.Round))
                    drawPath(
                        areaUnder(smoothLine(pts), pts, padT.toPx() + plotH),
                        color.copy(alpha = 0.10f),
                    )
                }
            }

            // Selection marker.
            if (selected in buckets.indices) {
                val x = xAt(selected)
                drawLine(
                    selectionColor,
                    Offset(x, padT.toPx()),
                    Offset(x, padT.toPx() + plotH),
                    2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
                val bucket = buckets[selected]
                variants.forEachIndexed { vi, variant ->
                    val v = bucket.counts[variant.key] ?: 0
                    if (v > 0 || variants.size == 1) {
                        val c = Offset(x, yAt(v))
                        drawCircle(Color.White, 13f, c)
                        drawCircle(seriesColor(vi, variant.key), 9f, c)
                    }
                }
            }
        }

        // Tooltip overlay: auto-sized to its content and anchored to the
        // free side of the chart so it never clips.
        val selBucket = buckets.getOrNull(selected)
        if (selBucket != null && variants.isNotEmpty()) {
            val n = buckets.size
            val leftSide = if (n > 1) selected < n / 2.0 else true
            val rows = variants.mapNotNull { v ->
                val c = selBucket.counts[v.key] ?: 0
                if (c > 0) v to c else null
            }
            Box(
                modifier = Modifier
                    .align(if (leftSide) Alignment.TopEnd else Alignment.TopStart)
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                    .widthIn(min = 160.dp, max = 280.dp)
                    .shadow(8.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(NTColors.Surface)
                    .border(1.dp, NTColors.Divider, RoundedCornerShape(14.dp))
                    // Tapping the tooltip (or its close button) dismisses it.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(-1) },
                    )
                    .padding(14.dp),
            ) {
                Column(
                    modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            selBucket.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NTColors.TextTertiary,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close details",
                            tint = NTColors.TextTertiary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    rows.forEachIndexed { i, (variant, count) ->
                        val colorIndex = variants.indexOf(variant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(
                                        seriesColor(colorIndex, variant.key),
                                        RoundedCornerShape(4.dp),
                                    ),
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                variant.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = NTColors.TextSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${formatCount(count)} cases",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NTColors.TextPrimary,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Total",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NTColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${formatCount(selBucket.total)} cases",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = NTColors.Primary,
                        )
                    }
                }
            }
        }
    }
}

// ── Breakdown sheet ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DispatchBreakdownSheet(
    data: DispatchAnalyticsData,
    onDismiss: () -> Unit,
) {
    val maxTotal = (data.variants.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                "Water Case Breakdown",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary,
            )
            Text(
                "${data.range.title} · ${formatCount(data.total)} cases total",
                fontSize = 12.sp,
                color = NTColors.TextTertiary,
            )
            Spacer(Modifier.height(16.dp))
            data.variants.forEachIndexed { i, variant ->
                val color = seriesColor(i, variant.key)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, RoundedCornerShape(5.dp)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        variant.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NTColors.TextPrimary,
                        modifier = Modifier.width(64.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(NTColors.Divider.copy(alpha = 0.6f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(variant.total.toFloat() / maxTotal)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${formatCount(variant.total)} cases",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NTColors.TextSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(84.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Total",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NTColors.TextPrimary,
                )
                Text(
                    "${formatCount(data.total)} cases",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = NTColors.Primary,
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

// ── Skeleton / empty / error ──────────────────────────────────────────────────

@Composable
private fun DispatchSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NTColors.Divider.copy(alpha = 0.5f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(NTColors.Divider.copy(alpha = 0.35f)),
        )
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(NTColors.Divider.copy(alpha = 0.5f)),
        )
    }
}

@Composable
private fun DispatchEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(NTColors.PrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Inventory2,
                contentDescription = null,
                tint = NTColors.Primary,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "No dispatch data",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NTColors.TextPrimary,
        )
        Text(
            "There are no water-case dispatches for this period.",
            fontSize = 12.sp,
            color = NTColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DispatchError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(NTColors.ErrorLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = NTColors.Error,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Couldn't load dispatch analytics",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NTColors.TextPrimary,
        )
        Text(
            message,
            fontSize = 12.sp,
            color = NTColors.TextTertiary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onRetry,
            modifier = Modifier.height(44.dp),
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = null,
                tint = NTColors.Primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Retry", color = NTColors.Primary, fontWeight = FontWeight.Bold)
        }
    }
}
