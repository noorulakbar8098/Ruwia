package com.example.ruwia.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.domain.MonthlyExpense
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.SaleEntry
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.unitsPerCase
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ── Summary model ─────────────────────────────────────────────────────────────



private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun currentMonthIndex(): Int {
    return try {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber - 1
    } catch (_: Exception) { 0 }
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@Composable
fun SalesSummaryScreen(
    saleEntries: List<SaleEntry> = emptyList(),
    productCategories: List<ProductCategory> = emptyList(),
    stockMovements: List<StockMovement> = emptyList(),
    currentExpense: MonthlyExpense? = null,
    monthlyExpenses: Map<String, MonthlyExpense> = emptyMap(),
    onExpenseMonthSelected: (String) -> Unit = {},
    onExpenseSave: (MonthlyExpense) -> Unit = {},
    onBack: () -> Unit,
    onProductClick: (String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val currentMonth = currentMonthIndex()
    var selectedMonth  by remember { mutableStateOf(currentMonth) }
    val selectedMonthKey = "${Clock.System.now().toLocalDateTime(
        TimeZone.currentSystemDefault()).year}-${(selectedMonth + 1).toString().padStart(2, '0')}"

    LaunchedEffect(selectedMonthKey) {
        onExpenseMonthSelected(selectedMonthKey)
    }

    val selectedExpense = monthlyExpenses[selectedMonthKey]
        ?: currentExpense?.takeIf { it.month == selectedMonthKey }

    var expense by remember(selectedExpense, selectedMonthKey) {
        mutableStateOf(selectedExpense ?: MonthlyExpense(month = selectedMonthKey))
    }
    var editingField   by remember { mutableStateOf<String?>(null) }
    var editValue      by remember { mutableStateOf("") }
    var customName     by remember { mutableStateOf("") }
    var customAmount   by remember { mutableStateOf("") }

    LaunchedEffect(editingField) {
        val field = editingField ?: return@LaunchedEffect
        if (field == "new") {
            customName = ""
            customAmount = ""
        } else if (field.startsWith("custom_")) {
            val idx = field.substringAfter("custom_").toIntOrNull() ?: return@LaunchedEffect
            val custom = expense.customExpensesList.getOrNull(idx)
            customName = custom?.name ?: ""
            customAmount = custom?.amount?.toInt()?.toString() ?: ""
        } else {
            customName = ""
            customAmount = ""
        }
    }

    // ── Build summary rows from real sale entries ──────────────────────────────
    val filteredEntries = saleEntries.filter { entry ->
        entry.date.startsWith(selectedMonthKey)
    }

    val totalQty      = filteredEntries.sumOf { it.qty }
    val totalPurchase = filteredEntries.sumOf { it.purchasePricePerUnit * it.qty }
    val totalSelling  = filteredEntries.sumOf { it.totalSelling }
    val totalMargin   = filteredEntries.sumOf { it.totalMargin }

    val daysInPeriod  = filteredEntries.map { it.date }.distinct().size.coerceAtLeast(1)
    val perDayMargin  = totalMargin / daysInPeriod
    val perDayExpense = expense.total / 30.0
    val netPerDay     = perDayMargin - perDayExpense

    // ── Stock movement aggregate (inward + outward) for the selected month ───
    // The admin sees movements logged by every employee — RLS already exposes
    // the cross-employee view, we just bucket by month here.
    val monthMovements = stockMovements.filter { it.createdAt?.startsWith(selectedMonthKey) == true }
    val totalInwardCases = monthMovements.filter { it.type == "inward" }.sumOf { mvt ->
        val upc = productCategories.find { it.id == mvt.productId }?.unitsPerCase ?: 1
        mvt.qty.toDouble() / upc
    }
    val totalOutwardCases = monthMovements.filter { it.type == "outward" }.sumOf { mvt ->
        val upc = productCategories.find { it.id == mvt.productId }?.unitsPerCase ?: 1
        mvt.qty.toDouble() / upc
    }
    val inwardEntries    = monthMovements.count { it.type == "inward" }
    val outwardEntries   = monthMovements.count { it.type == "outward" }
    val netStockChangeCases = totalInwardCases - totalOutwardCases

    Scaffold(
        containerColor = NTColors.Background,
        topBar = { SummaryTopBar(onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            // ── Month selector ─────────────────────────────────
            item {
                MonthSelector(
                    months = months,
                    selected = selectedMonth,
                    onSelect = { selectedMonth = it },
                    modifier = Modifier.padding(horizontal = NTDp.screenPad, vertical = NTDp.md),
                )
            }

            // ── KPI strip ──────────────────────────────────────
            item {
                SummaryKpiStrip(
                    totalMargin  = totalMargin,
                    totalSelling = totalSelling,
                    modifier     = Modifier.padding(horizontal = NTDp.screenPad),
                )
                Spacer(Modifier.height(NTDp.md))
            }

            // ── Stock activity (inward + outward from all employees) ───
            item {
                StockActivityCard(
                    inwardCases     = totalInwardCases,
                    outwardCases    = totalOutwardCases,
                    inwardEntries   = inwardEntries,
                    outwardEntries  = outwardEntries,
                    netChange       = netStockChangeCases,
                    modifier        = Modifier.padding(horizontal = NTDp.screenPad),
                )
                Spacer(Modifier.height(NTDp.md))
            }

            // ── Report Summary block ───────────────────────────
            item {
                SaleEntriesBlock(
                    entries = filteredEntries,
                    onProductClick = onProductClick,
                    modifier = Modifier.padding(horizontal = NTDp.screenPad),
                )
                Spacer(Modifier.height(NTDp.md))
            }

            // ── Grand total row ────────────────────────────────
            item {
                GrandTotalCard(
                    totalQty      = totalQty,
                    totalPurchase = totalPurchase,
                    totalSelling  = totalSelling,
                    totalMargin   = totalMargin,
                    modifier      = Modifier.padding(horizontal = NTDp.screenPad),
                )
                Spacer(Modifier.height(NTDp.lg))
            }

            // ── Expense tracker ────────────────────────────────
            item {
                ExpenseTrackerSection(
                    expense  = expense,
                    onEdit   = { field ->
                        editingField = field
                        editValue = when (field) {
                            "shopRent"      -> expense.shopRent.toInt().toString()
                            "adminSalary"   -> expense.adminSalary.toInt().toString()
                            "deliveryStaff" -> expense.deliveryStaff.toInt().toString()
                            "misc"          -> expense.miscellaneous.toInt().toString()
                            "bike"          -> expense.bikeExpense.toInt().toString()
                            else -> ""
                        }
                    },
                    onAddNewExpense = {
                        editingField = "new"
                    },
                    modifier = Modifier.padding(horizontal = NTDp.screenPad),
                )
                Spacer(Modifier.height(NTDp.lg))
            }

            // ── Per-day KPI footer ─────────────────────────────
            item {
                PerDayKpiRow(
                    perDayMargin  = perDayMargin,
                    perDayExpense = perDayExpense,
                    netPerDay     = netPerDay,
                    modifier      = Modifier.padding(horizontal = NTDp.screenPad),
                )
            }
        }
    }

    // ── Edit expense dialog ────────────────────────────────────
    if (editingField != null) {
        val field = editingField!!
        val isCustom = field == "new" || field.startsWith("custom_")
        val label = if (isCustom) {
            if (field == "new") "Add New Expense" else "Edit Expense"
        } else {
            when (field) {
                "shopRent" -> "Shop Rent"; "adminSalary" -> "Admin Salary"
                "deliveryStaff" -> "Delivery Staff"; "misc" -> "Miscellaneous"
                "bike" -> "Bike Expense"; else -> ""
            }
        }

        Dialog(onDismissRequest = { editingField = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NTColors.Surface, RoundedCornerShape(20.dp))
                    .padding(24.dp),
            ) {
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
                Spacer(Modifier.height(16.dp))

                if (isCustom) {
                    Text("Expense Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, NTColors.Border, RoundedCornerShape(12.dp))
                            .background(NTColors.SurfaceVar, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = NTColors.TextPrimary),
                            cursorBrush = SolidColor(NTColors.Primary),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text("Amount", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, NTColors.Primary, RoundedCornerShape(12.dp))
                        .background(NTColors.SurfaceVar, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("₹", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NTColors.Primary)
                    Spacer(Modifier.width(6.dp))
                    BasicTextField(
                        value = if (isCustom) customAmount else editValue,
                        onValueChange = { if (isCustom) customAmount = it else editValue = it },
                        textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary),
                        cursorBrush = SolidColor(NTColors.Primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (field.startsWith("custom_")) {
                        Button(
                            onClick = {
                                val idx = field.substringAfter("custom_").toIntOrNull()
                                if (idx != null) {
                                    val currentList = expense.customExpensesList.toMutableList()
                                    if (idx in currentList.indices) {
                                        currentList.removeAt(idx)
                                        val updatedJson = if (currentList.isEmpty()) null else {
                                            kotlinx.serialization.json.Json.encodeToString(currentList)
                                        }
                                        val updated = expense.copy(customExpenses = updatedJson)
                                        expense = updated
                                        onExpenseSave(updated)
                                    }
                                }
                                editingField = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NTColors.Error),
                        ) {
                            Text("Delete")
                        }
                    } else {
                        OutlinedButton(onClick = { editingField = null }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                    }
                    Button(
                        onClick = {
                            if (isCustom) {
                                val name = customName.trim()
                                if (name.isEmpty()) return@Button
                                val amount = customAmount.trim().ifEmpty { "0" }.toDoubleOrNull() ?: return@Button
                                val currentList = expense.customExpensesList.toMutableList()
                                if (field == "new") {
                                    currentList.add(com.example.ruwia.domain.CustomExpense(name, amount))
                                } else {
                                    val idx = field.substringAfter("custom_").toIntOrNull() ?: return@Button
                                    if (idx in currentList.indices) {
                                        currentList[idx] = com.example.ruwia.domain.CustomExpense(name, amount)
                                    }
                                }
                                val updatedJson = kotlinx.serialization.json.Json.encodeToString(currentList)
                                val updated = expense.copy(customExpenses = updatedJson)
                                expense = updated
                                onExpenseSave(updated)
                            } else {
                                val v = editValue.trim().ifEmpty { "0" }.toDoubleOrNull() ?: return@Button
                                val updated = when (field) {
                                    "shopRent"      -> expense.copy(shopRent = v)
                                    "adminSalary"   -> expense.copy(adminSalary = v)
                                    "deliveryStaff" -> expense.copy(deliveryStaff = v)
                                    "misc"          -> expense.copy(miscellaneous = v)
                                    "bike"          -> expense.copy(bikeExpense = v)
                                    else -> expense
                                }
                                expense = updated
                                onExpenseSave(updated)
                            }
                            editingField = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                    ) { Text("Save") }
                }
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Background)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(NTColors.Surface, RoundedCornerShape(10.dp))
                    .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack).align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.ArrowBack, "Back", tint = NTColors.TextPrimary, modifier = Modifier.size(18.dp)) }
            Text("Sales & Profit", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary, modifier = Modifier.align(Alignment.Center))
        }
    }
}

// ── Month selector ─────────────────────────────────────────────────────────────

@Composable
private fun MonthSelector(months: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CalendarMonth, null, tint = NTColors.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("2026", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
        }
        Spacer(Modifier.weight(1f))
        // Show 3 months centred around current month
        val cm = currentMonthIndex()
        listOf((cm - 1).coerceAtLeast(0), cm, (cm + 1).coerceAtMost(11)).distinct().forEach { idx ->
            Box(
                modifier = Modifier
                    .background(
                        if (selected == idx) NTColors.Primary else NTColors.SurfaceVar,
                        RoundedCornerShape(20.dp),
                    )
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(months[idx], fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = if (selected == idx) Color.White else NTColors.TextSecondary)
            }
        }
    }
}

// ── Summary KPI strip ─────────────────────────────────────────────────────────

@Composable
private fun SummaryKpiStrip(totalMargin: Double, totalSelling: Double, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryKpiCard("Total Revenue", "₹${totalSelling.toInt()}", NTColors.PrimaryLight, NTColors.Primary, Modifier.weight(1f))
        SummaryKpiCard("Total Margin", "₹${totalMargin.toInt()}", NTColors.SuccessLight, NTColors.Success, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryKpiCard(label: String, value: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 11.sp, color = fg.copy(alpha = 0.70f), fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = fg)
    }
}

// ── Supplier block ─────────────────────────────────────────────────────────────

@Composable
private fun SaleEntriesBlock(
    entries: List<SaleEntry>,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val displayList = if (isExpanded) entries else entries.take(8)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NTColors.Surface, RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NTColors.Primary.copy(alpha = 0.08f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Assessment, null, tint = NTColors.Primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Report Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
            }
            Text(
                text = "${entries.size} items",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NTColors.Primary
            )
        }

        // Table Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text("Name", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary,
                letterSpacing = 0.5.sp, modifier = Modifier.weight(1.1f))
            Text("Product", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary,
                letterSpacing = 0.5.sp, modifier = Modifier.weight(1f))
            Text("Qty", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary,
                letterSpacing = 0.5.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
            Text("Purchase", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary,
                letterSpacing = 0.5.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
            Text("Sell", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary,
                letterSpacing = 0.5.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
            Text("Margin", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary,
                letterSpacing = 0.5.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
        }
        HorizontalDivider(color = NTColors.Divider)

        // Sales rows
        displayList.forEachIndexed { idx, entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProductClick(entry.productId) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(entry.customerName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = NTColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1.1f))
                Text(entry.productName, fontSize = 12.sp, color = NTColors.TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                Text("${entry.qty}", fontSize = 12.sp, color = NTColors.TextPrimary,
                    textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                Text("₹${(entry.purchasePricePerUnit * entry.qty).toInt()}", fontSize = 12.sp, color = NTColors.TextSecondary,
                    textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                Text("₹${entry.totalSelling.toInt()}", fontSize = 12.sp, color = NTColors.TextSecondary,
                    textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                Text("₹${entry.totalMargin.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = NTColors.Success, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
            }
            if (idx < displayList.lastIndex) {
                HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.5f))
            }
        }

        if (entries.size > 8) {
            HorizontalDivider(color = NTColors.Divider)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) "Show Less" else "Show More (+${entries.size - 8})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NTColors.Primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = NTColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ── Grand total ────────────────────────────────────────────────────────────────

@Composable
private fun GrandTotalCard(
    totalQty: Int, totalPurchase: Double, totalSelling: Double, totalMargin: Double,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NTColors.PrimaryDark, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("TOTAL", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
            letterSpacing = 1.sp, modifier = Modifier.weight(1.6f))
        Text("$totalQty", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text("₹${totalPurchase.toInt()}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.80f),
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text("₹${totalSelling.toInt()}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.80f),
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text("₹${totalMargin.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6EE7B7),
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

// ── Expense tracker ────────────────────────────────────────────────────────────

@Composable
private fun ExpenseTrackerSection(
    expense: MonthlyExpense,
    onEdit: (String) -> Unit,
    onAddNewExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccountBalance, null, tint = NTColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Monthly Expenses", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = NTColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(12.dp))

        if (expanded) {
            Column(modifier = Modifier.background(NTColors.Surface, RoundedCornerShape(16.dp)).padding(4.dp)) {
                ExpenseRow("Shop Rent",      expense.shopRent,      Icons.Rounded.Home,          NTColors.Info)     { onEdit("shopRent") }
                HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.5f))
                ExpenseRow("Admin Salary",   expense.adminSalary,   Icons.Rounded.ManageAccounts, NTColors.AvatarPurple) { onEdit("adminSalary") }
                HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.5f))
                ExpenseRow("Delivery Staff", expense.deliveryStaff, Icons.Rounded.LocalShipping,  NTColors.Primary)  { onEdit("deliveryStaff") }
                HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.5f))
                ExpenseRow("Miscellaneous",  expense.miscellaneous, Icons.Rounded.MoreHoriz,      NTColors.Warning)  { onEdit("misc") }
                HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.5f))
                ExpenseRow("Bike Expense",   expense.bikeExpense,   Icons.Rounded.TwoWheeler,     NTColors.Accent)   { onEdit("bike") }

                // Dynamic custom expenses list
                expense.customExpensesList.forEachIndexed { index, custom ->
                    HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.5f))
                    ExpenseRow(
                        label = custom.name,
                        amount = custom.amount,
                        icon = Icons.Rounded.ReceiptLong,
                        color = NTColors.Primary,
                        onEdit = { onEdit("custom_$index") }
                    )
                }

                // Add custom expense row button
                HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddNewExpense)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).background(NTColors.Success.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = NTColors.Success, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Add New Expense", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NTColors.Success)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NTColors.ErrorLight, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Total Expenses", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NTColors.ErrorText)
                Text("₹${expense.total.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = NTColors.Error)
            }
        }
    }
}

@Composable
private fun ExpenseRow(label: String, amount: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NTColors.TextPrimary, modifier = Modifier.weight(1f))
        Text("₹${amount.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextPrimary)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.Edit, null, tint = NTColors.TextTertiary, modifier = Modifier.size(15.dp))
    }
}

// ── Per-day KPI row ────────────────────────────────────────────────────────────

@Composable
private fun PerDayKpiRow(perDayMargin: Double, perDayExpense: Double, netPerDay: Double, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Daily Performance", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PerDayCard("Per Day\nRevenue",  "₹${perDayMargin.toInt()}",   NTColors.SuccessLight, NTColors.Success,  Modifier.weight(1f))
            PerDayCard("Per Day\nExpense",  "₹${perDayExpense.toInt()}",  NTColors.ErrorLight,   NTColors.Error,    Modifier.weight(1f))
            PerDayCard("Net\nPer Day", if (netPerDay >= 0) "+₹${netPerDay.toInt()}" else "-₹${(-netPerDay).toInt()}",
                if (netPerDay >= 0) NTColors.SuccessLight else NTColors.ErrorLight,
                if (netPerDay >= 0) NTColors.Success      else NTColors.Error,
                Modifier.weight(1f))
        }
    }
}

@Composable
private fun PerDayCard(label: String, value: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(bg, RoundedCornerShape(14.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 10.sp, color = fg.copy(alpha = 0.70f), textAlign = TextAlign.Center, lineHeight = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = fg, textAlign = TextAlign.Center)
    }
}

// ── Stock activity card ────────────────────────────────────────────────────────
//   Aggregates inward & outward stock movements logged by every employee under
//   this admin (RLS already exposes the cross-employee view), bucketed for the
//   currently-selected month.

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
private fun StockActivityCard(
    inwardCases: Double,
    outwardCases: Double,
    inwardEntries: Int,
    outwardEntries: Int,
    netChange: Double,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NTColors.Surface, RoundedCornerShape(NTDp.radLg))
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radLg))
            .padding(NTDp.md),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "STOCK ACTIVITY",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp, color = NTColors.Primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Movements logged by your team this month",
                    fontSize = 12.sp, color = NTColors.TextTertiary,
                )
            }
            // Net delta pill
            val netLabel = when {
                netChange > 0.0 -> "+${formatCases(netChange)} net"
                netChange < 0.0 -> "${formatCases(netChange)} net"
                else            -> "0 net"
            }
            val (netBg, netFg) = when {
                netChange > 0.0 -> NTColors.SuccessLight to NTColors.Success
                netChange < 0.0 -> NTColors.ErrorLight   to NTColors.Error
                else            -> NTColors.SurfaceVar   to NTColors.TextTertiary
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(NTDp.radFull))
                    .background(netBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(netLabel, color = netFg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(NTDp.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NTDp.sm),
        ) {
            StockActivityTile(
                title    = "INWARD",
                subtitle = "Came in",
                qty      = inwardCases,
                entries  = inwardEntries,
                icon     = Icons.Rounded.ArrowDownward,
                bg       = NTColors.SuccessLight,
                fg       = NTColors.Success,
                modifier = Modifier.weight(1f),
            )
            StockActivityTile(
                title    = "OUTWARD",
                subtitle = "Went out",
                qty      = outwardCases,
                entries  = outwardEntries,
                icon     = Icons.Rounded.ArrowUpward,
                bg       = NTColors.ErrorLight,
                fg       = NTColors.Error,
                modifier = Modifier.weight(1f),
            )
        }

        if (inwardEntries == 0 && outwardEntries == 0) {
            Spacer(Modifier.height(NTDp.sm))
            Text(
                "No stock movements logged for this month yet.",
                fontSize = 11.sp,
                color = NTColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StockActivityTile(
    title: String,
    subtitle: String,
    qty: Double,
    entries: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(fg.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp, color = fg)
                Text(subtitle, fontSize = 10.sp, color = fg.copy(alpha = 0.70f))
            }
        }
        Spacer(Modifier.height(NTDp.sm))
        Text("${formatCases(qty)} cases", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = fg)
        Spacer(Modifier.height(2.dp))
        Text(
            if (entries == 1) "1 entry" else "$entries entries",
            fontSize = 11.sp, color = fg.copy(alpha = 0.70f),
        )
    }
}
