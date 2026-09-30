package com.example.ruwia.presentation

import com.example.ruwia.domain.SaleEntry
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.deriveShopStockTotals
import com.example.ruwia.domain.isEmptyCansSource


/**
 * Selectable time window the dashboard chart can display.
 *
 * Mirrors the user-facing range picker so the UI can ask
 * `state.toDashboardMetrics(range)` for a specific view without re-doing
 * any of the bucketing math itself.
 */
enum class DashboardRange { WEEK, MONTH, QUARTER, YEAR }

/**
 * All numbers that drive the admin home dashboard, derived purely from
 * [AdminState] so the UI never has to re-do this math itself.
 */
data class DashboardMetrics(
    val thisMonthRevenue: Double,
    val lastMonthRevenue: Double,
    val revenueGrowthPercent: Double?,

    val thisMonthProfit: Double,
    val lastMonthProfit: Double,
    val profitGrowthPercent: Double?,

    /** 0..1 normalised series for the hero card chart. Always non-empty. */
    val chartPoints: List<Float>,
    /** Raw rupee total for each [chartPoints] bucket (same length). */
    val chartRaw: List<Double>,
    /** X-axis labels — content depends on the selected [range]. */
    val xAxisLabels: List<String>,
    /** Y-axis labels (top → bottom), e.g. "30K / 20K / 10K / 0". */
    val yAxisLabels: List<String>,
    /** Window-relative total revenue (matches the chart's window). */
    val rangeRevenue: Double,
    /** Date-label for the hero card header, e.g. "This Month" / "Jun 2026". */
    val rangeLabel: String,
    /** Which [DashboardRange] produced these chart values. */
    val range: DashboardRange,

    /** 0..1 normalised distinct-customer series for the selected [range]. */
    val customerChartPoints: List<Float>,
    /** Raw distinct-customer count per bucket (same length as [customerChartPoints]). */
    val customerChartRaw: List<Double>,
    /** X-axis labels for the customer chart (same buckets as revenue). */
    val customerXLabels: List<String>,
    /** Distinct customers in the selected window. */
    val rangeCustomers: Int,
    /** Growth for the selected customer window vs previous window. */
    val customerRangeGrowthPercent: Double?,
    /** Revenue growth for the selected window vs the previous equivalent window. */
    val revenueRangeGrowthPercent: Double?,

    val weeklyGrowthPercent: Double?,
    val stockGrowthPercent: Double?,
    val customerGrowthPercent: Double?,

    val newCustomersThisMonth: Int,
    val activeSkus: Int,
    /** Total units currently on hand across all shops. */
    val totalStockUnits: Double,
    /** Total empty cans currently at all shops (derived from movements). */
    val emptyCansAtShop: Int,
    /** Total cans currently held by all customers (derived from movements). */
    val emptyCansOut: Int,

    /** Sum of all `inward` stock movements for the current month. */
    val stockInwardThisMonth: Int,
    /** Sum of all `outward` stock movements for the current month. */
    val stockOutwardThisMonth: Int,

    val isRevenueUp: Boolean,
    val isProfitUp: Boolean,
)

/**
 * Pure derivation — safe to call directly from a Composable.
 *
 * @param range  which time window to plot in the hero chart. Defaults to
 *               [DashboardRange.MONTH] (the current calendar month).
 */
fun AdminState.toDashboardMetrics(
    range: DashboardRange = DashboardRange.MONTH,
): DashboardMetrics {
    val current  = currentMonth
    val previous = previousMonth

    // ── Month-level revenue / profit (independent of chart range) ────────────
    val thisMonthRev = saleEntries.sumByMonth(current)
    val lastMonthRev = saleEntries.sumByMonth(previous)
    val revGrowth    = pctChange(thisMonthRev, lastMonthRev)

    val thisExpense  = currentMonthExpense?.total ?: 0.0
    val lastExpense  = lastMonthExpense?.total ?: 0.0
    val thisProfit   = thisMonthRev - thisExpense
    val lastProfit   = lastMonthRev - lastExpense
    val profitGrowth = pctChange(thisProfit, lastProfit)

    // ── Build chart series for the selected window ───────────────────────────
    val (rawSeries, xLabels, rangeLabel, rangeTotal) = saleEntries.seriesFor(range, current)
    val maxPt        = rawSeries.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
    val normalised   = rawSeries.map { (it / maxPt).toFloat().coerceIn(0f, 1f) }
    val yLabels      = buildYAxisLabels(maxPt)

    // ── Build distinct-customer series for the same window ───────────────────
    val (custRawSeries, custXLabels, _, _) = saleEntries.customerSeriesFor(range, current)
    val custMax = custRawSeries.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
    val custNormalised = custRawSeries.map { (it / custMax).toFloat().coerceIn(0f, 1f) }
    val rangeCustTotal = saleEntries.distinctCustomersForRange(range, current)
    val custRangeGrowth = saleEntries.customerRangeGrowth(range, current)

    // ── Weekly: last 7 days vs prior 7 days ──────────────────────────────────
    val (last7, prev7) = saleEntries.lastTwoWeekTotals()
    val weeklyGrowth   = pctChange(last7, prev7)

    // ── Revenue growth for the selected window vs the previous one ───────────
    val revRangeGrowth = saleEntries.revenueRangeGrowth(range, current)

    // ── Stock movements: net inward this/last month ──────────────────────────
    val thisStockNet = recentMovements.netInwardForMonth(current)
    val lastStockNet = recentMovements.netInwardForMonth(previous)
    val stockGrowth  = pctChange(thisStockNet.toDouble(), lastStockNet.toDouble())

    val thisInward   = recentMovements.totalForType(current, "inward")
    val thisOutward  = recentMovements.totalForType(current, "outward")

    // ── Customer counts ──────────────────────────────────────────────────────
    val thisCust   = saleEntries.distinctCustomersForMonth(current)
    val lastCust   = saleEntries.distinctCustomersForMonth(previous)
    val custGrowth = pctChange(thisCust.toDouble(), lastCust.toDouble())

    val activeSkus = productCategories.count { it.isActive }
    
    // ── Live Stock derivation ────────────────────────────────────────────────
    // Taken directly from the stock_movements table (inward minus outward).
    val totalStockUnits = productCategories.filter { it.isActive }.sumOf { it.stockAvailable.toDouble() }
    
    // ── Live Empty Can derivation ────────────────────────────────────────────
    // Sum of all 'inward' movements where source starts with "Empty cans",
    // reported relative to the admin-set reset baseline so a reset zeroes it.
    val liveEmptyAtShop = (recentMovements
        .filter { it.source.isEmptyCansSource() }
        .sumOf { m -> if (m.type == "inward") m.qty else -m.qty }
        .coerceAtLeast(0) - emptyCansBaseline)
        .coerceAtLeast(0)
    
    // Sum of all 'outward' movements (delivered) - Sum of all 'inward' empty returns.
    val liveCansWithCustomers = recentMovements
        .filter { it.type == "outward" && !it.source.isEmptyCansSource() }
        .sumOf { it.qty } - 
        recentMovements
        .filter { it.type == "inward" && it.source.isEmptyCansSource() }
        .sumOf { it.qty }
        .let { it.coerceAtLeast(0) }

    return DashboardMetrics(
        thisMonthRevenue       = thisMonthRev,
        lastMonthRevenue       = lastMonthRev,
        revenueGrowthPercent   = revGrowth,
        thisMonthProfit        = thisProfit,
        lastMonthProfit        = lastProfit,
        profitGrowthPercent    = profitGrowth,
        chartPoints            = normalised,
        chartRaw               = rawSeries,
        xAxisLabels            = xLabels,
        yAxisLabels            = yLabels,
        rangeRevenue           = rangeTotal,
        rangeLabel             = rangeLabel,
        range                  = range,
        customerChartPoints    = custNormalised,
        customerChartRaw       = custRawSeries,
        customerXLabels        = custXLabels,
        rangeCustomers         = rangeCustTotal,
        customerRangeGrowthPercent = custRangeGrowth,
        revenueRangeGrowthPercent = revRangeGrowth,
        weeklyGrowthPercent    = weeklyGrowth,
        stockGrowthPercent     = stockGrowth,
        customerGrowthPercent  = custGrowth,
        newCustomersThisMonth  = thisCust,
        activeSkus             = activeSkus,
        totalStockUnits        = totalStockUnits,
        emptyCansAtShop        = liveEmptyAtShop,
        emptyCansOut           = liveCansWithCustomers,
        stockInwardThisMonth   = thisInward,
        stockOutwardThisMonth  = thisOutward,
        isRevenueUp            = (revGrowth ?: 0.0) >= 0.0,
        isProfitUp             = (profitGrowth ?: 0.0) >= 0.0,
    )
}

// ─── Series builders per window ──────────────────────────────────────────────

private data class ChartSeries(
    val raw: List<Double>,
    val xLabels: List<String>,
    val rangeLabel: String,
    val total: Double,
)

private operator fun ChartSeries.component1() = raw
private operator fun ChartSeries.component2() = xLabels
private operator fun ChartSeries.component3() = rangeLabel
private operator fun ChartSeries.component4() = total

private fun List<SaleEntry>.seriesFor(
    range: DashboardRange,
    currentYearMonth: String,
): ChartSeries = when (range) {
    DashboardRange.WEEK    -> weekSeries()
    DashboardRange.MONTH   -> monthSeries(currentYearMonth)
    DashboardRange.QUARTER -> quarterSeries(currentYearMonth)
    DashboardRange.YEAR    -> yearSeries(currentYearMonth)
}

/** Last 7 calendar days (oldest → newest) ending today. */
private fun List<SaleEntry>.weekSeries(): ChartSeries {
    val byDate = groupBy { it.date }
    val days = byDate.keys.sortedDescending().take(7).reversed()  // oldest → newest
    val raw = days.map { d -> byDate[d]!!.sumOf { it.totalSelling } }
    val labels = days.map { d ->
        // "YYYY-MM-DD" → "DD Mon"
        val day   = d.substring(8, 10).toIntOrNull()?.toString() ?: "?"
        val mon   = d.substring(5, 7).toMonthAbbr()
        "$day $mon"
    }.ifEmpty { listOf("—", "—", "—", "—", "—", "—", "—") }
    val padded   = if (raw.size < 2) List(7) { 0.0 } else raw
    val padLbls  = if (labels.size < 2) List(7) { "—" } else labels
    val rangeLbl = if (days.isNotEmpty()) "Last 7 days" else "Last 7 days"
    return ChartSeries(padded, padLbls, rangeLbl, padded.sum())
}

/**
 * One bucket per day in [yearMonth]. Returns [daysInMonth] entries with 5
 * evenly-spaced X-labels carrying the actual month abbreviation.
 */
private fun List<SaleEntry>.monthSeries(yearMonth: String): ChartSeries {
    if (yearMonth.isBlank()) {
        return ChartSeries(List(7) { 0.0 }, listOf("1", "8", "15", "22", "30"), "This Month", 0.0)
    }
    val year   = yearMonth.substring(0, 4).toIntOrNull() ?: return ChartSeries(
        List(7) { 0.0 }, listOf("1", "8", "15", "22", "30"), "This Month", 0.0,
    )
    val month  = yearMonth.substring(5, 7).toIntOrNull() ?: return ChartSeries(
        List(7) { 0.0 }, listOf("1", "8", "15", "22", "30"), "This Month", 0.0,
    )
    val nDays  = daysInMonth(year, month)
    val mAbbr  = monthAbbr(month)

    val buckets = DoubleArray(nDays)
    forEach { e ->
        val dStr = e.date
        if (!dStr.startsWith(yearMonth)) return@forEach
        val d = dStr.substring(8, 10).toIntOrNull() ?: return@forEach
        if (d in 1..nDays) buckets[d - 1] += e.totalSelling
    }
    val raw = buckets.toList()

    // 5 evenly-spaced labels: 1, 1/4, 1/2, 3/4, last
    val markers = listOf(
        1,
        (nDays * 0.25).toInt().coerceAtLeast(2),
        (nDays * 0.5).toInt().coerceAtLeast(3),
        (nDays * 0.75).toInt().coerceAtLeast(4),
        nDays,
    ).distinct()
    val labels = markers.map { "$it $mAbbr" }
    val rangeLbl = "$mAbbr $year"
    return ChartSeries(raw, labels, rangeLbl, raw.sum())
}

/** 13 weekly buckets across the last quarter (~90 days, oldest → newest). */
private fun List<SaleEntry>.quarterSeries(currentYearMonth: String): ChartSeries {
    if (currentYearMonth.isBlank()) {
        return ChartSeries(List(13) { 0.0 }, listOf("W1", "W4", "W7", "W10", "W13"), "Last 3 months", 0.0)
    }
    val year  = currentYearMonth.substring(0, 4).toIntOrNull() ?: 0
    val month = currentYearMonth.substring(5, 7).toIntOrNull() ?: 0
    // Build the three target months (oldest → newest)
    val months = (0..2).map { offset ->
        val (y, m) = subtractMonths(year, month, 2 - offset)
        "$y-${m.toString().padStart(2, '0')}"
    }
    val raw = months.map { ym -> sumByMonth(ym) }
    val labels = months.map { ym ->
        val mNum = ym.substring(5, 7).toIntOrNull() ?: 0
        monthAbbr(mNum)
    }
    return ChartSeries(raw, labels, "Last 3 months", raw.sum())
}

/** 12 monthly buckets ending at the current month (oldest → newest). */
private fun List<SaleEntry>.yearSeries(currentYearMonth: String): ChartSeries {
    if (currentYearMonth.isBlank()) {
        return ChartSeries(List(12) { 0.0 },
            listOf("Jan", "Apr", "Jul", "Oct", "Dec"), "Last 12 months", 0.0)
    }
    val year  = currentYearMonth.substring(0, 4).toIntOrNull() ?: 0
    val month = currentYearMonth.substring(5, 7).toIntOrNull() ?: 0
    val months = (0..11).map { offset ->
        val (y, m) = subtractMonths(year, month, 11 - offset)
        Triple(y, m, "$y-${m.toString().padStart(2, '0')}")
    }
    val raw = months.map { (_, _, ym) -> sumByMonth(ym) }
    // Show ~5 labels evenly across 12 buckets so the axis stays readable.
    val markers = listOf(0, 3, 6, 9, 11)
    val labels  = markers.map { i -> monthAbbr(months[i].second) }
    return ChartSeries(raw, labels, "Last 12 months", raw.sum())
}

// ─── Customer series per window (distinct customers per bucket) ─────────────

private fun List<SaleEntry>.customerSeriesFor(
    range: DashboardRange,
    currentYearMonth: String,
): ChartSeries = when (range) {
    DashboardRange.WEEK    -> customerWeekSeries()
    DashboardRange.MONTH   -> customerMonthSeries(currentYearMonth)
    DashboardRange.QUARTER -> customerQuarterSeries(currentYearMonth)
    DashboardRange.YEAR    -> customerYearSeries(currentYearMonth)
}

/** Distinct customers per day for the last 7 days with sales. */
private fun List<SaleEntry>.customerWeekSeries(): ChartSeries {
    val byDate = groupBy { it.date }
    val days = byDate.keys.sortedDescending().take(7).reversed()
    val raw = days.map { d -> byDate[d]!!.distinctCustomerCount().toDouble() }
    val labels = days.map { d ->
        val day = d.substring(8, 10).toIntOrNull()?.toString() ?: "?"
        val mon = d.substring(5, 7).toMonthAbbr()
        "$day $mon"
    }.ifEmpty { listOf("—", "—", "—", "—", "—", "—", "—") }
    val padded = if (raw.size < 2) List(7) { 0.0 } else raw
    val padLbls = if (labels.size < 2) List(7) { "—" } else labels
    return ChartSeries(padded, padLbls, "Last 7 days", padded.sum())
}

/** Distinct customers per day in [yearMonth]. */
private fun List<SaleEntry>.customerMonthSeries(yearMonth: String): ChartSeries {
    if (yearMonth.isBlank()) {
        return ChartSeries(List(7) { 0.0 }, listOf("1", "8", "15", "22", "30"), "This Month", 0.0)
    }
    val year = yearMonth.substring(0, 4).toIntOrNull() ?: return ChartSeries(
        List(7) { 0.0 }, listOf("1", "8", "15", "22", "30"), "This Month", 0.0,
    )
    val month = yearMonth.substring(5, 7).toIntOrNull() ?: return ChartSeries(
        List(7) { 0.0 }, listOf("1", "8", "15", "22", "30"), "This Month", 0.0,
    )
    val nDays = daysInMonth(year, month)
    val mAbbr = monthAbbr(month)
    val byDate = groupBy { it.date }
    val raw = (1..nDays).map { d ->
        val key = "$yearMonth-${d.toString().padStart(2, '0')}"
        byDate[key]?.distinctCustomerCount()?.toDouble() ?: 0.0
    }
    val markers = listOf(
        1,
        (nDays * 0.25).toInt().coerceAtLeast(2),
        (nDays * 0.5).toInt().coerceAtLeast(3),
        (nDays * 0.75).toInt().coerceAtLeast(4),
        nDays,
    ).distinct()
    val labels = markers.map { "$it $mAbbr" }
    return ChartSeries(raw, labels, "$mAbbr $year", raw.sum())
}

/** Distinct customers per month for the last 3 months. */
private fun List<SaleEntry>.customerQuarterSeries(currentYearMonth: String): ChartSeries {
    if (currentYearMonth.isBlank()) {
        return ChartSeries(List(3) { 0.0 }, listOf("—", "—", "—"), "Last 3 months", 0.0)
    }
    val year = currentYearMonth.substring(0, 4).toIntOrNull() ?: 0
    val month = currentYearMonth.substring(5, 7).toIntOrNull() ?: 0
    val months = (0..2).map { offset ->
        val (y, m) = subtractMonths(year, month, 2 - offset)
        "$y-${m.toString().padStart(2, '0')}"
    }
    val raw = months.map { ym -> distinctCustomersForMonth(ym).toDouble() }
    val labels = months.map { ym -> monthAbbr(ym.substring(5, 7).toIntOrNull() ?: 0) }
    return ChartSeries(raw, labels, "Last 3 months", raw.sum())
}

/** Distinct customers per month for the last 12 months. */
private fun List<SaleEntry>.customerYearSeries(currentYearMonth: String): ChartSeries {
    if (currentYearMonth.isBlank()) {
        return ChartSeries(List(12) { 0.0 },
            listOf("Jan", "Apr", "Jul", "Oct", "Dec"), "Last 12 months", 0.0)
    }
    val year = currentYearMonth.substring(0, 4).toIntOrNull() ?: 0
    val month = currentYearMonth.substring(5, 7).toIntOrNull() ?: 0
    val months = (0..11).map { offset ->
        val (y, m) = subtractMonths(year, month, 11 - offset)
        Triple(y, m, "$y-${m.toString().padStart(2, '0')}")
    }
    val raw = months.map { (_, _, ym) -> distinctCustomersForMonth(ym).toDouble() }
    val markers = listOf(0, 3, 6, 9, 11)
    val labels = markers.map { i -> monthAbbr(months[i].second) }
    return ChartSeries(raw, labels, "Last 12 months", raw.sum())
}

/** Distinct customers across the whole selected window. */
private fun List<SaleEntry>.distinctCustomersForRange(
    range: DashboardRange,
    currentYearMonth: String,
): Int = when (range) {
    DashboardRange.WEEK -> {
        val days = groupBy { it.date }.keys.sortedDescending().take(7).toSet()
        filter { it.date in days }.distinctCustomerCount()
    }
    DashboardRange.MONTH -> distinctCustomersForMonth(currentYearMonth)
    DashboardRange.QUARTER -> {
        val months = quarterMonthKeys(currentYearMonth)
        filter { e -> months.any { e.date.startsWith(it) } }.distinctCustomerCount()
    }
    DashboardRange.YEAR -> {
        val months = yearMonthKeys(currentYearMonth, 12)
        filter { e -> months.any { e.date.startsWith(it) } }.distinctCustomerCount()
    }
}

/** Window-over-window growth for distinct customers. Null when no baseline. */
private fun List<SaleEntry>.customerRangeGrowth(
    range: DashboardRange,
    currentYearMonth: String,
): Double? = when (range) {
    DashboardRange.WEEK -> {
        val days = groupBy { it.date }.keys.sortedDescending()
        val last = days.take(7).toSet()
        val prev = days.drop(7).take(7).toSet()
        val lastCount = filter { it.date in last }.distinctCustomerCount().toDouble()
        val prevCount = filter { it.date in prev }.distinctCustomerCount().toDouble()
        pctChange(lastCount, prevCount)
    }
    DashboardRange.MONTH -> {
        val prev = previousMonthKey(currentYearMonth)
        pctChange(
            distinctCustomersForMonth(currentYearMonth).toDouble(),
            distinctCustomersForMonth(prev).toDouble()
        )
    }
    DashboardRange.QUARTER -> {
        val cur = quarterMonthKeys(currentYearMonth)
        val prev = previousQuarterMonthKeys(currentYearMonth)
        pctChange(
            distinctCountForMonths(cur).toDouble(),
            distinctCountForMonths(prev).toDouble()
        )
    }
    DashboardRange.YEAR -> {
        val cur = yearMonthKeys(currentYearMonth, 12)
        val prev = previousYearMonthKeys(currentYearMonth)
        pctChange(
            distinctCountForMonths(cur).toDouble(),
            distinctCountForMonths(prev).toDouble()
        )
    }
}

private fun List<SaleEntry>.distinctCustomerCount(): Int =
    map { it.customerName.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()
        .size

private fun List<SaleEntry>.distinctCountForMonths(months: List<String>): Int =
    filter { e -> months.any { e.date.startsWith(it) } }.distinctCustomerCount()

/** Window-over-window revenue growth for the selected range. Null when no baseline. */
private fun List<SaleEntry>.revenueRangeGrowth(
    range: DashboardRange,
    currentYearMonth: String,
): Double? = when (range) {
    DashboardRange.WEEK -> {
        val (last7, prev7) = lastTwoWeekTotals()
        pctChange(last7, prev7)
    }
    DashboardRange.MONTH -> {
        val prev = previousMonthKey(currentYearMonth)
        pctChange(sumByMonth(currentYearMonth), sumByMonth(prev))
    }
    DashboardRange.QUARTER -> {
        pctChange(
            sumForMonths(quarterMonthKeys(currentYearMonth)),
            sumForMonths(previousQuarterMonthKeys(currentYearMonth))
        )
    }
    DashboardRange.YEAR -> {
        pctChange(
            sumForMonths(yearMonthKeys(currentYearMonth, 12)),
            sumForMonths(previousYearMonthKeys(currentYearMonth))
        )
    }
}

private fun List<SaleEntry>.sumForMonths(months: List<String>): Double =
    filter { e -> months.any { e.date.startsWith(it) } }.sumOf { it.totalSelling }

private fun quarterMonthKeys(currentYearMonth: String): List<String> {
    if (currentYearMonth.isBlank()) return emptyList()
    val year = currentYearMonth.substring(0, 4).toIntOrNull() ?: return emptyList()
    val month = currentYearMonth.substring(5, 7).toIntOrNull() ?: return emptyList()
    return (0..2).map { offset ->
        val (y, m) = subtractMonths(year, month, 2 - offset)
        "$y-${m.toString().padStart(2, '0')}"
    }
}

private fun previousQuarterMonthKeys(currentYearMonth: String): List<String> {
    if (currentYearMonth.isBlank()) return emptyList()
    val year = currentYearMonth.substring(0, 4).toIntOrNull() ?: return emptyList()
    val month = currentYearMonth.substring(5, 7).toIntOrNull() ?: return emptyList()
    return (3..5).map { offset ->
        val (y, m) = subtractMonths(year, month, offset)
        "$y-${m.toString().padStart(2, '0')}"
    }.reversed()
}

private fun yearMonthKeys(currentYearMonth: String, count: Int): List<String> {
    if (currentYearMonth.isBlank()) return emptyList()
    val year = currentYearMonth.substring(0, 4).toIntOrNull() ?: return emptyList()
    val month = currentYearMonth.substring(5, 7).toIntOrNull() ?: return emptyList()
    return (0 until count).map { offset ->
        val (y, m) = subtractMonths(year, month, (count - 1) - offset)
        "$y-${m.toString().padStart(2, '0')}"
    }
}

private fun previousYearMonthKeys(currentYearMonth: String): List<String> {
    if (currentYearMonth.isBlank()) return emptyList()
    val year = currentYearMonth.substring(0, 4).toIntOrNull() ?: return emptyList()
    val month = currentYearMonth.substring(5, 7).toIntOrNull() ?: return emptyList()
    return (12 until 24).map { offset ->
        val (y, m) = subtractMonths(year, month, offset)
        "$y-${m.toString().padStart(2, '0')}"
    }.reversed()
}

private fun previousMonthKey(currentYearMonth: String): String {
    if (currentYearMonth.isBlank()) return ""
    val year = currentYearMonth.substring(0, 4).toIntOrNull() ?: return ""
    val month = currentYearMonth.substring(5, 7).toIntOrNull() ?: return ""
    val (y, m) = subtractMonths(year, month, 1)
    return "$y-${m.toString().padStart(2, '0')}"
}

// ─── Internal helpers ────────────────────────────────────────────────────────

private fun List<SaleEntry>.sumByMonth(yearMonth: String): Double {
    if (yearMonth.isBlank()) return 0.0
    return filter { it.date.startsWith(yearMonth) }
        .sumOf { it.totalSelling }
}

private fun List<SaleEntry>.distinctCustomersForMonth(yearMonth: String): Int {
    if (yearMonth.isBlank()) return 0
    return filter { it.date.startsWith(yearMonth) }
        .map { it.customerName.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()
        .size
}

private fun List<SaleEntry>.lastTwoWeekTotals(): Pair<Double, Double> {
    val byDate = groupBy { it.date }
    val days = byDate.keys.sortedDescending()
    val last7 = days.take(7).sumOf { d -> byDate[d]!!.sumOf { it.totalSelling } }
    val prev7 = days.drop(7).take(7).sumOf { d -> byDate[d]!!.sumOf { it.totalSelling } }
    return last7 to prev7
}

private fun List<StockMovement>.netInwardForMonth(yearMonth: String): Int {
    if (yearMonth.isBlank()) return 0
    return filter { it.createdAt?.startsWith(yearMonth) == true && !it.source.isEmptyCansSource() }
        .sumOf { m ->
            when (m.type) {
                "inward"     -> m.qty
                "outward"    -> -m.qty
                "adjustment" -> m.qty
                else         -> 0
            }
        }
}

/** Total qty for one specific [type] (e.g. "inward" / "outward") in [yearMonth].
 *  Empty-can/case movements are excluded — they are tracked separately by the
 *  "Empty Cases" figure, not stock activity. */
private fun List<StockMovement>.totalForType(yearMonth: String, type: String): Int {
    if (yearMonth.isBlank()) return 0
    return filter { it.createdAt?.startsWith(yearMonth) == true && it.type == type && !it.source.isEmptyCansSource() }
        .sumOf { it.qty }
}

private fun pctChange(current: Double, previous: Double): Double? {
    if (previous == 0.0) return if (current == 0.0) 0.0 else null
    return (current - previous) / previous * 100.0
}

private fun buildYAxisLabels(maxRupees: Double): List<String> {
    if (maxRupees <= 0.0) return listOf("0", "0", "0", "0")
    val top = niceCeiling(maxRupees)
    val stops = listOf(top, top * 2 / 3, top / 3, 0.0)
    return stops.map { compactCurrencyShort(it) }
}

private fun niceCeiling(v: Double): Double {
    val targets = listOf(
        100.0, 250.0, 500.0,
        1_000.0, 2_000.0, 5_000.0, 10_000.0, 25_000.0, 50_000.0,
        1_00_000.0, 2_00_000.0, 5_00_000.0, 10_00_000.0,
    )
    return targets.firstOrNull { it >= v } ?: (((v / 1_00_000.0).toInt() + 1) * 1_00_000.0)
}

private fun compactCurrencyShort(v: Double): String = when {
    v >= 1_00_000.0 -> "${(v / 1_00_000.0 * 10).toLong() / 10.0}L"
    v >= 1_000.0    -> "${(v / 1_000.0).toLong()}K"
    else            -> "${v.toLong()}"
}

// ── Calendar helpers (no kotlinx-datetime types leak from this file) ────────

private fun daysInMonth(year: Int, month: Int): Int {
    val isLeap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11           -> 30
        2                     -> if (isLeap) 29 else 28
        else                  -> 30
    }
}

private fun monthAbbr(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
    5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
    9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> "—"
}

private fun String.toMonthAbbr(): String = monthAbbr(toIntOrNull() ?: 0)

private fun subtractMonths(year: Int, month: Int, n: Int): Pair<Int, Int> {
    var y = year
    var m = month - n
    while (m <= 0) { m += 12; y -= 1 }
    while (m > 12) { m -= 12; y += 1 }
    return y to m
}

