package com.example.ruwia.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.*
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.unitsPerCase
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
    /** Shop-scoped stock movements used to compute live per-product available
     *  stock. Pass the employee's `shopMovements` from the ViewModel state. */
    stockMovements: List<StockMovement> = emptyList(),
    errorMessage: String? = null,
    onNewCustomer: (Customer) -> Unit = {},
    onClearError: () -> Unit = {},
    onBack: () -> Unit,
    /** Called when the employee taps "Save sale". The third arg is the number
     *  of empty cans the employee collected from this customer at delivery.
     *  The fourth arg is the selected sale date in display format (e.g. "25 Jun 2026"). */
    onSave: (customerName: String, items: List<OutwardLineItem>, emptyCans: Int, saleDate: String) -> Unit = { _, _, _, _ -> },
    shopName: String = "",
) {
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
            title = { Text("Database Error", fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary) },
            text = { Text(errorDialogText ?: "") },
            confirmButton = {
                TextButton(onClick = { errorDialogText = null; onClearError() }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = RuwiaColor.TealDark)
                }
            },
            shape = RoundedCornerShape(20.dp),
        )
    }

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
    var productSearchQuery by remember { mutableStateOf("") }
    /** Number of empty cans the employee collected from the customer at the
     *  same time as delivering the new ones. Persists as an `inward` stock
     *  movement so the admin can see returned empties immediately. */
    var emptyCansText      by remember { mutableStateOf("") }

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

    // ── Per-product available stock (derived from shop movements) ─────────────
    // Maps product.id -> available units at this shop (inward - outward).
    // Used to block the employee from selling more than what's in stock.
    val cleanShop = shopName.trim().lowercase().substringBefore("·").trim()
    val isMainShop = cleanShop.startsWith("shop 1") ||
                     cleanShop.contains("main") ||
                     cleanShop.contains("warehouse") ||
                     cleanShop.contains("primary") ||
                     cleanShop.isBlank()
    val availableUnitsMap: Map<String, Int> = remember(stockMovements, products, shopName) {
        products.associate { product ->
            if (isMainShop) {
                product.id to product.stockAvailable
            } else {
                val rows = stockMovements.filter { it.productId == product.id }
                val inward  = rows.filter { it.type == "inward"  && !it.source.trim().startsWith("Empty cans", ignoreCase = true) }.sumOf { it.qty }
                val outward = rows.filter { it.type == "outward" }.sumOf { it.qty }
                product.id to (inward - outward).coerceAtLeast(0)
            }
        }
    }

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
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                SaleBottomBar(
                    isSaveEnabled = isSaveEnabled,
                    lineCount = lineItems.size,
                    totalQty = totalQty,
                    totalAmount = totalValue,
                    onCancel = onBack,
                    onSave   = {
                        val empties = emptyCansText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        // ── Stock validation ───────────────────────────────────
                        // Check each line item against available stock. Block the
                        // sale and show a clear error if any product is over-limit.
                        // item.qty is in cases, so convert available units→cases.
                        val overStockLine = lineItems.firstOrNull { item ->
                            val product = products.getOrNull(item.productIdx) ?: return@firstOrNull false
                            val availRaw = availableUnitsMap[product.id] ?: product.stockAvailable
                            val upc = product.unitsPerCase.coerceAtLeast(1)
                            item.qty > availRaw / upc
                        }
                        if (overStockLine != null) {
                            val product = products.getOrNull(overStockLine.productIdx)
                            val availRaw = product?.let { availableUnitsMap[it.id] ?: it.stockAvailable } ?: 0
                            val upc = product?.unitsPerCase?.coerceAtLeast(1) ?: 1
                            val availCases = availRaw / upc
                            val availRem   = availRaw % upc
                            val availLabel = if (upc > 1) {
                                if (availRem > 0) "$availCases cases + $availRem units" else "$availCases cases"
                            } else {
                                "$availRaw cans"
                            }
                            val sellLabel = if (upc > 1) {
                                "${overStockLine.qty} cases"
                            } else {
                                "${overStockLine.qty} cans"
                            }
                            errorDialogText = "Not enough stock for ${product?.displayName ?: "this product"}.\n\nRequested: $sellLabel\nAvailable: $availLabel\n\nPlease reduce the quantity and try again."
                            return@SaleBottomBar
                        }
                        onSave(selectedCustomer!!.name, lineItems, empties, displayDate)
                    },
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
                    availableUnitsMap = availableUnitsMap,
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

                // ── Section 4: Empty cans collected ────────────
                // Captures how many empty cans the employee picked up from the
                // customer at delivery time. Saved as an inward stock movement
                // so the admin's stock dashboard reflects the returned empties.
                EmptyCansSection(
                    sectionNumber = 4,
                    value = emptyCansText,
                    onValueChange = { newVal ->
                        if (newVal.all { it.isDigit() }) emptyCansText = newVal
                    },
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
            val filteredProducts = remember(products, productSearchQuery) {
                if (productSearchQuery.isBlank()) products
                else products.filter {
                    it.displayName.contains(productSearchQuery, ignoreCase = true) ||
                    it.name.contains(productSearchQuery, ignoreCase = true)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showProductPicker = false; editingLineIdx = null; productSearchQuery = "" },
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
                Spacer(Modifier.height(12.dp))
                // Search bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RuwiaColor.Background, RoundedCornerShape(10.dp))
                        .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    if (productSearchQuery.isEmpty()) {
                        Text("Search product...", fontSize = 13.sp, color = RuwiaColor.TextMuted)
                    }
                    BasicTextField(
                        value = productSearchQuery,
                        onValueChange = { productSearchQuery = it },
                        textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = RuwiaColor.TextPrimary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(14.dp))
                val currentIdx = editingLineIdx?.let { lineItems.getOrNull(it)?.productIdx } ?: -1
                Column(
                    modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filteredProducts.forEachIndexed { fIdx, p ->
                        val originalIdx = products.indexOf(p)
                        val selected = originalIdx == currentIdx
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) RuwiaColor.TealExtraLight else Color.Transparent)
                                .clickable {
                                    editingLineIdx?.let { lineIdx ->
                                        lineItems = lineItems.mapIndexed { i, item ->
                                            if (i == lineIdx) item.copy(
                                                productIdx    = originalIdx,
                                                sellPriceText = if (p.defaultSellPrice > 0)
                                                    p.defaultSellPrice.toInt().toString() else "",
                                            ) else item
                                        }
                                    }
                                    showProductPicker = false
                                    editingLineIdx    = null
                                    productSearchQuery = ""
                                }
                                .padding(12.dp),
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
                                    Text(p.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary)
                                    val upc = p.unitsPerCase
                                    val limitText = availableUnitsMap[p.id]?.let {
                                        if (upc > 1) "${it / upc} cases available" else "$it cans available"
                                    } ?: "Available stock: ${p.stockAvailable}"
                                    Text("₹${p.defaultSellPrice.toInt()}/unit  ·  $limitText", fontSize = 11.sp, color = RuwiaColor.TextMuted)
                                }
                            }
                            if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(18.dp))
                        }
                        if (fIdx < filteredProducts.lastIndex) HorizontalDivider(color = RuwiaColor.Divider)
                    }
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
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
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
    availableUnitsMap: Map<String, Int> = emptyMap(),
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
            val availableUnitsRaw = availableUnitsMap[product.id] ?: product.stockAvailable
            val upc = product.unitsPerCase.coerceAtLeast(1)
            val availableCases = availableUnitsRaw / upc
            if (idx > 0) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = RuwiaColor.Divider.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
            }
            SaleLineCard(
                lineNumber   = idx + 1,
                product      = product,
                qty          = item.qty,
                availableUnits = availableUnitsRaw,
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
    availableUnits: Int,
    priceText: String,
    showRemove: Boolean,
    onProductTap: () -> Unit,
    onQtyChange: (Int) -> Unit,
    onPriceChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val upc = product.unitsPerCase.coerceAtLeast(1)
    val availableCases = availableUnits / upc
    val isOverStock = qty > availableCases
    val cardBorder  = if (isOverStock) Color(0xFFEF4444) else RuwiaColor.Divider
    val cardBg      = if (isOverStock) Color(0xFF3B1616) else RuwiaColor.Background
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(12.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
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
                        .background(Color(0xFF3B1616), RoundedCornerShape(8.dp))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
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
                    AnimatedContent(
                        targetState = qty,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) { height -> height } +
                                 fadeIn() +
                                 scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut())
                            } else {
                                (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) { height -> -height } +
                                 fadeIn() +
                                 scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))) togetherWith
                                (slideOutVertically { height -> height } + fadeOut())
                            }.using(SizeTransform(clip = false))
                        }
                    ) { targetQty ->
                        Text(
                            text = "$targetQty",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverStock) Color(0xFFCC2222) else RuwiaColor.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                SaleStepBtn(Icons.Rounded.Add, qty < availableCases) { onQtyChange(qty + 1) }
            }

            Spacer(Modifier.width(10.dp))

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
                    Text(
                        text = priceText.ifBlank { "0" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RuwiaColor.TealPrimary
                    )
                }
                Text("/unit", fontSize = 11.sp, color = RuwiaColor.TextMuted)
            }
        }

        val upc = product.unitsPerCase
        val availCases = availableUnits / upc.coerceAtLeast(1)
        val availRem   = availableUnits % upc.coerceAtLeast(1)
        val availLabel = if (upc > 1) {
            if (availRem > 0) "$availCases cases + $availRem units available" else "$availCases cases available"
        } else {
            "$availableUnits cans available"
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isOverStock) Color(0xFF3B1616) else RuwiaColor.TealExtraLight,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isOverStock) Icons.Rounded.Warning else Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = if (isOverStock) Color(0xFFEF4444) else RuwiaColor.TealPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    if (isOverStock) "Over limit! $availLabel" else availLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOverStock) Color(0xFFFCA5A5) else RuwiaColor.TealPrimary,
                )
            }
        }

        val caseHint = if (upc > 1) "1 case = $upc units" else null



        if (caseHint != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .background(Color(0xFF332005), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Info, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = caseHint,
                    fontSize = 11.sp,
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.SemiBold
                )
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total qty", fontSize = 13.sp, color = Color.White.copy(alpha = 0.74f))
                AnimatedContent(
                    targetState = totalQty,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) { height -> height } + fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) { height -> -height } + fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))) togetherWith
                            (slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    }
                ) { targetQty ->
                    Text("$targetQty units", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.30f), thickness = 0.8.dp)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total sale value", fontSize = 13.sp, color = Color.White.copy(alpha = 0.78f))
                AnimatedContent(
                    targetState = sellingTotal,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) { height -> height } + fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) { height -> -height } + fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))) togetherWith
                            (slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    }
                ) { targetTotal ->
                    Text("₹${targetTotal.toInt()}", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
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
private fun SaleBottomBar(
    isSaveEnabled: Boolean,
    lineCount: Int,
    totalQty: Int,
    totalAmount: Double,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(totalQty, totalAmount) {
        if (totalQty > 0) {
            scale.animateTo(
                targetValue = 1.03f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .background(RuwiaColor.Surface)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = totalQty,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) { height -> height } + fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) { height -> -height } + fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))) togetherWith
                            (slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    }
                ) { targetQty ->
                    val itemsLabel = if (lineCount == 1) "1 Item" else "$lineCount Items"
                    val unitsLabel = if (targetQty == 1) "1 Unit" else "$targetQty Units"
                    Text(
                        text = "$itemsLabel, $unitsLabel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = RuwiaColor.TextSecondary
                    )
                }

                AnimatedContent(
                    targetState = totalAmount,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) { height -> height } + fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) { height -> -height } + fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))) togetherWith
                            (slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    }
                ) { targetAmount ->
                    Text(
                        text = "₹${targetAmount.toInt()}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RuwiaColor.TextPrimary
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RuwiaColor.TextSecondary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(RuwiaColor.Divider))
                ) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onSave,
                    enabled = isSaveEnabled,
                    modifier = Modifier.weight(2f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RuwiaColor.TealPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = RuwiaColor.LightGray,
                        disabledContentColor = RuwiaColor.TextMuted
                    )
                ) {
                    Text("Save Sale", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
private fun SaleStepBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) RuwiaColor.TealExtraLight else RuwiaColor.Background,
        animationSpec = tween(150)
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(32.dp)
            .background(bgColor, CircleShape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) RuwiaColor.TextSecondary else RuwiaColor.TextMuted, modifier = Modifier.size(16.dp)) }
}

// ── Empty cans section ────────────────────────────────────────────────────────
//   Asks the employee how many empty cans they picked up from this customer at
//   delivery time. Stored as an `inward` stock movement so the admin's stock
//   dashboard reflects returned empties immediately.

@Composable
private fun EmptyCansSection(
    sectionNumber: Int,
    value: String,
    onValueChange: (String) -> Unit,
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
            ) { Text("$sectionNumber", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Column(modifier = Modifier.weight(1f)) {
                Text("Empty cans collected", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary)
                Text("Returned by this customer at delivery", fontSize = 11.sp, color = RuwiaColor.TextMuted)
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RuwiaColor.Background, RoundedCornerShape(12.dp))
                .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Recycling, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text("0", fontSize = 16.sp, color = RuwiaColor.TextMuted, fontWeight = FontWeight.Bold)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = RuwiaColor.TealPrimary),
                    cursorBrush = SolidColor(RuwiaColor.TealPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text("cans", fontSize = 11.sp, color = RuwiaColor.TextMuted)
        }
    }
}
