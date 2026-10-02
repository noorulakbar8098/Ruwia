package com.example.ruwia.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.isEmptyCansSource
import com.example.ruwia.domain.resolveMovementProductName
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTPrimaryTopBar
import com.example.ruwia.util.isoToDisplayDate
import com.example.ruwia.util.isoToDisplayTime
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

// ─────────────────────────────────────────────────────────────
//  Stock History — Premium SaaS ledger view
//  Deep-teal glossy hero · stats · search (with filter icon) ·
//  movement-type filter chips · date-wise order · date-grouped,
//  colour-coded movement cards with expandable details.
// ─────────────────────────────────────────────────────────────

// Deep teal shared with the home Business Overview card.
private val HistTeal = Color(0xFF0F2E2C)

private enum class SortMode(val label: String, val icon: ImageVector) {
    Newest("Newest first", Icons.Rounded.ArrowDownward),
    Oldest("Oldest first", Icons.Rounded.ArrowUpward),
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
            icon = Icons.Rounded.Download,
            iconBg = NTColors.PrimaryLight,
            iconFg = HistTeal,
            badge = when {
                m.source.contains("Opening", ignoreCase = true) -> "OPENING"
                m.source.contains("Restock", ignoreCase = true) -> "RESTOCK"
                m.source.contains("Adjustment", ignoreCase = true) -> "STOCK ADJUST"
                m.source.contains("Purchase", ignoreCase = true) -> "PURCHASE"
                else -> "STOCK IN"
            },
            amountColor = HistTeal,
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

// ── Movement-type filter ─────────────────────────────────────
//  Every ledger row belongs to exactly one bucket, so no entry can vanish
//  behind a filter: transfers and empty-can returns get their own chips
//  alongside All / Sold / Purchase / Adjust Stock.

private enum class HistoryTypeFilter(val label: String, val icon: ImageVector) {
    All("All", Icons.Rounded.Category),
    Sold("Sold", Icons.Rounded.ShoppingCart),
    Purchase("Purchase", Icons.Rounded.ShoppingBag),
    Adjust("Adjust Stock", Icons.Rounded.Tune),
    Transfer("Transfer", Icons.Rounded.SwapHoriz),
    Returns("Returns", Icons.Rounded.Recycling),
}

private fun StockMovement.typeFilter(): HistoryTypeFilter = when {
    source.startsWith("Transfer", ignoreCase = true) -> HistoryTypeFilter.Transfer
    source.contains("Adjustment", ignoreCase = true) -> HistoryTypeFilter.Adjust
    source.isNotEmpty() && source.isEmptyCansSource() -> HistoryTypeFilter.Returns
    type == "outward" -> HistoryTypeFilter.Sold
    else -> HistoryTypeFilter.Purchase
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

    var typeFilter by remember { mutableStateOf(HistoryTypeFilter.All) }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<String?>(null) }
    // Sale-record names by product id: recovers real product names for rows
    // written before the movement product_name snapshot existed, so deleted
    // products (e.g. Kinly 2L) keep their names in history.
    val salesByProduct = remember(state.saleEntries) {
        state.saleEntries.groupBy { it.productId }
            .mapValues { (_, rows) -> rows.firstOrNull()?.productName.orEmpty() }
    }
    /** Display name for a row that survives product soft-delete. */
    fun movementDisplayName(m: StockMovement): String =
        resolveMovementProductName(m, products, salesByProduct) ?: m.source
    // Date-wise order only: newest first (default) or oldest first.
    var sortMode by remember { mutableStateOf(SortMode.Newest) }
    var query by remember { mutableStateOf("") }

    val scoped = remember(movements, typeFilter, dateFrom, dateTo) {
        var list = movements
        if (typeFilter != HistoryTypeFilter.All) {
            list = list.filter { it.typeFilter() == typeFilter }
        }
        if (dateFrom.isNotBlank() || dateTo.isNotBlank()) {
            list = list.filter { m ->
                // Rows without a parseable ISO date stay visible.
                val d = m.createdAt?.take(10) ?: return@filter true
                if (d.length != 10 || d[4] != '-') return@filter true
                if (dateFrom.isNotBlank() && d < dateFrom) return@filter false
                if (dateTo.isNotBlank() && d > dateTo) return@filter false
                true
            }
        }
        list
    }

    val sorted = remember(scoped, sortMode) {
        when (sortMode) {
            SortMode.Newest -> scoped.sortedByDescending { it.createdAt }
            SortMode.Oldest -> scoped.sortedBy { it.createdAt ?: "9999" }
        }
    }

    val filtered = remember(sorted, query) {
        val q = query.trim()
        if (q.isBlank()) sorted
        else {
            val low = q.lowercase()
            sorted.filter {
                movementDisplayName(it).lowercase().contains(low)
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
                    typeFilter = typeFilter,
                    onSelectType = { typeFilter = it },
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    onDateFromChange = { dateFrom = it },
                    onDateToChange = { dateTo = it },
                    sortMode = sortMode,
                    onSelectSort = { sortMode = it },
                    query = query,
                    onQueryChange = { query = it },
                    resultCount = totalMovements,
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
                    val prodName = if (isReturn) "Empty Units" else movementDisplayName(m)

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
//  Uniform primary top bar shared with every inner screen.

@Composable
private fun StockHistoryHeader(
    entryCount: Int,
    onBack: () -> Unit,
) {
    NTPrimaryTopBar(
        title = "Stock History",
        subtitle = "Inventory ledger · every movement across all shops",
        onBack = onBack,
        horizontalPadding = 16.dp,
        trailingText = "$entryCount movements",
    )
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
            iconFg = HistTeal,
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
            iconBg = NTColors.PrimaryLight,
            iconFg = HistTeal,
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

// ── Controls (search with filter icon + movement-type chips) ───────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockHistoryControls(
    typeFilter: HistoryTypeFilter,
    onSelectType: (HistoryTypeFilter) -> Unit,
    dateFrom: String,
    dateTo: String,
    onDateFromChange: (String) -> Unit,
    onDateToChange: (String) -> Unit,
    sortMode: SortMode,
    onSelectSort: (SortMode) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int = 0,
) {
    var showFilterSheet by remember { mutableStateOf(false) }

    val hasActiveFilter = typeFilter != HistoryTypeFilter.All ||
        dateFrom.isNotBlank() || dateTo.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        // Search with the filter entry point inside the box
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
                Spacer(Modifier.width(4.dp))
            }
            // Filter icon inside the search box (opens type + sort sheet)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (hasActiveFilter) HistTeal else NTColors.Surface)
                    .border(
                        1.dp,
                        if (hasActiveFilter) HistTeal else NTColors.Border,
                        CircleShape,
                    )
                    .clickable(onClickLabel = "Open filters", onClick = { showFilterSheet = true }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Tune,
                    contentDescription = "Filters",
                    tint = if (hasActiveFilter) Color.White else NTColors.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Movement-type chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(HistoryTypeFilter.entries) { option ->
                HistoryChip(
                    text = option.label,
                    selected = typeFilter == option,
                    icon = option.icon,
                    onClick = { onSelectType(option) },
                )
            }
        }
    }

    if (showFilterSheet) {
        HistoryFilterSheet(
            typeFilter = typeFilter,
            onSelectType = onSelectType,
            dateFrom = dateFrom,
            dateTo = dateTo,
            onDateFromChange = onDateFromChange,
            onDateToChange = onDateToChange,
            sortMode = sortMode,
            onSelectSort = onSelectSort,
            resultCount = resultCount,
            onClearAll = {
                onSelectType(HistoryTypeFilter.All)
                onDateFromChange("")
                onDateToChange("")
            },
            onDismiss = { showFilterSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryFilterSheet(
    typeFilter: HistoryTypeFilter,
    onSelectType: (HistoryTypeFilter) -> Unit,
    dateFrom: String,
    dateTo: String,
    onDateFromChange: (String) -> Unit,
    onDateToChange: (String) -> Unit,
    sortMode: SortMode,
    onSelectSort: (SortMode) -> Unit,
    resultCount: Int,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NTColors.Surface,
        contentColor = NTColors.TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Filters & sort",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp,
                        color = NTColors.TextPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Refine the ledger by movement type, date or order",
                        fontSize = 13.sp,
                        color = NTColors.TextSecondary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NTColors.SurfaceVar)
                        .border(1.dp, NTColors.Border, CircleShape)
                        .clickable(onClickLabel = "Close filters", onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        tint = NTColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            FilterSectionLabel("MOVEMENT TYPE")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HistoryTypeFilter.entries) { option ->
                    HistoryChip(
                        text = option.label,
                        selected = typeFilter == option,
                        icon = option.icon,
                        onClick = { onSelectType(option) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            FilterSectionLabel("DATES")
            Spacer(Modifier.height(8.dp))
            DateRangeRow(
                dateFrom = dateFrom,
                dateTo = dateTo,
                onFromChange = onDateFromChange,
                onToChange = onDateToChange,
            )

            Spacer(Modifier.height(16.dp))
            FilterSectionLabel("SORT ORDER")
            Spacer(Modifier.height(8.dp))
            SortMode.entries.forEach { mode ->
                SortOptionRow(
                    mode = mode,
                    selected = sortMode == mode,
                    onClick = { onSelectSort(mode) },
                )
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(onClick = onClearAll) {
                    Text("Clear all", color = HistTeal, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HistTeal,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                ) {
                    Text(
                        "Show $resultCount movements",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        color = NTColors.TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun SortOptionRow(
    mode: SortMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) NTColors.PrimaryLight else Color.Transparent)
            .clickable(onClickLabel = mode.label, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) HistTeal else NTColors.SurfaceVar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                mode.icon,
                contentDescription = null,
                tint = if (selected) Color.White else NTColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = mode.label,
            color = NTColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = HistTeal,
                modifier = Modifier.size(20.dp),
            )
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
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateStrToEpochMillis(dateFrom),
        )
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onFromChange(epochMillisToDateStr(it)) }
                    showFromPicker = false
                }) { Text("OK", color = HistTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) { Text("Cancel", color = NTColors.TextSecondary) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showToPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateStrToEpochMillis(dateTo),
        )
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onToChange(epochMillisToDateStr(it)) }
                    showToPicker = false
                }) { Text("OK", color = HistTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) { Text("Cancel", color = NTColors.TextSecondary) }
            },
        ) {
            DatePicker(state = pickerState)
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
            tint = if (isActive) HistTeal else NTColors.TextSecondary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = displayText,
            color = if (isActive) HistTeal else NTColors.TextSecondary,
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
                tint = HistTeal,
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
            .background(if (selected) HistTeal else NTColors.SurfaceVar)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                null,
                tint = if (selected) Color.White else NTColors.TextSecondary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            color = if (selected) Color.White else NTColors.TextSecondary,
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
    val expandRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "historyExpand",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(24.dp))
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
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NTColors.SurfaceVar)
                    .border(1.dp, NTColors.Border, CircleShape)
                    .clickable(
                        onClickLabel = if (isExpanded) "Collapse details" else "Expand details",
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = NTColors.TextSecondary,
                    modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = expandRotation }
                )
            }
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
                tint = HistTeal,
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
            text = "Try a different type, date range or search term.",
            fontSize = 13.sp,
            color = NTColors.TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}