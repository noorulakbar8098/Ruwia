package com.example.ruwia.ui

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.CustomerLedgerEntry
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.util.isoToDisplayDate

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
    val ledgerCustomer = state.customers.firstOrNull { it.id == state.ledgerCustomerId }
        ?: state.ledger?.let { ledger ->
            state.customers.firstOrNull { customer ->
                ledger.entries.any { it.customerId == customer.id }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.ledgerCustomerId != null) {
            CustomerLedgerContent(
                state = state,
                customer = ledgerCustomer,
                modifier = Modifier.padding(padding),
                onBack = onCloseLedger,
                onCollect = { paymentCustomer = it },
            )
        } else {
            ReceivablesContent(
                state = state,
                modifier = Modifier.padding(padding),
                onBack = onBack,
                onOpenCustomer = onOpenCustomer,
                onCollect = { paymentCustomer = it },
            )
        }
    }

    paymentCustomer?.let { customer ->
        RecordPaymentDialog(
            customer = customer,
            isSaving = state.paymentSaving,
            onDismiss = { if (!state.paymentSaving) paymentCustomer = null },
            onConfirm = { amount, mode, note ->
                val id = customer.id ?: return@RecordPaymentDialog
                onRecordPayment(id, amount, mode, note)
            },
        )
    }
}

@Composable
private fun ReceivablesContent(
    state: AdminState,
    modifier: Modifier,
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onCollect: (Customer) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    val outstanding = remember(state.customers) {
        state.customers
            .filter { it.balance > 0.001 }
            .sortedByDescending { it.balance }
    }
    val visible = remember(outstanding, state.customers, search, showAll) {
        val source = if (showAll) state.customers.sortedByDescending { it.balance } else outstanding
        if (search.isBlank()) source else source.filter {
            it.name.contains(search, ignoreCase = true) ||
                it.phone.orEmpty().contains(search, ignoreCase = true)
        }
    }
    val ledgerPayments = remember(state.payments) {
        state.payments.filter { !it.referenceKey.isNullOrBlank() }
    }
    val totalOutstanding = state.customers.sumOf { it.balance.coerceAtLeast(0.0) }
    val totalCollected = ledgerPayments.sumOf { it.amount }
    val totalBilled = totalOutstanding + totalCollected

    Column(modifier = modifier.fillMaxSize()) {
        ReceivablesTopBar(onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneySummaryCard("Billed", totalBilled, NTColors.TextPrimary, Modifier.weight(1f))
                    MoneySummaryCard("Collected", totalCollected, NTColors.Success, Modifier.weight(1f))
                    MoneySummaryCard("Outstanding", totalOutstanding, NTColors.Error, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReceivablesTab("Outstanding", !showAll) { showAll = false }
                    ReceivablesTab("All customers", showAll) { showAll = true }
                }
            }
            item {
                SearchField(value = search, onValueChange = { search = it })
            }
            item {
                Text(
                    if (showAll) "All customers" else "Customers with dues",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NTColors.TextSecondary,
                )
            }
            if (visible.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.CheckCircle,
                        title = if (showAll) "No customers yet" else "No outstanding dues",
                        subtitle = if (showAll) "Add a customer from Settings to start billing." else "Every customer is fully paid.",
                    )
                }
            } else {
                items(items = visible, key = { it.id ?: it.name }) { customer ->
                    CustomerDueRow(
                        customer = customer,
                        onClick = { customer.id?.let(onOpenCustomer) },
                        onCollect = { onCollect(customer) },
                    )
                }
            }
            if (ledgerPayments.isNotEmpty()) {
                item {
                    Text(
                        "Recent payments",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NTColors.TextSecondary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(items = ledgerPayments.take(8), key = { it.id ?: it.hashCode().toString() }) { payment ->
                    val name = state.customers.firstOrNull { it.id == payment.customerId }?.name ?: "Customer"
                    PaymentRow(name = name, amount = payment.amount, mode = payment.mode, date = payment.createdAt)
                }
            }
        }
    }
}

@Composable
private fun CustomerLedgerContent(
    state: AdminState,
    customer: Customer?,
    modifier: Modifier,
    onBack: () -> Unit,
    onCollect: (Customer) -> Unit,
) {
    val ledger = state.ledger
    val name = customer?.name ?: ledger?.entries?.firstOrNull()?.customerName.orEmpty().ifBlank { "Customer" }

    Column(modifier = modifier.fillMaxSize()) {
        ReceivablesTopBar(title = name, onBack = onBack)
        if (state.ledgerLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NTColors.Primary)
            }
            return@Column
        }
        val billed = ledger?.billed ?: customer?.balance ?: 0.0
        val collected = ledger?.collected ?: 0.0
        val outstanding = ledger?.outstanding ?: customer?.balance ?: 0.0
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneySummaryCard("Billed", billed, NTColors.TextPrimary, Modifier.weight(1f))
                    MoneySummaryCard("Collected", collected, NTColors.Success, Modifier.weight(1f))
                    MoneySummaryCard("Remaining", outstanding, NTColors.Error, Modifier.weight(1f))
                }
            }
            item {
                Button(
                    onClick = { customer?.let(onCollect) },
                    enabled = customer != null && outstanding > 0.001 && !state.paymentSaving,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                ) {
                    Icon(Icons.Rounded.Payments, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Record payment", fontWeight = FontWeight.SemiBold)
                }
            }
            item {
                Text(
                    "Statement",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NTColors.TextSecondary,
                )
            }
            if (ledger == null || ledger.entries.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "No entries yet",
                        subtitle = "Sales and payments will appear here.",
                    )
                }
            } else {
                items(items = ledger.entries, key = { it.id ?: it.referenceKey }) { entry ->
                    LedgerEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun ReceivablesTopBar(title: String = "Payments & Dues", onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(NTColors.SurfaceVar)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.ArrowBack, "Back", tint = NTColors.TextPrimary, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Sales, payments and outstanding", fontSize = 11.sp, color = NTColors.TextTertiary)
        }
    }
}

@Composable
private fun CustomerDueRow(customer: Customer, onClick: () -> Unit, onCollect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NTColors.Surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(NTColors.PrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                customer.name.take(1).uppercase(),
                color = NTColors.PrimaryDark,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(customer.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextPrimary)
            Text(
                listOfNotNull(
                    customer.phone?.takeIf { it.isNotBlank() },
                    "${customer.cansHeld} cans".takeIf { customer.cansHeld > 0 },
                ).joinToString(" · ").ifBlank { "No contact details" },
                fontSize = 11.sp,
                color = NTColors.TextTertiary,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(money(customer.balance), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (customer.balance > 0.0) NTColors.Error else NTColors.Success)
            Text(if (customer.balance > 0.0) "Due" else "Paid", fontSize = 10.sp, color = NTColors.TextTertiary)
        }
        if (customer.balance > 0.001) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NTColors.Primary)
                    .clickable(onClick = onCollect)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("Collect", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NTColors.TextOnPrimary)
            }
        }
    }
}

@Composable
private fun LedgerEntryRow(entry: CustomerLedgerEntry) {
    val isPayment = entry.credit > 0.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NTColors.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isPayment) NTColors.SuccessLight else NTColors.ErrorLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPayment) Icons.Rounded.Payments else Icons.Rounded.ReceiptLong,
                null,
                tint = if (isPayment) NTColors.SuccessText else NTColors.ErrorText,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isPayment) "Payment received" else "Sale billed",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = NTColors.TextPrimary,
            )
            Text(
                listOfNotNull(
                    entry.createdAt?.let { isoToDisplayDate(it) },
                    entry.mode?.replaceFirstChar { it.uppercaseChar() },
                    entry.note?.takeIf { it.isNotBlank() },
                ).joinToString(" · ").ifBlank { entry.referenceKey },
                fontSize = 10.sp,
                color = NTColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (isPayment) "+" else "-") + money(if (isPayment) entry.credit else entry.debit),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPayment) NTColors.Success else NTColors.TextPrimary,
            )
            Text("Bal ${money(entry.balanceAfter)}", fontSize = 10.sp, color = NTColors.TextTertiary)
        }
    }
}

@Composable
private fun PaymentRow(name: String, amount: Double, mode: String, date: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NTColors.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Payments, null, tint = NTColors.Success, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextPrimary)
            Text(
                listOfNotNull(date?.let { isoToDisplayDate(it) }, mode.replaceFirstChar { it.uppercaseChar() }).joinToString(" · "),
                fontSize = 10.sp,
                color = NTColors.TextTertiary,
            )
        }
        Text(money(amount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NTColors.Success)
    }
}

@Composable
private fun MoneySummaryCard(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NTColors.Surface)
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 10.sp, color = NTColors.TextTertiary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(money(value), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
private fun CountSummaryCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NTColors.Surface)
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 10.sp, color = NTColors.TextTertiary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text("$value", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
    }
}

@Composable
private fun ReceivablesTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) NTColors.Primary else NTColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) NTColors.TextOnPrimary else NTColors.TextSecondary,
        )
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NTColors.Surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = NTColors.TextTertiary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Search customer or phone", fontSize = 13.sp, color = NTColors.TextTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(fontSize = 13.sp, color = NTColors.TextPrimary),
                cursorBrush = SolidColor(NTColors.Primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NTColors.Surface)
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = NTColors.Primary, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextPrimary)
        Text(subtitle, fontSize = 11.sp, color = NTColors.TextTertiary)
    }
}

@Composable
private fun RecordPaymentDialog(
    customer: Customer,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: String, mode: String, note: String) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("cash") }
    var note by remember { mutableStateOf("") }
    val parsed = amount.toDoubleOrNull() ?: 0.0
    val invalid = parsed <= 0.0 || parsed > customer.balance + 0.001

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(NTColors.Surface)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Record payment", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
                    Text(customer.name, fontSize = 12.sp, color = NTColors.TextTertiary)
                }
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(NTColors.SurfaceVar).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Close", tint = NTColors.TextSecondary, modifier = Modifier.size(17.dp)) }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoneySummaryCard("Outstanding", customer.balance, NTColors.Error, Modifier.weight(1f))
                CountSummaryCard("Cans held", customer.cansHeld, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Text("Amount", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NTColors.SurfaceVar)
                    .border(1.dp, NTColors.Divider, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("₹", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (amount.isEmpty()) {
                        Text("0", fontSize = 15.sp, color = NTColors.TextTertiary)
                    }
                    BasicTextField(
                        value = amount,
                        onValueChange = { value -> if (value.all { it.isDigit() } || value.isEmpty()) amount = value },
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary),
                        cursorBrush = SolidColor(NTColors.Primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text("Max ${money(customer.balance)}", fontSize = 10.sp, color = NTColors.TextTertiary)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("cash" to "Cash", "upi" to "UPI", "card" to "Card", "bank" to "Bank").forEach { (key, label) ->
                    ReceivablesTab(label, mode == key) { mode = key }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(13.dp),
                ) { Text("Cancel") }
                Button(
                    onClick = { onConfirm(amount, mode, note) },
                    enabled = !isSaving && !invalid,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NTColors.TextOnPrimary)
                    } else {
                        Text("Save payment", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun money(value: Double): String {
    val rounded = (value * 100).toLong() / 100.0
    return if (rounded % 1.0 == 0.0) "₹${rounded.toInt()}" else "₹$rounded"
}
