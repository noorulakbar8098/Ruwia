package com.example.ruwia.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.isEmptyCansSource
import com.example.ruwia.domain.shopMatchKey
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.util.isoToDisplayDate
import com.example.ruwia.util.isoToDisplayTime
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

// ─────────────────────────────────────────────────────────────
//  Stock History — Premium SaaS ledger view
//  Hero stats · search · shop · employee · date range filter ·
//  sort chip row · date-grouped, colour-coded movement cards
//  with an expandable detail sheet (NT design tokens).
// ─────────────────────────────────────────────────────────────

private enum class SortMode(val label: String, val icon: ImageVector) {
    Latest("Latest", Icons.Rounded.Refresh),
    Date("Date", Icons.Rounded.CalendarToday),
    Delivered("Delivered", Icons.Rounded.LocalShipping),
    Stock("Stock", Icons.Rounded.Inventory2),
}

private class MoveStyle(
    val icon: ImageVector,
    val iconBg: Color,
    val iconFg: Color,
    val badge: String,
    val amountColor: Color,
)

private fun classify(m: StockMovement): MoveStyle {
    val isTransfer = m.source.startsWith("Transfer", ignoreCase = true)
    val isReturn = m.source.isNotEmpty() && m.source.isEmptyCansSource()
    val isAdd = m.type == "inward"

    return when {
        isTransfer -> MoveStyle(
            icon = Icons.Rounded.SwapHoriz,
            iconBg = NTColors.InfoLight,
            iconFg = NTColors.InfoText,
            badge = if (isAdd) "TRANSFER IN" else "TRANSFER OUT",
            amountColor = NTColors.InfoText,
        )
        isReturn -> MoveStyle(
            icon = Icons.Rounded.Recycling,
            iconBg = NTColors.StockIconBg,
            iconFg = NTColors.StockIconFg,
            badge = "EMPTY UNITS",
            amountColor = NTColors.StockIconFg,
        )
        isAdd -> MoveStyle(
            icon = Icons.Rounded.ArrowDownward,
            iconBg = NTColors.SuccessLight,
            iconFg = NTColors.SuccessText,
            badge = when {
                m.source.contains("Opening", ignoreCase = true) -> "OPENING"
                m.source.contains("Restock", ignoreCase = true) -> "RESTOCK"
                m.source.contains("Adjustment", ignoreCase = true) -> "STOCK ADJUST"
                m.source.contains("Purchase", ignoreCase = true) -> "PURCHASE"
                else -> "STOCK IN"
            },
            amountColor = NTColors.SuccessText,
        )
        else -> MoveStyle(
            icon = Icons.Rounded.ArrowUpward,
            iconBg = NTColors.OverdueIconBg,
            iconFg = NTColors.OverdueIconFg,
            badge = when {
                m.source.startsWith("Sale", ignoreCase = true) -> "SALE"
                m.source.contains("Adjustment", ignoreCase = true) -> "STOCK ADJUST"
                else -> "STOCK OUT"
            },
            amountColor = NTColors.OverdueIconFg,
        )
    }
}

// ── Date helpers ────────────────────────────────────────────

private fun isoDateToDisplay(s: String): String {
    val parts = s.split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else s
}

private fun dateStrToEpochMillis(s: String): Long? = try {
    LocalDate.parse(s).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
} catch (_: Exception) {
    null
}

private fun epochMillisToDateStr(millis: Long): String {
    val d = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
    return "${d.year}-${d.monthNumber.toString().padStart(2, '0')}-${d.dayOfMonth.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockHistoryScreen(
    state: AdminState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val movements = state.recentMovements
    val products = state.productCategories
    val employees = state.employees

    // Shop filter pinned to the business's two configured shops
    val shopOptions = remember(state.shopNames) {
        val configured = state.shopNames.take(2).map { it.trim() }.filter { it.isNotBlank() }
        if (configured.isNotEmpty()) configured
        else movements.map { it.shopName.trim() }.filter { it.isNotBlank() && it.lowercase() != "all shops" }
            .distinctBy { shopMatchKey(it) }.take(2)
    }

    // Employee options derived from movements + state. Only resolvable
    // employees are offered as filter chips (never a raw employee id) —
    // soft-deleted employees stay in `state.employees` so their names remain.
    val employeeOptions = remember(employees, movements) {
        val movementEmpIds = movements.mapNotNull { it.employeeId }.distinct().toSet()
        movementEmpIds.mapNotNull { empId ->
            employees.find { it.id == empId }?.let { empId to it.name }
        }.distinctBy { it.second }.sortedBy { it.second }
    }

    var selectedShop by remember { mutableStateOf("") }
    var selectedEmployee by remember { mutableStateOf("") }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.Latest) }
    var query by remember { mutableStateOf("") }

    val scoped = remember(movements, selectedShop, selectedEmployee, dateFrom, dateTo) {
        var list = movements
        if (selectedShop.isNotBlank()) {
            val key = shopMatchKey(selectedShop)
            list = list.filter { shopMatchKey(it.shopName) == key }
        }
        if (selectedEmployee.isNotBlank()) {
            list = list.filter { it.employeeId == selectedEmployee }
        }
        if (dateFrom.isNotBlank() || dateTo.isNotBlank()) {
            list = list.filter { m ->
                val d = m.createdAt?.take(10) ?: return@filter true
                if (dateFrom.isNotBlank() && d < dateFrom) return@filter false
                if (dateTo.isNotBlank() && d > dateTo) return@filter false
                true
            }
        }
        list
    }

    val sorted = remember(scoped, sortMode) {
        when (sortMode) {
            SortMode.Delivered -> scoped.sortedWith(
                compareByDescending<StockMovement> { it.type == "outward" }
                    .thenByDescending { it.qty }
            )
            SortMode.Stock -> scoped.sortedByDescending { it.qty }
            SortMode.Date -> scoped.sortedBy { it.createdAt }
            SortMode.Latest -> scoped
        }
    }

    val filtered = remember(sorted, query) {
        val q = query.trim()
        if (q.isBlank()) sorted
        else {
            val low = q.lowercase()
            sorted.filter {
                products.find { p -> p.id == it.productId }?.displayName?.lowercase()?.contains(low) == true
                    || it.source.lowercase().contains(low)
                    || it.shopName.lowercase().contains(low)
            }
        }
    }

    val totalMovements = filtered.size
    val deliveredUnits = filtered.filter { it.type == "outward" }.sumOf { it.qty }
    val restockedUnits = filtered.filter { it.type == "inward" && !it.source.isEmptyCansSource() }.sumOf { it.qty }

    SystemBackHandler(onBack)

    Column(modifier = modifier.fillMaxSize().background(NTColors.Background)) {
        StockHistoryHeader(
            entryCount = totalMovements,
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                StockHistoryStats(totalMovements, deliveredUnits, restockedUnits)
            }
            item {
                StockHistoryControls(
                    shopOptions = shopOptions,
                    selectedShop = selectedShop,
                    onSelectShop = { selectedShop = it },
                    employees = employeeOptions,
                    selectedEmployee = selectedEmployee,
                    onSelectEmployee = { selectedEmployee = it },
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    onDateFromChange = { dateFrom = it },
                    onDateToChange = { dateTo = it },
                    sortMode = sortMode,
                    onSelectSort = { sortMode = it },
                    query = query,
                    onQueryChange = { query = it },
                )
            }

            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        StockHistoryEmpty()
                    }
                }
            } else {
                itemsIndexed(filtered, key = { _, m -> m.id }) { index, m ->
                    val showDateHeader = index == 0 || dateKey(filtered[index - 1]) != dateKey(m)
                    if (showDateHeader) {
                        StockHistoryDateHeader(m)
                    }
                    val isExpanded = expandedId == m.id
                    val isReturn = m.source.isNotEmpty() && m.source.isEmptyCansSource()
                    val product = products.find { it.id == m.productId }
                    val prodName = if (isReturn) "Empty Units" else (product?.displayName ?: "Water Bottle")

                    MovementCard(
                        movement = m,
                        productName = prodName,
                        isExpanded = isExpanded,
                        onClick = { expandedId = if (isExpanded) null else m.id },
                    )
                    if (isExpanded) {
                        ActivityDetailSheet(
                            movement = m,
                            employees = employees,
                            bg = NTColors.SurfaceVar,
                            border = NTColors.Border,
                            textPrimary = NTColors.TextPrimary,
                            textMuted = NTColors.TextTertiary,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

private fun dateKey(m: StockMovement): String = m.createdAt?.take(10) ?: "unknown"

// ── Hero header ──────────────────────────────────────────────

@Composable
private fun StockHistoryHeader(
    entryCount: Int,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(NTColors.PrimaryDeep, NTColors.GradStart, NTColors.GradEnd),
                    start = Offset.Zero,
                    end = Offset(1200f, 400f),
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = NTColors.TextPrimary, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stock History",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NTColors.TextOnPrimary,
                )
                Text(
                    text = "Inventory ledger · every movement across all shops",
                    fontSize = 12.sp,
                    color = NTColors.CancelIconBg,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(NTColors.PrimaryLight)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "$entryCount movements",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NTColors.Primary,
                )
            }
        }
    }
}

// ── KPI stats strip ──────────────────────────────────────────

@Composable
private fun StockHistoryStats(
    movements: Int,
    delivered: Int,
    restocked: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HistoryStat(
            icon = Icons.Rounded.History,
            iconBg = NTColors.PrimaryLight,
            iconFg = NTColors.Primary,
            value = "$movements",
            label = "MOVEMENTS",
            modifier = Modifier.weight(1f),
        )
        HistoryStat(
            icon = Icons.Rounded.LocalShipping,
            iconBg = NTColors.InfoLight,
            iconFg = NTColors.InfoText,
            value = "$delivered",
            label = "DELIVERED",
            modifier = Modifier.weight(1f),
        )
        HistoryStat(
            icon = Icons.Rounded.Inventory2,
            iconBg = NTColors.SuccessLight,
            iconFg = NTColors.SuccessText,
            value = "$restocked",
            label = "RESTOCKED",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HistoryStat(
    icon: ImageVector,
    iconBg: Color,
    iconFg: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconFg, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = value,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NTColors.TextPrimary,
                maxLines = 1,
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = NTColors.TextTertiary,
                maxLines = 1,
            )
        }
    }
}

// ── Controls (search + shop + employee + date + sort) ───────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockHistoryControls(
    shopOptions: List<String>,
    selectedShop: String,
    onSelectShop: (String) -> Unit,
    employees: List<Pair<String, String>>,
    selectedEmployee: String,
    onSelectEmployee: (String) -> Unit,
    dateFrom: String,
    dateTo: String,
    onDateFromChange: (String) -> Unit,
    onDateToChange: (String) -> Unit,
    sortMode: SortMode,
    onSelectSort: (SortMode) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        // Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NTColors.SurfaceVar)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = NTColors.TextTertiary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = NTColors.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(NTColors.Primary),
            )
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Rounded.Close,
                    "Clear search",
                    tint = NTColors.TextTertiary,
                    modifier = Modifier.size(16.dp).clickable { onQueryChange("") },
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Shop chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                HistoryChip(
                    text = "All Shops",
                    selected = selectedShop.isBlank(),
                    icon = Icons.Rounded.Store,
                    onClick = { onSelectShop("") },
                )
            }
            items(shopOptions) { name ->
                HistoryChip(
                    text = name,
                    selected = shopMatchKey(selectedShop) == shopMatchKey(name),
                    onClick = { onSelectShop(name) },
                )
            }
        }

        // Employee chips (only shown if employees contributed movements)
        if (employees.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    HistoryChip(
                        text = "All Employees",
                        selected = selectedEmployee.isBlank(),
                        icon = Icons.Rounded.Person,
                        onClick = { onSelectEmployee("") },
                    )
                }
                items(employees, key = { it.first }) { (id, name) ->
                    HistoryChip(
                        text = name,
                        selected = selectedEmployee == id,
                        icon = Icons.Rounded.Person,
                        onClick = { onSelectEmployee(id) },
                    )
                }
            }
        }

        // Date range
        Spacer(Modifier.height(8.dp))
        DateRangeRow(
            dateFrom = dateFrom,
            dateTo = dateTo,
            onFromChange = onDateFromChange,
            onToChange = onDateToChange,
        )

        Spacer(Modifier.height(8.dp))

        // Sort chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SortMode.entries) { mode ->
                HistoryChip(
                    text = mode.label,
                    selected = sortMode == mode,
                    icon = mode.icon,
                    onClick = { onSelectSort(mode) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeRow(
    dateFrom: String,
    dateTo: String,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
) {
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DateFilterChip(
            label = "From",
            date = dateFrom,
            onClick = { showFromPicker = true },
            onClear = { onFromChange("") },
            modifier = Modifier.weight(1f),
        )
        DateFilterChip(
            label = "To",
            date = dateTo,
            onClick = { showToPicker = true },
            onClear = { onToChange("") },
            modifier = Modifier.weight(1f),
        )
    }

    if (showFromPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dateStrToEpochMillis(dateFrom),
        )
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onFromChange(epochMillisToDateStr(it)) }
                    showFromPicker = false
                }) { Text("OK", color = NTColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) { Text("Cancel", color = NTColors.TextSecondary) }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showToPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dateStrToEpochMillis(dateTo),
        )
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onToChange(epochMillisToDateStr(it)) }
                    showToPicker = false
                }) { Text("OK", color = NTColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) { Text("Cancel", color = NTColors.TextSecondary) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DateFilterChip(
    label: String,
    date: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayText = if (date.isNotBlank()) isoDateToDisplay(date) else label
    val isActive = date.isNotBlank()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (isActive) NTColors.PrimaryLight else NTColors.SurfaceVar)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.CalendarToday,
            null,
            tint = if (isActive) NTColors.Primary else NTColors.TextSecondary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = displayText,
            color = if (isActive) NTColors.Primary else NTColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (isActive) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Rounded.Close,
                "Clear",
                tint = NTColors.Primary,
                modifier = Modifier.size(12.dp).clickable(onClick = onClear),
            )
        }
    }
}

@Composable
private fun HistoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) NTColors.Primary else NTColors.SurfaceVar)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                null,
                tint = if (selected) NTColors.TextOnPrimary else NTColors.TextSecondary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            color = if (selected) NTColors.TextOnPrimary else NTColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

// ── Date grouping ────────────────────────────────────────────

@Composable
private fun StockHistoryDateHeader(m: StockMovement) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = isoToDisplayDate(m.createdAt),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = NTColors.TextTertiary,
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(NTColors.Divider))
    }
}

// ── Movement card ────────────────────────────────────────────

@Composable
private fun MovementCard(
    movement: StockMovement,
    productName: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val style = classify(movement)
    val isAdd = movement.type == "inward"
    val qtyText = "${if (isAdd) "+" else "-"}${movement.qty}"
    val time = isoToDisplayTime(movement.createdAt).ifBlank { "—" }
    val shop = movement.shopName.ifBlank { "—" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(20.dp))
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(style.iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(style.icon, null, tint = style.iconFg, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = productName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NTColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(style.iconBg.copy(alpha = 0.55f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = style.badge,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = style.iconFg,
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = movement.source,
                    fontSize = 11.sp,
                    color = NTColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Store, null, tint = NTColors.TextTertiary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = shop, fontSize = 10.sp, color = NTColors.TextTertiary, maxLines = 1)
                    Text(text = " · ", fontSize = 10.sp, color = NTColors.TextTertiary)
                    Icon(Icons.Rounded.Schedule, null, tint = NTColors.TextTertiary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = time, fontSize = 10.sp, color = NTColors.TextTertiary)
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = qtyText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = style.amountColor,
                )
                Text(
                    text = "UNITS",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = NTColors.TextTertiary,
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = NTColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Empty state ──────────────────────────────────────────────

@Composable
private fun StockHistoryEmpty() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(NTColors.PrimaryLight, NTColors.SurfaceVar))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Inventory2,
                contentDescription = null,
                tint = NTColors.Primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No movements found",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = NTColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Try adjusting the shop, employee, date range or search term.",
            fontSize = 13.sp,
            color = NTColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}