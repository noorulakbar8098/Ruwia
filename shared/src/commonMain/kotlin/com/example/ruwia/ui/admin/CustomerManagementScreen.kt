package com.example.ruwia.ui.admin

import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.CustomerProductPrice
import com.example.ruwia.domain.SaleEntry
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.isEmptyCansSource
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import com.example.ruwia.ui.dashboard.GlossyTealBox
import com.example.ruwia.util.capitalizeWords
import com.example.ruwia.util.dbToDisplayDate
import com.example.ruwia.util.isTextField

/**
 * Admin-side customer directory. Lists every customer in the system and lets
 * the admin add new ones inline. RLS already exposes `customers` to all
 * authenticated users so anything saved here is immediately visible to every
 * employee in their Add Sale picker — fulfilling the cross-app sync expectation.
 */
@Composable
fun CustomerManagementScreen(
    customers: List<Customer>,
    errorMessage: String? = null,
    onAddCustomer: (Customer) -> Unit,
    onUpdateCustomer: (Customer) -> Unit = {},
    onDeleteCustomer: (String) -> Unit = {},
    onClearError: () -> Unit = {},
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    saleEntries: List<SaleEntry> = emptyList(),
    movements: List<StockMovement> = emptyList(),
    customerPrices: Map<String, List<CustomerProductPrice>> = emptyMap(),
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var deletingCustomer by remember { mutableStateOf<Customer?>(null) }
    var openedCustomer by remember { mutableStateOf<Customer?>(null) }
    var search by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(CustomerSort.NAME) }

    // Live per-customer stats derived from sales + movements (single source).
    val statsByKey = remember(customers, saleEntries, movements, customerPrices) {
        customers.associate { c ->
            customerKey(c) to deriveCustomerStats(c, saleEntries, movements, customerPrices)
        }
    }
    fun statsOf(c: Customer): CustomerStats =
        statsByKey[customerKey(c)] ?: CustomerStats()

    openedCustomer?.let { opened ->
        SystemBackHandler { openedCustomer = null }
        CustomerDetailsScreen(
            customer = opened,
            stats = statsOf(opened),
            sales = customerSales(opened, saleEntries),
            hasCustomPricing = statsOf(opened).hasCustomPricing,
            onBack = { openedCustomer = null },
            onEdit = { editingCustomer = opened; openedCustomer = null },
        )
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var errorDialogText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            errorDialogText = it
            snackbarHostState.showSnackbar(it)
        }
    }

    if (errorDialogText != null) {
        AlertDialog(
            onDismissRequest = { errorDialogText = null; onClearError() },
            title = { Text("Database Error", fontWeight = FontWeight.Bold, color = NTColors.TextPrimary) },
            text = { Text(errorDialogText ?: "") },
            confirmButton = {
                TextButton(onClick = { errorDialogText = null; onClearError() }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = NTColors.Primary)
                }
            },
            shape = RoundedCornerShape(20.dp),
        )
    }

    val filtered = remember(search, customers, sortMode, statsByKey) {
        val base = if (search.isBlank()) customers
        else customers.filter {
            it.name.contains(search, ignoreCase = true) ||
            (it.phone   ?: "").contains(search) ||
            (it.address ?: "").contains(search, ignoreCase = true)
        }
        when (sortMode) {
            CustomerSort.NAME -> base.sortedBy { it.name.lowercase() }
            CustomerSort.SPENDER -> base.sortedByDescending { statsByKey[customerKey(it)]?.spent ?: 0.0 }
            CustomerSort.DUES -> base.sortedByDescending { it.balance }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        Scaffold(
            containerColor = NTColors.Background,
            topBar = { CustomerTopBar(onBack = onBack, count = customers.size) },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NTDp.screenPad, vertical = NTDp.sm)
                        .background(NTColors.SurfaceVar, RoundedCornerShape(12.dp))
                        .border(1.dp, NTColors.Border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Search, null,
                        tint = NTColors.TextTertiary, modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (search.isEmpty()) {
                            Text(
                                "Search by name, phone or address",
                                fontSize = 13.sp, color = NTColors.TextTertiary,
                            )
                        }
                        BasicTextField(
                            value         = search,
                            onValueChange = { search = it },
                            textStyle     = TextStyle(fontSize = 14.sp, color = NTColors.TextPrimary),
                            cursorBrush   = SolidColor(NTColors.Primary),
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                        )
                    }
                    if (search.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close, null,
                            tint = NTColors.TextTertiary,
                            modifier = Modifier.size(16.dp).clickable { search = "" },
                        )
                    }
                }

                // Sort: Name · Top spender · Most dues
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NTDp.screenPad)
                        .padding(top = NTDp.sm),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CustomerSortChip(
                        text = "Name",
                        selected = sortMode == CustomerSort.NAME,
                        onClick = { sortMode = CustomerSort.NAME },
                    )
                    CustomerSortChip(
                        text = "Top spender",
                        selected = sortMode == CustomerSort.SPENDER,
                        onClick = { sortMode = CustomerSort.SPENDER },
                    )
                    CustomerSortChip(
                        text = "Most dues",
                        selected = sortMode == CustomerSort.DUES,
                        onClick = { sortMode = CustomerSort.DUES },
                    )
                }

                if (customers.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(NTColors.PrimaryLight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Group, null, tint = NTColors.Primary, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(NTDp.lg))
                        Text("No customers yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
                        Spacer(Modifier.height(NTDp.sm))
                        Text(
                            "Tap the + button to add your first customer. They'll be visible to every employee instantly.",
                            fontSize = 13.sp, color = NTColors.TextTertiary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = NTDp.screenPad,
                            vertical   = NTDp.sm,
                        ),
                        verticalArrangement = Arrangement.spacedBy(NTDp.sm),
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NTColors.PrimaryLight, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Info, null, tint = NTColors.Primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Customers added here are visible to all employees in the Add Sale picker.",
                                    fontSize = 12.sp, color = NTColors.Primary, lineHeight = 16.sp,
                                )
                            }
                        }
                        items(filtered, key = { it.id ?: it.name.hashCode().toString() }) { c ->
                            CustomerRow(
                                customer = c,
                                stats    = statsOf(c),
                                onOpen   = { openedCustomer = c },
                                onEdit   = { editingCustomer = c },
                                onDelete = { deletingCustomer = c },
                            )
                        }
                        item {
                            Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 80.dp))
                        }
                    }
                }
            }
        }

        GlossyTealBox(
            shape = CircleShape,
            onClick = { showAddSheet = true },
            onClickLabel = "Add customer",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp)
                .shadow(8.dp, CircleShape)
                .size(56.dp),
        ) {
            Icon(Icons.Rounded.Add, "Add customer", tint = Color.White, modifier = Modifier.size(26.dp))
        }

        if (showAddSheet) {
            CustomerFormDialog(
                onDismiss = { showAddSheet = false },
                onSave    = { name, phone, address, other ->
                    onAddCustomer(
                        Customer(
                            id           = null,
                            name         = name,
                            phone        = phone,
                            address      = address,
                            otherDetails = other,
                        )
                    )
                    showAddSheet = false
                },
            )
        }

        editingCustomer?.let { c ->
            CustomerFormDialog(
                title   = "Edit customer",
                initial = c,
                onDismiss = { editingCustomer = null },
                onSave    = { name, phone, address, other ->
                    onUpdateCustomer(
                        c.copy(
                            name         = name,
                            phone        = phone,
                            address      = address,
                            otherDetails = other,
                        )
                    )
                    editingCustomer = null
                },
            )
        }

        deletingCustomer?.let { c ->
            AlertDialog(
                onDismissRequest = { deletingCustomer = null },
                icon  = { Icon(Icons.Rounded.Delete, null, tint = NTColors.Error) },
                title = { Text("Delete ${c.name}?", fontWeight = FontWeight.Bold, color = NTColors.TextPrimary) },
                text  = {
                    Text(
                        "This will permanently delete this customer. This action cannot be undone.",
                        fontSize = 13.sp, color = NTColors.TextSecondary,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            c.id?.let(onDeleteCustomer)
                            deletingCustomer = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NTColors.Error, contentColor = Color.White),
                    ) { Text("Delete", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { deletingCustomer = null },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NTColors.TextSecondary),
                        border = BorderStroke(1.dp, NTColors.Border)
                    ) { Text("Cancel") }
                },
                containerColor = NTColors.Surface,
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
}

// ── Per-customer derivation (single source of truth) ─────────────────────────
//  Sales match by trimmed, case-insensitive name. Movements attribute via
//  their "Prefix · <name>" source convention ("Sale", "Empty cans",
//  "Empty cases"); shop transfers never match a customer name pattern.

private enum class CustomerSort { NAME, SPENDER, DUES }

private data class CustomerStats(
    val cansBought: Int = 0,
    val spent: Double = 0.0,
    val firstDate: String = "",
    val lastDate: String = "",
    val activeMonths: Int = 0,
    val favoriteProduct: String = "",
    val delivered: Int = 0,
    val returned: Int = 0,
    val withCustomer: Int = 0,
    val hasCustomPricing: Boolean = false,
)

private fun customerKey(c: Customer): String =
    c.id?.takeIf { it.isNotBlank() } ?: "name:${c.name.trim().lowercase()}"

private fun customerSales(customer: Customer, sales: List<SaleEntry>): List<SaleEntry> {
    val target = customer.name.trim()
    if (target.isEmpty()) return emptyList()
    return sales.filter { it.customerName.trim().equals(target, ignoreCase = true) }
}

/** Extracts the customer name from "Sale · X" / "Empty cans · X" sources. */
private fun movementCustomer(source: String): String? {
    val s = source.trim()
    for (prefix in listOf("Sale", "Empty cans", "Empty cases")) {
        if (s.startsWith(prefix, ignoreCase = true) && s.contains("·")) {
            return s.substringAfter("·").trim().takeIf { it.isNotEmpty() }
        }
    }
    return null
}

private fun deriveCustomerStats(
    customer: Customer,
    sales: List<SaleEntry>,
    movements: List<StockMovement>,
    customerPrices: Map<String, List<CustomerProductPrice>>,
): CustomerStats {
    val mine = customerSales(customer, sales)
    val cansBought = mine.sumOf { it.qty }
    val spent = mine.sumOf { it.totalSelling }
    val dates = mine.map { it.date }.filter { it.isNotBlank() }.sorted()
    val favorite = mine.groupBy { it.productName.trim() }
        .maxByOrNull { (_, rows) -> rows.sumOf { it.qty } }
        ?.key.orEmpty()

    var delivered = 0
    var returned = 0
    val target = customer.name.trim()
    if (target.isNotEmpty()) {
        for (m in movements) {
            val who = movementCustomer(m.source) ?: continue
            if (!who.equals(target, ignoreCase = true)) continue
            if (m.type == "outward" && !m.source.isEmptyCansSource()) delivered += m.qty
            else if (m.type == "inward" && m.source.isEmptyCansSource()) returned += m.qty
        }
    }

    val hasCustom = customer.id != null && customerPrices.values.flatten()
        .any { it.customerId == customer.id && it.isActive }

    return CustomerStats(
        cansBought = cansBought,
        spent = spent,
        firstDate = dates.firstOrNull().orEmpty(),
        lastDate = dates.lastOrNull().orEmpty(),
        activeMonths = dates.map { it.take(7) }.distinct().size,
        favoriteProduct = favorite,
        delivered = delivered,
        returned = returned,
        withCustomer = (delivered - returned).coerceAtLeast(0),
        hasCustomPricing = hasCustom,
    )
}

private fun formatCustomerMoney(v: Double): String {
    val neg = v < 0
    val digits = kotlin.math.abs(v).toLong().toString()
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

@Composable
private fun CustomerTopBar(onBack: () -> Unit, count: Int) {
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
                modifier = Modifier.size(36.dp)
                    .background(NTColors.Surface, RoundedCornerShape(10.dp))
                    .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.ArrowBack, "Back", tint = NTColors.TextPrimary, modifier = Modifier.size(18.dp)) }

            Text(
                "Customers", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary, modifier = Modifier.align(Alignment.Center),
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(NTDp.radFull))
                    .background(NTColors.PrimaryLight)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .align(Alignment.CenterEnd),
            ) {
                Text("$count", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NTColors.Primary)
            }
        }
    }
}

@Composable
private fun CustomerSortChip(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        GlossyTealBox(
            shape = RoundedCornerShape(NTDp.radFull),
            onClick = onClick,
            onClickLabel = text,
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(0.dp),
        ) {
            Text(
                text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        return
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(NTDp.radFull))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radFull))
            .clickable(onClickLabel = text, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = NTColors.TextSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun CustomerRow(
    customer: Customer,
    stats: CustomerStats,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Surface, RoundedCornerShape(NTDp.radLg))
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radLg))
            .clickable(onClickLabel = "Open ${customer.name}", onClick = onOpen)
            .padding(NTDp.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(NTDp.radMd))
                .background(NTColors.PrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                customer.name.firstOrNull()?.uppercase() ?: "?",
                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = NTColors.Primary,
            )
        }
        Spacer(Modifier.width(NTDp.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                customer.name,
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = NTColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${stats.cansBought} cans · ${formatCustomerMoney(stats.spent)} spent",
                fontSize = 12.sp, color = NTColors.TextSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (customer.balance > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(NTDp.radFull))
                        .background(NTColors.ErrorLight)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "${formatCustomerMoney(customer.balance)} DUE",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = NTColors.Error,
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NTColors.SurfaceVar)
                        .clickable(onClickLabel = "Edit", onClick = onEdit),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Edit, "Edit", tint = NTColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NTColors.ErrorLight)
                        .clickable(onClickLabel = "Delete", onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Delete, "Delete", tint = NTColors.Error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomerFormDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String?, address: String?, other: String?) -> Unit,
    title: String = "Add customer",
    initial: Customer? = null,
) {
    var name    by remember { mutableStateOf(initial?.name ?: "") }
    var phone   by remember { mutableStateOf(initial?.phone ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var other   by remember { mutableStateOf(initial?.otherDetails ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NTColors.Surface, RoundedCornerShape(20.dp))
                .padding(24.dp),
        ) {
            Text(
                title,
                fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Saved customers are visible in the admin and employee apps.",
                fontSize = 12.sp, color = NTColors.TextTertiary,
            )

            Spacer(Modifier.height(NTDp.md))
            CustomerFormFieldLabel("Customer name *")
            CustomerFormInput(
                value = name, onValueChange = { name = it },
                placeholder = "e.g. Murugan Stores",
                keyboardType = KeyboardType.Text,
            )

            Spacer(Modifier.height(NTDp.md))
            CustomerFormFieldLabel("Phone")
            CustomerFormInput(
                value = phone,
                onValueChange = {
                    if (it.length <= 15 && it.all { c -> c.isDigit() || c == '+' }) phone = it
                },
                placeholder = "10-digit mobile",
                keyboardType = KeyboardType.Phone,
            )

            Spacer(Modifier.height(NTDp.md))
            CustomerFormFieldLabel("Address")
            CustomerFormInput(
                value = address, onValueChange = { address = it },
                placeholder = "Area / street / landmark",
                keyboardType = KeyboardType.Text,
            )

            Spacer(Modifier.height(NTDp.md))
            CustomerFormFieldLabel("Notes")
            CustomerFormInput(
                value = other, onValueChange = { other = it },
                placeholder = "Delivery preferences, billing, etc.",
                keyboardType = KeyboardType.Text,
            )

            Spacer(Modifier.height(NTDp.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(NTDp.sm)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            name.trim(),
                            phone.trim().ifEmpty { null },
                            address.trim().ifEmpty { null },
                            other.trim().ifEmpty { null },
                        )
                    },
                    enabled  = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CustomerFormFieldLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        color = NTColors.TextSecondary,
        letterSpacing = 0.4.sp,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun CustomerFormInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 14.sp, color = NTColors.TextTertiary)
        }
        BasicTextField(
            value = value,
            onValueChange = if (keyboardType.isTextField()) { { v -> onValueChange(capitalizeWords(v)) } } else onValueChange,
            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NTColors.TextPrimary),
            cursorBrush = SolidColor(NTColors.Primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = if (keyboardType.isTextField()) KeyboardCapitalization.Words else KeyboardCapitalization.None,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


// ── Customer Details (read-only overview) ────────────────────────────────────
//  Header · 4 stat tiles · behaviour strip · per-product breakdown · history.
//  Dues change only via recorded payments, never by direct edit.

@Composable
private fun CustomerDetailsScreen(
    customer: Customer,
    stats: CustomerStats,
    sales: List<SaleEntry>,
    hasCustomPricing: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val ordered = remember(sales) { sales.sortedByDescending { it.date } }
    val byProduct = remember(sales) {
        sales.groupBy { it.productName.trim().ifBlank { "Product" } }
            .map { (name, rows) -> Triple(name, rows.sumOf { it.qty }, rows.sumOf { it.totalSelling }) }
            .sortedByDescending { it.third }
    }

    Column(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = NTDp.screenPad, vertical = NTDp.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NTColors.Surface)
                    .border(1.dp, NTColors.Border, RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Back", onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ArrowBack, null, tint = NTColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Customer Details",
                fontSize = 19.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp, color = NTColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NTColors.Surface)
                    .border(1.dp, NTColors.Border, RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Edit customer", onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Edit, null, tint = NTColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
        }

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // Identity card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NTDp.screenPad)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NTColors.Surface)
                        .border(1.dp, NTColors.Border, RoundedCornerShape(20.dp))
                        .padding(NTDp.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(NTColors.PrimaryLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            customer.name.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = NTColors.Primary,
                        )
                    }
                    Spacer(Modifier.width(NTDp.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            customer.name,
                            fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            color = NTColors.TextPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        if (customer.phone?.isNotBlank() == true) {
                            Text(
                                customer.phone!!,
                                fontSize = 13.sp, color = NTColors.Primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable(onClickLabel = "Call ${customer.name}") {
                                    runCatching { uriHandler.openUri("tel:${customer.phone}") }
                                },
                            )
                        }
                        if (customer.address?.isNotBlank() == true) {
                            Text(
                                customer.address!!,
                                fontSize = 12.sp, color = NTColors.TextSecondary,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (hasCustomPricing) {
                                DetailBadge("Custom pricing", NTColors.Primary, NTColors.PrimaryLight)
                            }
                            if (customer.balance > 0) {
                                DetailBadge(
                                    "${formatCustomerMoney(customer.balance)} DUE",
                                    NTColors.Error, NTColors.ErrorLight,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(NTDp.md))
            }

            // 4 stat tiles
            item {
                Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailStatTile(
                            label = "Cans bought",
                            value = "${stats.cansBought}",
                            icon = Icons.Rounded.Inventory2,
                            iconBg = NTColors.PrimaryLight,
                            iconFg = NTColors.Primary,
                            modifier = Modifier.weight(1f),
                        )
                        DetailStatTile(
                            label = "Total spent",
                            value = formatCustomerMoney(stats.spent),
                            icon = Icons.Rounded.AccountBalanceWallet,
                            iconBg = NTColors.SuccessLight,
                            iconFg = NTColors.Success,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailStatTile(
                            label = "Dues pending",
                            value = formatCustomerMoney(customer.balance),
                            icon = Icons.Rounded.ReceiptLong,
                            iconBg = NTColors.ErrorLight,
                            iconFg = NTColors.Error,
                            modifier = Modifier.weight(1f),
                        )
                        DetailStatTile(
                            label = "Cans with them",
                            value = "${stats.withCustomer}",
                            icon = Icons.Rounded.Undo,
                            iconBg = NTColors.WarningLight,
                            iconFg = NTColors.Warning,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(NTDp.md))
            }

            // Behaviour strip
            item {
                DetailSectionCard(title = "BUYING BEHAVIOUR") {
                    DetailLine(
                        "First purchase",
                        stats.firstDate.takeIf { it.isNotBlank() }?.let { dbToDisplayDate(it) } ?: "—",
                    )
                    DetailLine(
                        "Last purchase",
                        stats.lastDate.takeIf { it.isNotBlank() }?.let { dbToDisplayDate(it) } ?: "—",
                    )
                    DetailLine(
                        "Active months",
                        if (stats.activeMonths > 0) "${stats.activeMonths}" else "—",
                    )
                    DetailLine(
                        "Favourite product",
                        stats.favoriteProduct.takeIf { it.isNotBlank() } ?: "—",
                        last = true,
                    )
                }
                Spacer(Modifier.height(NTDp.md))
            }

            // Per-product breakdown
            if (byProduct.isNotEmpty()) {
                item {
                    DetailSectionCard(title = "PER-PRODUCT BREAKDOWN") {
                        byProduct.forEachIndexed { idx, (name, qty, revenue) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        name.ifBlank { "Product" },
                                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                        color = NTColors.TextPrimary,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "$qty cans bought",
                                        fontSize = 11.sp, color = NTColors.TextSecondary,
                                    )
                                }
                                Text(
                                    formatCustomerMoney(revenue),
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    color = NTColors.TextPrimary,
                                )
                            }
                            if (idx < byProduct.lastIndex) {
                                HorizontalDivider(color = NTColors.Border)
                            }
                        }
                    }
                    Spacer(Modifier.height(NTDp.md))
                }
            }

            // Purchase history
            item {
                DetailSectionCard(title = "PURCHASE HISTORY") {
                    if (ordered.isEmpty()) {
                        Text(
                            "No purchases recorded yet.",
                            fontSize = 13.sp, color = NTColors.TextTertiary,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        ordered.forEachIndexed { idx, s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        s.productName.ifBlank { "Sale" },
                                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                        color = NTColors.TextPrimary,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${dbToDisplayDate(s.date)} · ${s.qty} cans",
                                        fontSize = 11.sp, color = NTColors.TextSecondary,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    formatCustomerMoney(s.totalSelling),
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    color = NTColors.TextPrimary,
                                )
                            }
                            if (idx < ordered.lastIndex) {
                                HorizontalDivider(color = NTColors.Border)
                            }
                        }
                    }
                }
            }

            // Actions: call + edit (dues move only via recorded payments).
            item {
                Spacer(Modifier.height(NTDp.md))
                Row(
                    modifier = Modifier.padding(horizontal = NTDp.screenPad),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (customer.phone?.isNotBlank() == true) {
                        GlossyTealBox(
                            shape = RoundedCornerShape(14.dp),
                            onClick = {
                                runCatching { uriHandler.openUri("tel:${customer.phone}") }
                            },
                            onClickLabel = "Call ${customer.name}",
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Call, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Call", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 13.dp),
                        border = BorderStroke(1.dp, NTColors.Border),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit details", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailBadge(text: String, fg: Color, bg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(NTDp.radFull))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
    }
}

@Composable
private fun DetailStatTile(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconFg: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(NTDp.radLg))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radLg))
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
            value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
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

@Composable
private fun DetailSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad)
            .clip(RoundedCornerShape(NTDp.radLg))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radLg))
            .padding(NTDp.md),
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
private fun DetailLine(label: String, value: String, last: Boolean = false) {
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
            color = NTColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
    if (!last) {
        HorizontalDivider(color = NTColors.Border)
    }
}
