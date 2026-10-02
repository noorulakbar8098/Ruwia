package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.SaleEntry
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import com.example.ruwia.ui.dashboard.NTPrimaryTopBar
import com.example.ruwia.util.dbToDisplayDate
import com.example.ruwia.util.isoToDisplayTime
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import ruwia.shared.generated.resources.*
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────
//  Transactions — complete sales history (admin).
//
//  Every value derives from AdminState.saleEntries (+ customer /
//  employee / shop lookups). Fields with no backing data — payment
//  method, bill numbers, variants — are deliberately NOT shown.
// ─────────────────────────────────────────────────────────────

private enum class TxPreset { ALL, TODAY, WEEK, MONTH }
private enum class TxSort { NEWEST, OLDEST, AMOUNT_DESC }

// Deep-teal glossy tone shared with the home overview card.
private val TxGlossTeal = Color(0xFF0F2E2C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: AdminState,
    contentPadding: PaddingValues,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var openedId by remember { mutableStateOf<String?>(null) }
    val opened = state.saleEntries.find { it.id == openedId }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val todayStr = remember {
        try {
            val t = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            "%04d-%02d-%02d".format(t.year, t.monthNumber, t.dayOfMonth)
        } catch (_: Exception) {
            "2026-06-24"
        }
    }

    var query by remember { mutableStateOf("") }
    var preset by remember { mutableStateOf(TxPreset.ALL) }
    var sort by remember { mutableStateOf(TxSort.NEWEST) }
    var showFilters by remember { mutableStateOf(false) }
    var fCustomer by remember { mutableStateOf("") }
    var fProduct by remember { mutableStateOf("") }
    var fEmployee by remember { mutableStateOf("") }
    var fShop by remember { mutableStateOf("") }

    val customerByName = remember(state.customers) {
        state.customers.associateBy { it.name.trim().lowercase() }
    }
    val employeeById = remember(state.employees) {
        state.employees.associateBy { it.id }
    }

    fun shopNameOf(sale: SaleEntry): String {
        val raw = sale.shopId.trim()
        if (raw.isBlank()) return "—"
        state.shopStocks.find { it.id == raw }?.let { return it.name }
        state.shopNames.find { it.equals(raw, ignoreCase = true) }?.let { return it }
        return raw
    }
    fun employeeNameOf(sale: SaleEntry): String =
        sale.employeeId?.let { employeeById[it]?.name } ?: "—"

    val filtered = remember(
        state.saleEntries, query, preset, sort,
        fCustomer, fProduct, fEmployee, fShop, todayStr,
    ) {
        val q = query.trim().lowercase()
        state.saleEntries
            .filter { s ->
                // sale_entries.date is TEXT — older rows carry a "YYYY-MM-DD h:mm AM"
                // suffix, so compare on the 10-char day prefix. This also keeps
                // Today/Week/Month working without a backfill migration.
                val day = s.date.take(10)
                when (preset) {
                    TxPreset.ALL -> true
                    TxPreset.TODAY -> day == todayStr
                    TxPreset.WEEK -> day >= weekAgoStr(todayStr)
                    TxPreset.MONTH -> day.startsWith(todayStr.substring(0, 7))
                }
            }
            .filter { s -> fCustomer.isBlank() || s.customerName.equals(fCustomer, ignoreCase = true) }
            .filter { s -> fProduct.isBlank() || s.productName.equals(fProduct, ignoreCase = true) }
            .filter { s ->
                fEmployee.isBlank() ||
                    (s.employeeId?.let { employeeById[it]?.name }.orEmpty()
                        .equals(fEmployee, ignoreCase = true))
            }
            .filter { s -> fShop.isBlank() || shopNameOf(s).equals(fShop, ignoreCase = true) }
            .filter { s ->
                if (q.isBlank()) true
                else {
                    val cust = customerByName[s.customerName.trim().lowercase()]
                    s.customerName.lowercase().contains(q) ||
                        (cust?.phone.orEmpty().lowercase().contains(q)) ||
                        (s.id.orEmpty().lowercase().contains(q)) ||
                        s.productName.lowercase().contains(q) ||
                        employeeNameOf(s).lowercase().contains(q) ||
                        shopNameOf(s).lowercase().contains(q)
                }
            }
            .let { list ->
                when (sort) {
                    TxSort.NEWEST -> list.sortedByDescending { it.date }
                    TxSort.OLDEST -> list.sortedBy { it.date }
                    TxSort.AMOUNT_DESC -> list.sortedByDescending { it.totalSelling }
                }
            }
    }

    val totalSales = filtered.sumOf { it.totalSelling }
    val totalProfit = filtered.sumOf { it.totalMargin }
    val totalQty = filtered.sumOf { it.qty }
    val filterCount =
        (if (fCustomer.isNotBlank()) 1 else 0) +
        (if (fProduct.isNotBlank()) 1 else 0) +
        (if (fEmployee.isNotBlank()) 1 else 0) +
        (if (fShop.isNotBlank()) 1 else 0)

    val customerOptions = remember(state.saleEntries) {
        state.saleEntries.map { it.customerName.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    val productOptions = remember(state.saleEntries) {
        state.saleEntries.map { it.productName.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    val employeeOptions = remember(state.saleEntries, state.employees) {
        state.saleEntries.mapNotNull { it.employeeId }
            .distinct()
            .mapNotNull { employeeById[it]?.name }
            .distinct().sorted()
    }
    val shopOptions = remember(state.saleEntries, state.shopStocks, state.shopNames) {
        state.saleEntries.map { shopNameOf(it) }.filter { it.isNotBlank() && it != "—" }
            .distinct().sorted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NTColors.Background)
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        NTPrimaryTopBar(
            title = "Transactions",
            subtitle = "Complete sales history",
            onBack = onBackHome,
            trailingText = "${filtered.size} entries",
        )

        // Controls stay pinned above the scrolling list.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NTDp.screenPad)
                .padding(top = 12.dp, bottom = 8.dp),
        ) {
            TxSearchField(
                query = query,
                onQueryChange = { query = it },
                onFilterClick = { showFilters = true },
                filterCount = filterCount,
            )
            Spacer(Modifier.height(10.dp))
            TxChipsRow(
                preset = preset,
                onPreset = { preset = it },
                sortLabel = sort.label,
                onSortClick = { sort = sort.next() },
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {

        item {
            Spacer(Modifier.height(12.dp))
            TxSummaryRow(
                totalSales = totalSales,
                totalProfit = totalProfit,
                count = filtered.size,
                totalQty = totalQty,
            )
            Spacer(Modifier.height(16.dp))
        }

        if (filtered.isEmpty()) {
            item {
                TxEmptyState(hasQuery = query.isNotBlank() || filterCount > 0 || preset != TxPreset.ALL)
            }
        } else {
            items(filtered, key = { it.id ?: "${it.date}-${it.customerName}-${it.productName}-${it.qty}" }) { sale ->
                val cust = customerByName[sale.customerName.trim().lowercase()]
                TransactionCard(
                    sale = sale,
                    customer = cust,
                    employeeName = employeeNameOf(sale),
                    shopName = shopNameOf(sale),
                    onClick = { openedId = sale.id },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    }
    if (showFilters) {
        TxFilterSheet(
            customerOptions = customerOptions,
            selectedCustomer = fCustomer,
            onSelectCustomer = { fCustomer = it },
            productOptions = productOptions,
            selectedProduct = fProduct,
            onSelectProduct = { fProduct = it },
            employeeOptions = employeeOptions,
            selectedEmployee = fEmployee,
            onSelectEmployee = { fEmployee = it },
            shopOptions = shopOptions,
            selectedShop = fShop,
            onSelectShop = { fShop = it },
            resultCount = filtered.size,
            onClearAll = {
                fCustomer = ""; fProduct = ""; fEmployee = ""; fShop = ""
            },
            onDismiss = { showFilters = false },
        )
    }

    opened?.let { sale ->
        TransactionDetailsSheet(
            sale = sale,
            state = state,
            sheetState = sheetState,
            onDismiss = { openedId = null },
        )
    }
}

private val TxSort.label: String
    get() = when (this) {
        TxSort.NEWEST -> "Newest"
        TxSort.OLDEST -> "Oldest"
        TxSort.AMOUNT_DESC -> "Amount"
    }

private fun TxSort.next(): TxSort = when (this) {
    TxSort.NEWEST -> TxSort.OLDEST
    TxSort.OLDEST -> TxSort.AMOUNT_DESC
    TxSort.AMOUNT_DESC -> TxSort.NEWEST
}

private fun weekAgoStr(today: String): String {
    val p = today.split("-")
    if (p.size != 3) return today
    val y = p[0].toIntOrNull() ?: return today
    val m = p[1].toIntOrNull() ?: return today
    val d = p[2].toIntOrNull() ?: return today
    var days = d - 6
    var mm = m
    var yy = y
    while (days < 1) {
        mm -= 1
        if (mm < 1) {
            mm = 12; yy -= 1
        }
        days += daysInTxMonth(yy, mm)
    }
    return "%04d-%02d-%02d".format(yy, mm, days)
}

private fun daysInTxMonth(year: Int, month: Int): Int {
    val leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (leap) 29 else 28
        else -> 30
    }
}

/** Indian digit grouping: 124850 → ₹1,24,850. */
private fun formatTxAmount(v: Double): String {
    val neg = v < 0
    val digits = abs(v).toLong().toString()
    if (digits.length <= 3) return (if (neg) "-₹" else "₹") + digits
    val last3 = digits.takeLast(3)
    var rest = digits.dropLast(3)
    val parts = mutableListOf<String>()
    while (rest.length > 2) {
        parts.add(0, rest.takeLast(2))
        rest = rest.dropLast(2)
    }
    if (rest.isNotEmpty()) parts.add(0, rest)
    return (if (neg) "-₹" else "₹") + (parts + last3).joinToString(",")
}

private fun txRef(id: String?): String =
    if (id.isNullOrBlank()) "—" else "#" + id.takeLast(6).uppercase()

// ── Search + presets + icon buttons ─────────────────────────────────────────

@Composable
private fun TxSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    filterCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = NTColors.TextTertiary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search customer, phone, product or bill…",
                    color = NTColors.TextTertiary, fontSize = 14.sp, maxLines = 1,
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = NTColors.TextPrimary, fontSize = 14.sp,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(NTColors.Primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close, "Clear search",
                tint = NTColors.TextTertiary,
                modifier = Modifier.size(18.dp).clickable { onQueryChange("") },
            )
            Spacer(Modifier.width(4.dp))
        }
        Box {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (filterCount > 0) NTColors.Primary
                        else NTColors.SurfaceVar
                    )
                    .clickable(onClickLabel = "Open filters", onClick = onFilterClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Tune, null,
                    tint = if (filterCount > 0) Color.White else NTColors.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (filterCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(NTColors.Error)
                        .border(2.dp, NTColors.Surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (filterCount > 9) "9+" else "$filterCount",
                        color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TxChipsRow(
    preset: TxPreset,
    onPreset: (TxPreset) -> Unit,
    sortLabel: String,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val presets = listOf(
            TxPreset.ALL to "All",
            TxPreset.TODAY to "Today",
            TxPreset.WEEK to "This Week",
            TxPreset.MONTH to "This Month",
        )
        items(presets, key = { it.first }) { (mode, label) ->
            val isSelected = preset == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) TxGlossTeal else NTColors.Surface)
                    .then(
                        if (isSelected) Modifier.background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.04f),
                                    Color.Transparent,
                                )
                            )
                        ) else Modifier
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color.White.copy(alpha = 0.16f) else NTColors.Border,
                        RoundedCornerShape(50),
                    )
                    .clickable(onClickLabel = label, onClick = { onPreset(mode) })
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (isSelected) Color.White else NTColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
        item {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(NTColors.Surface)
                    .border(1.dp, NTColors.Border, RoundedCornerShape(50))
                    .clickable(onClickLabel = "Change sort", onClick = onSortClick)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Sort, null,
                        tint = NTColors.TextSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Sort: $sortLabel",
                        color = NTColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ── Summary ─────────────────────────────────────────────────────────────────

@Composable
private fun TxSummaryRow(
    totalSales: Double,
    totalProfit: Double,
    count: Int,
    totalQty: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TxSummaryCard(
                label = "Revenue",
                value = formatTxAmount(totalSales),
                icon = Icons.Rounded.AccountBalanceWallet,
                iconBg = NTColors.PrimaryLight,
                iconFg = NTColors.Primary,
                modifier = Modifier.weight(1f),
            )
            TxSummaryCard(
                label = "Profit",
                value = formatTxAmount(totalProfit),
                icon = Icons.Rounded.Savings,
                iconBg = NTColors.SuccessLight,
                iconFg = NTColors.Success,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TxSummaryCard(
                label = "Transactions",
                value = formatTxCount(count),
                icon = Icons.Rounded.ReceiptLong,
                iconBg = NTColors.InfoLight,
                iconFg = NTColors.Info,
                modifier = Modifier.weight(1f),
            )
            TxSummaryCard(
                label = "Quantity",
                value = formatTxCount(totalQty),
                icon = Icons.Rounded.Inventory2,
                iconBg = NTColors.WarningLight,
                iconFg = NTColors.WarningText,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Plain-number Indian digit grouping for counts: 124850 → 1,24,850. */
private fun formatTxCount(v: Int): String {
    val digits = kotlin.math.abs(v).toLong().toString()
    val neg = v < 0
    if (digits.length <= 3) return (if (neg) "-" else "") + digits
    val last3 = digits.takeLast(3)
    var rest = digits.dropLast(3)
    val parts = mutableListOf<String>()
    while (rest.length > 2) {
        parts.add(0, rest.takeLast(2))
        rest = rest.dropLast(2)
    }
    if (rest.isNotEmpty()) parts.add(0, rest)
    return (if (neg) "-" else "") + (parts + last3).joinToString(",")
}

@Composable
private fun TxSummaryCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconFg: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(20.dp))
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconFg, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp, color = NTColors.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp, color = NTColors.TextTertiary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Transaction card ────────────────────────────────────────────────────────

/** Size token derived from the product name itself (e.g. "20L", "250ml"). */
private fun txSizeChip(productName: String): String? {
    val match = Regex("""(\d+)\s?(ml|l)\b""", RegexOption.IGNORE_CASE).find(productName)
        ?: return null
    val unit = match.groupValues[2].lowercase()
    return match.groupValues[1] + if (unit == "l") "L" else "ml"
}

@Composable
private fun TxVariantChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(NTColors.SurfaceVar)
            .border(1.dp, NTColors.Border, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            color = NTColors.TextSecondary, maxLines = 1,
        )
    }
}

@Composable
private fun TransactionCard(
    sale: SaleEntry,
    customer: Customer?,
    employeeName: String,
    shopName: String,
    onClick: () -> Unit,
) {
    val profitUp = sale.totalMargin >= 0.0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad)
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(20.dp))
            .clickable(onClickLabel = "Open transaction details", onClick = onClick)
            .padding(16.dp),
    ) {
        // Header row: customer + ref + chevron.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                sale.customerName.ifBlank { "Walk-in" },
                fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                txRef(sale.id),
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = NTColors.TextTertiary, maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.ChevronRight, "Open details",
                tint = NTColors.TextTertiary, modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        // Product section.
        Row(verticalAlignment = Alignment.CenterVertically) {
            TxBottleThumb(productName = sale.productName)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sale.productName.ifBlank { "Product" },
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = NTColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    txSizeChip(sale.productName)?.let { TxVariantChip(it) }
                    Text(
                        "Qty ${sale.qty} · ${unitPriceOf(sale)}",
                        fontSize = 11.sp, color = NTColors.TextSecondary, maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Financial section.
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                formatTxAmount(sale.totalSelling),
                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp, color = NTColors.TextPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Profit ${if (profitUp) "+" else ""}${formatTxAmount(sale.totalMargin)}",
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = if (profitUp) NTColors.Success else NTColors.Error,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = NTColors.Border)
        Spacer(Modifier.height(8.dp))
        // Metadata footer (only recorded fields — no payment data exists).
        Text(
            buildMetaLine(sale, customer, employeeName, shopName),
            fontSize = 11.sp, color = NTColors.TextTertiary,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun unitPriceOf(sale: SaleEntry): String {
    val unit = if (sale.sellingPricePerUnit > 0) sale.sellingPricePerUnit
    else if (sale.qty > 0) sale.totalSelling / sale.qty else 0.0
    return "₹${unit.toLong()}/unit"
}

private fun buildMetaLine(
    sale: SaleEntry,
    customer: Customer?,
    employeeName: String,
    shopName: String,
): String {
    val parts = mutableListOf(dbToDisplayDate(sale.date))
    val time = isoToDisplayTime(sale.createdAt)
    if (time.isNotBlank()) parts.add(time)
    if (employeeName.isNotBlank() && employeeName != "—") parts.add(employeeName)
    if (shopName.isNotBlank() && shopName != "—") parts.add(shopName)
    if (customer?.phone?.isNotBlank() == true) parts.add(customer.phone!!)
    return parts.joinToString(" · ")
}

@Composable
private fun TxBottleThumb(productName: String) {
    // Default tile: 20L can when no image matches the product name.
    val painter = txBottlePainter(productName) ?: painterResource(Res.drawable.bottle_20l)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NTColors.SurfaceVar),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun txBottlePainter(productName: String): androidx.compose.ui.graphics.painter.Painter? {
    val n = productName.lowercase()
    val res = when {
        n.contains("300") -> Res.drawable.bottle_200ml
        n.contains("500") -> Res.drawable.bottle_500ml
        n.contains("2l") || n.contains("2 l") || n.contains("20l") -> Res.drawable.bottle_2l
        n.contains("1l") || n.contains("1 l") -> Res.drawable.bottle_2l
        n.contains("5l") || n.contains("5 l") -> Res.drawable.bottle_5l
        n.contains("20") -> Res.drawable.bottle_20l
        else -> null
    }
    return res?.let { painterResource(it) }
}

// ── Filter sheet ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TxFilterSheet(
    customerOptions: List<String>,
    selectedCustomer: String,
    onSelectCustomer: (String) -> Unit,
    productOptions: List<String>,
    selectedProduct: String,
    onSelectProduct: (String) -> Unit,
    employeeOptions: List<String>,
    selectedEmployee: String,
    onSelectEmployee: (String) -> Unit,
    shopOptions: List<String>,
    selectedShop: String,
    onSelectShop: (String) -> Unit,
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
                        "Filters",
                        fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp, color = NTColors.TextPrimary,
                    )
                    Text(
                        "Refine by customer, product, staff or shop",
                        fontSize = 13.sp, color = NTColors.TextSecondary,
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
                    Icon(Icons.Rounded.Close, null, tint = NTColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            if (shopOptions.isNotEmpty()) {
                TxFilterSection(
                    label = "SHOP",
                    options = shopOptions,
                    selected = selectedShop,
                    allLabel = "All shops",
                    onSelect = onSelectShop,
                )
            }
            TxFilterSection(
                label = "PRODUCT",
                options = productOptions,
                selected = selectedProduct,
                allLabel = "All products",
                onSelect = onSelectProduct,
            )
            TxFilterSection(
                label = "CUSTOMER",
                options = customerOptions,
                selected = selectedCustomer,
                allLabel = "All customers",
                onSelect = onSelectCustomer,
            )
            if (employeeOptions.isNotEmpty()) {
                TxFilterSection(
                    label = "SALESPERSON",
                    options = employeeOptions,
                    selected = selectedEmployee,
                    allLabel = "Everyone",
                    onSelect = onSelectEmployee,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(onClick = onClearAll) {
                    Text("Clear all", color = NTColors.Primary, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                ) {
                    Text("Show $resultCount", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun TxFilterSection(
    label: String,
    options: List<String>,
    selected: String,
    allLabel: String,
    onSelect: (String) -> Unit,
) {
    Text(
        label, color = NTColors.TextTertiary, fontSize = 11.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
    )
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            TxFilterChip(text = allLabel, selected = selected.isBlank(), onClick = { onSelect("") })
        }
        items(options.take(30), key = { it }) { name ->
            TxFilterChip(text = name, selected = selected == name, onClick = { onSelect(name) })
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun TxFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) NTColors.Primary else NTColors.SurfaceVar)
            .border(
                1.dp,
                if (selected) NTColors.Primary else NTColors.Border,
                RoundedCornerShape(50),
            )
            .clickable(onClickLabel = text, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else NTColors.TextSecondary,
            maxLines = 1,
        )
    }
}

// ── Empty state ─────────────────────────────────────────────────────────────

@Composable
private fun TxEmptyState(hasQuery: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(NTColors.SurfaceVar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ReceiptLong, null,
                tint = NTColors.TextTertiary, modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "No transactions found",
            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasQuery) "Try adjusting the search, dates or filters."
            else "Sales will appear here once recorded.",
            fontSize = 13.sp, color = NTColors.TextTertiary, textAlign = TextAlign.Center,
        )
    }
}

// ── Transaction Details ─────────────────────────────────────────────────────
//  Premium SaaS-style bottom sheet. Every value derives from the SaleEntry +
//  customer / employee / shop lookups — no invented business data, no
//  navigation changes (back/close only dismisses the sheet).

private val TxDetailsHeaderTop = Color(0xFF155E59)
private val TxDetailsHeaderBottom = TxGlossTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailsSheet(
    sale: SaleEntry,
    state: AdminState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    val customer = state.customers.find { it.name.trim().equals(sale.customerName.trim(), ignoreCase = true) }
    val employeeName = sale.employeeId?.let { id -> state.employees.find { it.id == id }?.name } ?: "—"
    val shopName = sale.shopId.trim().ifBlank { "—" }.let { raw ->
        state.shopStocks.find { it.id == raw }?.name ?: raw
    }
    val unitPrice = if (sale.sellingPricePerUnit > 0) sale.sellingPricePerUnit
    else if (sale.qty > 0) sale.totalSelling / sale.qty else 0.0
    val unitCost = if (sale.purchasePricePerUnit > 0) sale.purchasePricePerUnit
    else if (sale.qty > 0) (sale.totalSelling - sale.totalMargin) / sale.qty else 0.0
    val totalCost = unitCost * sale.qty
    val profitUp = sale.totalMargin >= 0.0

    val ref = txRef(sale.id)
    val dateText = dbToDisplayDate(sale.date)
    val timeText = isoToDisplayTime(sale.createdAt)
    val dateTimeText = if (timeText.isBlank()) dateText else "$dateText · $timeText"
    val customerName = sale.customerName.ifBlank { "Walk-in" }
    val customerIdShort = customer?.id?.take(8).orEmpty()
    val phoneText = customer?.phone.orEmpty()
    val addressText = customer?.address.orEmpty()
    val sizeText = txSizeChip(sale.productName).orEmpty()
    val unitPriceText = "${formatTxAmount(unitPrice)}/unit"

    var overflowOpen by remember { mutableStateOf(false) }
    var refCopied by remember { mutableStateOf(false) }
    var idCopied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(refCopied) {
        if (refCopied) {
            kotlinx.coroutines.delay(1500)
            refCopied = false
        }
    }
    LaunchedEffect(idCopied) {
        if (idCopied) {
            kotlinx.coroutines.delay(1500)
            idCopied = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NTColors.Background,
        contentColor = NTColors.TextPrimary,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Premium dark-teal header (full-bleed, status-bar aware) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(TxDetailsHeaderTop, TxDetailsHeaderBottom)
                        )
                    ),
            ) {
                // Soft decorative depth — single translucent circle, no clutter.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 72.dp, y = 72.dp)
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f)),
                )
                // Subtle top gloss for a premium finish.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.03f),
                                    Color.Transparent,
                                )
                            )
                        ),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp, bottom = 18.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.30f)),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                                .clickable(onClickLabel = "Back", onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack, "Back",
                                tint = Color.White, modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Transaction Details",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Complete information about this sale",
                                color = Color.White.copy(alpha = 0.62f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                                    .clickable(
                                        onClickLabel = "More options",
                                        onClick = { overflowOpen = true },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.MoreVert, "More options",
                                    tint = Color.White, modifier = Modifier.size(20.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = overflowOpen,
                                onDismissRequest = { overflowOpen = false },
                                containerColor = NTColors.Surface,
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (refCopied) "Reference copied" else "Copy reference",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (refCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                            null,
                                            tint = NTColors.TextSecondary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    },
                                    onClick = {
                                        runCatching {
                                            clipboard.setText(AnnotatedString(ref))
                                        }
                                        refCopied = true
                                        overflowOpen = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Close details",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.Close, null,
                                            tint = NTColors.TextSecondary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    },
                                    onClick = {
                                        overflowOpen = false
                                        onDismiss()
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Tag, null,
                                tint = Color(0xFF5EEAD4),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                ref,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp,
                                maxLines = 1,
                            )
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Icon(
                                Icons.Rounded.CalendarToday, null,
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                dateTimeText,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                }
            }

            // ── Content cards ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Customer ──
                TxPremiumCard(
                    title = "Customer Details",
                    subtitle = "Who this sale was billed to",
                    icon = Icons.Rounded.Person,
                    iconBg = NTColors.PrimaryLight,
                    iconFg = NTColors.Primary,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(TxDetailsHeaderBottom),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                customerName.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                customerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.2).sp,
                                color = NTColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(3.dp))
                            if (customerIdShort.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "ID · $customerIdShort",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NTColors.TextTertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NTColors.SurfaceVar)
                                            .clickable(
                                                onClickLabel = "Copy customer ID",
                                                onClick = {
                                                    runCatching {
                                                        clipboard.setText(AnnotatedString(customerIdShort))
                                                    }
                                                    idCopied = true
                                                },
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            if (idCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                            "Copy customer ID",
                                            tint = if (idCopied) NTColors.Success else NTColors.TextTertiary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    "Walk-in customer",
                                    fontSize = 12.sp,
                                    color = NTColors.TextSecondary,
                                )
                            }
                        }
                    }
                    if (phoneText.isNotBlank()) {
                        TxStackedInfoRow(
                            icon = Icons.Rounded.Phone,
                            iconBg = NTColors.InfoLight,
                            iconFg = NTColors.Info,
                            label = "Phone",
                            value = phoneText,
                            showDivider = addressText.isNotBlank() || customerIdShort.isNotBlank(),
                        )
                    }
                    if (addressText.isNotBlank()) {
                        TxStackedInfoRow(
                            icon = Icons.Rounded.LocationOn,
                            iconBg = NTColors.ErrorLight,
                            iconFg = NTColors.Error,
                            label = "Address",
                            value = addressText,
                            showDivider = customerIdShort.isNotBlank(),
                        )
                    }
                    if (customerIdShort.isNotBlank()) {
                        TxStackedInfoRow(
                            icon = Icons.Rounded.Badge,
                            iconBg = NTColors.StockIconBg,
                            iconFg = NTColors.StockIconFg,
                            label = "Customer ID",
                            value = customerIdShort,
                            showDivider = false,
                        )
                    }
                    if (phoneText.isNotBlank() || addressText.isNotBlank() || customerIdShort.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (phoneText.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(NTColors.Primary)
                                        .clickable(
                                            onClickLabel = "Call $customerName",
                                            onClick = {
                                                runCatching { uriHandler.openUri("tel:$phoneText") }
                                            },
                                        )
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Call, null,
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp),
                                        )
                                        Spacer(Modifier.width(7.dp))
                                        Text(
                                            "Call",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                            if (addressText.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(NTColors.SurfaceVar)
                                        .border(1.dp, NTColors.Border, RoundedCornerShape(14.dp))
                                        .clickable(
                                            onClickLabel = "Open address in maps",
                                            onClick = {
                                                val query = addressText.trim().replace(" ", "+")
                                                runCatching {
                                                    uriHandler.openUri(
                                                        "https://www.google.com/maps/search/?api=1&query=$query"
                                                    )
                                                }
                                            },
                                        )
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.LocationOn, null,
                                            tint = NTColors.TextSecondary,
                                            modifier = Modifier.size(17.dp),
                                        )
                                        Spacer(Modifier.width(7.dp))
                                        Text(
                                            "Map",
                                            color = NTColors.TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                            if (customerIdShort.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(NTColors.SurfaceVar)
                                        .border(1.dp, NTColors.Border, RoundedCornerShape(14.dp))
                                        .clickable(
                                            onClickLabel = "Copy customer ID",
                                            onClick = {
                                                runCatching {
                                                    clipboard.setText(AnnotatedString(customerIdShort))
                                                }
                                                idCopied = true
                                            },
                                        )
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (idCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                            null,
                                            tint = if (idCopied) NTColors.Success else NTColors.TextSecondary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(7.dp))
                                        Text(
                                            if (idCopied) "Copied" else "Copy",
                                            color = NTColors.TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Transaction ──
                TxPremiumCard(
                    title = "Transaction Info",
                    subtitle = "Reference, date and time",
                    icon = Icons.Rounded.ReceiptLong,
                    iconBg = NTColors.InfoLight,
                    iconFg = NTColors.Info,
                ) {
                    TxStackedInfoRow(
                        icon = Icons.Rounded.Tag,
                        iconBg = NTColors.SurfaceVar,
                        iconFg = NTColors.TextSecondary,
                        label = "Reference",
                        value = ref,
                        valueColor = NTColors.TextPrimary,
                        showDivider = true,
                    )
                    TxStackedInfoRow(
                        icon = Icons.Rounded.CalendarToday,
                        iconBg = NTColors.SurfaceVar,
                        iconFg = NTColors.TextSecondary,
                        label = "Date",
                        value = dateText,
                        showDivider = timeText.isNotBlank(),
                    )
                    if (timeText.isNotBlank()) {
                        TxStackedInfoRow(
                            icon = Icons.Rounded.Schedule,
                            iconBg = NTColors.SurfaceVar,
                            iconFg = NTColors.TextSecondary,
                            label = "Time",
                            value = timeText,
                            showDivider = false,
                        )
                    }
                }

                // ── Product ──
                TxPremiumCard(
                    title = "Product Details",
                    subtitle = "Item sold in this transaction",
                    icon = Icons.Rounded.Inventory2,
                    iconBg = NTColors.WarningLight,
                    iconFg = NTColors.WarningText,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(NTColors.SurfaceVar)
                                .border(1.dp, NTColors.Border, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            val painter = txBottlePainter(sale.productName)
                                ?: painterResource(Res.drawable.bottle_20l)
                            androidx.compose.foundation.Image(
                                painter = painter,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                sale.productName.ifBlank { "Product" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.2).sp,
                                color = NTColors.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (sizeText.isNotBlank()) TxVariantChip(sizeText)
                                TxVariantChip("Qty ${sale.qty}")
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                unitPriceText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NTColors.TextSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TxStatTile(
                            label = "Size",
                            value = sizeText.ifBlank { "—" },
                            modifier = Modifier.weight(1f),
                        )
                        TxStatTile(
                            label = "Quantity",
                            value = "${sale.qty}",
                            modifier = Modifier.weight(1f),
                        )
                        TxStatTile(
                            label = "Price",
                            value = formatTxAmount(unitPrice),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NTColors.SurfaceVar)
                            .border(1.dp, NTColors.Border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "TOTAL AMOUNT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                color = NTColors.TextTertiary,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "${sale.qty} × ${formatTxAmount(unitPrice)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NTColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            formatTxAmount(sale.totalSelling),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            color = NTColors.TextPrimary,
                            maxLines = 1,
                        )
                    }
                }

                // ── Financial summary ──
                TxPremiumCard(
                    title = "Financial Summary",
                    subtitle = "Revenue, cost and margin",
                    icon = Icons.Rounded.Savings,
                    iconBg = NTColors.SuccessLight,
                    iconFg = NTColors.Success,
                ) {
                    TxLedgerRow(label = "Subtotal", value = formatTxAmount(sale.totalSelling))
                    TxLedgerRow(label = "Total cost", value = formatTxAmount(totalCost))
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.6f))
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (profitUp) NTColors.SuccessLight
                                else NTColors.ErrorLight
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.75f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.TrendingUp, null,
                                tint = if (profitUp) NTColors.Success else NTColors.Error,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Total Profit",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (profitUp) NTColors.SuccessText else NTColors.ErrorText,
                                maxLines = 1,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Margin on this sale",
                                fontSize = 11.sp,
                                color = (if (profitUp) NTColors.SuccessText else NTColors.ErrorText)
                                    .copy(alpha = 0.72f),
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            formatTxAmount(sale.totalMargin),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.4).sp,
                            color = if (profitUp) NTColors.SuccessText else NTColors.ErrorText,
                            maxLines = 1,
                        )
                    }
                }

                // ── Employee & shop ──
                TxPremiumCard(
                    title = "Employee & Shop",
                    subtitle = "Who made the sale and where",
                    icon = Icons.Rounded.Store,
                    iconBg = NTColors.StockIconBg,
                    iconFg = NTColors.StockIconFg,
                ) {
                    TxStackedInfoRow(
                        icon = Icons.Rounded.Badge,
                        iconBg = NTColors.SurfaceVar,
                        iconFg = NTColors.TextSecondary,
                        label = "Sold by",
                        value = employeeName,
                        showDivider = true,
                    )
                    TxStackedInfoRow(
                        icon = Icons.Rounded.Store,
                        iconBg = NTColors.SurfaceVar,
                        iconFg = NTColors.TextSecondary,
                        label = "Shop",
                        value = shopName,
                        showDivider = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun TxPremiumCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconFg: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconFg, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp,
                    color = NTColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = NTColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.55f))
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun TxStackedInfoRow(
    icon: ImageVector,
    iconBg: Color,
    iconFg: Color,
    label: String,
    value: String,
    valueColor: Color = NTColors.TextPrimary,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconFg, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    color = NTColors.TextTertiary,
                    maxLines = 1,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    value.ifBlank { "—" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.45f))
        }
    }
}

@Composable
private fun TxLedgerRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = NTColors.TextSecondary,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NTColors.TextPrimary,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TxStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NTColors.SurfaceVar)
            .border(1.dp, NTColors.Border.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            color = NTColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NTColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
