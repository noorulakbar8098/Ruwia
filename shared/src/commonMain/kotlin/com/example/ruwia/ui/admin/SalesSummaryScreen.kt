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
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ── Summary model ─────────────────────────────────────────────────────────────

private data class ProductSummaryRow(
    val product: String,
    val supplier: String,     // "GC" | "MB"
    val qty: Int,
    val purchase: Double,
    val selling: Double,
    val margin: Double,
)

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
    currentExpense: MonthlyExpense? = null,
    onExpenseSave: (MonthlyExpense) -> Unit = {},
    onBack: () -> Unit,
    onProductClick: (String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val currentMonth = currentMonthIndex()
    var selectedMonth  by remember { mutableStateOf(currentMonth) }
    val selectedMonthKey = "${Clock.System.now().toLocalDateTime(
        TimeZone.currentSystemDefault()).year}-${(selectedMonth + 1).toString().padStart(2, '0')}"

    var expense by remember(currentExpense) {
        mutableStateOf(currentExpense ?: MonthlyExpense(month = selectedMonthKey))
    }
    var editingField   by remember { mutableStateOf<String?>(null) }
    var editValue      by remember { mutableStateOf("") }

    // ── Build summary rows from real sale entries ──────────────────────────────
    val filteredEntries = saleEntries.filter { entry ->
        entry.createdAt?.startsWith(selectedMonthKey) == true
        || entry.date.contains(months.getOrNull(selectedMonth) ?: "")
    }

    val summaryMap = mutableMapOf<Pair<String,String>, ProductSummaryRow>()
    filteredEntries.forEach { entry ->
        val cat = productCategories.firstOrNull { it.id == entry.productId || it.name == entry.productName }
        val supplier = cat?.supplierGroup ?: "GC"
        val key = entry.productName to supplier
        val cur = summaryMap[key]
        if (cur == null) {
            summaryMap[key] = ProductSummaryRow(
                product  = entry.productName,
                supplier = supplier,
                qty      = entry.qty,
                purchase = entry.purchasePricePerUnit * entry.qty,
                selling  = entry.totalSelling,
                margin   = entry.totalMargin,
            )
        } else {
            summaryMap[key] = cur.copy(
                qty      = cur.qty + entry.qty,
                purchase = cur.purchase + entry.purchasePricePerUnit * entry.qty,
                selling  = cur.selling + entry.totalSelling,
                margin   = cur.margin + entry.totalMargin,
            )
        }
    }

    val allRows   = summaryMap.values.toList()
    val gcRows    = allRows.filter { it.supplier == "GC" }
    val mbRows    = allRows.filter { it.supplier == "MB" }

    val totalQty      = allRows.sumOf { it.qty }
    val totalPurchase = allRows.sumOf { it.purchase }
    val totalSelling  = allRows.sumOf { it.selling }
    val totalMargin   = allRows.sumOf { it.margin }

    val daysInPeriod  = filteredEntries.mapNotNull { it.createdAt?.take(10) }.distinct().size.coerceAtLeast(1)
    val perDayMargin  = totalMargin / daysInPeriod
    val perDayExpense = expense.total / 30.0
    val netPerDay     = perDayMargin - perDayExpense

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

            // ── Global Creators block ──────────────────────────
            item {
                SupplierBlock(
                    supplierName = "Global Creators",
                    supplierTag  = "GC",
                    rows         = gcRows,
                    color        = NTColors.Primary,
                    onProductClick = onProductClick,
                )
                Spacer(Modifier.height(NTDp.md))
            }

            // ── Multi Brands block ─────────────────────────────
            item {
                SupplierBlock(
                    supplierName = "Multi Brands",
                    supplierTag  = "MB",
                    rows         = mbRows,
                    color        = NTColors.Accent,
                    onProductClick = onProductClick,
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
        val label = when (editingField) {
            "shopRent" -> "Shop Rent"; "adminSalary" -> "Admin Salary"
            "deliveryStaff" -> "Delivery Staff"; "misc" -> "Miscellaneous"
            "bike" -> "Bike Expense"; else -> ""
        }
        Dialog(onDismissRequest = { editingField = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NTColors.Surface, RoundedCornerShape(20.dp))
                    .padding(24.dp),
            ) {
                Text("Edit $label", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
                Spacer(Modifier.height(16.dp))
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
                        value = editValue,
                        onValueChange = { editValue = it },
                        textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary),
                        cursorBrush = SolidColor(NTColors.Primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { editingField = null }, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val v = editValue.toDoubleOrNull() ?: return@Button
                            val updated = when (editingField) {
                                "shopRent"      -> expense.copy(shopRent = v)
                                "adminSalary"   -> expense.copy(adminSalary = v)
                                "deliveryStaff" -> expense.copy(deliveryStaff = v)
                                "misc"          -> expense.copy(miscellaneous = v)
                                "bike"          -> expense.copy(bikeExpense = v)
                                else -> expense
                            }
                            expense = updated
                            onExpenseSave(updated)
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
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(NTColors.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
                .clickable(onClick = onBack).align(Alignment.CenterStart),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.ArrowBack, "Back", tint = NTColors.TextPrimary, modifier = Modifier.size(18.dp)) }
        Text("Sales & Profit", fontSize = 17.sp, fontWeight = FontWeight.Bold,
            color = NTColors.TextPrimary, modifier = Modifier.align(Alignment.Center))
        Box(
            modifier = Modifier.size(36.dp).background(NTColors.PrimaryLight, CircleShape)
                .align(Alignment.CenterEnd).clickable {},
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.FileDownload, null, tint = NTColors.Primary, modifier = Modifier.size(18.dp)) }
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
private fun SupplierBlock(
    supplierName: String,
    supplierTag: String,
    rows: List<ProductSummaryRow>,
    color: Color,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val subTotal = rows.sumOf { it.margin }

    Column(
        modifier = modifier
            .padding(horizontal = NTDp.screenPad)
            .background(NTColors.Surface, RoundedCornerShape(16.dp)),
    ) {
        // Supplier header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.08f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.background(color, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                ) { Text(supplierTag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                Spacer(Modifier.width(10.dp))
                Text(supplierName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
            }
            Text("₹${subTotal.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }

        // Column headers
        SummaryTableHeader()
        HorizontalDivider(color = NTColors.Divider)

        // Product rows
        rows.forEachIndexed { idx, row ->
            SummaryProductRow(row = row, onClick = { onProductClick(row.product) })
            if (idx < rows.lastIndex) HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun SummaryTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text("Product", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary,
            letterSpacing = 0.5.sp, modifier = Modifier.weight(1.6f))
        listOf("Qty", "Purchase", "Selling", "Margin").forEach { h ->
            Text(h, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary,
                letterSpacing = 0.5.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryProductRow(row: ProductSummaryRow, onClick: () -> Unit) {
    val dimmed = row.qty == 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !dimmed, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1.6f), verticalAlignment = Alignment.CenterVertically) {
            Text(row.product, fontSize = 13.sp, fontWeight = if (dimmed) FontWeight.Normal else FontWeight.SemiBold,
                color = if (dimmed) NTColors.TextDisabled else NTColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!dimmed) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = NTColors.TextTertiary, modifier = Modifier.size(14.dp))
            }
        }
        Text("${row.qty}", fontSize = 13.sp, color = if (dimmed) NTColors.TextDisabled else NTColors.TextPrimary,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text("₹${row.purchase.toInt()}", fontSize = 12.sp, color = if (dimmed) NTColors.TextDisabled else NTColors.TextSecondary,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text("₹${row.selling.toInt()}", fontSize = 12.sp, color = if (dimmed) NTColors.TextDisabled else NTColors.TextSecondary,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text("₹${row.margin.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = if (dimmed) NTColors.TextDisabled else NTColors.Success,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
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
private fun ExpenseTrackerSection(expense: MonthlyExpense, onEdit: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AccountBalance, null, tint = NTColors.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Monthly Expenses", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
        }
        Spacer(Modifier.height(12.dp))

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
