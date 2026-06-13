package com.example.ruwia.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.StockItem
import com.example.ruwia.theme.RuwiaColor
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ── Inward line item ──────────────────────────────────────────────────────────

data class InwardLineItem(
    val productIdx: Int,
    val qty: Int,
)

private val fallbackSuppliers = listOf(
    "Global Creators  ·  Tiru",
    "Multi Brands  ·  Coimbatore",
    "Aqua Pure Plant  ·  Tiru",
)

private val fallbackShops = listOf(
    "Shop 1" to "SAIBABA",
    "Shop 2" to "RS PURAM",
)

// ── Internal inward product wrapper ──────────────────────────────────────────

private data class InwardProduct(
    val name: String,
    val displayName: String,
    val purchasePriceGC: Double,
)

// ── Public entry point ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockPurchaseScreen(
    stockItems: List<StockItem> = emptyList(),
    products: List<ProductCategory> = emptyList(),
    suppliers: List<String> = emptyList(),
    shops: List<Pair<String, String>> = fallbackShops,
    onBack: () -> Unit,
    onClose: () -> Unit,
    isEmployee: Boolean = false,
    onSave: (
        supplier: String,
        vehicleNum: String,
        invoiceNum: String,
        quantities: Map<String, Int>,
        empties: Int,
    ) -> Unit,
) {
    val effectiveProducts  = if (products.isNotEmpty())
        products.map { InwardProduct(it.name, it.displayName, it.purchasePriceGC) }
    else emptyList<InwardProduct>()

    val effectiveSuppliers = suppliers.ifEmpty { fallbackSuppliers }
    val effectiveShops     = shops.ifEmpty { fallbackShops }

    val defaultLineIdx = effectiveProducts.indexOfLast { it.name.contains("20") }
        .takeIf { it >= 0 } ?: effectiveProducts.indices.lastOrNull() ?: 0

    var lineItems          by remember(effectiveProducts) {
        mutableStateOf(listOf(InwardLineItem(defaultLineIdx, 50)))
    }
    var selectedSupplier   by remember(effectiveSuppliers) { mutableStateOf(effectiveSuppliers.firstOrNull() ?: "") }
    var selectedShop       by remember { mutableStateOf(0) }
    var showSupplierPicker by remember { mutableStateOf(false) }
    var showProductPicker  by remember { mutableStateOf(false) }
    var editingLineIdx     by remember { mutableStateOf<Int?>(null) }

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
    val timePickerState = rememberTimePickerState(initialHour = 8, initialMinute = 45)

    // ── Dialogs ────────────────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        displayDate = millis.toInwardDisplayDate()
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
            Surface(shape = RoundedCornerShape(28.dp), color = RuwiaColor.Surface) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Select time",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary,
                        modifier = Modifier.align(Alignment.Start),
                    )
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            displayTime = inwardFormatTime(timePickerState.hour, timePickerState.minute)
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
            topBar         = { AddInwardTopBar(onBack = onBack, onClose = onClose) },
            bottomBar      = {
                AddInwardBottomBar(
                    onCancel = onBack,
                    onSave   = {
                        val quantities = lineItems.associate { item ->
                            (effectiveProducts.getOrNull(item.productIdx)?.name ?: "") to item.qty
                        }
                        onSave(selectedSupplier, "", "", quantities, 0)
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
                Spacer(Modifier.height(16.dp))
                InwardHeroCard()
                Spacer(Modifier.height(14.dp))

                // ── Section 1: Products (multi) ────────────────
                InwardMultiProductSection(
                    products       = effectiveProducts,
                    lineItems      = lineItems,
                    isEmployee     = isEmployee,
                    onChangeProduct = { lineIdx ->
                        editingLineIdx    = lineIdx
                        showProductPicker = true
                    },
                    onQtyChange    = { lineIdx, qty ->
                        lineItems = lineItems.mapIndexed { i, item ->
                            if (i == lineIdx) item.copy(qty = qty) else item
                        }
                    },
                    onRemove       = { lineIdx ->
                        lineItems = lineItems.filterIndexed { i, _ -> i != lineIdx }
                    },
                    onAdd          = {
                        val usedIdx = lineItems.map { it.productIdx }.toSet()
                        val nextIdx = effectiveProducts.indices.firstOrNull { it !in usedIdx } ?: 0
                        lineItems = lineItems + InwardLineItem(nextIdx, 10)
                    },
                )
                Spacer(Modifier.height(10.dp))

                // ── Section 2: Supplier & shop ─────────────────
                FormSectionCard(number = 2, title = "Supplier & shop") {
                    SupplierShopPicker(
                        supplier       = selectedSupplier,
                        shops          = effectiveShops,
                        selectedShop   = selectedShop,
                        onSupplierClick = { showSupplierPicker = true },
                        onShopSelect   = { selectedShop = it },
                    )
                }
                Spacer(Modifier.height(10.dp))

                // ── Section 3: Date & time ─────────────────────
                FormSectionCard(number = 3, title = "Date & time") {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        InwardDateTimeButton(
                            icon    = Icons.Rounded.DateRange,
                            label   = displayDate,
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        InwardDateTimeButton(
                            icon    = Icons.Rounded.Schedule,
                            label   = displayTime,
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                // ── Summary card ───────────────────────────────
                InwardSummaryCard(
                    products   = effectiveProducts,
                    lineItems  = lineItems,
                    isEmployee = isEmployee,
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Supplier picker overlay ────────────────────────────
        if (showSupplierPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showSupplierPicker = false },
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
                Text("Select supplier", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary)
                Spacer(Modifier.height(16.dp))
                effectiveSuppliers.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (s == selectedSupplier) RuwiaColor.TealExtraLight else Color.Transparent)
                            .clickable { selectedSupplier = s; showSupplierPicker = false }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Factory, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(s, fontSize = 14.sp, color = RuwiaColor.TextPrimary,
                            fontWeight = if (s == selectedSupplier) FontWeight.SemiBold else FontWeight.Normal)
                    }
                    HorizontalDivider(color = RuwiaColor.Divider)
                }
                Spacer(Modifier.height(16.dp))
            }
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
                Text("Select product", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary)
                Spacer(Modifier.height(14.dp))
                val currentIdx = editingLineIdx?.let { lineItems.getOrNull(it)?.productIdx } ?: -1
                effectiveProducts.forEachIndexed { idx, p ->
                    val selected = idx == currentIdx
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) RuwiaColor.TealExtraLight else Color.Transparent)
                            .clickable {
                                editingLineIdx?.let { lineIdx ->
                                    lineItems = lineItems.mapIndexed { i, item ->
                                        if (i == lineIdx) item.copy(productIdx = idx) else item
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
                                WaterCanIcon(modifier = Modifier.size(width = 14.dp, height = 22.dp), selected = selected)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(p.displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary)
                                if (!isEmployee && p.purchasePriceGC > 0) {
                                    Text("Purchase ₹${p.purchasePriceGC.fmt2}/unit", fontSize = 11.sp, color = RuwiaColor.TextMuted)
                                }
                            }
                        }
                        if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(18.dp))
                    }
                    if (idx < effectiveProducts.lastIndex) HorizontalDivider(color = RuwiaColor.Divider)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun AddInwardTopBar(onBack: () -> Unit, onClose: () -> Unit) {
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
            "Add inward stock", fontSize = 17.sp, fontWeight = FontWeight.Bold,
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

// ── Hero card ─────────────────────────────────────────────────────────────────

@Composable
private fun InwardHeroCard() {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(RuwiaColor.TealPrimary),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.06f), size.height * 1.5f, Offset(size.width * 0.82f, size.height * 0.5f))
        }
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp).rotate(45f))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("INWARD  ·  FROM SUPPLIER", fontSize = 10.sp, letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.70f))
                Spacer(Modifier.height(3.dp))
                Text("Stock arriving from plant", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── Generic form section card ─────────────────────────────────────────────────

@Composable
private fun FormSectionCard(
    number: Int,
    title: String,
    badgeText: String? = null,
    badgeColor: Color = RuwiaColor.TealPrimary,
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
            if (badgeText != null) {
                Box(
                    modifier = Modifier.background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = badgeColor)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

// ── Multi-product section for inward ─────────────────────────────────────────

@Composable
private fun InwardMultiProductSection(
    products: List<InwardProduct>,
    lineItems: List<InwardLineItem>,
    isEmployee: Boolean,
    onChangeProduct: (lineIdx: Int) -> Unit,
    onQtyChange: (lineIdx: Int, qty: Int) -> Unit,
    onRemove: (lineIdx: Int) -> Unit,
    onAdd: () -> Unit,
) {
    val totalQty = lineItems.sumOf { it.qty }
    Column(
        modifier = Modifier.fillMaxWidth().background(RuwiaColor.Surface, RoundedCornerShape(16.dp)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(26.dp).background(RuwiaColor.TealPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("1", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Text("Products", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary, modifier = Modifier.weight(1f))
            if (totalQty > 0) {
                Box(
                    modifier = Modifier.background(RuwiaColor.TealExtraLight, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
                ) { Text("$totalQty units", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TealPrimary) }
            }
        }
        Spacer(Modifier.height(14.dp))

        lineItems.forEachIndexed { idx, item ->
            if (idx > 0) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = RuwiaColor.Divider.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
            }
            InwardLineCard(
                lineNumber   = idx + 1,
                product      = products.getOrNull(item.productIdx) ?: products.lastOrNull() ?: InwardProduct("","",0.0),
                qty          = item.qty,
                isEmployee   = isEmployee,
                showRemove   = lineItems.size > 1,
                onProductTap = { onChangeProduct(idx) },
                onQtyChange  = { onQtyChange(idx, it) },
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
private fun InwardLineCard(
    lineNumber: Int,
    product: InwardProduct,
    qty: Int,
    isEmployee: Boolean,
    showRemove: Boolean,
    onProductTap: () -> Unit,
    onQtyChange: (Int) -> Unit,
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
                modifier = Modifier.size(22.dp).background(RuwiaColor.TealExtraLight, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("$lineNumber", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TealPrimary) }
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
                Icon(Icons.Rounded.Lock, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(product.displayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary, modifier = Modifier.weight(1f))
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
                ) { Icon(Icons.Rounded.Close, null, tint = Color(0xFFCC3333), modifier = Modifier.size(14.dp)) }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Qty row ────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Qty:", fontSize = 12.sp, color = RuwiaColor.TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(10.dp))
            Row(
                modifier = Modifier
                    .background(RuwiaColor.Surface, RoundedCornerShape(24.dp))
                    .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(24.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InwardStepBtn(Icons.Rounded.Remove, qty > 1) { onQtyChange(qty - 1) }
                Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                    Text("$qty", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary, textAlign = TextAlign.Center)
                }
                InwardStepBtn(Icons.Rounded.Add, true) { onQtyChange(qty + 1) }
            }
        }

        // ── Purchase price (admin only) ────────────────────────
        if (!isEmployee && product.purchasePriceGC > 0) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RuwiaColor.OrangeSurface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Lock, null, tint = RuwiaColor.Orange, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text("Purchase", fontSize = 11.sp, color = RuwiaColor.Orange.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                Text(
                    "₹${product.purchasePriceGC.fmt2} / unit",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.Orange,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(RuwiaColor.Orange.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text("ADMIN", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = RuwiaColor.Orange)
                }
            }
        }
    }
}

// ── Date & time button ─────────────────────────────────────────────────────────

@Composable
private fun InwardDateTimeButton(
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

// ── Section 2: Supplier & shop ────────────────────────────────────────────────

@Composable
private fun SupplierShopPicker(
    supplier: String,
    shops: List<Pair<String, String>>,
    selectedShop: Int,
    onSupplierClick: () -> Unit,
    onShopSelect: (Int) -> Unit,
) {
    Column {
        RequiredFieldLabel("Supplier name")
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.2.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
                .clickable(onClick = onSupplierClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(30.dp).background(RuwiaColor.TealExtraLight, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Factory, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(10.dp))
            Text(supplier, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.height(14.dp))
        RequiredFieldLabel("Shop location")
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            shops.forEachIndexed { idx, (name, location) ->
                if (idx > 0) Spacer(Modifier.width(10.dp))
                ShopOptionCard(
                    name     = name,
                    location = location,
                    selected = selectedShop == idx,
                    onClick  = { onShopSelect(idx) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ShopOptionCard(name: String, location: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val borderColor = if (selected) RuwiaColor.TealPrimary else RuwiaColor.Divider
    val bgColor     = if (selected) RuwiaColor.TealExtraLight.copy(alpha = 0.45f) else RuwiaColor.Surface
    val iconBg      = if (selected) RuwiaColor.TealPrimary else RuwiaColor.TealExtraLight
    val nameColor   = if (selected) RuwiaColor.TealPrimary else RuwiaColor.TextPrimary

    Box(
        modifier = modifier
            .border(if (selected) 1.8.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(bgColor, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(38.dp).background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Store, null,
                    tint = if (selected) Color.White else RuwiaColor.TealPrimary,
                    modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(name,     fontSize = 13.sp, fontWeight = FontWeight.Bold, color = nameColor)
            Text(location, fontSize = 10.sp, letterSpacing = 0.5.sp, color = RuwiaColor.TextMuted)
        }
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun InwardSummaryCard(
    products: List<InwardProduct>,
    lineItems: List<InwardLineItem>,
    isEmployee: Boolean,
) {
    val totalQty   = lineItems.sumOf { it.qty }
    val totalValue = lineItems.sumOf {
        val p = products.getOrNull(it.productIdx) ?: return@sumOf 0.0
        p.purchasePriceGC * it.qty
    }

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(RuwiaColor.Orange)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.07f), size.height * 1.6f, Offset(size.width * 0.68f, size.height * 0.95f))
        }
        Column(modifier = Modifier.padding(20.dp)) {
            SummaryLine("Products", "${lineItems.size} type${if (lineItems.size != 1) "s" else ""}")
            Spacer(Modifier.height(8.dp))
            SummaryLine("Total qty", "$totalQty units")
            if (!isEmployee) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.30f), thickness = 0.8.dp)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total inward value", fontSize = 13.sp, color = Color.White.copy(alpha = 0.78f))
                    Text("₹${totalValue.toInt()}", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.74f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

// ── Bottom action bar ─────────────────────────────────────────────────────────

@Composable
private fun AddInwardBottomBar(onCancel: () -> Unit, onSave: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(RuwiaColor.Background).navigationBarsPadding()) {
        HorizontalDivider(color = RuwiaColor.Divider, thickness = 0.6.dp)
        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RuwiaColor.TextSecondary)) {
                Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = onSave, modifier = Modifier.weight(2f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RuwiaColor.TealPrimary, contentColor = Color.White)) {
                Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save inward", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun RequiredFieldLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = RuwiaColor.TextSecondary)
        Text(" *", fontSize = 13.sp, color = Color.Red.copy(alpha = 0.65f))
    }
}

@Composable
private fun InwardStepBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(32.dp).background(RuwiaColor.Background, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) RuwiaColor.TextSecondary else RuwiaColor.TextMuted, modifier = Modifier.size(16.dp)) }
}

@Composable
private fun WaterCanIcon(modifier: Modifier = Modifier, selected: Boolean = false) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val capW = w * 0.38f; val capH = h * 0.10f
        val capX = (w - capW) / 2f
        val bodyW = w * 0.58f; val bodyX = (w - bodyW) / 2f
        val bodyTop = capH + h * 0.04f
        val bodyH = h - bodyTop - h * 0.04f
        val bodyRad = CornerRadius(4.dp.toPx())
        val capColor  = if (selected) Color.White.copy(0.9f) else Color(0xFF8FBDB8)
        val bodyColor = if (selected) Color.White.copy(0.6f) else Color(0xFFCCDEDB)
        val fillColor = if (selected) Color.White.copy(0.8f) else Color(0xFF9DC5C0)
        drawRoundRect(capColor, Offset(capX, h * 0.02f), Size(capW, capH), CornerRadius(3.dp.toPx()))
        drawRoundRect(bodyColor, Offset(bodyX, bodyTop), Size(bodyW, bodyH), bodyRad)
        val fillH = bodyH * 0.60f
        drawRoundRect(fillColor, Offset(bodyX, bodyTop + bodyH - fillH), Size(bodyW, fillH), bodyRad)
        drawArc(
            color = capColor, startAngle = -90f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(bodyX + bodyW - 1.dp.toPx(), bodyTop + bodyH * 0.25f),
            size = Size(w * 0.18f, bodyH * 0.30f),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

// Date/time helpers (file-private copies for this file)
private fun Long.toInwardDisplayDate(): String {
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

private fun inwardFormatTime(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val ampm = if (hour >= 12) "PM" else "AM"
    return "$h:${minute.toString().padStart(2, '0')} $ampm"
}

private val Double.fmt2: String get() {
    val scaled = (this * 100).toLong()
    val whole  = scaled / 100
    val frac   = (scaled % 100).toInt().let { if (it < 0) -it else it }
    return "$whole.${frac.toString().padStart(2, '0')}"
}
