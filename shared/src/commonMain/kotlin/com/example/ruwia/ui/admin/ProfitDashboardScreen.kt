package com.example.ruwia.ui.admin

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.domain.*
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.dashboard.NTColors
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDateTime
import kotlin.time.Instant
import kotlin.math.*

private object SaaSColors {
    val Primary       get() = NTColors.Primary
    val Background    get() = NTColors.Background
    val Surface       get() = NTColors.Surface
    val SurfaceVar    get() = NTColors.SurfaceVar
    val TextPrimary   get() = NTColors.TextPrimary
    val TextSecondary get() = NTColors.TextSecondary
    val TextMuted     get() = NTColors.TextTertiary
    val Border        get() = NTColors.Border
    val Success       get() = NTColors.Success
    val Error         get() = NTColors.Error
    val ErrorLight    get() = NTColors.ErrorLight
    val Warning       get() = NTColors.Warning
    val PremiumCard   get() = Brush.linearGradient(listOf(NTColors.Primary, NTColors.PrimaryDark))
    val DeepTeal      get() = Color(0xFF0F2E2C)
    val Mint          get() = Color(0xFF5EEAD4)
    val MintDeep      get() = Color(0xFF0B6B5E)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitDashboardScreen(
    state: AdminState,
    onBack: () -> Unit,
    onExpenseMonthSelected: (String) -> Unit = {},
    onExpenseSave: (MonthlyExpense) -> Unit = {},
    onProductClick: (String) -> Unit = {},
    onAddStock: () -> Unit = {},
    onAddProduct: () -> Unit = {},
    onOpenTransactions: () -> Unit = {},
    onClearData: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val today = try {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    } catch (_: Exception) {
        LocalDateTime(2026, 6, 24, 12, 0)
    }
    val todayMs = dateToMs(today.year, today.monthNumber, today.dayOfMonth)
    var selectedYear  by remember { mutableStateOf(today.year) }
    var selectedMonth by remember { mutableStateOf(today.monthNumber - 1) } // 0-based
    var periodMode    by remember { mutableStateOf(AnalyticsPeriod.MONTH) }
    var filterFrom    by remember { mutableStateOf("") } // YYYY-MM-DD or ""
    var filterTo      by remember { mutableStateOf("") }
    var showRangeDialog by remember { mutableStateOf(false) }
    // Chart-card scope: independent 7D / 30D / 3M / 1Y window ending at the analysis end.
    var chartRange    by remember { mutableStateOf(ChartRange.M3) }

    val customActive = filterFrom.isNotBlank() || filterTo.isNotBlank()
    val monthKey = ymKey(selectedYear, selectedMonth + 1)
    val monthLen = daysInMonth(selectedYear, selectedMonth + 1)
    val monthStartMs = dateToMs(selectedYear, selectedMonth + 1, 1)
    val monthEndMs = dateToMs(selectedYear, selectedMonth + 1, monthLen)
    val anchorMs = monthEndMs

    // Years actually present in the data (never hard-coded).
    val availableYears = remember(state.saleEntries, state.monthlyExpenses, today.year) {
        (state.saleEntries.mapNotNull { it.date.take(4).toIntOrNull() } +
         state.monthlyExpenses.keys.mapNotNull { it.take(4).toIntOrNull() } +
         today.year)
            .distinct().sortedDescending().ifEmpty { listOf(today.year) }
    }
    LaunchedEffect(selectedYear, availableYears) {
        if (selectedYear !in availableYears) selectedYear = availableYears.first()
    }

    // Resolve the analysis window [winStartMs, winEndMs] (inclusive, UTC).
    val (winStartMs, winEndMs) = remember(selectedYear, selectedMonth, periodMode, filterFrom, filterTo) {
        when {
            customActive -> {
                var s = filterFrom.takeIf { it.isNotBlank() }?.let { strToMs(it) } ?: 0L
                var e = filterTo.takeIf { it.isNotBlank() }?.let { strToMs(it) } ?: todayMs
                if (s > e) { val t = s; s = e; e = t }
                s to e
            }
            periodMode == AnalyticsPeriod.DAY -> anchorMs to anchorMs
            periodMode == AnalyticsPeriod.WEEK -> (anchorMs - 6 * DAY_MS) to anchorMs
            periodMode == AnalyticsPeriod.QUARTER -> {
                val (qy, qm) = shiftMonths(selectedYear, selectedMonth + 1, -2)
                dateToMs(qy, qm, 1) to monthEndMs
            }
            periodMode == AnalyticsPeriod.YEAR -> dateToMs(selectedYear, 1, 1) to dateToMs(selectedYear, 12, 31)
            else -> monthStartMs to monthEndMs
        }
    }
    val spanDays = ((winEndMs - winStartMs) / DAY_MS + 1).toInt().coerceAtLeast(1)
    val prevEndMs = winStartMs - DAY_MS
    val prevStartMs = winStartMs - spanDays * DAY_MS

    // Chart-card window: 7D / 30D / 3M / 1Y ending at the analysis end.
    val (chartStartMs, chartEndMs) = remember(chartRange, winEndMs) {
        val (ey, em) = yearMonthOf(winEndMs)
        when (chartRange) {
            ChartRange.D7 -> (winEndMs - 6 * DAY_MS) to winEndMs
            ChartRange.D30 -> (winEndMs - 29 * DAY_MS) to winEndMs
            ChartRange.M3 -> {
                val (sy, sm) = shiftMonths(ey, em, -2)
                dateToMs(sy, sm, 1) to winEndMs
            }
            ChartRange.Y1 -> {
                val (sy, sm) = shiftMonths(ey, em, -11)
                dateToMs(sy, sm, 1) to winEndMs
            }
        }
    }

    // Months touched by analysis + previous + chart windows (expense lazy-load).
    val windowMonthKeys = remember(winStartMs, winEndMs, prevStartMs, chartStartMs) {
        monthKeysBetween(minOf(prevStartMs, chartStartMs), winEndMs)
    }
    LaunchedEffect(windowMonthKeys) {
        windowMonthKeys.forEach { key ->
            if (!state.monthlyExpenses.containsKey(key)) onExpenseMonthSelected(key)
        }
    }

    // ── Single source of truth: figures derive from saleEntries + ────────────
    // monthlyExpenses (+ movements for in/out) and recompute on any change.
    fun salesIn(s: Long, e: Long): List<SaleEntry> =
        state.saleEntries.filter { saleMs(it.date)?.let { ms -> ms in s..e } == true }

    fun monthlyTotal(key: String): Double =
        state.monthlyExpenses[key]?.total
            ?: state.currentMonthExpense?.takeIf { it.month == key }?.total
            ?: state.lastMonthExpense?.takeIf { it.month == key }?.total
            ?: 0.0

    fun expenseForRange(s: Long, e: Long): Double {
        val (sy, sm) = yearMonthOf(s)
        val (ey, em) = yearMonthOf(e)
        var (y, m) = sy to sm
        var total = 0.0
        while (ymKey(y, m) <= ymKey(ey, em)) {
            val mStart = dateToMs(y, m, 1)
            val mEnd = dateToMs(y, m, daysInMonth(y, m))
            val overlap = (minOf(e, mEnd) - maxOf(s, mStart)) / DAY_MS + 1
            if (overlap > 0) total += monthlyTotal(ymKey(y, m)) * overlap / daysInMonth(y, m)
            val (ny, nm) = shiftMonths(y, m, 1)
            y = ny; m = nm
        }
        return total
    }

    // Day revenue caches for cost matching below.
    val dateRevenueCache = remember(state.saleEntries) {
        state.saleEntries.groupBy { it.date }
            .mapValues { (_, rows) -> rows.sumOf { it.totalSelling } }
    }
    val monthRevenueCache = remember(dateRevenueCache) {
        dateRevenueCache.entries.groupBy({ it.key.take(7) }, { it.value })
            .mapValues { (_, v) -> v.sum() }
    }

    /**
     * Day-wise expense for one calendar day.
     *
     * Expenses are recorded monthly, so each day bears its revenue share of
     * its month's total (cost matched to the sales it supported): a day with
     * half the month's revenue carries half the expense, a zero-sale day
     * carries zero. Months with no revenue fall back to an even spread.
     * Either way the days always sum back to the exact monthly total.
     */
    fun dailyExpense(dayMs: Long): Double {
        val (y, m) = yearMonthOf(dayMs)
        val key = ymKey(y, m)
        val total = monthlyTotal(key)
        if (total <= 0.0) return 0.0
        val monthRev = monthRevenueCache[key] ?: 0.0
        if (monthRev <= 0.0) return total / daysInMonth(y, m)
        val dayRev = dateRevenueCache[msToDateStr(dayMs)] ?: 0.0
        return total * dayRev / monthRev
    }

    fun expenseDaysIn(s: Long, e: Long): Double {
        var total = 0.0
        var d = s
        while (d <= e) {
            total += dailyExpense(d)
            d += DAY_MS
        }
        return total
    }

    fun customersIn(s: Long, e: Long): Int =
        state.saleEntries.filter { saleMs(it.date)?.let { ms -> ms in s..e } == true }
            .map { it.customerName.trim().lowercase() }
            .filter { it.isNotEmpty() }.distinct().size

    fun movementsIn(s: Long, e: Long) = state.recentMovements.filter { m ->
        val d = m.createdAt?.take(10) ?: return@filter false
        val ms = strToMs(d)
        ms in s..e && !m.source.isEmptyCansSource()
    }

    val wSales = salesIn(winStartMs, winEndMs)
    val revenue = wSales.sumOf { it.totalSelling }
    val gross = wSales.sumOf { it.totalMargin }
    val expenses = expenseForRange(winStartMs, winEndMs)
    val netProfit = revenue - expenses
    val netMarginPct = if (revenue > 0) netProfit / revenue * 100.0 else 0.0
    val customers = customersIn(winStartMs, winEndMs)

    val pSales = salesIn(prevStartMs, prevEndMs)
    val prevRevenue = pSales.sumOf { it.totalSelling }
    val prevExpenses = expenseForRange(prevStartMs, prevEndMs)
    val prevProfit = prevRevenue - prevExpenses
    val prevNetMargin = if (prevRevenue > 0) prevProfit / prevRevenue * 100.0 else 0.0
    val prevCustomers = customersIn(prevStartMs, prevEndMs)
    val hasPrevBaseline = prevRevenue > 0.0 || prevExpenses > 0.0

    val revenueGrowth = pctOrNull(revenue, prevRevenue)
    val profitGrowth = pctOrNull(netProfit, prevProfit)
    val expenseGrowth = pctOrNull(expenses, prevExpenses)
    val customerGrowth = pctOrNull(customers.toDouble(), prevCustomers.toDouble())
    val marginDeltaPp = if (hasPrevBaseline) netMarginPct - prevNetMargin else null
    val prevRangeLabel = rangeLabel(prevStartMs, prevEndMs)
    val windowLabel = rangeLabel(winStartMs, winEndMs)

    // Chart buckets follow the chart-card range (independent of analysis window).
    val buckets = remember(chartStartMs, chartEndMs) { buildBuckets(chartStartMs, chartEndMs) }
    val bucketRevenue = buckets.map { b -> salesIn(b.startMs, b.endMs).sumOf { it.totalSelling } }
    // Day-wise costs: each bucket sums its days' revenue-matched expenses,
    // so the bars always reconcile exactly with the window totals.
    val bucketExpenses = buckets.map { b -> expenseDaysIn(b.startMs, b.endMs) }
    // Monthly buckets get the fixed Jan · Mar · May · … · Dec rhythm.
    val chartMonthly = ((chartEndMs - chartStartMs) / DAY_MS + 1) > 92
    val tickIndices = remember(buckets, chartMonthly) {
        chartTickIndices(buckets.size, chartMonthly)
    }

    // Month-scoped expense record kept for the editor (edits one month).
    val selectedExpense = state.monthlyExpenses[monthKey]
        ?: state.currentMonthExpense?.takeIf { it.month == monthKey }
        ?: MonthlyExpense(month = monthKey)

    // Report helpers: product ranks + in/out for the window.
    val wMovements = movementsIn(winStartMs, winEndMs)
    val inwardUnits = wMovements.filter { it.type == "inward" }.sumOf { it.qty }
    val outwardUnits = wMovements.filter { it.type == "outward" }.sumOf { it.qty }
    val topSellers = wSales.groupBy { it.productName }
        .map { (name, entries) ->
            TopProduct(name = name, qty = entries.sumOf { it.qty }, revenue = entries.sumOf { it.totalSelling })
        }
        .sortedByDescending { it.revenue }
        .take(5)

    // Expense Editing State
    var editingField by remember { mutableStateOf<String?>(null) }
    var editValue by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var customAmount by remember { mutableStateOf("") }

    val onCategoryTap: (String) -> Unit = { field ->
        editingField = field
        editValue = when (field) {
            "shopRent"      -> selectedExpense.shopRent.toInt().toString()
            "adminSalary"   -> selectedExpense.adminSalary.toInt().toString()
            "deliveryStaff" -> selectedExpense.deliveryStaff.toInt().toString()
            "misc"          -> selectedExpense.miscellaneous.toInt().toString()
            "bike"          -> selectedExpense.bikeExpense.toInt().toString()
            else -> ""
        }
    }
    val onDeleteField: (String) -> Unit = { field ->
        val updated = when (field) {
            "shopRent"      -> selectedExpense.copy(shopRent = 0.0)
            "adminSalary"   -> selectedExpense.copy(adminSalary = 0.0)
            "deliveryStaff" -> selectedExpense.copy(deliveryStaff = 0.0)
            "misc"          -> selectedExpense.copy(miscellaneous = 0.0)
            "bike"          -> selectedExpense.copy(bikeExpense = 0.0)
            else -> {
                val idx = field.substringAfter("custom_").toIntOrNull()
                val list = selectedExpense.customExpensesList.toMutableList()
                if (idx != null && idx in list.indices) {
                    list.removeAt(idx)
                    selectedExpense.copy(customExpenses = jsonEncode(list))
                } else {
                    selectedExpense
                }
            }
        }
        if (updated != selectedExpense) onExpenseSave(updated)
    }

    var reportNewestFirst by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(SaaSColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBarSection(
                year     = selectedYear,
                availableYears = availableYears,
                onYearChange = { selectedYear = it },
                onBack   = onBack,
                onOpenRange = { showRangeDialog = true },
                customActive = customActive,
                customLabel = if (customActive) rangeLabel(
                    filterFrom.takeIf { it.isNotBlank() }?.let { strToMs(it) } ?: winStartMs,
                    filterTo.takeIf { it.isNotBlank() }?.let { strToMs(it) } ?: winEndMs,
                ) else "",
                onClearCustom = { filterFrom = ""; filterTo = "" },
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    bottom = contentPadding.calculateBottomPadding() + 32.dp,
                )
            ) {
                // ── Period presets ─────────────────────────────────────────
                item {
                    PeriodPills(
                        selected = periodMode,
                        customActive = customActive,
                        onSelect = {
                            periodMode = it
                            filterFrom = ""
                            filterTo = ""
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // ── Month anchor ───────────────────────────────────────────
                item {
                    MonthSelectorRow(
                        months   = months,
                        selected = selectedMonth,
                        onSelect = {
                            selectedMonth = it
                            filterFrom = ""
                            filterTo = ""
                            periodMode = AnalyticsPeriod.MONTH
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // A. Business Summary ──────────────────────────────────────
                item {
                    BusinessSummaryCard(
                        windowLabel = windowLabel,
                        revenue = revenue,
                        expenses = expenses,
                        profit = netProfit,
                        marginPct = netMarginPct,
                        growth = revenueGrowth,
                        prevLabel = prevRangeLabel,
                        hasPrevBaseline = hasPrevBaseline,
                        prevRevenue = prevRevenue,
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // B. Revenue Analytics ─────────────────────────────────────
                item {
                    RevenueAnalyticsSection(
                        totalRev = revenue,
                        totalProfit = netProfit,
                        margin = netMarginPct,
                        growth = revenueGrowth,
                        profitGrowth = profitGrowth,
                        chartRange = chartRange,
                        onChartRangeChange = { chartRange = it },
                        chartBuckets = buckets,
                        chartRevenue = bucketRevenue,
                        chartExpenses = bucketExpenses,
                        tickIndices = tickIndices,
                    )
                    Spacer(Modifier.height(24.dp))
                }

                // D. Report Summary (+ inward / outward) ───────────────────
                item {
                    ReportSummarySection(
                        sales = wSales,
                        inward = inwardUnits,
                        outward = outwardUnits,
                        windowLabel = windowLabel,
                        newestFirst = reportNewestFirst,
                        onOrderChange = { reportNewestFirst = it },
                        onProductClick = onProductClick,
                        onOpenTransactions = onOpenTransactions,
                        movements = wMovements,
                        products = state.productCategories,
                    )
                    Spacer(Modifier.height(24.dp))
                }

                // E. Expense Analytics (+ Add Expense) ─────────────────────
                item {
                    ExpenseAnalyticsSection(
                        expense = selectedExpense,
                        onCategoryTap = onCategoryTap,
                        onDeleteField = onDeleteField,
                        onAddExpense = {
                            customName = ""
                            customAmount = ""
                            editingField = "new"
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // AI Insights ──────────────────────────────────────────────
                item {
                    AIInsightsSection(
                        revenue = revenue,
                        profit = netProfit,
                        marginPct = netMarginPct,
                        revenueGrowth = revenueGrowth,
                        profitGrowth = profitGrowth,
                        expenses = expenses,
                        topSellers = topSellers,
                        customerGrowth = customerGrowth,
                        hasPrevBaseline = hasPrevBaseline,
                    )
                    Spacer(Modifier.height(24.dp))
                }

                // Top Best Sellers ─────────────────────────────────────────
                item {
                    TopSellersSection(
                        products = topSellers,
                        onProductClick = onProductClick,
                    )
                }
            }
        }
    }

    // From/To range dialog (calendar entry point).
    if (showRangeDialog) {
        DateRangeDialog(
            from = filterFrom,
            to = filterTo,
            onFromChange = { filterFrom = it },
            onToChange = { filterTo = it },
            onClear = { filterFrom = ""; filterTo = "" },
            onDismiss = { showRangeDialog = false },
        )
    }
    // ── Edit Expense Dialog ──────────────────────────────────────────────────
    if (editingField != null) {
        val field = editingField!!
        val isCustom = field == "new" || field.startsWith("custom_")
        val label = if (isCustom) {
            if (field == "new") "Add New Expense" else "Edit Expense"
        } else {
            when (field) {
                "shopRent" -> "Shop Rent"
                "adminSalary" -> "Admin Salary"
                "deliveryStaff" -> "Delivery Staff"
                "misc" -> "Miscellaneous"
                "bike" -> "Bike Expense"
                else -> ""
            }
        }

        Dialog(onDismissRequest = { editingField = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SaaSColors.Surface, RoundedCornerShape(24.dp))
                    .border(1.dp, SaaSColors.Border, RoundedCornerShape(24.dp))
                    .padding(24.dp),
            ) {
                Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
                Spacer(Modifier.height(16.dp))

                if (isCustom) {
                    Text("Expense Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SaaSColors.TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SaaSColors.Border, RoundedCornerShape(12.dp))
                            .background(SaaSColors.Background, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            textStyle = TextStyle(fontSize = 14.sp, color = SaaSColors.TextPrimary),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Text("Amount (₹)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SaaSColors.TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SaaSColors.Border, RoundedCornerShape(12.dp))
                        .background(SaaSColors.Background, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = if (isCustom) customAmount else editValue,
                        onValueChange = { if (isCustom) customAmount = it else editValue = it },
                        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { editingField = null },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = SaaSColors.TextSecondary)
                    }
                    Button(
                        onClick = {
                            val amount = (if (isCustom) customAmount else editValue).toDoubleOrNull() ?: 0.0
                            val updated = if (isCustom) {
                                if (field == "new") {
                                    val list = selectedExpense.customExpensesList.toMutableList()
                                    list.add(CustomExpense(customName.ifBlank { "Expense" }, amount))
                                    selectedExpense.copy(customExpenses = jsonEncode(list))
                                } else {
                                    val idx = field.substringAfter("custom_").toIntOrNull() ?: 0
                                    val list = selectedExpense.customExpensesList.toMutableList()
                                    if (idx in list.indices) {
                                        list[idx] = CustomExpense(customName.ifBlank { list[idx].name }, amount)
                                    }
                                    selectedExpense.copy(customExpenses = jsonEncode(list))
                                }
                            } else {
                                when (field) {
                                    "shopRent"      -> selectedExpense.copy(shopRent = amount)
                                    "adminSalary"   -> selectedExpense.copy(adminSalary = amount)
                                    "deliveryStaff" -> selectedExpense.copy(deliveryStaff = amount)
                                    "misc"          -> selectedExpense.copy(miscellaneous = amount)
                                    "bike"          -> selectedExpense.copy(bikeExpense = amount)
                                    else -> selectedExpense
                                }
                            }
                            onExpenseSave(updated)
                            editingField = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaaSColors.Primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ── Analytics window engine (all dynamic, single source of truth) ─────────

private enum class AnalyticsPeriod { DAY, WEEK, MONTH, QUARTER, YEAR }

/** Chart-card scope: independent 7D / 30D / 3M / 1Y window. */
private enum class ChartRange(val label: String) {
    D7("7D"), D30("30D"), M3("3M"), Y1("1Y"),
}

private data class TopProduct(val name: String, val qty: Int, val revenue: Double)
private data class ChartBucket(val label: String, val startMs: Long, val endMs: Long)

private const val DAY_MS = 86_400_000L

private fun dateToMs(year: Int, month: Int, day: Int): Long =
    LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun msToDateStr(ms: Long): String {
    val d = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date
    return "%04d-%02d-%02d".format(d.year, d.monthNumber, d.dayOfMonth)
}

private fun strToMs(s: String): Long {
    val p = s.split("-")
    return dateToMs(p[0].toInt(), p[1].toInt(), p[2].toInt())
}

private fun saleMs(date: String): Long? = try {
    val p = date.split("-")
    if (p.size < 3) null else dateToMs(p[0].toInt(), p[1].toInt(), p[2].toInt())
} catch (_: Exception) {
    null
}

private fun ymKey(year: Int, month: Int): String = "%04d-%02d".format(year, month)

private fun daysInMonth(year: Int, month: Int): Int {
    val leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (leap) 29 else 28
        else -> 30
    }
}

private fun shiftMonths(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val total = (year * 12 + (month - 1)) + delta
    return (total / 12) to (total % 12 + 1)
}

private fun yearMonthOf(ms: Long): Pair<Int, Int> {
    val d = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date
    return d.year to d.monthNumber
}

private fun monthKeysBetween(startMs: Long, endMs: Long): List<String> {
    val (sy, sm) = yearMonthOf(startMs)
    val (ey, em) = yearMonthOf(endMs)
    val out = mutableListOf<String>()
    var (y, m) = sy to sm
    while (ymKey(y, m) <= ymKey(ey, em)) {
        out.add(ymKey(y, m))
        val (ny, nm) = shiftMonths(y, m, 1)
        y = ny; m = nm
    }
    return out
}

private val monthAbbr = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun displayDate(ms: Long): String {
    val d = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date
    return "${monthAbbr[d.monthNumber - 1]} ${d.dayOfMonth.toString().padStart(2, '0')}"
}

private fun displayDateFull(ms: Long): String {
    val d = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date
    return "${monthAbbr[d.monthNumber - 1]} ${d.dayOfMonth}, ${d.year}"
}

/** Human window label: "Jan 2026", "Jan 5 – Jan 11", "Q1 2026" style range text. */
private fun rangeLabel(startMs: Long, endMs: Long): String {
    if (startMs == endMs) return displayDateFull(startMs)
    val (sy, sm, sd) = Triple(
        Instant.fromEpochMilliseconds(startMs).toLocalDateTime(TimeZone.UTC).date.year,
        Instant.fromEpochMilliseconds(startMs).toLocalDateTime(TimeZone.UTC).date.monthNumber,
        Instant.fromEpochMilliseconds(startMs).toLocalDateTime(TimeZone.UTC).date.dayOfMonth,
    )
    val (ey, em, ed) = Triple(
        Instant.fromEpochMilliseconds(endMs).toLocalDateTime(TimeZone.UTC).date.year,
        Instant.fromEpochMilliseconds(endMs).toLocalDateTime(TimeZone.UTC).date.monthNumber,
        Instant.fromEpochMilliseconds(endMs).toLocalDateTime(TimeZone.UTC).date.dayOfMonth,
    )
    if (sy == ey && sm == em) {
        if (sd == 1 && ed == daysInMonth(sy, sm)) return "${monthAbbr[sm - 1]} $sy"
        return "${monthAbbr[sm - 1]} $sd–$ed, $sy"
    }
    return "${monthAbbr[sm - 1]} $sd – ${monthAbbr[em - 1]} $ed, $ey"
}

/** Buckets adapt to span: 1 day, daily (≤31d), weekly (≤92d), else monthly. */
/**
 * X-axis tick indices for a bucket count.
 * Monthly buckets use the fixed Jan · Mar · May · Jul · Sep · Nov · Dec
 * rhythm (even indices plus the last month); everything else shows at most
 * 6 evenly spaced ticks so neighbours never collapse into each other.
 */
private fun chartTickIndices(count: Int, monthly: Boolean): List<Int> {
    if (count <= 0) return emptyList()
    if (monthly) {
        return ((0 until count) step 2).toList().let {
            if (it.last() == count - 1) it else it + (count - 1)
        }
    }
    if (count <= 6) return (0 until count).toList()
    val every = ((count - 1) / 5 + 1)
    return ((0 until count) step every).toList().let {
        if (it.last() == count - 1) it else it + (count - 1)
    }
}

private fun buildBuckets(startMs: Long, endMs: Long): List<ChartBucket> {
    val days = ((endMs - startMs) / DAY_MS + 1).toInt().coerceAtLeast(1)
    if (days <= 1) return listOf(ChartBucket(displayDate(startMs), startMs, endMs))
    if (days <= 31) {
        return (0 until days).map { i ->
            val ms = startMs + i * DAY_MS
            val dd = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date
            val label = "${monthAbbr[dd.monthNumber - 1]} ${dd.dayOfMonth.toString().padStart(2, '0')}"
            ChartBucket(label, ms, ms)
        }
    }
    if (days <= 92) {
        val out = mutableListOf<ChartBucket>()
        var s = startMs
        while (s <= endMs) {
            val e = minOf(s + 6 * DAY_MS, endMs)
            out.add(ChartBucket(displayDate(s), s, e))
            s = e + DAY_MS
        }
        return out
    }
    val (sy, sm) = yearMonthOf(startMs)
    val (ey, em) = yearMonthOf(endMs)
    val sameYear = sy == ey
    val out = mutableListOf<ChartBucket>()
    var (y, m) = sy to sm
    while (ymKey(y, m) <= ymKey(ey, em)) {
        val label = if (sameYear) monthAbbr[m - 1] else "${monthAbbr[m - 1]} ${y.toString().takeLast(2)}"
        out.add(ChartBucket(label, dateToMs(y, m, 1), dateToMs(y, m, daysInMonth(y, m))))
        val (ny, nm) = shiftMonths(y, m, 1)
        y = ny; m = nm
    }
    return out
}

/**
 * Period-over-period change. Null = no previous baseline ("No previous
 * data"), never a fabricated 100%. Both-zero reads as flat 0%.
 */
private fun pctOrNull(current: Double, previous: Double): Double? = when {
    previous == 0.0 && current == 0.0 -> 0.0
    previous == 0.0 -> null
    else -> (current - previous) / previous * 100.0
}

private fun trim2(d: Double): String {
    val r = round(d * 100.0) / 100.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
}

/** Indian compact currency: ₹950 · ₹12.5K · ₹86.99K · ₹1.72L · ₹12.4L · ₹1.2Cr. */
private fun formatINR(v: Double): String {
    if (v.isNaN()) return "₹0"
    val a = abs(v)
    val sign = if (v < 0) "-" else ""
    return when {
        a >= 1_00_00_000.0 -> "$sign₹${trim2(a / 1_00_00_000.0)}Cr"
        a >= 1_00_000.0    -> "$sign₹${trim2(a / 1_00_000.0)}L"
        a >= 1_000.0       -> "$sign₹${trim2(a / 1_000.0)}K"
        else               -> "$sign₹${a.toLong()}"
    }
}

/** "+12.5%" / "-3.2%" / "0.0%" — one decimal, sign always shown. */
private fun formatSignedPct1(p: Double): String {
    val r = round(abs(p) * 10.0) / 10.0
    val s = if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
    return (if (p >= 0) "+" else "-") + s + "%"
}

/**
 * Clean Y-axis for a dataset: returns (ceiling, step) with 1/2/2.5/5 steps so
 * ticks read like ₹20K · ₹40K · ₹60K · ₹80K, with headroom above the max.
 * Fully data-driven — rescales from ₹0 to crores+.
 */
private fun niceYAxis(maxValue: Double, lines: Int = 5): Pair<Double, Double> {
    val intervals = (lines - 1).coerceAtLeast(1)
    val raw = maxValue * 1.12 / intervals
    if (raw <= 0.0) return 100.0 to 25.0
    val mag = Math.pow(10.0, floor(log10(raw)))
    val norm = raw / mag
    val step = when {
        norm <= 1.0 -> 1.0
        norm <= 2.0 -> 2.0
        norm <= 2.5 -> 2.5
        norm <= 5.0 -> 5.0
        else -> 10.0
    } * mag
    var ceiling = step * ceil(maxValue / step)
    if (ceiling <= maxValue) ceiling += step
    return ceiling to step
}

// ── Top bar (white, calendar → range, dynamic years) ───────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusinessSummaryCard(
    windowLabel: String,
    revenue: Double,
    expenses: Double,
    profit: Double,
    marginPct: Double,
    growth: Double?,
    prevLabel: String,
    hasPrevBaseline: Boolean,
    prevRevenue: Double,
) {
    val profitUp = profit >= 0.0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                6.dp, RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(SaaSColors.PremiumCard)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.AccountBalanceWallet, null,
                    tint = Color.White, modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Business Summary",
                    color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                )
                Text(
                    windowLabel,
                    color = Color.White.copy(alpha = 0.60f), fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            SummaryGrowthPill(growth = growth)
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "NET PROFIT",
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                formatINR(profit),
                color = Color.White,
                fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.8).sp, lineHeight = 36.sp,
                maxLines = 1, modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                if (profitUp) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                null,
                tint = if (profitUp) Color(0xFF6EE7B7) else Color(0xFFFB7185),
                modifier = Modifier.size(22.dp).padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        if (hasPrevBaseline && growth != null) {
            Text(
                "${formatSignedPct1(growth)}  vs $prevLabel ${formatINR(prevRevenue)}",
                color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp,
                maxLines = 1,
            )
        } else {
            Text(
                "No previous data",
                color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp,
            )
        }

        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeroSplitCard(
                label = "Total Revenue",
                value = formatINR(revenue),
                modifier = Modifier.weight(1f),
            )
            HeroSplitCard(
                label = "Total Expenses",
                value = formatINR(expenses),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Profitability Ratio",
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${trim2(marginPct.coerceIn(0.0, 100.0))}% net margin",
                color = Color.White,
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.14f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((marginPct / 100.0).toFloat().coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF5EEAD4))
            )
        }
    }
}

@Composable
private fun SummaryGrowthPill(growth: Double?) {
    if (growth == null) {
        Text(
            "—",
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 13.sp, fontWeight = FontWeight.Bold,
        )
        return
    }
    val isPos = growth >= 0.0
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            if (isPos) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
            null,
            tint = if (isPos) Color(0xFF6EE7B7) else Color(0xFFFB7185),
            modifier = Modifier.size(14.dp),
        )
        Text(
            formatSignedPct1(growth),
            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HeroSplitCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = Color.White,
            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4).sp,
            maxLines = 1,
        )
    }
}

// ── B. Revenue Analytics (2×2 + grouped bars) ──────────────────────────────

@Composable
private fun RevenueAnalyticsSection(
    totalRev: Double,
    totalProfit: Double,
    margin: Double,
    growth: Double?,
    profitGrowth: Double?,
    chartRange: ChartRange,
    onChartRangeChange: (ChartRange) -> Unit,
    chartBuckets: List<ChartBucket>,
    chartRevenue: List<Double>,
    chartExpenses: List<Double>,
    tickIndices: List<Int>,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Revenue Analytics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniMetricCard(
                label = "Total Revenue",
                value = formatINR(totalRev),
                growth = growth,
                modifier = Modifier.weight(1f),
            )
            MiniMetricCard(
                label = "Total Profit",
                value = formatINR(totalProfit),
                growth = profitGrowth,
                isProfit = true,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniMetricCard(
                label = "Profit Margin",
                value = "${trim2(margin)}%",
                growth = null,
                modifier = Modifier.weight(1f),
            )
            MiniMetricCard(
                label = "Growth",
                value = if (growth == null) "—" else formatSignedPct1(growth),
                growth = growth,
                isGrowth = true,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))

        SaaSBarChart(
            revenues = chartRevenue,
            expenses = chartExpenses,
            labels = chartBuckets.map { it.label },
            tickIndices = tickIndices,
            chartRange = chartRange,
            onChartRangeChange = onChartRangeChange,
            modifier = Modifier.fillMaxWidth().height(340.dp),
        )
    }
}

@Composable
private fun MiniMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    growth: Double? = null,
    isProfit: Boolean = false,
    isGrowth: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SaaSColors.Surface,
        border = BorderStroke(1.dp, SaaSColors.Border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SaaSColors.TextMuted)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = when {
                    isProfit -> SaaSColors.Primary
                    isGrowth && (growth ?: 0.0) >= 0.0 -> SaaSColors.Success
                    isGrowth -> SaaSColors.Error
                    else -> SaaSColors.TextPrimary
                },
                maxLines = 1,
            )
            if (growth != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    formatSignedPct1(growth),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (growth >= 0) SaaSColors.Success else SaaSColors.Error,
                )
            }
        }
    }
}

@Composable
private fun SaaSBarChart(
    revenues: List<Double>,
    expenses: List<Double>,
    labels: List<String>,
    tickIndices: List<Int>,
    chartRange: ChartRange,
    onChartRangeChange: (ChartRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    var tapped by remember { mutableStateOf<Int?>(null) }
    val maxV = (revenues + expenses).maxOrNull()?.takeIf { it > 0 } ?: 0.0
    // Clean dynamic ticks (e.g. ₹20K · ₹40K · ₹60K · ₹80K), headroom included.
    val (ceiling, step) = remember(maxV) { niceYAxis(maxV, lines = 5) }
    // Axis ticks come pre-computed (Jan · Mar · May · … rhythm for months).
    val labelIdx = remember(tickIndices, labels.size) {
        tickIndices.filter { it in labels.indices }
    }
    // Measured gutter so long labels (₹78.1K) never clip.
    val tickStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SaaSColors.TextSecondary)
    val axisTick = SaaSColors.Border
    val density = LocalDensity.current
    val gutterPx = remember(maxV) {
        ((0..4).maxOf { textMeasurer.measure(formatINR(step * it), tickStyle).size.width } +
            with(density) { 12.dp.toPx() })
    }.coerceAtLeast(with(density) { 46.dp.toPx() })

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SaaSColors.Surface),
        border = BorderStroke(1.dp, SaaSColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Revenue vs Expenses",
                        color = SaaSColors.TextPrimary,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                    )
                    Text(
                        "Track your sales and expenses over time",
                        color = SaaSColors.TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
                ChartRangeSegment(
                    selected = chartRange,
                    onSelect = onChartRangeChange,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendDot(color = SaaSColors.Primary, label = "Revenue")
                LegendDot(color = Color(0xFF64748B), label = "Expenses")
            }
            Spacer(Modifier.height(8.dp))
            if (maxV <= 0.0) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "No revenue data for this period",
                        color = SaaSColors.TextMuted, fontSize = 13.sp,
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(revenues, expenses, gutterPx) {
                            detectTapGestures { offset ->
                                val gutter = gutterPx
                                val plotW = size.width - gutter
                                val n = revenues.size
                                if (n == 0 || offset.x < gutter) {
                                    tapped = null
                                } else {
                                    val i = (((offset.x - gutter) / plotW) * n).toInt()
                                        .coerceIn(0, n - 1)
                                    tapped = if (tapped == i) null else i
                                }
                            }
                        }
                ) {
                    val gutter = gutterPx
                    val labelH = 26.dp.toPx()
                    val topPad = 14.dp.toPx()
                    val w = size.width
                    val h = size.height
                    val plotW = w - gutter
                    val plotH = (h - labelH).coerceAtLeast(10f)
                    val n = revenues.size
                    if (n == 0) return@Canvas
                    fun frac(v: Double) = (v / ceiling).toFloat().coerceIn(0f, 1f)
                    fun xAt(i: Int) = if (n == 1) gutter + plotW / 2f
                        else gutter + plotW * i / (n - 1).toFloat()
                    fun yAt(v: Double) = topPad + plotH * (1f - frac(v))

                    // Subtle dashed gridlines + clean ticks.
                    val dash = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                    repeat(5) { i ->
                        val value = step * i
                        val y = yAt(value)
                        drawLine(
                            color = Color(0xFFE8EFED),
                            start = Offset(gutter, y), end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dash,
                        )
                        drawLine(
                            color = axisTick,
                            start = Offset(gutter - 5.dp.toPx(), y), end = Offset(gutter, y),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        val layout = textMeasurer.measure(text = formatINR(value), style = tickStyle)
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(gutter - 8.dp.toPx() - layout.size.width, y - layout.size.height / 2f),
                        )
                    }
                    // Zero baseline, slightly stronger.
                    val baseY = topPad + plotH
                    drawLine(
                        color = axisTick,
                        start = Offset(gutter, baseY), end = Offset(w, baseY),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    // Solid vertical axis line.
                    drawLine(
                        color = axisTick,
                        start = Offset(gutter, topPad), end = Offset(gutter, baseY),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )

                    val revLine = SaaSColors.Primary
                    val expLine = Color(0xFF64748B)
                    val revPts = revenues.mapIndexed { i, v -> Offset(xAt(i), yAt(v)) }
                    val expPts = expenses.mapIndexed { i, v -> Offset(xAt(i), yAt(v)) }

                    // Translucent area fills under each line.
                    drawPath(
                        smoothLinePath(revPts, baseY),
                        brush = Brush.verticalGradient(
                            colors = listOf(revLine.copy(alpha = 0.16f), revLine.copy(alpha = 0.0f)),
                            startY = topPad, endY = baseY,
                        ),
                    )
                    drawPath(
                        smoothLinePath(expPts, baseY),
                        brush = Brush.verticalGradient(
                            colors = listOf(expLine.copy(alpha = 0.12f), expLine.copy(alpha = 0.0f)),
                            startY = topPad, endY = baseY,
                        ),
                    )
                    // Smooth strokes, ~2.75px.
                    drawPath(
                        smoothLinePath(revPts, baseY, close = false),
                        color = revLine,
                        style = Stroke(width = 2.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    drawPath(
                        smoothLinePath(expPts, baseY, close = false),
                        color = expLine,
                        style = Stroke(width = 2.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )

                    // Small data-point markers (non-zero only, keeps flat stretches clean).
                    revenues.forEachIndexed { i, v ->
                        if (v <= 0.0 || tapped == i) return@forEachIndexed
                        drawCircle(color = revLine, radius = 2.5.dp.toPx(), center = revPts[i])
                    }
                    expenses.forEachIndexed { i, v ->
                        if (v <= 0.0 || tapped == i) return@forEachIndexed
                        drawCircle(color = expLine, radius = 2.5.dp.toPx(), center = expPts[i])
                    }

                    // Tapped point: vertical dashed guide + enlarged white-ringed dots.
                    tapped?.let { idx ->
                        if (idx in revenues.indices) {
                            val cx = xAt(idx)
                            drawLine(
                                color = SaaSColors.TextMuted.copy(alpha = 0.55f),
                                start = Offset(cx, topPad), end = Offset(cx, baseY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dash,
                            )
                            listOf(revLine to revPts[idx], expLine to expPts[idx]).forEach { (col, pt) ->
                                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = pt)
                                drawCircle(color = col, radius = 4.dp.toPx(), center = pt)
                            }
                            // White tooltip card with soft shadow.
                            val titleLayout = textMeasurer.measure(
                                text = labels.getOrElse(idx) { "" },
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = SaaSColors.TextPrimary),
                            )
                            val revLayout = textMeasurer.measure(
                                text = "Revenue",
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = SaaSColors.TextSecondary),
                            )
                            val revValLayout = textMeasurer.measure(
                                text = formatINR(revenues[idx]),
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary),
                            )
                            val expLayout = textMeasurer.measure(
                                text = "Expenses",
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = SaaSColors.TextSecondary),
                            )
                            val expValLayout = textMeasurer.measure(
                                text = formatINR(expenses.getOrElse(idx) { 0.0 }),
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary),
                            )
                            val profitVal = revenues[idx] - expenses.getOrElse(idx) { 0.0 }
                            val profitDot = if (profitVal >= 0) SaaSColors.MintDeep else SaaSColors.Error
                            val profitLayout = textMeasurer.measure(
                                text = "Profit",
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = SaaSColors.TextSecondary),
                            )
                            val profitValLayout = textMeasurer.measure(
                                text = formatINR(profitVal),
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary),
                            )
                            val dotR = 3.dp.toPx()
                            val rowH = maxOf(revLayout.size.height, revValLayout.size.height)
                            val tw = maxOf(
                                titleLayout.size.width.toFloat(),
                                2 * dotR + 6.dp.toPx() + maxOf(revLayout.size.width, expLayout.size.width, profitLayout.size.width) +
                                    8.dp.toPx() + maxOf(revValLayout.size.width, expValLayout.size.width, profitValLayout.size.width),
                            ) + 24.dp.toPx()
                            val th = titleLayout.size.height + 6.dp.toPx() + 3 * rowH + 2 * 3.dp.toPx() + 14.dp.toPx()
                            val left = (cx - tw / 2f).coerceIn(0f, (w - tw).coerceAtLeast(0f))
                            val top = (minOf(revPts[idx].y, expPts[idx].y) - th - 10.dp.toPx()).coerceAtLeast(0f)
                            // Soft shadow + white body + hairline border.
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.10f),
                                topLeft = Offset(left, top + 2.dp.toPx()),
                                size = Size(tw, th),
                                cornerRadius = CornerRadius(12.dp.toPx()),
                            )
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(left, top),
                                size = Size(tw, th),
                                cornerRadius = CornerRadius(12.dp.toPx()),
                            )
                            drawRoundRect(
                                color = SaaSColors.Border,
                                topLeft = Offset(left, top),
                                size = Size(tw, th),
                                cornerRadius = CornerRadius(12.dp.toPx()),
                                style = Stroke(width = 1.dp.toPx()),
                            )
                            var ty = top + 7.dp.toPx()
                            drawText(textLayoutResult = titleLayout, topLeft = Offset(left + 12.dp.toPx(), ty))
                            ty += titleLayout.size.height + 3.dp.toPx()
                            drawCircle(color = revLine, radius = dotR, center = Offset(left + 12.dp.toPx() + dotR, ty + rowH / 2f))
                            drawText(textLayoutResult = revLayout, topLeft = Offset(left + 12.dp.toPx() + 2 * dotR + 6.dp.toPx(), ty + (rowH - revLayout.size.height) / 2f))
                            drawText(
                                textLayoutResult = revValLayout,
                                topLeft = Offset(left + tw - 12.dp.toPx() - revValLayout.size.width, ty + (rowH - revValLayout.size.height) / 2f),
                            )
                            ty += rowH + 3.dp.toPx()
                            drawCircle(color = expLine, radius = dotR, center = Offset(left + 12.dp.toPx() + dotR, ty + rowH / 2f))
                            drawText(textLayoutResult = expLayout, topLeft = Offset(left + 12.dp.toPx() + 2 * dotR + 6.dp.toPx(), ty + (rowH - expLayout.size.height) / 2f))
                            drawText(
                                textLayoutResult = expValLayout,
                                topLeft = Offset(left + tw - 12.dp.toPx() - expValLayout.size.width, ty + (rowH - expValLayout.size.height) / 2f),
                            )
                            ty += rowH + 3.dp.toPx()
                            drawCircle(color = profitDot, radius = dotR, center = Offset(left + 12.dp.toPx() + dotR, ty + rowH / 2f))
                            drawText(textLayoutResult = profitLayout, topLeft = Offset(left + 12.dp.toPx() + 2 * dotR + 6.dp.toPx(), ty + (rowH - profitLayout.size.height) / 2f))
                            drawText(
                                textLayoutResult = profitValLayout,
                                topLeft = Offset(left + tw - 12.dp.toPx() - profitValLayout.size.width, ty + (rowH - profitValLayout.size.height) / 2f),
                            )
                        }
                    }

                    // X labels with tick marks; tapped label highlighted teal.
                    labelIdx.forEach { i ->
                        val isTapped = tapped == i
                        val cx = xAt(i)
                        drawLine(
                            color = if (isTapped) SaaSColors.MintDeep else axisTick,
                            start = Offset(cx, baseY),
                            end = Offset(cx, baseY + 4.dp.toPx()),
                            strokeWidth = if (isTapped) 2.dp.toPx() else 1.5.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        val layout = textMeasurer.measure(
                            text = labels.getOrElse(i) { "" },
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = if (isTapped) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isTapped) SaaSColors.MintDeep else SaaSColors.TextSecondary,
                            ),
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(
                                (cx - layout.size.width / 2f).coerceIn(gutter, w - layout.size.width),
                                baseY + 7.dp.toPx(),
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** Smooth cubic path through points; optionally closed down to [baseY] for fills. */
private fun smoothLinePath(pts: List<Offset>, baseY: Float, close: Boolean = true): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    if (pts.size == 1) {
        if (!close) return path
        path.moveTo(pts.first().x, pts.first().y)
        path.lineTo(pts.first().x, baseY)
        path.close()
        return path
    }
    path.moveTo(pts.first().x, pts.first().y)
    for (i in 1 until pts.size) {
        val midX = (pts[i - 1].x + pts[i].x) / 2f
        path.cubicTo(midX, pts[i - 1].y, midX, pts[i].y, pts[i].x, pts[i].y)
    }
    if (close) {
        path.lineTo(pts.last().x, baseY)
        path.lineTo(pts.first().x, baseY)
        path.close()
    }
    return path
}
@Composable
private fun ChartRangeSegment(
    selected: ChartRange,
    onSelect: (ChartRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(SaaSColors.SurfaceVar)
            .border(1.dp, SaaSColors.Border, RoundedCornerShape(100.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChartRange.entries.forEach { range ->
            val isSelected = range == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isSelected) SaaSColors.DeepTeal else Color.Transparent)
                    .then(
                        if (isSelected) Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    Color.White.copy(alpha = 0.03f),
                                    Color.Transparent,
                                )
                            )
                        ) else Modifier
                    )
                    .clickable(onClickLabel = "Last ${range.label}", onClick = { onSelect(range) })
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    range.label,
                    color = if (isSelected) Color.White else SaaSColors.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = SaaSColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ── C. Daily Performance sparklines ────────────────────────────────────────

// ── D. Report Summary (+ inward / outward) ────────────────────────────────

@Composable
private fun ReportSummarySection(
    sales: List<SaleEntry>,
    inward: Int,
    outward: Int,
    windowLabel: String,
    newestFirst: Boolean,
    onOrderChange: (Boolean) -> Unit,
    onProductClick: (String) -> Unit,
    onOpenTransactions: () -> Unit,
    movements: List<StockMovement> = emptyList(),
    products: List<ProductCategory> = emptyList(),
) {
    // Per-product window in/out (empty-can movements already excluded upstream).
    val prodById = remember(products) { products.associateBy { it.id } }
    val inwardById = remember(movements) {
        movements.filter { it.type == "inward" }.groupBy { it.productId }
            .mapValues { (_, rows) -> rows.sumOf { it.qty } }
    }
    val outwardById = remember(movements) {
        movements.filter { it.type == "outward" }.groupBy { it.productId }
            .mapValues { (_, rows) -> rows.sumOf { it.qty } }
    }
    fun displayNameOf(id: String?, fallback: String): String =
        prodById[id]?.displayName?.takeIf { it.isNotBlank() }
            ?: prodById[id]?.name?.takeIf { it.isNotBlank() }
            ?: fallback
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Report Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
        Spacer(Modifier.height(2.dp))
        Text(windowLabel, fontSize = 12.sp, color = SaaSColors.TextMuted)
        Spacer(Modifier.height(12.dp))

        // Inward / outward strip for the window.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SaaSColors.Surface)
                .border(1.dp, SaaSColors.Border, RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StockFlowCell(
                label = "Inward",
                value = "$inward units",
                icon = Icons.Rounded.ArrowDownward,
                tint = SaaSColors.MintDeep,
                bg = SaaSColors.Mint.copy(alpha = 0.16f),
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.width(1.dp).height(40.dp).align(Alignment.CenterVertically)
                    .background(SaaSColors.Border)
            )
            StockFlowCell(
                label = "Outward",
                value = "$outward units",
                icon = Icons.Rounded.ArrowUpward,
                tint = SaaSColors.Error,
                bg = SaaSColors.Error.copy(alpha = 0.10f),
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.width(1.dp).height(40.dp).align(Alignment.CenterVertically)
                    .background(SaaSColors.Border)
            )
            StockFlowCell(
                label = "Net",
                value = "${if (inward - outward >= 0) "+" else ""}${inward - outward} units",
                icon = Icons.Rounded.Sync,
                tint = SaaSColors.Primary,
                bg = SaaSColors.Primary.copy(alpha = 0.10f),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        if (sales.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SaaSColors.Surface)
                    .border(1.dp, SaaSColors.Border, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No sales in this period", color = SaaSColors.TextMuted, fontSize = 13.sp)
            }
        } else {
            // Top products — one row per product, ranked by revenue.
            Text(
                "Top products",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = SaaSColors.TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Ranked by revenue · tap a product for details",
                fontSize = 11.sp, color = SaaSColors.TextMuted,
            )
            Spacer(Modifier.height(8.dp))
            val ranked = sales.groupBy { it.productId }
                .map { (id, entries) ->
                    val name = displayNameOf(id, entries.firstOrNull()?.productName.orEmpty())
                    val revenue = entries.sumOf { it.totalSelling }
                    ReportProductRow(
                        id = id,
                        name = name,
                        qty = entries.sumOf { it.qty },
                        revenue = revenue,
                        inward = inwardById[id] ?: 0,
                        outward = outwardById[id] ?: 0,
                    )
                }
                .sortedByDescending { it.revenue }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SaaSColors.Surface)
                    .border(1.dp, SaaSColors.Border, RoundedCornerShape(16.dp))
                    .padding(vertical = 6.dp),
            ) {
                ranked.forEachIndexed { idx, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = row.name, onClick = { onProductClick(row.name) })
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(SaaSColors.SurfaceVar),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${idx + 1}",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = SaaSColors.TextSecondary,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                row.name.ifBlank { "Product" },
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = SaaSColors.TextPrimary, maxLines = 1,
                            )
                            Text(
                                "${row.qty} units sold",
                                fontSize = 11.sp, color = SaaSColors.TextMuted,
                            )
                            Text(
                                "In ↑${row.inward} · Out ↓${row.outward}",
                                fontSize = 11.sp, color = SaaSColors.TextMuted,
                                maxLines = 1,
                            )
                        }
                        Text(
                            formatINR(row.revenue),
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = SaaSColors.TextPrimary,
                        )
                    }
                    if (idx < ranked.lastIndex) {
                        HorizontalDivider(
                            color = SaaSColors.Border,
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Recent sales — latest individual transactions with full history one tap away.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Recent sales",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = SaaSColors.TextPrimary,
                    )
                    Text(
                        "Every sale in this period, latest first",
                        fontSize = 11.sp, color = SaaSColors.TextMuted,
                    )
                }
                Spacer(Modifier.width(8.dp))
                ReportToggleChip(
                    text = if (newestFirst) "Newest" else "Oldest",
                    selected = true,
                    icon = if (newestFirst) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    onClick = { onOrderChange(!newestFirst) },
                )
            }
            Spacer(Modifier.height(8.dp))
            val recent = (if (newestFirst) sales.sortedByDescending { it.date } else sales.sortedBy { it.date })
                .take(8)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SaaSColors.Surface)
                    .border(1.dp, SaaSColors.Border, RoundedCornerShape(16.dp))
                    .padding(vertical = 6.dp),
            ) {
                recent.forEachIndexed { idx, s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                s.productName.ifBlank { "Sale" },
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = SaaSColors.TextPrimary, maxLines = 1,
                            )
                            Text(
                                "${s.date} · ${s.qty} units",
                                fontSize = 11.sp, color = SaaSColors.TextMuted,
                                maxLines = 1,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SaaSColors.SurfaceVar)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "OUT",
                                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = SaaSColors.TextSecondary,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatINR(s.totalSelling),
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = SaaSColors.MintDeep,
                        )
                    }
                    if (idx < recent.lastIndex) {
                        HorizontalDivider(
                            color = SaaSColors.Border,
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                    }
                }
                HorizontalDivider(
                    color = SaaSColors.Border,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Open all transactions", onClick = onOpenTransactions)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "View all in Transactions",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = SaaSColors.Primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.ChevronRight, null,
                        tint = SaaSColors.Primary, modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private data class ReportProductRow(
    val id: String?,
    val name: String,
    val qty: Int,
    val revenue: Double,
    val inward: Int,
    val outward: Int,
)

@Composable
private fun StockFlowCell(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp, color = SaaSColors.TextMuted,
            )
            Text(
                value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                color = SaaSColors.TextPrimary, maxLines = 1,
            )
        }
    }
}

@Composable
private fun ReportToggleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) SaaSColors.DeepTeal else SaaSColors.SurfaceVar)
            .border(
                1.dp,
                if (selected) SaaSColors.DeepTeal else SaaSColors.Border,
                RoundedCornerShape(50)
            )
            .clickable(onClickLabel = text, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                icon, null,
                tint = if (selected) Color.White else SaaSColors.TextSecondary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else SaaSColors.TextSecondary,
            maxLines = 1,
        )
    }
}

// ── AI Insights + Top Best Sellers (bottom) ────────────────────────────────

@Composable
private fun AIInsightsSection(
    revenue: Double,
    profit: Double,
    marginPct: Double,
    revenueGrowth: Double?,
    profitGrowth: Double?,
    expenses: Double,
    topSellers: List<TopProduct>,
    customerGrowth: Double?,
    hasPrevBaseline: Boolean,
) {
    val insights = remember(revenue, profit, marginPct, revenueGrowth, profitGrowth, expenses, topSellers, customerGrowth, hasPrevBaseline) {
        buildList {
            if (revenueGrowth != null && revenueGrowth >= 0.5) {
                add("Revenue is up ${formatSignedPct1(revenueGrowth)} versus the previous period.")
            } else if (revenueGrowth != null && revenueGrowth <= -0.5) {
                add("Revenue is down ${formatSignedPct1(revenueGrowth).drop(1)} versus the previous period — review pricing or stock.")
            }
            if (profitGrowth != null && profitGrowth >= 0.5) {
                add("Profit is up ${formatSignedPct1(profitGrowth)} versus the previous period.")
            } else if (profitGrowth != null && profitGrowth <= -0.5) {
                add("Profit is down ${formatSignedPct1(profitGrowth).drop(1)} — check margins and spend.")
            }
            if (revenue > 0 && marginPct >= 40.0) {
                add("Healthy ${trim2(marginPct)}% net margin — the business is in the green.")
            } else if (revenue > 0 && marginPct < 0.0) {
                add("Running at a loss — expenses exceed revenue this period.")
            }
            topSellers.firstOrNull()?.let {
                add("${it.name} leads sales at ${formatINR(it.revenue)} (${it.qty} units).")
            }
            if (revenue > 0 && expenses > revenue) {
                add("Expenses (${formatINR(expenses)}) exceed revenue — check discretionary spend.")
            }
            if (customerGrowth != null && customerGrowth >= 0.5) {
                add("Customer base grew ${formatSignedPct1(customerGrowth)} versus the previous period.")
            }
            if (!hasPrevBaseline && revenue > 0) {
                add("No previous-period data yet — trends will appear once history builds up.")
            }
            if (revenue <= 0.0) {
                add("No sales recorded in this period yet.")
            }
        }.take(4)
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AI Insights", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
            Spacer(Modifier.width(8.dp))
            AIInsightBadge(text = "${insights.size} tips", icon = Icons.Rounded.AutoAwesome, color = SaaSColors.Primary)
        }
        Spacer(Modifier.height(12.dp))
        if (insights.isEmpty()) {
            Text("Not enough data for insights yet.", fontSize = 13.sp, color = SaaSColors.TextMuted)
        } else {
            insights.forEach { tip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SaaSColors.Primary.copy(alpha = 0.07f))
                        .border(1.dp, SaaSColors.Primary.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Rounded.Lightbulb, null,
                        tint = SaaSColors.Primary, modifier = Modifier.size(16.dp).padding(top = 2.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tip, fontSize = 12.sp, lineHeight = 17.sp, color = SaaSColors.TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AIInsightBadge(text: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun TopSellersSection(
    products: List<TopProduct>,
    onProductClick: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Top Best Sellers", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("Ranked by revenue in this period", fontSize = 12.sp, color = SaaSColors.TextMuted)
        Spacer(Modifier.height(12.dp))
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SaaSColors.Surface)
                    .border(1.dp, SaaSColors.Border, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No sales in this period", color = SaaSColors.TextMuted, fontSize = 13.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SaaSColors.Surface)
                    .border(1.dp, SaaSColors.Border, RoundedCornerShape(16.dp))
                    .padding(vertical = 6.dp),
            ) {
                products.forEachIndexed { idx, p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClickLabel = p.name, onClick = { onProductClick(p.name) })
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx == 0) Color(0xFFFFF3E8) else SaaSColors.SurfaceVar
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (idx == 0) {
                                Icon(Icons.Rounded.Star, null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                            } else {
                                Text(
                                    "${idx + 1}",
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    color = SaaSColors.TextSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                p.name.ifBlank { "Product" },
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = SaaSColors.TextPrimary, maxLines = 1,
                            )
                            Text(
                                "${p.qty} units sold",
                                fontSize = 11.sp, color = SaaSColors.TextMuted,
                            )
                        }
                        Text(
                            formatINR(p.revenue),
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = SaaSColors.MintDeep,
                        )
                    }
                    if (idx < products.lastIndex) {
                        HorizontalDivider(
                            color = SaaSColors.Border,
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                    }
                }
            }
        }
    }
}
// ── Top bar (white, calendar → range, dynamic years) ───────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBarSection(
    year: Int,
    availableYears: List<Int>,
    onYearChange: (Int) -> Unit,
    onBack: () -> Unit,
    onOpenRange: () -> Unit,
    customActive: Boolean,
    customLabel: String,
    onClearCustom: () -> Unit,
) {
    var yearOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SaaSColors.Surface)
            .statusBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SaaSColors.SurfaceVar)
                            .border(1.dp, SaaSColors.Border, RoundedCornerShape(12.dp))
                            .clickable(onClickLabel = "Back", onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = SaaSColors.TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sales & Profit",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp,
                            color = SaaSColors.TextPrimary
                        )
                        Text(
                            text = "Revenue and expense insights",
                            fontSize = 12.sp,
                            color = SaaSColors.TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (customActive) SaaSColors.Primary.copy(alpha = 0.12f)
                                else SaaSColors.SurfaceVar
                            )
                            .border(
                                1.dp,
                                if (customActive) SaaSColors.Primary else SaaSColors.Border,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(onClickLabel = "Select date range", onClick = onOpenRange),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.CalendarToday, null, tint = SaaSColors.Primary, modifier = Modifier.size(18.dp))
                    }
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SaaSColors.SurfaceVar)
                                .border(1.dp, SaaSColors.Border, RoundedCornerShape(12.dp))
                                .clickable(onClickLabel = "Select year", onClick = { yearOpen = true })
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("$year", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = SaaSColors.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = yearOpen,
                            onDismissRequest = { yearOpen = false },
                        ) {
                            availableYears.forEach { y ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "$y",
                                            fontWeight = if (y == year) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    trailingIcon = if (y == year) {
                                        { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    onClick = { onYearChange(y); yearOpen = false },
                                )
                            }
                        }
                    }
                }
            }

            if (customActive) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SaaSColors.Error.copy(alpha = 0.10f))
                            .border(1.dp, SaaSColors.Error.copy(alpha = 0.30f), RoundedCornerShape(50))
                            .clickable(onClickLabel = "Clear custom range", onClick = onClearCustom)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.DateRange, null, tint = SaaSColors.Error, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(customLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SaaSColors.Error)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Close, null, tint = SaaSColors.Error, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ── Period presets: Day · Week · Month · Quarter · Year ────────────────────

@Composable
private fun PeriodPills(
    selected: AnalyticsPeriod,
    customActive: Boolean,
    onSelect: (AnalyticsPeriod) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (customActive) 0.45f else 1f },
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(AnalyticsPeriod.entries) { mode ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) SaaSColors.Primary else SaaSColors.Surface)
                    .border(
                        1.dp,
                        if (isSelected) SaaSColors.Primary else SaaSColors.Border,
                        RoundedCornerShape(50)
                    )
                    .clickable(onClickLabel = mode.label, onClick = { onSelect(mode) })
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    mode.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else SaaSColors.TextMuted,
                )
            }
        }
    }
}

private val AnalyticsPeriod.label: String
    get() = when (this) {
        AnalyticsPeriod.DAY -> "Day"
        AnalyticsPeriod.WEEK -> "Week"
        AnalyticsPeriod.MONTH -> "Month"
        AnalyticsPeriod.QUARTER -> "Quarter"
        AnalyticsPeriod.YEAR -> "Year"
    }

// ── Month anchor pills ─────────────────────────────────────────────────────

@Composable
private fun MonthSelectorRow(months: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(months.size) { idx ->
            val isSelected = selected == idx
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) SaaSColors.Primary else Color.Transparent)
                    .clickable(onClickLabel = months[idx], onClick = { onSelect(idx) })
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    months[idx],
                    fontSize   = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color      = if (isSelected) Color.White else SaaSColors.TextMuted
                )
            }
        }
    }
}

// ── From / To range dialog ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeDialog(
    from: String,
    to: String,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var picking by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SaaSColors.Surface)
                .border(1.dp, SaaSColors.Border, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Text(
                "Custom date range",
                fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                color = SaaSColors.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Analyse any duration — analytics compare against the previous equal window.",
                fontSize = 12.sp, color = SaaSColors.TextSecondary, lineHeight = 17.sp,
            )
            Spacer(Modifier.height(16.dp))
            RangeFieldRow(
                label = "From",
                value = from,
                onClick = { picking = "from" },
                onClear = { onFromChange("") },
            )
            Spacer(Modifier.height(10.dp))
            RangeFieldRow(
                label = "To",
                value = to,
                onClick = { picking = "to" },
                onClear = { onToChange("") },
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Text("Clear", color = SaaSColors.TextSecondary, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SaaSColors.Primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (picking != null) {
        val tag = picking!!
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                val cur = if (tag == "from") from else to
                if (cur.isBlank()) null
                else LocalDate.parse(cur).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
            }.getOrNull(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val v = msToDateStr(millis)
                        if (tag == "from") onFromChange(v) else onToChange(v)
                    }
                    picking = null
                }) { Text("OK", color = SaaSColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { picking = null }) { Text("Cancel", color = SaaSColors.TextSecondary) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun RangeFieldRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    val active = value.isNotBlank()
    val display = if (active) isoToDisplay(value) else when (label) {
        "From" -> "Start date"
        else -> "End date"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) SaaSColors.Primary.copy(alpha = 0.08f) else SaaSColors.SurfaceVar)
            .border(
                1.dp,
                if (active) SaaSColors.Primary else SaaSColors.Border,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClickLabel = "Pick $label date", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.CalendarToday, null,
            tint = if (active) SaaSColors.Primary else SaaSColors.TextMuted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label.uppercase(),
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp, color = SaaSColors.TextMuted,
            )
            Text(
                display,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = if (active) SaaSColors.TextPrimary else SaaSColors.TextMuted,
            )
        }
        if (active) {
            Icon(
                Icons.Rounded.Close, "Clear",
                tint = SaaSColors.TextMuted,
                modifier = Modifier.size(16.dp).clickable(onClick = onClear),
            )
        }
    }
}

private fun isoToDisplay(iso: String): String {
    val p = iso.split("-")
    if (p.size != 3) return iso
    val m = monthAbbr.getOrElse((p[1].toIntOrNull() ?: 1) - 1) { "" }
    return "$m ${p[2].toIntOrNull() ?: p[2]}, ${p[0]}"
}
// ── Components ───────────────────────────────────────────────────────────────

@Composable
private fun ExpenseAnalyticsSection(
    expense: MonthlyExpense,
    onCategoryTap: (String) -> Unit,
    onDeleteField: (String) -> Unit,
    onAddExpense: () -> Unit = {},
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Expense Analytics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
            Button(
                onClick = onAddExpense,
                colors = ButtonDefaults.buttonColors(containerColor = SaaSColors.Primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.heightIn(min = 40.dp),
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SaaSColors.Surface,
            border = BorderStroke(1.dp, SaaSColors.Border)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total Monthly", fontSize = 12.sp, color = SaaSColors.TextMuted)
                        Text(formatINR(expense.total), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SaaSColors.TextPrimary)
                    }
                    SaaSPieChart(
                        data = listOf(
                            expense.shopRent.toFloat(),
                            expense.adminSalary.toFloat(),
                            expense.deliveryStaff.toFloat(),
                            expense.bikeExpense.toFloat(),
                            expense.miscellaneous.toFloat()
                        ),
                        modifier = Modifier.size(80.dp)
                    )
                }
                
                Spacer(Modifier.height(20.dp))
                
                ExpenseCategoryRow("Shop Rent",      expense.shopRent,      Color(0xFF6366F1), onClick = { onCategoryTap("shopRent") },       onDelete = { onDeleteField("shopRent") })
                ExpenseCategoryRow("Admin Salary",   expense.adminSalary,   Color(0xFF8B5CF6), onClick = { onCategoryTap("adminSalary") },   onDelete = { onDeleteField("adminSalary") })
                ExpenseCategoryRow("Delivery Staff", expense.deliveryStaff, SaaSColors.Primary, onClick = { onCategoryTap("deliveryStaff") }, onDelete = { onDeleteField("deliveryStaff") })
                ExpenseCategoryRow("Bike Expense",   expense.bikeExpense,   Color(0xFFF59E0B), onClick = { onCategoryTap("bike") },       onDelete = { onDeleteField("bike") })
                ExpenseCategoryRow("Miscellaneous",  expense.miscellaneous, Color(0xFF94A3B8), onClick = { onCategoryTap("misc") },       onDelete = { onDeleteField("misc") })

                expense.customExpensesList.forEachIndexed { index, custom ->
                    ExpenseCategoryRow(custom.name, custom.amount, Color(0xFF475569), onClick = { onCategoryTap("custom_$index") }, onDelete = { onDeleteField("custom_$index") })
                }
            }
        }
    }
}

@Composable
private fun ExpenseCategoryRow(
    label: String,
    amount: Double,
    color: Color,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, color = SaaSColors.TextSecondary, modifier = Modifier.weight(1f))
        Text("₹${amount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Rounded.Edit, null, tint = SaaSColors.TextMuted, modifier = Modifier.size(12.dp))
        if (onDelete != null) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    null,
                    tint = SaaSColors.Error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Custom Charts ────────────────────────────────────────────────────────────

@Composable
private fun SaaSPieChart(data: List<Float>, modifier: Modifier = Modifier) {
    val colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), SaaSColors.Primary, Color(0xFFF59E0B), Color(0xFF94A3B8))
    Canvas(modifier = modifier) {
        val total = data.sum().coerceAtLeast(1f)
        var startAngle = -90f
        
        data.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f
            if (sweepAngle > 0f) {
                drawArc(
                    color = colors.getOrElse(index) { Color.Gray },
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun jsonEncode(list: List<CustomExpense>): String {
    return "[" + list.joinToString(",") { "{\"name\":\"${it.name}\",\"amount\":${it.amount}}" } + "]"
}
