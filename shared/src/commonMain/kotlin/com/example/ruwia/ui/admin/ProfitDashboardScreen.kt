package com.example.ruwia.ui.admin

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.domain.*
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.dashboard.NTColors
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDateTime
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
    val ChartCurve    get() = NTColors.Primary
    val ChartGradient get() = Brush.verticalGradient(listOf(NTColors.Primary.copy(alpha = 0.15f), Color.Transparent))
    val PremiumCard   get() = Brush.linearGradient(listOf(NTColors.Primary, NTColors.PrimaryDark))
}

@Composable
fun ProfitDashboardScreen(
    state: AdminState,
    onBack: () -> Unit,
    onExpenseMonthSelected: (String) -> Unit = {},
    onExpenseSave: (MonthlyExpense) -> Unit = {},
    onProductClick: (String) -> Unit = {},
    onAddStock: () -> Unit = {},
    onAddProduct: () -> Unit = {},
    onClearData: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val today = try {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    } catch (_: Exception) {
        LocalDateTime(2026, 6, 24, 12, 0)
    }
    var selectedYear  by remember { mutableStateOf(today.year) }
    var selectedMonth by remember { mutableStateOf(today.monthNumber - 1) }

    val selectedMonthKey = "$selectedYear-${(selectedMonth + 1).toString().padStart(2, '0')}"

    LaunchedEffect(selectedMonthKey) {
        onExpenseMonthSelected(selectedMonthKey)
    }

    // ── Data Processing ──────────────────────────────────────────────────────
    val monthSales = state.saleEntries.filter { it.date.startsWith(selectedMonthKey) }
    val todaySales = monthSales.filter { it.date == today.date.toString() }

    val todayRevenue = todaySales.sumOf { it.totalSelling }
    val todayMargin  = todaySales.sumOf { it.totalMargin }
    
    val selectedExpense = state.monthlyExpenses[selectedMonthKey] 
        ?: state.currentMonthExpense?.takeIf { it.month == selectedMonthKey } 
        ?: MonthlyExpense(month = selectedMonthKey)
        
    val dailyExpenseFactor = selectedExpense.total / 30.0
    val todayExpense = if (todaySales.isNotEmpty()) dailyExpenseFactor else 0.0
    val todayProfit = todayMargin - todayExpense

    val totalRevenue = monthSales.sumOf { it.totalSelling }
    val totalProfit  = monthSales.sumOf { it.totalMargin } - selectedExpense.total
    val profitMargin = if (totalRevenue > 0) (totalProfit / totalRevenue) * 100 else 0.0
    val prevMonthIndex = if (selectedMonth == 0) 11 else selectedMonth - 1
    val prevYear = if (selectedMonth == 0) selectedYear - 1 else selectedYear
    val prevMonthKey = "$prevYear-${(prevMonthIndex + 1).toString().padStart(2, '0')}"
    val prevMonthSales = state.saleEntries.filter { it.date.startsWith(prevMonthKey) }
    val prevMonthRevenue = prevMonthSales.sumOf { it.totalSelling }
    val prevMonthExpense = state.monthlyExpenses[prevMonthKey]
        ?: state.currentMonthExpense?.takeIf { it.month == prevMonthKey }
        ?: MonthlyExpense(month = prevMonthKey)
    val prevMonthProfit  = prevMonthSales.sumOf { it.totalMargin } - prevMonthExpense.total
    val prevMonthMargin  = if (prevMonthRevenue > 0) (prevMonthProfit / prevMonthRevenue) * 100 else 0.0

    val growthRate = if (prevMonthRevenue > 0) {
        ((totalRevenue - prevMonthRevenue) / prevMonthRevenue) * 100
    } else {
        if (totalRevenue > 0) 100.0 else 0.0
    }

    // Stock Activity (Case-Wise)
    val monthMvts = state.recentMovements.filter { it.createdAt?.startsWith(selectedMonthKey) == true }
    val inward    = monthMvts.filter { it.type == "inward" }.sumOf { mvt ->
        val upc = state.productCategories.find { it.id == mvt.productId }?.unitsPerCase ?: 1
        mvt.qty.toDouble() / upc
    }
    val outward   = monthMvts.filter { it.type == "outward" }.sumOf { mvt ->
        val upc = state.productCategories.find { it.id == mvt.productId }?.unitsPerCase ?: 1
        mvt.qty.toDouble() / upc
    }

    // Top Products
    val topProducts = monthSales.groupBy { it.productName }
        .map { (name, entries) ->
            ProductRank(
                name     = name,
                qty      = entries.sumOf { it.qty },
                revenue  = entries.sumOf { it.totalSelling },
                profit   = entries.sumOf { it.totalMargin }
            )
        }
        .sortedByDescending { it.revenue }
        .take(5)

    // Low Stock
    val lowStock = state.stockItems.filter { it.stockAvailable <= 10 }

    // Daily Performance Metrics
    val dailySalesMap = monthSales.groupBy { it.date }
    val dailyRevenueList = (1..30).map { day ->
        val dateStr = "$selectedMonthKey-${day.toString().padStart(2, '0')}"
        dailySalesMap[dateStr]?.sumOf { it.totalSelling }?.toFloat() ?: 0f
    }
    val dailyProfitList = (1..30).map { day ->
        val dateStr = "$selectedMonthKey-${day.toString().padStart(2, '0')}"
        val margin = dailySalesMap[dateStr]?.sumOf { it.totalMargin } ?: 0.0
        val cost = if (margin > 0) dailyExpenseFactor else 0.0
        (margin - cost).toFloat()
    }
    val dailyExpenseList = (1..30).map { day ->
        val dateStr = "$selectedMonthKey-${day.toString().padStart(2, '0')}"
        if (dailySalesMap[dateStr]?.isNotEmpty() == true) dailyExpenseFactor.toFloat() else 0f
    }

    var showClearConfirm by remember { mutableStateOf(false) }
    var reportViewMode by remember { mutableStateOf("product") }
    var transactionSortOrder by remember { mutableStateOf("newest") }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Stock & Revenue?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all sale entries and stock movements. Product stock counts will be reset to zero.") },
            confirmButton = {
                Button(
                    onClick = { 
                        onClearData()
                        showClearConfirm = false 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaaSColors.Error)
                ) { Text("Clear All", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
            containerColor = SaaSColors.Surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Expense Editing State
    var editingField by remember { mutableStateOf<String?>(null) }
    var editValue by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var customAmount by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(SaaSColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBarSection(
                title    = "Sales & Profit",
                year     = selectedYear,
                onYearChange = { selectedYear = it },
                onBack   = onBack,
                onClear  = { showClearConfirm = true }
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    bottom = contentPadding.calculateBottomPadding() + 80.dp,
                )
            ) {
                // ── Top Section ──────────────────────────────────────────────────
                item {
                    MonthSelectorRow(
                        months   = months,
                        selected = selectedMonth,
                        onSelect = { selectedMonth = it }
                    )
                    Spacer(Modifier.height(16.dp))
                }

            // ── Business Summary Card ────────────────────────────────────────
            item {
                TodaySummaryCard(
                    revenue  = totalRevenue,
                    expenses = selectedExpense.total,
                    profit   = totalProfit
                )
                Spacer(Modifier.height(20.dp))
            }

            // ── Revenue Analytics ────────────────────────────────────────────
            item {
                RevenueAnalyticsSection(
                    totalRev     = totalRevenue,
                    totalProfit  = totalProfit,
                    margin       = profitMargin,
                    growth       = growthRate,
                    chartData    = dailyRevenueList
                )
                Spacer(Modifier.height(24.dp))
            }

            // ── AI Business Insights ─────────────────────────────────────────
            item {
                AIInsightsSection(
                    totalRevenue = totalRevenue,
                    profitMargin = profitMargin,
                    growthRate = growthRate,
                    selectedExpense = selectedExpense,
                    topProducts = topProducts,
                    prevMonthRevenue = prevMonthRevenue,
                    prevMonthMargin = prevMonthMargin
                )
                Spacer(Modifier.height(24.dp))
            }

            // ── Daily Performance (Sparklines) ───────────────────────────────
            item {
                DailyPerformanceSection(
                    revData = dailyRevenueList,
                    expData = dailyExpenseList,
                    profitData = dailyProfitList,
                    avgRev = if (dailyRevenueList.any { it > 0 }) dailyRevenueList.filter { it > 0 }.average() else 0.0,
                    avgExp = dailyExpenseFactor,
                    avgProfit = if (dailyProfitList.any { it != 0f }) dailyProfitList.filter { it != 0f }.average() else 0.0
                )
                Spacer(Modifier.height(24.dp))
            }

            // ── Stock Activity Section ───────────────────────────────────────
            item {
                StockActivitySection(inward = inward, outward = outward)
                Spacer(Modifier.height(24.dp))
            }

            // ── Top Products Section ─────────────────────────────────────────
            item {
                TopProductsSection(products = topProducts)
                Spacer(Modifier.height(24.dp))
            }

            // ── Low Stock Alerts ─────────────────────────────────────────────
            if (lowStock.isNotEmpty()) {
                item {
                    LowStockAlertSection(items = lowStock)
                    Spacer(Modifier.height(24.dp))
                }
            }

            // ── Expense Analytics ────────────────────────────────────────────
            item {
                ExpenseAnalyticsSection(
                    expense = selectedExpense,
                    onCategoryTap = { field ->
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
                )
                Spacer(Modifier.height(24.dp))
            }

            // ── Report Summary ───────────────────────────────────────────────
            item {
                ReportSummarySection(
                    sales = monthSales,
                    viewMode = reportViewMode,
                    onViewModeChange = { reportViewMode = it },
                    sortOrder = transactionSortOrder,
                    onSortOrderChange = { transactionSortOrder = it },
                    onProductClick = onProductClick
                )
            }
        }
        }

        // Quick Actions FAB
        QuickActionsFAB(
            onAddStock = onAddStock,
            onAddProduct = onAddProduct,
            onAddExpense = { editingField = "new" },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 20.dp)
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

// ── Components ───────────────────────────────────────────────────────────────

@Composable
private fun TopBarSection(title: String, year: Int, onYearChange: (Int) -> Unit, onBack: () -> Unit, onClear: () -> Unit) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SaaSColors.Surface)
            .statusBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SaaSColors.SurfaceVar, RoundedCornerShape(10.dp))
                        .border(1.dp, SaaSColors.Border, RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = SaaSColors.TextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
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
                IconButton(onClick = onClear, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.DeleteSweep, "Clear Data", tint = SaaSColors.Error, modifier = Modifier.size(20.dp))
                }
                Box {
                    Surface(
                        onClick = { dropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        color = SaaSColors.Surface,
                        border = BorderStroke(1.dp, SaaSColors.Border),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$year", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = SaaSColors.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(SaaSColors.Surface)
                    ) {
                        listOf(2025, 2026, 2027).forEach { y ->
                            DropdownMenuItem(
                                text = { Text("$y", fontWeight = FontWeight.Medium, color = SaaSColors.TextPrimary) },
                                onClick = {
                                    onYearChange(y)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelectorRow(months: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(months.size) { idx ->
            val isSelected = selected == idx
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) SaaSColors.Primary else Color.Transparent)
                    .clickable { onSelect(idx) }
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

@Composable
private fun TodaySummaryCard(revenue: Double, expenses: Double, profit: Double) {
    val marginPercentage = if (revenue > 0) ((profit / revenue) * 100).toInt().coerceIn(0, 100) else 0
    val progressValue = if (revenue > 0) (profit / revenue).toFloat().coerceIn(0f, 1f) else 0f
    
    val netProfitColor = if (profit >= 0) Color(0xFF6EE7B7) else Color(0xFFFB7185)
    val trendIcon = if (profit >= 0) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown
    
    val backgroundBrush = if (NTColors.isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E293B), // Slate 800
                Color(0xFF0F172A)  // Slate 900
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                NTColors.Primary,
                NTColors.PrimaryDark
            )
        )
    }

    val shadowColor = if (NTColors.isDarkMode) Color.Black else NTColors.Primary.copy(alpha = 0.2f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = shadowColor,
                spotColor = shadowColor
            ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(24.dp)
        ) {
            Column {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulsing active dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF34D399), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "BUSINESS SUMMARY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.5.sp
                        )
                    }
                    
                    // Efficiency badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Margin: $marginPercentage%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Net Profit Hero Metric
                Column {
                    Text(
                        text = "Net Profit",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${profit.toInt()}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = trendIcon,
                            contentDescription = null,
                            tint = netProfitColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Divider
                HorizontalDivider(color = Color.White.copy(alpha = 0.10f), thickness = 1.dp)
                
                Spacer(Modifier.height(20.dp))
                
                // Split columns for Revenue & Expenses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Revenue Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color.White.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = "Revenue",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "₹${revenue.toInt()}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    
                    // Expenses Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color.White.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = "Expenses",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "₹${expenses.toInt()}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Efficiency bar description
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profitability Ratio",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$marginPercentage% net margin",
                        fontSize = 11.sp,
                        color = netProfitColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = netProfitColor,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
private fun RevenueAnalyticsSection(totalRev: Double, totalProfit: Double, margin: Double, growth: Double, chartData: List<Float>) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Revenue Analytics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            Badge(containerColor = SaaSColors.Primary.copy(alpha = 0.1f), contentColor = SaaSColors.Primary) {
                Text("Last 30 Days", modifier = Modifier.padding(4.dp))
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniMetricCard(label = "Total Revenue", value = "₹${formatValue(totalRev)}", modifier = Modifier.weight(1f))
            MiniMetricCard(label = "Total Profit",  value = "₹${formatValue(totalProfit)}", modifier = Modifier.weight(1f), isProfit = true)
        }
        
        Spacer(Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniMetricCard(label = "Profit Margin", value = "${margin.toInt()}%", modifier = Modifier.weight(1f))
            MiniMetricCard(label = "Growth",        value = "+$growth%", modifier = Modifier.weight(1f), isGrowth = true)
        }
        
        Spacer(Modifier.height(24.dp))
        
        SaaSBarChart(points = chartData, modifier = Modifier.fillMaxWidth().height(180.dp))
    }
}

@Composable
private fun MiniMetricCard(label: String, value: String, modifier: Modifier = Modifier, isProfit: Boolean = false, isGrowth: Boolean = false) {
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
                    isGrowth -> SaaSColors.Success
                    else -> SaaSColors.TextPrimary
                }
            )
        }
    }
}

@Composable
private fun AIInsightsSection(
    totalRevenue: Double,
    profitMargin: Double,
    growthRate: Double,
    selectedExpense: MonthlyExpense,
    topProducts: List<ProductRank>,
    prevMonthRevenue: Double,
    prevMonthMargin: Double
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("AI Business Insights", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        
        // 1. Growth Insight
        val (growthText, growthIcon, growthColor) = if (prevMonthRevenue > 0) {
            when {
                growthRate >= 0.5 -> Triple(
                    "Revenue increased ${growthRate.absoluteValue.toInt()}% vs last month",
                    Icons.AutoMirrored.Rounded.TrendingUp,
                    SaaSColors.Success
                )
                growthRate <= -0.5 -> Triple(
                    "Revenue decreased ${growthRate.absoluteValue.toInt()}% vs last month",
                    Icons.AutoMirrored.Rounded.TrendingDown,
                    SaaSColors.Error
                )
                else -> Triple(
                    "Revenue remained stable vs last month",
                    Icons.Rounded.Remove,
                    SaaSColors.TextSecondary
                )
            }
        } else {
            Triple(
                "First month analytics: Revenue is ₹${totalRevenue.toInt()}",
                Icons.AutoMirrored.Rounded.TrendingUp,
                SaaSColors.Success
            )
        }
        AIInsightBadge(text = growthText, icon = growthIcon, color = growthColor)

        // 2. Expense Insight
        val expenseItems = listOf(
            "Shop Rent" to selectedExpense.shopRent,
            "Admin Salary" to selectedExpense.adminSalary,
            "Delivery Staff" to selectedExpense.deliveryStaff,
            "Miscellaneous" to selectedExpense.miscellaneous,
            "Bike Expense" to selectedExpense.bikeExpense
        ) + selectedExpense.customExpensesList.map { it.name to it.amount }
        
        val highestExpense = expenseItems.maxByOrNull { it.second }
        if (selectedExpense.total > 0.0 && highestExpense != null && highestExpense.second > 0.0) {
            val highestExpensePercent = (highestExpense.second / selectedExpense.total) * 100
            AIInsightBadge(
                text = "${highestExpense.first} is your highest expense (${highestExpensePercent.toInt()}%)",
                icon = Icons.Rounded.PieChart,
                color = SaaSColors.Warning
            )
        } else {
            AIInsightBadge(
                text = "No operational expenses recorded yet for this period",
                icon = Icons.Rounded.PieChart,
                color = SaaSColors.TextMuted
            )
        }

        // 3. Top-selling product
        if (topProducts.isNotEmpty()) {
            val topProduct = topProducts.first()
            AIInsightBadge(
                text = "${topProduct.name} is the top-selling product (${topProduct.qty} sold)",
                icon = Icons.Rounded.Star,
                color = SaaSColors.Primary
            )
        } else {
            AIInsightBadge(
                text = "No sales entries registered for this period",
                icon = Icons.Rounded.Star,
                color = SaaSColors.TextMuted
            )
        }

        // 4. Margin Insight
        val marginChange = profitMargin - prevMonthMargin
        val (marginText, marginIcon, marginColor) = if (prevMonthRevenue > 0) {
            when {
                marginChange >= 0.5 -> Triple(
                    "Margin rose ${marginChange.absoluteValue.toInt()}% vs last month",
                    Icons.AutoMirrored.Rounded.TrendingUp,
                    SaaSColors.Success
                )
                marginChange <= -0.5 -> Triple(
                    "Margin dropped ${marginChange.absoluteValue.toInt()}% vs last month",
                    Icons.AutoMirrored.Rounded.TrendingDown,
                    SaaSColors.Error
                )
                else -> Triple(
                    "Margin remained stable vs last month",
                    Icons.Rounded.Remove,
                    SaaSColors.TextSecondary
                )
            }
        } else {
            Triple(
                "Average profit margin is at a healthy ${profitMargin.toInt()}%",
                Icons.AutoMirrored.Rounded.TrendingUp,
                SaaSColors.Success
            )
        }
        AIInsightBadge(text = marginText, icon = marginIcon, color = marginColor)
    }
}

@Composable
private fun AIInsightBadge(text: String, icon: ImageVector, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SaaSColors.TextPrimary)
        }
    }
}

@Composable
private fun DailyPerformanceSection(
    revData: List<Float>,
    expData: List<Float>,
    profitData: List<Float>,
    avgRev: Double,
    avgExp: Double,
    avgProfit: Double
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Daily Performance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DailySparkCard(
                label = "Avg Revenue",
                value = "₹${avgRev.toInt()}",
                points = revData,
                color = SaaSColors.Primary,
                modifier = Modifier.weight(1f)
            )
            DailySparkCard(
                label = "Avg Expense",
                value = "₹${avgExp.toInt()}",
                points = expData,
                color = SaaSColors.Warning,
                modifier = Modifier.weight(1f)
            )
            DailySparkCard(
                label = "Avg Net Profit",
                value = "₹${avgProfit.toInt()}",
                points = profitData,
                color = SaaSColors.Success,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DailySparkCard(
    label: String,
    value: String,
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SaaSColors.Surface,
        border = BorderStroke(1.dp, SaaSColors.Border)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 10.sp, color = SaaSColors.TextMuted)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = SaaSColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            
            Canvas(modifier = Modifier.fillMaxWidth().height(30.dp)) {
                if (points.isEmpty()) return@Canvas
                val width = size.width
                val height = size.height
                val spacing = width / (points.size - 1)
                val maxPoint = points.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                val minPoint = points.minOrNull() ?: 0f
                val delta = (maxPoint - minPoint).coerceAtLeast(1f)
                
                val path = Path()
                points.forEachIndexed { i, p ->
                    val x = i * spacing
                    val y = height - ((p - minPoint) / delta * height)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

private fun formatCases(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        val rounded = (value * 10).toLong() / 10.0
        if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }
}

@Composable
private fun StockActivitySection(inward: Double, outward: Double) {
    val net = inward - outward
    val inwardStr = formatCases(inward)
    val outwardStr = formatCases(outward)
    val netStr = if (net >= 0) "+${formatCases(net)}" else formatCases(net)
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Stock Activity (This Month)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
        Text("Movements logged by your team for the selected month", fontSize = 11.sp, color = SaaSColors.TextMuted)
        Spacer(Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StockKPICard(label = "Inward (Cases)", value = inwardStr, icon = Icons.Rounded.ArrowDownward, color = SaaSColors.Success, modifier = Modifier.weight(1f))
            StockKPICard(label = "Outward (Cases)", value = outwardStr, icon = Icons.Rounded.ArrowUpward, color = SaaSColors.Error, modifier = Modifier.weight(1f))
            StockKPICard(label = "Net (Cases)", value = netStr, icon = Icons.Rounded.Sync, color = SaaSColors.Primary, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StockKPICard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SaaSColors.Surface,
        border = BorderStroke(1.dp, SaaSColors.Border)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = SaaSColors.TextPrimary)
            Text(label, fontSize = 10.sp, color = SaaSColors.TextMuted)
        }
    }
}

@Composable
private fun TopProductsSection(products: List<ProductRank>) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Top Best Sellers", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        
        products.forEachIndexed { index, product ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = SaaSColors.Surface,
                border = BorderStroke(1.dp, SaaSColors.Border)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(SaaSColors.Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SaaSColors.Primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
                        Text("${product.qty} units sold", fontSize = 11.sp, color = SaaSColors.TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹${product.revenue.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
                        Text("+₹${product.profit.toInt()} profit", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SaaSColors.Success)
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockAlertSection(items: List<StockItem>) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Low Stock Alerts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SaaSColors.ErrorLight,
                    border = BorderStroke(1.dp, Color(0xFFFFC0C0))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, tint = SaaSColors.Error, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SaaSColors.Error)
                        Spacer(Modifier.width(4.dp))
                        Text("(${item.stockAvailable} left)", fontSize = 12.sp, color = SaaSColors.Error.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseAnalyticsSection(expense: MonthlyExpense, onCategoryTap: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Expense Analytics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
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
                        Text("₹${expense.total.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SaaSColors.TextPrimary)
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
                
                ExpenseCategoryRow("Shop Rent",      expense.shopRent,      Color(0xFF6366F1), onClick = { onCategoryTap("shopRent") })
                ExpenseCategoryRow("Admin Salary",   expense.adminSalary,   Color(0xFF8B5CF6), onClick = { onCategoryTap("adminSalary") })
                ExpenseCategoryRow("Delivery Staff", expense.deliveryStaff, SaaSColors.Primary, onClick = { onCategoryTap("deliveryStaff") })
                ExpenseCategoryRow("Bike Expense",   expense.bikeExpense,   Color(0xFFF59E0B), onClick = { onCategoryTap("bike") })
                ExpenseCategoryRow("Miscellaneous",  expense.miscellaneous, Color(0xFF94A3B8), onClick = { onCategoryTap("misc") })

                expense.customExpensesList.forEachIndexed { index, custom ->
                    ExpenseCategoryRow(custom.name, custom.amount, Color(0xFF475569), onClick = { onCategoryTap("custom_$index") })
                }
            }
        }
    }
}

@Composable
private fun ExpenseCategoryRow(label: String, amount: Double, color: Color, onClick: () -> Unit) {
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
    }
}

@Composable
private fun ReportSummarySection(
    sales: List<SaleEntry>,
    viewMode: String,
    onViewModeChange: (String) -> Unit,
    sortOrder: String,
    onSortOrderChange: (String) -> Unit,
    onProductClick: (String) -> Unit
) {
    val aggregated = remember(sales) {
        sales.groupBy { it.productId }
            .mapNotNull { (pid, entries) ->
                if (pid.isEmpty()) return@mapNotNull null
                val pName = entries.firstOrNull()?.productName ?: "Product"
                val qty = entries.sumOf { it.qty }
                val cost = entries.sumOf { it.purchasePricePerUnit * it.qty }
                val sell = entries.sumOf { it.totalSelling }
                val profit = entries.sumOf { it.totalMargin }
                ProductSummary(pid, pName, qty, cost, sell, profit)
            }
            .sortedByDescending { it.profit }
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Report Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SaaSColors.SurfaceVar)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (viewMode == "product") SaaSColors.Primary else Color.Transparent)
                        .clickable { onViewModeChange("product") }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "By Product",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewMode == "product") Color.White else SaaSColors.TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (viewMode == "transaction") SaaSColors.Primary else Color.Transparent)
                        .clickable { onViewModeChange("transaction") }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Transactions",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewMode == "transaction") Color.White else SaaSColors.TextSecondary
                    )
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        if (viewMode == "product") {
            if (aggregated.isEmpty()) {
                Text(
                    "No sales entries logged for this period.",
                    fontSize = 13.sp,
                    color = SaaSColors.TextMuted,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                aggregated.forEach { p ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { onProductClick(p.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = SaaSColors.Surface,
                        border = BorderStroke(1.dp, SaaSColors.Border)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
                                Text("${p.qty} sold", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SaaSColors.Primary)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Cost", fontSize = 10.sp, color = SaaSColors.TextMuted)
                                    Text("₹${p.cost.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SaaSColors.TextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Sales", fontSize = 10.sp, color = SaaSColors.TextMuted)
                                    Text("₹${p.sell.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SaaSColors.TextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Profit", fontSize = 10.sp, color = SaaSColors.TextMuted)
                                    Text("₹${p.profit.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SaaSColors.Success)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val sortedSales = remember(sales, sortOrder) {
                if (sortOrder == "newest") sales.sortedByDescending { it.date }
                else sales.sortedBy { it.date }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${sales.size} transactions logged",
                    fontSize = 12.sp,
                    color = SaaSColors.TextMuted,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                
                Surface(
                    onClick = {
                        val nextOrder = if (sortOrder == "newest") "oldest" else "newest"
                        onSortOrderChange(nextOrder)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = SaaSColors.Surface,
                    border = BorderStroke(1.dp, SaaSColors.Border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (sortOrder == "newest") Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = SaaSColors.TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (sortOrder == "newest") "Date: Newest" else "Date: Oldest",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaaSColors.TextSecondary
                        )
                    }
                }
            }
            
            if (sortedSales.isEmpty()) {
                Text(
                    "No transactions logged for this period.",
                    fontSize = 13.sp,
                    color = SaaSColors.TextMuted,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SaaSColors.SurfaceVar, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1.0f)
                            .clickable {
                                val nextOrder = if (sortOrder == "newest") "oldest" else "newest"
                                onSortOrderChange(nextOrder)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextMuted)
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = if (sortOrder == "newest") Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = SaaSColors.TextMuted,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Text("Customer", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextMuted, modifier = Modifier.weight(1.5f))
                    Text("Product", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextMuted, modifier = Modifier.weight(1.5f))
                    Text("Qty", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextMuted, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text("Margin", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextMuted, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                }
                
                sortedSales.forEach { tx ->
                    val marginColor = when {
                        tx.totalMargin > 100 -> SaaSColors.Success
                        tx.totalMargin > 0   -> SaaSColors.Primary
                        else                 -> SaaSColors.Error
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SaaSColors.Surface)
                            .border(BorderStroke(0.5.dp, SaaSColors.Border.copy(alpha = 0.5f)))
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(formatDateString(tx.date), fontSize = 11.sp, color = SaaSColors.TextMuted, modifier = Modifier.weight(1.0f))
                        Text(tx.customerName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SaaSColors.TextPrimary,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.5f))
                        Text(tx.productName, fontSize = 11.sp, color = SaaSColors.TextSecondary,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1.5f))
                        Text("${tx.qty}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary,
                            textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                        Text("₹${tx.totalMargin.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = marginColor,
                            textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsFAB(
    onAddStock: () -> Unit,
    onAddProduct: () -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FABActionItem(label = "Add Stock", icon = Icons.Rounded.Inventory2, onClick = { onAddStock(); expanded = false })
                FABActionItem(label = "Add Product", icon = Icons.Rounded.Category, onClick = { onAddProduct(); expanded = false })
                FABActionItem(label = "Add Expense", icon = Icons.Rounded.Receipt, onClick = { onAddExpense(); expanded = false })
                Spacer(Modifier.height(10.dp))
            }
        }
        
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = SaaSColors.Primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(60.dp)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.Close else Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun FABActionItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SaaSColors.TextPrimary,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Text(label, fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        FloatingActionButton(
            onClick = onClick,
            containerColor = SaaSColors.Surface,
            contentColor = SaaSColors.Primary,
            shape = CircleShape,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Custom Charts ────────────────────────────────────────────────────────────

@Composable
private fun SaaSBarChart(points: List<Float>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium,
        color = SaaSColors.TextMuted
    )
    val maxPoint = points.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    
    Row(modifier = modifier) {
        // 1. Fixed Y-axis (Left)
        Canvas(modifier = Modifier.width(50.dp).fillMaxHeight()) {
            val height = size.height
            val bottomPadding = 20.dp.toPx()
            val topPadding = 10.dp.toPx()
            val graphHeight = height - bottomPadding - topPadding
            
            val yTicks = 4
            for (i in 0 until yTicks) {
                val fraction = i.toFloat() / (yTicks - 1)
                val y = height - bottomPadding - (fraction * graphHeight)
                
                val labelValue = fraction * maxPoint
                val labelText = "₹${formatValue(labelValue.toDouble())}"
                val textLayoutResult = textMeasurer.measure(labelText, style = textStyle)
                
                drawText(
                    textLayoutResult = textLayoutResult,
                    color = SaaSColors.TextMuted,
                    topLeft = Offset(
                        x = size.width - textLayoutResult.size.width - 8.dp.toPx(),
                        y = y - textLayoutResult.size.height / 2f
                    )
                )
            }
        }
        
        // 2. Scrollable Graph Area (Right)
        val barWidth = 28.dp
        val barGap = 5.dp
        val stepWidth = barWidth + barGap
        val scrollState = rememberScrollState()
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(scrollState)
        ) {
            val totalContentWidth = (points.size * stepWidth.value).dp
            Canvas(
                modifier = Modifier
                    .width(totalContentWidth)
                    .fillMaxHeight()
            ) {
                val width = size.width
                val height = size.height
                
                val bottomPadding = 20.dp.toPx()
                val topPadding = 10.dp.toPx()
                val graphHeight = height - bottomPadding - topPadding
                
                val yTicks = 4
                for (i in 0 until yTicks) {
                    val fraction = i.toFloat() / (yTicks - 1)
                    val y = height - bottomPadding - (fraction * graphHeight)
                    
                    drawLine(
                        color = SaaSColors.Border.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                points.forEachIndexed { i, p ->
                    val barHeight = (p / maxPoint) * graphHeight
                    val left = i * stepWidth.toPx() + (barGap.toPx() / 2f)
                    val top = height - bottomPadding - barHeight
                    val right = left + barWidth.toPx()
                    val bottom = height - bottomPadding
                    
                    if (barHeight > 0f) {
                        val path = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = left,
                                    top = top,
                                    right = right,
                                    bottom = bottom,
                                    topLeftCornerRadius = CornerRadius(4.dp.toPx()),
                                    topRightCornerRadius = CornerRadius(4.dp.toPx()),
                                    bottomLeftCornerRadius = CornerRadius(0f),
                                    bottomRightCornerRadius = CornerRadius(0f)
                                )
                            )
                        }
                        drawPath(
                            path = path,
                            color = SaaSColors.Primary
                        )
                    }
                    
                    val day = i + 1
                    if (day == 1 || day % 5 == 0 || day == points.size) {
                        val labelText = day.toString()
                        val textLayoutResult = textMeasurer.measure(labelText, style = textStyle)
                        drawText(
                            textLayoutResult = textLayoutResult,
                            color = SaaSColors.TextMuted,
                            topLeft = Offset(
                                x = left + (barWidth.toPx() - textLayoutResult.size.width) / 2f,
                                y = height - bottomPadding + 4.dp.toPx()
                            )
                        )
                    }
                }
                
                drawLine(
                    color = SaaSColors.Border,
                    start = Offset(0f, height - bottomPadding),
                    end = Offset(width, height - bottomPadding),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

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

private fun formatValue(v: Double): String = when {
    v >= 100000 -> "${(v / 100000).toInt()}L"
    v >= 1000   -> "${(v / 1000).toInt()}K"
    else        -> "${v.toInt()}"
}

private fun jsonEncode(list: List<CustomExpense>): String {
    return "[" + list.joinToString(",") { "{\"name\":\"${it.name}\",\"amount\":${it.amount}}" } + "]"
}

private fun formatDateString(dateStr: String): String {
    val parts = dateStr.split("-")
    if (parts.size == 3) {
        val y = parts[0].takeLast(2)
        val m = parts[1]
        val d = parts[2]
        return "$d/$m/$y"
    }
    return dateStr
}

private data class ProductRank(val name: String, val qty: Int, val revenue: Double, val profit: Double)
private data class ProductSummary(val id: String, val name: String, val qty: Int, val cost: Double, val sell: Double, val profit: Double)
