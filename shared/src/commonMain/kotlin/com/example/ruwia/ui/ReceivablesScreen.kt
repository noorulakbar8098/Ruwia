package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.CustomerLedgerEntry
import com.example.ruwia.domain.Payment
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.components.formKeyboardActions
import com.example.ruwia.ui.dashboard.GlossyTealBox
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import com.example.ruwia.ui.dashboard.NTPrimaryTopBar
import com.example.ruwia.ui.dashboard.NTSectionHeader
import com.example.ruwia.util.isoToDisplayDate
import kotlin.math.roundToInt

private val HeroMint = Color(0xFF5EEAD4)

/** Deep forest teal with the shared top-light sheen used by glossy actions. */
private val GlossyTeal = Color(0xFF0F2E2C)
private val GlossyTealInk: Color get() = NTColors.BrandInk

private fun Modifier.glossyTealSurface(shape: Shape): Modifier = this
    .clip(shape)
    .background(GlossyTeal)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.03f),
                Color.Transparent,
            ),
        ),
    )
    .border(1.dp, Color.White.copy(alpha = 0.16f), shape)

private enum class ReceivablesListTab { OUTSTANDING, ALL }

private enum class PaymentMode(
    val key: String,
    val label: String,
    val icon: ImageVector,
) {
    CASH("cash", "Cash", Icons.Rounded.Payments),
    UPI("upi", "UPI", Icons.Rounded.QrCodeScanner),
    CARD("card", "Card", Icons.Rounded.CreditCard),
    BANK("bank", "Bank", Icons.Rounded.AccountBalance),
}

@Composable
fun ReceivablesScreen(
    state: AdminState,
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onCloseLedger: () -> Unit,
    onLoadPayments: () -> Unit,
    onRecordPayment: (customerId: String, amount: String, mode: String, note: String) -> Unit,
    onClearPaymentMessage: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var paymentCustomer by remember { mutableStateOf<Customer?>(null) }
    val ledgerCustomer = remember(state.customers, state.ledgerCustomerId, state.ledger) {
        state.customers.firstOrNull { it.id == state.ledgerCustomerId }
            ?: state.ledger?.entries?.firstOrNull()?.let { entry ->
                state.customers.firstOrNull { it.id == entry.customerId }
            }
    }

    LaunchedEffect(Unit) { onLoadPayments() }

    LaunchedEffect(state.paymentMessage) {
        state.paymentMessage?.let {
            snackbarHostState.showSnackbar(it)
            paymentCustomer = null
            onClearPaymentMessage()
        }
    }

    SystemBackHandler {
        if (state.ledgerCustomerId != null) onCloseLedger() else onBack()
    }

    Scaffold(
        containerColor = NTColors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.ledgerCustomerId != null) {
                CustomerLedgerContent(
                    state = state,
                    customer = ledgerCustomer,
                    onBack = onCloseLedger,
                    onCollect = { paymentCustomer = it },
                )
            } else {
                ReceivablesContent(
                    state = state,
                    onBack = onBack,
                    onOpenCustomer = onOpenCustomer,
                    onCollect = { paymentCustomer = it },
                )
            }
        }
    }

    paymentCustomer?.let { customer ->
        RecordPaymentSheet(
            customer = customer,
            isSaving = state.paymentSaving,
            onDismiss = { if (!state.paymentSaving) paymentCustomer = null },
            onConfirm = { amount, mode, note ->
                val id = customer.id ?: return@RecordPaymentSheet
                onRecordPayment(id, amount, mode, note)
            },
        )
    }
}

@Composable
private fun ReceivablesContent(
    state: AdminState,
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onCollect: (Customer) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(ReceivablesListTab.OUTSTANDING) }

    val totals = remember(state.customers, state.payments) {
        computeReceivableTotals(state.customers, state.payments)
    }
    val outstanding = remember(state.customers) {
        state.customers
            .filter { it.balance > 0.001 }
            .sortedByDescending { it.balance }
    }
    val allCustomers = remember(state.customers) {
        state.customers.sortedWith(
            compareByDescending<Customer> { it.balance > 0.001 }
                .thenByDescending { it.balance }
                .thenBy { it.name.lowercase() },
        )
    }
    val recentPayments = remember(state.payments) {
        state.payments
            .filter { !it.referenceKey.isNullOrBlank() }
            .sortedByDescending { it.createdAt.orEmpty() }
            .take(6)
    }
    val customerNames = remember(state.customers) {
        state.customers.mapNotNull { customer ->
            customer.id?.let { it to customer.name }
        }.toMap()
    }

    val source = if (selectedTab == ReceivablesListTab.OUTSTANDING) outstanding else allCustomers
    val visible = remember(source, query) {
        if (query.isBlank()) {
            source
        } else {
            source.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.phone.orEmpty().contains(query, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        NTPrimaryTopBar(
            title = "Payments & Dues",
            subtitle = "Collections and customer balances",
            onBack = onBack,
            trailingText = if (outstanding.isEmpty()) "All clear" else "${outstanding.size} due",
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = NTDp.screenPad,
                end = NTDp.screenPad,
                top = NTDp.md,
                bottom = NTDp.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MoneyHeroCard(
                    outstanding = totals.outstanding,
                    collected = totals.collected,
                    billed = totals.billed,
                    label = "TOTAL OUTSTANDING",
                    caption = when {
                        outstanding.isEmpty() -> "Every customer is fully settled"
                        outstanding.size == 1 -> "Across 1 customer account"
                        else -> "Across ${outstanding.size} customer accounts"
                    },
                )
            }

            item {
                ReceivablesSegmentedTabs(
                    selected = selectedTab,
                    outstandingCount = outstanding.size,
                    allCount = allCustomers.size,
                    onSelect = { selectedTab = it },
                )
            }

            item {
                ReceivablesSearchField(
                    query = query,
                    onQueryChange = { query = it },
                )
            }

            item {
                NTSectionHeader(
                    label = "RECEIVABLES",
                    title = if (selectedTab == ReceivablesListTab.OUTSTANDING) {
                        "Customers with dues"
                    } else {
                        "All customers"
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (visible.isEmpty()) {
                item {
                    when {
                        query.isNotBlank() -> ReceivablesEmptyState(
                            icon = Icons.Rounded.SearchOff,
                            title = "No customers match",
                            subtitle = "Try a different name or phone number.",
                            actionLabel = "Clear search",
                            onAction = { query = "" },
                        )

                        selectedTab == ReceivablesListTab.OUTSTANDING -> ReceivablesEmptyState(
                            icon = Icons.Rounded.TaskAlt,
                            title = "All settled up",
                            subtitle = "No outstanding dues right now. Every account is clear.",
                            actionLabel = "View all customers",
                            onAction = { selectedTab = ReceivablesListTab.ALL },
                        )

                        else -> ReceivablesEmptyState(
                            icon = Icons.Rounded.PeopleOutline,
                            title = "No customers yet",
                            subtitle = "Customers added from Settings will appear here.",
                        )
                    }
                }
            } else {
                items(items = visible, key = { it.id ?: it.name }) { customer ->
                    CustomerDueCard(
                        customer = customer,
                        onOpen = { customer.id?.let(onOpenCustomer) },
                        onCollect = { onCollect(customer) },
                    )
                }
            }

            if (recentPayments.isNotEmpty()) {
                item {
                    NTSectionHeader(
                        label = "ACTIVITY",
                        title = "Recent payments",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(
                    items = recentPayments,
                    key = { it.id ?: it.hashCode().toString() },
                ) { payment ->
                    PaymentFeedRow(
                        customerName = payment.customerId?.let { customerNames[it] } ?: "Customer",
                        payment = payment,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerLedgerContent(
    state: AdminState,
    customer: Customer?,
    onBack: () -> Unit,
    onCollect: (Customer) -> Unit,
) {
    val ledger = state.ledger
    val name = customer?.name
        ?: ledger?.entries?.firstOrNull()?.customerName.orEmpty().ifBlank { "Customer" }
    val billed = ledger?.billed ?: customer?.balance ?: 0.0
    val collected = ledger?.collected ?: 0.0
    val outstanding = ledger?.outstanding ?: customer?.balance ?: 0.0
    val entries = remember(ledger?.entries) { ledger?.entries?.asReversed().orEmpty() }
    val canCollect = customer != null && outstanding > 0.001 && !state.paymentSaving

    Column(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        NTPrimaryTopBar(
            title = name,
            subtitle = "Customer statement",
            onBack = onBack,
            trailingText = if (outstanding > 0.001) "Due" else "Paid",
        )

        if (state.ledgerLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = NTColors.Primary)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = NTDp.screenPad,
                end = NTDp.screenPad,
                top = NTDp.md,
                bottom = NTDp.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MoneyHeroCard(
                    outstanding = outstanding,
                    collected = collected,
                    billed = billed,
                    label = "OUTSTANDING BALANCE",
                    caption = customer?.phone?.takeIf { it.isNotBlank() }
                        ?: "Sales and payments in one place",
                )
            }

            item {
                GlossyTealBox(
                    shape = RoundedCornerShape(16.dp),
                    enabled = canCollect,
                    onClick = { customer?.let(onCollect) },
                    onClickLabel = "Record payment",
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Payments,
                            contentDescription = null,
                            tint = if (canCollect) HeroMint else NTColors.TextTertiary,
                            modifier = Modifier.size(19.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (outstanding > 0.001) "Record payment" else "Account settled",
                            color = if (canCollect) Color.White else NTColors.TextTertiary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            item {
                NTSectionHeader(
                    label = "STATEMENT",
                    title = "Money statement",
                    trailingText = if (entries.isEmpty()) "" else "${entries.size} entries",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (entries.isEmpty()) {
                item {
                    ReceivablesEmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "No entries yet",
                        subtitle = "Sales and payments for this customer will appear here.",
                    )
                }
            } else {
                items(
                    items = entries,
                    key = { it.id ?: it.referenceKey },
                ) { entry ->
                    LedgerEntryRow(entry)
                }
            }
        }
    }
}

// ── Summary hero ──────────────────────────────────────────────

@Composable
private fun MoneyHeroCard(
    outstanding: Double,
    collected: Double,
    billed: Double,
    label: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val progress = collectionProgress(collected, billed)
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, clip = false)
            .glossyTealSurface(shape)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    formatReceivableAmount(outstanding),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    caption,
                    color = Color.White.copy(alpha = 0.66f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    tint = HeroMint,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Collection rate",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${(progress * 100).roundToInt()}%",
                color = HeroMint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(HeroMint),
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HeroMetric(
                label = "Collected",
                value = formatReceivableAmount(collected),
                valueColor = HeroMint,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(Color.White.copy(alpha = 0.14f)),
            )
            Spacer(Modifier.width(16.dp))
            HeroMetric(
                label = "Billed",
                value = formatReceivableAmount(billed),
                valueColor = Color.White,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            color = valueColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── List controls ─────────────────────────────────────────────

@Composable
private fun ReceivablesSegmentedTabs(
    selected: ReceivablesListTab,
    outstandingCount: Int,
    allCount: Int,
    onSelect: (ReceivablesListTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NTColors.SurfaceVar)
            .border(1.dp, NTColors.Border, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SegmentTab(
            label = "Outstanding",
            count = outstandingCount,
            selected = selected == ReceivablesListTab.OUTSTANDING,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(ReceivablesListTab.OUTSTANDING) },
        )
        SegmentTab(
            label = "All customers",
            count = allCount,
            selected = selected == ReceivablesListTab.ALL,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(ReceivablesListTab.ALL) },
        )
    }
}

@Composable
private fun SegmentTab(
    label: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) NTColors.Surface else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) NTColors.PrimaryLight else Color.Transparent,
                shape = shape,
            )
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (selected) NTColors.TextPrimary else NTColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) NTColors.PrimaryLight else NTColors.Surface)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                "$count",
                color = if (selected) NTColors.PrimaryDark else NTColors.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ReceivablesSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
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
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = NTColors.TextTertiary,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search by name or phone",
                    fontSize = 13.sp,
                    color = NTColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(fontSize = 13.sp, color = NTColors.TextPrimary),
                cursorBrush = SolidColor(NTColors.Primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = formKeyboardActions(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(onClickLabel = "Clear search", onClick = { onQueryChange("") }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = NTColors.TextSecondary,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

// ── Customer rows ─────────────────────────────────────────────

@Composable
private fun CustomerDueCard(
    customer: Customer,
    onOpen: () -> Unit,
    onCollect: () -> Unit,
) {
    val due = customer.balance > 0.001
    val accent = avatarAccent(customer.name)
    val shape = RoundedCornerShape(18.dp)
    val meta = listOfNotNull(
        customer.phone?.takeIf { it.isNotBlank() }
            ?: customer.address?.takeIf { it.isNotBlank() }?.let { it.take(28) },
        customer.cansHeld.takeIf { it > 0 }?.let { "$it cans" },
    ).joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .receivableCard(shape)
            .clickable(onClickLabel = "Open ${customer.name}'s statement", onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    customer.name.trim().take(1).uppercase().ifBlank { "?" },
                    color = accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    customer.name,
                    color = NTColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    meta.ifBlank { "No contact details" },
                    color = NTColors.TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatReceivableAmount(customer.balance),
                    color = if (due) NTColors.Error else NTColors.Success,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (due) "Outstanding" else "Settled",
                    color = NTColors.TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.7f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(NTDp.radSm))
                    .clickable(
                        onClickLabel = "View ${customer.name}'s statement",
                        onClick = onOpen,
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = NTColors.Primary,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "View statement",
                    color = NTColors.Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (due) {
                GlossyTealBox(
                    shape = RoundedCornerShape(50),
                    onClick = onCollect,
                    onClickLabel = "Collect payment from ${customer.name}",
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Payments,
                            contentDescription = null,
                            tint = HeroMint,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Collect",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(NTColors.SuccessLight)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = NTColors.SuccessText,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Paid",
                        color = NTColors.SuccessText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun avatarAccent(name: String): Color {
    val palette = listOf(
        NTColors.AvatarTeal,
        NTColors.AvatarGold,
        NTColors.AvatarPurple,
        NTColors.AvatarBlue,
    )
    return palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
}

// ── Payment feed ──────────────────────────────────────────────

@Composable
private fun PaymentFeedRow(customerName: String, payment: Payment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .receivableCard(RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NTColors.SuccessLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Payments,
                contentDescription = null,
                tint = NTColors.SuccessText,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                customerName,
                color = NTColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(
                    payment.createdAt?.let { isoToDisplayDate(it) },
                    payment.mode.replaceFirstChar { it.uppercaseChar() },
                    payment.note?.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                color = NTColors.TextTertiary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "+${formatReceivableAmount(payment.amount)}",
                color = NTColors.Success,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Received",
                color = NTColors.TextTertiary,
                fontSize = 9.sp,
            )
        }
    }
}

// ── Ledger rows ───────────────────────────────────────────────

@Composable
private fun LedgerEntryRow(entry: CustomerLedgerEntry) {
    val isPayment = entry.credit > 0.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .receivableCard(RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isPayment) NTColors.SuccessLight else NTColors.PrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPayment) Icons.Rounded.Payments else Icons.Rounded.ReceiptLong,
                contentDescription = null,
                tint = if (isPayment) NTColors.SuccessText else NTColors.PrimaryDark,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isPayment) "Payment received" else "Sale billed",
                color = NTColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(
                    entry.createdAt?.let { isoToDisplayDate(it) },
                    entry.mode?.replaceFirstChar { it.uppercaseChar() },
                    entry.note?.takeIf { it.isNotBlank() },
                ).joinToString(" · ").ifBlank { entry.referenceKey },
                color = NTColors.TextTertiary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (isPayment) "+" else "-") +
                    formatReceivableAmount(if (isPayment) entry.credit else entry.debit),
                color = if (isPayment) NTColors.Success else NTColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Bal ${formatReceivableAmount(entry.balanceAfter)}",
                color = NTColors.TextTertiary,
                fontSize = 10.sp,
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────

@Composable
private fun ReceivablesEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(NTColors.PrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = NTColors.PrimaryDark,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            color = NTColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            color = NTColors.TextTertiary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(NTColors.PrimaryLight)
                    .clickable(onClickLabel = actionLabel, onClick = onAction)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(
                    actionLabel,
                    color = NTColors.PrimaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ── Record payment sheet ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordPaymentSheet(
    customer: Customer,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: String, mode: String, note: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amount by remember(customer.id) { mutableStateOf("") }
    var mode by remember(customer.id) { mutableStateOf(PaymentMode.CASH) }
    var note by remember(customer.id) { mutableStateOf("") }

    val parsed = parsePaymentAmount(amount)
    val valid = isValidPaymentAmount(parsed, customer.balance)
    val showError = amount.isNotBlank() && !valid

    fun savePayment() {
        if (!isSaving && valid) onConfirm(amount, mode.key, note)
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = NTColors.Surface,
        contentColor = NTColors.TextPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NTDp.screenPad)
                .padding(bottom = NTDp.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .glossyTealSurface(RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Payments,
                        contentDescription = null,
                        tint = HeroMint,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Record payment",
                        color = NTColors.TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                    )
                    Text(
                        customer.name,
                        color = NTColors.TextTertiary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NTColors.SurfaceVar)
                        .border(1.dp, NTColors.Border, CircleShape)
                        .clickable(enabled = !isSaving, onClickLabel = "Close", onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = NTColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossyTealSurface(RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Outstanding balance",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        formatReceivableAmount(customer.balance),
                        color = HeroMint,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                if (customer.cansHeld > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Cans held",
                            color = Color.White.copy(alpha = 0.62f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${customer.cansHeld}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Amount",
                    color = NTColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .glossyTealSurface(RoundedCornerShape(50))
                        .clickable(onClickLabel = "Pay full balance") {
                            amount = paymentAmountInput(customer.balance)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        "Pay full",
                        color = HeroMint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NTColors.SurfaceVar)
                    .border(
                        width = 1.dp,
                        color = when {
                            showError -> NTColors.Error
                            amount.isNotBlank() -> GlossyTealInk
                            else -> NTColors.Border
                        },
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "₹",
                    color = NTColors.TextSecondary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (amount.isEmpty()) {
                        Text(
                            "0",
                            color = NTColors.TextTertiary,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    BasicTextField(
                        value = amount,
                        onValueChange = { amount = sanitizePaymentAmountInput(it) },
                        textStyle = TextStyle(
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NTColors.TextPrimary,
                        ),
                        cursorBrush = SolidColor(GlossyTealInk),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = formKeyboardActions(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Max ${formatReceivableAmount(customer.balance)}",
                    color = NTColors.TextTertiary,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
            if (showError) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (parsed > customer.balance) {
                        "Amount exceeds the outstanding balance."
                    } else {
                        "Enter an amount greater than zero."
                    },
                    color = NTColors.Error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "Payment mode",
                color = NTColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PaymentMode.entries.forEach { option ->
                    PaymentModeChip(
                        option = option,
                        selected = mode == option,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = option },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = {
                    Text("Add a note (optional)", fontSize = 13.sp, color = NTColors.TextTertiary)
                },
                textStyle = TextStyle(fontSize = 13.sp, color = NTColors.TextPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = formKeyboardActions(onDone = ::savePayment),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GlossyTealInk,
                    unfocusedBorderColor = NTColors.Border,
                    focusedContainerColor = NTColors.SurfaceVar,
                    unfocusedContainerColor = NTColors.SurfaceVar,
                ),
            )

            Spacer(Modifier.height(20.dp))

            GlossyTealBox(
                shape = RoundedCornerShape(16.dp),
                enabled = valid,
                onClick = ::savePayment,
                onClickLabel = "Record payment",
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = HeroMint,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Payments,
                            contentDescription = null,
                            tint = if (valid) HeroMint else NTColors.TextTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (valid) {
                                "Record ${formatReceivableAmount(parsed)}"
                            } else {
                                "Record payment"
                            },
                            color = if (valid) Color.White else NTColors.TextTertiary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentModeChip(
    option: PaymentMode,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val background = if (selected) {
        Modifier.glossyTealSurface(shape)
    } else {
        Modifier
            .clip(shape)
            .background(NTColors.SurfaceVar)
            .border(1.dp, NTColors.Border, shape)
    }
    Row(
        modifier = modifier
            .then(background)
            .clickable(onClickLabel = option.label, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            option.icon,
            contentDescription = null,
            tint = if (selected) HeroMint else NTColors.TextSecondary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            option.label,
            color = if (selected) Color.White else NTColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

// ── Shared card surface ───────────────────────────────────────

@Composable
private fun Modifier.receivableCard(shape: Shape): Modifier = this
    .shadow(NTDp.cardElevation, shape, clip = false)
    .clip(shape)
    .background(NTColors.Surface)
    .border(1.dp, NTColors.Border.copy(alpha = 0.8f), shape)
