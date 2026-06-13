package com.example.ruwia.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.theme.RuwiaColor
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ── Multi-product line item ───────────────────────────────────────────────────

data class OutwardLineItem(
    val productIdx: Int,
    val qty: Int,
    val sellPriceText: String,
)

// ── Date helpers (KMP-compatible, no external deps) ───────────────────────────

private fun Long.toDisplayDate(): String {
    var days = (this / 86400000L).toInt()
    var year = 1970
    while (true) {
        val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        val diy = if (leap) 366 else 365
        if (days < diy) break
        days -= diy; year++
    }
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val dpm = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val mon = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    var m = 0
    while (days >= dpm[m]) { days -= dpm[m]; m++ }
    return "${days + 1} ${mon[m]} $year"
}

private fun formatTime(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val ampm = if (hour >= 12) "PM" else "AM"
    return "$h:${minute.toString().padStart(2, '0')} $ampm"
}

// ── Public entry-point ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleScreen(
    products: List<ProductCategory> = emptyList(),
    customers: List<Customer> = emptyList(),
    onNewCustomer: (Customer) -> Unit = {},
    onBack: () -> Unit,
    onSave: (customerName: String, items: List<OutwardLineItem>) -> Unit = { _, _ -> },
) {
    val defaultProductIdx = if (products.isNotEmpty()) products.indices.last else 0
    var lineItems by remember(products) {
        // Only seed an initial line item if at least one product exists.
        // Otherwise the line-item renderer would crash trying to look up a
        // product in an empty list.
        mutableStateOf(
            if (products.isNotEmpty()) {
                listOf(
                    OutwardLineItem(
                        defaultProductIdx, 1,
                        products.lastOrNull()?.defaultSellPrice?.let {
                            if (it > 0) it.toInt().toString() else ""
                        } ?: ""
                    )
                )
            } else emptyList()
        )
    }
    var selectedCustomer   by remember { mutableStateOf<Customer?>(null) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var editingLineIdx     by remember { mutableStateOf<Int?>(null) }
    var showProductPicker  by remember { mutableStateOf(false) }

    // Date / time — use current date/time via kotlinx-datetime
    var displayDate by remember {
        mutableStateOf(
            try {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val mon = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                "${now.dayOfMonth} ${mon[now.monthNumber - 1]} ${now.year}"
            } catch (_: Exception) { "" }
        )
    }
    var displayTime by remember {
        mutableStateOf(
            try {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val h = now.hour
                val hh = if (h == 0) 12 else if (h > 12) h - 12 else h
                val ampm = if (h >= 12) "PM" else "AM"
                "$hh:${now.minute.toString().padStart(2,'0')} $ampm"
            } catch (_: Exception) { "" }
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(initialHour = 11, initialMinute = 16)

    val totalQty   = lineItems.sumOf { it.qty }
    val totalValue = lineItems.sumOf {
        val p = products.getOrNull(it.productIdx) ?: return@sumOf 0.0
        (it.sellPriceText.toDoubleOrNull() ?: p.defaultSellPrice) * it.qty
    }
    val isSaveEnabled = selectedCustomer != null && lineItems.isNotEmpty() && lineItems.all { it.qty > 0 }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        displayDate = millis.toDisplayDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = RuwiaColor.Surface,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Select time",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = RuwiaColor.TextPrimary,
                        modifier   = Modifier.align(Alignment.Start),
                    )
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            displayTime = formatTime(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(RuwiaColor.Background)) {
        Scaffold(
            containerColor = RuwiaColor.Background,
            topBar    = { SaleTopBar(onBack = onBack) },
            bottomBar = {
                SaleBottomBar(
                    isSaveEnabled = isSaveEnabled,
                    onCancel = onBack,
                    onSave   = { onSave(selectedCustomer!!.name, lineItems) },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(14.dp))
                SaleHeroCard()
                Spacer(Modifier.height(14.dp))

                // ── Section 1: Customer ────────────────────────
                SaleFormSection(number = 1, title = "Customer") {
                    CustomerPickerRow(
                        selectedCustomer = selectedCustomer,
                        onClick          = { showCustomerPicker = true },
                    )
                }
                Spacer(Modifier.height(10.dp))

                // ── Section 2: Products (multi) ────────────────
                MultiProductSection(
                    sectionNumber = 2,
                    products      = products,
                    lineItems     = lineItems,
                    onChangeProduct = { lineIdx ->
                        editingLineIdx = lineIdx
                        showProductPicker  = true
                    },
                    onQtyChange   = { lineIdx, newQty ->
                        lineItems = lineItems.mapIndexed { i, item ->
                            if (i == lineIdx) item.copy(qty = newQty) else item
                        }
                    },
                    onPriceChange = { lineIdx, newPrice ->
                        lineItems = lineItems.mapIndexed { i, item ->
                            if (i == lineIdx) item.copy(sellPriceText = newPrice) else item
                        }
                    },
                    onRemove      = { lineIdx ->
                        lineItems = lineItems.filterIndexed { i, _ -> i != lineIdx }
                    },
                    onAdd         = {
                        // Guard against an empty products list — adding a
                        // line item with no product to bind to would crash
                        // the renderer.
                        val firstProduct = products.firstOrNull() ?: return@MultiProductSection
                        lineItems = lineItems + OutwardLineItem(
                            productIdx    = 0,
                            qty           = 1,
                            sellPriceText = firstProduct.defaultSellPrice.let {
                                if (it > 0) it.toInt().toString() else ""
                            },
                        )
                    },
                )
                Spacer(Modifier.height(10.dp))

                // ── Section 3: Date & time ─────────────────────
                SaleFormSection(number = 3, title = "Date & time") {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SaleDateTimeButton(
                            icon    = Icons.Rounded.DateRange,
                            label   = displayDate,
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        SaleDateTimeButton(
                            icon    = Icons.Rounded.Schedule,
                            label   = displayTime,
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                // ── Sale summary (no margin shown to employee) ─
                SaleSummaryCard(
                    customer     = selectedCustomer,
                    totalQty     = totalQty,
                    lineCount    = lineItems.size,
                    sellingTotal = totalValue,
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Customer picker overlay ────────────────────────────
        if (showCustomerPicker) {
            CustomerPickerOverlay(
                initialCustomers = customers,
                selectedCustomer = selectedCustomer,
                onDismiss        = { showCustomerPicker = false },
                onSelect         = { selectedCustomer = it },
                onNewCustomer    = onNewCustomer,
            )
        }

        // ── Product picker overlay ─────────────────────────────
        if (showProductPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showProductPicker = false; editingLineIdx = null },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(RuwiaColor.Surface)
                    .navigationBarsPadding()
                    .padding(20.dp),
            ) {
                Text(
                    "Change product",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary,
                )
                Spacer(Modifier.height(14.dp))
                val currentIdx = editingLineIdx?.let { lineItems.getOrNull(it)?.productIdx } ?: -1
                products.forEachIndexed { idx, p ->
                    val selected = idx == currentIdx
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) RuwiaColor.TealExtraLight else Color.Transparent)
                            .clickable {
                                editingLineIdx?.let { lineIdx ->
                                    lineItems = lineItems.mapIndexed { i, item ->
                                        if (i == lineIdx) item.copy(
                                            productIdx    = idx,
                                            sellPriceText = if (p.defaultSellPrice > 0)
                                                p.defaultSellPrice.toInt().toString() else "",
                                        ) else item
                                    }
                                }
                                showProductPicker = false
                                editingLineIdx    = null
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (selected) RuwiaColor.TealPrimary else RuwiaColor.TealExtraLight,
                                        RoundedCornerShape(8.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.WaterDrop, null,
                                    tint = if (selected) Color.White else RuwiaColor.TealPrimary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(p.displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary)
                                Text("Default ₹${p.defaultSellPrice.toInt()}/unit", fontSize = 11.sp, color = RuwiaColor.TextMuted)
                            }
                        }
                        if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(18.dp))
                    }
                    if (idx < products.lastIndex) HorizontalDivider(color = RuwiaColor.Divider)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun SaleTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.Background)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(RuwiaColor.Surface, RoundedCornerShape(10.dp))
                .clickable(onClick = onBack)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = RuwiaColor.TextPrimary, modifier = Modifier.size(18.dp))
        }
        Text(
            text = "Add sale", fontSize = 17.sp, fontWeight = FontWeight.Bold,
            color = RuwiaColor.TextPrimary, modifier = Modifier.align(Alignment.Center),
        )
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .border(1.2.dp, RuwiaColor.Divider, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.LocalShipping, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text("EMP", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, color = RuwiaColor.TextSecondary)
        }
    }
}

// ── Hero card ──────────────────────────────────────────────────────────────────

@Composable
private fun SaleHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RuwiaColor.TealDark),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.06f), size.height * 1.5f, Offset(size.width * 0.82f, size.height * 0.5f))
        }
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ArrowForward, null, tint = Color.White,
                    modifier = Modifier.size(20.dp).rotate(-45f))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("OUTWARD  ·  TO CUSTOMER", fontSize = 10.sp, letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.70f))
                Spacer(Modifier.height(3.dp))
                Text("Recording a customer sale", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── Generic section card ───────────────────────────────────────────────────────

@Composable
private fun SaleFormSection(
    number: Int,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(26.dp).background(RuwiaColor.TealPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("$number", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary)
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

// ── Multi-product section ──────────────────────────────────────────────────────

@Composable
private fun MultiProductSection(
    sectionNumber: Int,
    products: List<ProductCategory>,
    lineItems: List<OutwardLineItem>,
    onChangeProduct: (lineIdx: Int) -> Unit,
    onQtyChange: (lineIdx: Int, qty: Int) -> Unit,
    onPriceChange: (lineIdx: Int, price: String) -> Unit,
    onRemove: (lineIdx: Int) -> Unit,
    onAdd: () -> Unit,
) {
    val totalQty = lineItems.sumOf { it.qty }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(26.dp).background(RuwiaColor.TealPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("$sectionNumber", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Text("Products", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary, modifier = Modifier.weight(1f))
            if (totalQty > 0) {
                Box(
                    modifier = Modifier
                        .background(RuwiaColor.TealExtraLight, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text("$totalQty units", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TealPrimary)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // ── Empty state when no products are available ─────────────────
        // Without this guard the line-item renderer below would crash
        // trying to look up a product in an empty list.
        if (products.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RuwiaColor.Background, RoundedCornerShape(12.dp))
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = RuwiaColor.TextMuted,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "No products available",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RuwiaColor.TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ask the admin to add products in Product Management before recording a sale.",
                    fontSize = 11.sp,
                    color = RuwiaColor.TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        lineItems.forEachIndexed { idx, item ->
            // Resolve the product safely. If the cached productIdx is out of
            // range (e.g. the product list shrank since the line was added),
            // fall back to the first product instead of crashing.
            val product = products.getOrNull(item.productIdx) ?: products.first()
            if (idx > 0) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = RuwiaColor.Divider.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
            }
            SaleLineCard(
                lineNumber   = idx + 1,
                product      = product,
                qty          = item.qty,
                priceText    = item.sellPriceText,
                showRemove   = lineItems.size > 1,
                onProductTap = { onChangeProduct(idx) },
                onQtyChange  = { onQtyChange(idx, it) },
                onPriceChange = { onPriceChange(idx, it) },
                onRemove     = { onRemove(idx) },
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, RuwiaColor.TealLight, RoundedCornerShape(12.dp))
                .clickable(onClick = onAdd)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add another product", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TealPrimary)
        }
    }
}

@Composable
private fun SaleLineCard(
    lineNumber: Int,
    product: ProductCategory,
    qty: Int,
    priceText: String,
    showRemove: Boolean,
    onProductTap: () -> Unit,
    onQtyChange: (Int) -> Unit,
    onPriceChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.Background, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        // ── Product selector row ───────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(RuwiaColor.TealExtraLight, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$lineNumber", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TealPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(RuwiaColor.Surface, RoundedCornerShape(8.dp))
                    .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(8.dp))
                    .clickable(onClick = onProductTap)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.WaterDrop, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    product.displayName,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Rounded.SwapHoriz, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(14.dp))
            }
            if (showRemove) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color(0xFFFFEEEE), RoundedCornerShape(8.dp))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, null, tint = Color(0xFFCC3333), modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Qty stepper + price ────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Stepper
            Row(
                modifier = Modifier
                    .background(RuwiaColor.Surface, RoundedCornerShape(24.dp))
                    .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(24.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SaleStepBtn(Icons.Rounded.Remove, qty > 1) { onQtyChange(qty - 1) }
                Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                    Text("$qty", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = RuwiaColor.TextPrimary, textAlign = TextAlign.Center)
                }
                SaleStepBtn(Icons.Rounded.Add, true) { onQtyChange(qty + 1) }
            }

            Spacer(Modifier.width(10.dp))

            // Sell price field
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(RuwiaColor.TealExtraLight.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .border(1.dp, RuwiaColor.TealLight, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("₹", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TealPrimary)
                Spacer(Modifier.width(4.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (priceText.isEmpty()) {
                        Text("0", fontSize = 16.sp, color = RuwiaColor.TextMuted, fontWeight = FontWeight.Bold)
                    }
                    BasicTextField(
                        value       = priceText,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onPriceChange(it) },
                        textStyle   = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = RuwiaColor.TealPrimary),
                        cursorBrush = SolidColor(RuwiaColor.TealPrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine  = true,
                        modifier    = Modifier.fillMaxWidth(),
                    )
                }
                Text("/unit", fontSize = 11.sp, color = RuwiaColor.TextMuted)
            }
        }
    }
}

// ── Date & time button ────────────────────────────────────────────────────────

@Composable
private fun SaleDateTimeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary)
    }
}

// ── Sale summary card (no margin shown to employee) ────────────────────────────

@Composable
private fun SaleSummaryCard(
    customer: Customer?,
    totalQty: Int,
    lineCount: Int,
    sellingTotal: Double,
) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(RuwiaColor.TealDark),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.07f), size.height * 1.6f, Offset(size.width * 0.68f, size.height * 0.95f))
        }
        Column(modifier = Modifier.padding(20.dp)) {
            SaleSummaryRow("Customer",   customer?.name ?: "—")
            Spacer(Modifier.height(6.dp))
            SaleSummaryRow("Products",  "$lineCount type${if (lineCount != 1) "s" else ""}")
            Spacer(Modifier.height(6.dp))
            SaleSummaryRow("Total qty", "$totalQty units")
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.30f), thickness = 0.8.dp)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total sale value", fontSize = 13.sp, color = Color.White.copy(alpha = 0.78f))
                Text("₹${sellingTotal.toInt()}", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun SaleSummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.74f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

// ── Bottom bar ─────────────────────────────────────────────────────────────────

@Composable
private fun SaleBottomBar(isSaveEnabled: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(RuwiaColor.Background).navigationBarsPadding()) {
        HorizontalDivider(color = RuwiaColor.Divider, thickness = 0.6.dp)
        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RuwiaColor.TextSecondary)) {
                Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = onSave, enabled = isSaveEnabled, modifier = Modifier.weight(2f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RuwiaColor.TealDark, contentColor = Color.White,
                    disabledContainerColor = RuwiaColor.TextMuted.copy(alpha = 0.4f))) {
                Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save sale", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
private fun SaleStepBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(32.dp).background(RuwiaColor.Background, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) RuwiaColor.TextSecondary else RuwiaColor.TextMuted, modifier = Modifier.size(16.dp)) }
}
