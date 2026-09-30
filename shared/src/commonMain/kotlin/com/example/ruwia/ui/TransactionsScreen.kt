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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.EmployeeInfo
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
                when (preset) {
                    TxPreset.ALL -> true
                    TxPreset.TODAY -> s.date == todayStr
                    TxPreset.WEEK -> s.date >= weekAgoStr(todayStr)
                    TxPreset.MONTH -> s.date.startsWith(todayStr.substring(0, 7))
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
private fun TxSummaryRow(totalSales: Double, totalProfit: Double, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TxSummaryCard(
            label = "Total Sales",
            value = formatTxAmount(totalSales),
            icon = Icons.Rounded.AccountBalanceWallet,
            iconBg = NTColors.PrimaryLight,
            iconFg = NTColors.Primary,
            modifier = Modifier.weight(1f),
        )
        TxSummaryCard(
            label = "Total Profit",
            value = formatTxAmount(totalProfit),
            icon = Icons.Rounded.Savings,
            iconBg = NTColors.SuccessLight,
            iconFg = NTColors.Success,
            modifier = Modifier.weight(1f),
        )
        TxSummaryCard(
            label = "Transactions",
            value = "$count",
            icon = Icons.Rounded.ReceiptLong,
            iconBg = NTColors.InfoLight,
            iconFg = NTColors.Info,
            modifier = Modifier.weight(1f),
        )
    }
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
            TxFilterSection(
                label = "CUSTOMER",
                options = customerOptions,
                selected = selectedCustomer,
                allLabel = "All customers",
                onSelect = onSelectCustomer,
            )
            TxFilterSection(
                label = "PRODUCT",
                options = productOptions,
                selected = selectedProduct,
                allLabel = "All products",
                onSelect = onSelectProduct,
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
            if (shopOptions.isNotEmpty()) {
                TxFilterSection(
                    label = "SHOP",
                    options = shopOptions,
                    selected = selectedShop,
                    allLabel = "All shops",
                    onSelect = onSelectShop,
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

// ── Transaction Details bottom sheet ────────────────────────────────────────

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
    val profitUp = sale.totalMargin >= 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NTColors.Background,
        contentColor = NTColors.TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NTDp.screenPad),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Details",
                        fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp, color = NTColors.TextPrimary,
                    )
                    Text(
                        txRef(sale.id),
                        fontSize = 12.sp, color = NTColors.TextSecondary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NTColors.Surface)
                        .border(1.dp, NTColors.Border, CircleShape)
                        .clickable(onClickLabel = "Close details", onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, null, tint = NTColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            TxDetailCard(title = "CUSTOMER") {
                TxDetailRow("Name", sale.customerName.ifBlank { "Walk-in" })
                if (!customer?.phone.isNullOrBlank()) TxDetailRow("Phone", customer?.phone.orEmpty())
                if (!customer?.address.isNullOrBlank()) TxDetailRow("Address", customer?.address.orEmpty())
                if (!sale.customerName.isBlank() && customer?.id?.isNotBlank() == true) {
                    TxDetailRow("Customer ID", customer?.id?.take(8).orEmpty())
                }
            }
            Spacer(Modifier.height(12.dp))
            TxDetailCard(title = "TRANSACTION") {
                TxDetailRow("Reference", txRef(sale.id))
                TxDetailRow("Date", dbToDisplayDate(sale.date))
                val time = isoToDisplayTime(sale.createdAt)
                if (time.isNotBlank()) TxDetailRow("Time", time)
            }
            Spacer(Modifier.height(12.dp))
            TxDetailCard(title = "PRODUCT") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TxBottleThumb(productName = sale.productName)
                    Spacer(Modifier.width(12.dp))
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
                                "Qty ${sale.qty}",
                                fontSize = 12.sp, color = NTColors.TextSecondary,
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "₹${unitPrice.toLong()}/unit",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = NTColors.TextSecondary,
                        )
                        Text(
                            formatTxAmount(sale.totalSelling),
                            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                            color = NTColors.TextPrimary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            TxDetailCard(title = "FINANCIAL SUMMARY") {
                TxDetailRow("Subtotal", formatTxAmount(sale.totalSelling))
                TxDetailRow("Total cost", formatTxAmount(unitCost * sale.qty))
                TxDetailRow(
                    "Total profit", formatTxAmount(sale.totalMargin),
                    valueColor = if (profitUp) NTColors.Success else NTColors.Error,
                )
            }
            Spacer(Modifier.height(12.dp))
            TxDetailCard(title = "EMPLOYEE & SHOP") {
                TxDetailRow("Sold by", employeeName)
                TxDetailRow("Shop", shopName)
            }
        }
    }
}

@Composable
private fun TxDetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad)
            .clip(RoundedCornerShape(20.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(
            title, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp, color = NTColors.TextTertiary,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun TxDetailRow(label: String, value: String, valueColor: Color = NTColors.TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = NTColors.TextSecondary)
        Spacer(Modifier.width(12.dp))
        Text(
            value.ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
