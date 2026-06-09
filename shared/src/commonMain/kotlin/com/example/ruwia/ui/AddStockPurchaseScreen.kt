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
import com.example.ruwia.domain.StockItem
import com.example.ruwia.theme.RuwiaColor

private val defaultSuppliers = listOf(
    "Aqua Pure Plant  ·  Tiru",
    "Fresh Water  ·  Coimbatore",
    "Blue Nile  ·  Chennai",
)

private val defaultShops = listOf(
    "Shop 1" to "SAIBABA",
    "Shop 2" to "RS PURAM",
)

// ── Public entry point — keeps the same signature as before ─────────────────

@Composable
fun AddStockPurchaseScreen(
    stockItems: List<StockItem>,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onSave: (
        supplier: String,
        vehicleNum: String,
        invoiceNum: String,
        quantities: Map<String, Int>,
        empties: Int,
    ) -> Unit,
) {
    // ── Form state ─────────────────────────────────────────────────────────
    val items = stockItems.ifEmpty {
        listOf(StockItem(id = "20l", name = "20L Water Can  ·  NT-20",
            capacityLiters = 20, stockAvailable = 0,
            pricePerCan = 13.60, costPrice = 13.60))
    }
    var selectedItemIdx  by remember { mutableStateOf(0) }
    var quantity         by remember { mutableStateOf(50) }
    var selectedSupplier by remember { mutableStateOf(defaultSuppliers.first()) }
    var selectedShop     by remember { mutableStateOf(0) }
    var showSupplierPicker by remember { mutableStateOf(false) }

    val selectedItem = items[selectedItemIdx]
    val pricePerUnit = selectedItem.pricePerCan

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RuwiaColor.Background),
    ) {
        Scaffold(
            containerColor = RuwiaColor.Background,
            topBar = {
                AddInwardTopBar(onBack = onBack, onClose = onClose)
            },
            bottomBar = {
                AddInwardBottomBar(
                    onCancel = onBack,
                    onSave = {
                        val key = selectedItem.id ?: selectedItem.name
                        onSave(selectedSupplier, "", "", mapOf(key to quantity), 0)
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

                // ── Hero info card ─────────────────────────────
                InwardHeroCard()
                Spacer(Modifier.height(14.dp))

                // ── Section 1: Product ─────────────────────────
                FormSectionCard(number = 1, title = "Product") {
                    ProductDropdown(
                        items       = items,
                        selectedIdx = selectedItemIdx,
                        onSelect    = { selectedItemIdx = it },
                    )
                }
                Spacer(Modifier.height(10.dp))

                // ── Section 2: Quantity ────────────────────────
                FormSectionCard(number = 2, title = "Quantity") {
                    QuantityRow(
                        item     = selectedItem,
                        quantity = quantity,
                        onQtyChange = { quantity = it },
                    )
                }
                Spacer(Modifier.height(10.dp))

                // ── Section 3: Purchase price ──────────────────
                FormSectionCard(
                    number    = 3,
                    title     = "Purchase price",
                    badgeText = "ADMIN",
                    badgeColor = RuwiaColor.Orange,
                ) {
                    PurchasePriceReadOnly(price = pricePerUnit)
                }
                Spacer(Modifier.height(10.dp))

                // ── Section 4: Supplier & shop ─────────────────
                FormSectionCard(number = 4, title = "Supplier & shop") {
                    SupplierShopPicker(
                        supplier     = selectedSupplier,
                        selectedShop = selectedShop,
                        onSupplierClick = { showSupplierPicker = true },
                        onShopSelect = { selectedShop = it },
                    )
                }
                Spacer(Modifier.height(10.dp))

                // ── Section 5: Date & time ─────────────────────
                FormSectionCard(number = 5, title = "Date & time") {
                    DateTimeRow()
                }
                Spacer(Modifier.height(20.dp))

                // ── Summary card ───────────────────────────────
                InwardSummaryCard(quantity = quantity, pricePerUnit = pricePerUnit)
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
                Text(
                    text       = "Select supplier",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RuwiaColor.TextPrimary,
                )
                Spacer(Modifier.height(16.dp))
                defaultSuppliers.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (s == selectedSupplier)
                                    RuwiaColor.TealExtraLight else Color.Transparent
                            )
                            .clickable { selectedSupplier = s; showSupplierPicker = false }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Factory,
                            contentDescription = null,
                            tint     = RuwiaColor.TealPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text       = s,
                            fontSize   = 14.sp,
                            color      = RuwiaColor.TextPrimary,
                            fontWeight = if (s == selectedSupplier)
                                FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                    HorizontalDivider(color = RuwiaColor.Divider)
                }
                Spacer(Modifier.height(16.dp))
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
            Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                tint     = RuwiaColor.TextPrimary,
                modifier = Modifier.size(18.dp),
            )
        }

        Text(
            text       = "Add inward stock",
            fontSize   = 17.sp,
            fontWeight = FontWeight.Bold,
            color      = RuwiaColor.TextPrimary,
            modifier   = Modifier.align(Alignment.Center),
        )

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .border(1.2.dp, RuwiaColor.Divider, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.LocalShipping,
                contentDescription = null,
                tint     = RuwiaColor.TextMuted,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text         = "EMP",
                fontSize     = 11.sp,
                fontWeight   = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                color        = RuwiaColor.TextSecondary,
            )
        }
    }
}

// ── Hero card ─────────────────────────────────────────────────────────────────

@Composable
private fun InwardHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RuwiaColor.TealPrimary),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color  = Color.White.copy(alpha = 0.06f),
                radius = size.height * 1.5f,
                center = Offset(size.width * 0.82f, size.height * 0.5f),
            )
        }
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(20.dp).rotate(45f),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text         = "INWARD  ·  FROM SUPPLIER",
                    fontSize     = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight   = FontWeight.SemiBold,
                    color        = Color.White.copy(alpha = 0.70f),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text       = "Stock arriving from plant",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(RuwiaColor.TealPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "$number",
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text       = title,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = RuwiaColor.TextPrimary,
            )
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text         = badgeText,
                        fontSize     = 10.sp,
                        fontWeight   = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color        = badgeColor,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

// ── Section 1: Product ────────────────────────────────────────────────────────

@Composable
private fun ProductDropdown(
    items: List<StockItem>,
    selectedIdx: Int,
    onSelect: (Int) -> Unit,
) {
    val selected = items[selectedIdx]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.Background, RoundedCornerShape(10.dp))
            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint     = RuwiaColor.TextMuted,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text       = selected.name,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = RuwiaColor.TextPrimary,
            modifier   = Modifier.weight(1f),
        )
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint     = RuwiaColor.TextMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── Section 2: Quantity ───────────────────────────────────────────────────────

@Composable
private fun QuantityRow(
    item: StockItem,
    quantity: Int,
    onQtyChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.TealExtraLight.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .border(1.dp, RuwiaColor.TealLight, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(RuwiaColor.Surface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            WaterCanIllustration(modifier = Modifier.size(width = 30.dp, height = 46.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("${item.capacityLiters}L Can", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text("@ ₹${item.pricePerCan.fmt2}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.Orange)
            Text("LOCKED", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp, color = RuwiaColor.Orange.copy(alpha = 0.65f))
        }

        Spacer(Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .background(RuwiaColor.Surface, RoundedCornerShape(24.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton(icon = Icons.Rounded.Remove, enabled = quantity > 1, onClick = { onQtyChange(quantity - 1) })
            Box(modifier = Modifier.width(42.dp), contentAlignment = Alignment.Center) {
                Text(
                    text       = "$quantity",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RuwiaColor.TextPrimary,
                    textAlign  = TextAlign.Center,
                )
            }
            StepperButton(icon = Icons.Rounded.Add, enabled = true, onClick = { onQtyChange(quantity + 1) })
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(RuwiaColor.Background, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint     = if (enabled) RuwiaColor.TextSecondary else RuwiaColor.TextMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ── Section 3: Purchase price ─────────────────────────────────────────────────

@Composable
private fun PurchasePriceReadOnly(price: Double) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RuwiaColor.OrangeSurface, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.Orange)
                Spacer(Modifier.width(6.dp))
                Text(price.fmt2, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = RuwiaColor.Orange)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint     = RuwiaColor.Orange.copy(alpha = 0.55f),
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text         = "SET BY MR. KARTHIK  ·  ASK ADMIN TO CHANGE",
                fontSize     = 10.sp,
                letterSpacing = 0.4.sp,
                color        = RuwiaColor.Orange.copy(alpha = 0.65f),
                fontWeight   = FontWeight.Medium,
            )
        }
    }
}

// ── Section 4: Supplier & shop ────────────────────────────────────────────────

@Composable
private fun SupplierShopPicker(
    supplier: String,
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
                modifier = Modifier
                    .size(30.dp)
                    .background(RuwiaColor.TealExtraLight, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Factory,
                    contentDescription = null,
                    tint     = RuwiaColor.TealPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text       = supplier,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = RuwiaColor.TextPrimary,
                modifier   = Modifier.weight(1f),
            )
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint     = RuwiaColor.TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.height(14.dp))

        RequiredFieldLabel("Shop location")
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            defaultShops.forEachIndexed { idx, (name, location) ->
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
private fun ShopOptionCard(
    name: String,
    location: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderWidth = if (selected) 1.8.dp else 1.dp
    val borderColor = if (selected) RuwiaColor.TealPrimary else RuwiaColor.Divider
    val bgColor     = if (selected) RuwiaColor.TealExtraLight.copy(alpha = 0.45f) else RuwiaColor.Surface
    val iconBg      = if (selected) RuwiaColor.TealPrimary else RuwiaColor.TealExtraLight
    val iconTint    = if (selected) Color.White else RuwiaColor.TealPrimary
    val nameColor   = if (selected) RuwiaColor.TealPrimary else RuwiaColor.TextPrimary

    Box(
        modifier = modifier
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .background(bgColor, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Store,
                    contentDescription = null,
                    tint     = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(name,     fontSize = 13.sp, fontWeight = FontWeight.Bold, color = nameColor)
            Text(location, fontSize = 10.sp, letterSpacing = 0.5.sp, color = RuwiaColor.TextMuted)
        }
    }
}

// ── Section 5: Date & time ────────────────────────────────────────────────────

@Composable
private fun DateTimeRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DateTimeField(
            icon  = Icons.Rounded.DateRange,
            label = "17 May 2026",
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        DateTimeField(
            icon  = Icons.Rounded.Schedule,
            label = "08:45 AM",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DateTimeField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary)
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun InwardSummaryCard(quantity: Int, pricePerUnit: Double) {
    val total = (quantity * pricePerUnit).toInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(RuwiaColor.Orange),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color  = Color.White.copy(alpha = 0.07f),
                radius = size.height * 1.6f,
                center = Offset(size.width * 0.68f, size.height * 0.95f),
            )
        }
        Column(modifier = Modifier.padding(20.dp)) {
            SummaryLine("Quantity", "$quantity units")
            Spacer(Modifier.height(8.dp))
            SummaryLine("Purchase price / unit", "₹${pricePerUnit.fmt2}")
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.30f), thickness = 0.8.dp)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Total inward value", fontSize = 13.sp, color = Color.White.copy(alpha = 0.78f))
                Text("₹$total", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.74f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

// ── Bottom action bar ─────────────────────────────────────────────────────────

@Composable
private fun AddInwardBottomBar(onCancel: () -> Unit, onSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.Background)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = RuwiaColor.Divider, thickness = 0.6.dp)
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick  = onCancel,
                modifier = Modifier.weight(1f).height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = RuwiaColor.TextSecondary,
                ),
            ) {
                Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick  = onSave,
                modifier = Modifier.weight(2f).height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = RuwiaColor.TealPrimary,
                    contentColor   = Color.White,
                ),
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
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
private fun WaterCanIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val capW = w * 0.38f; val capH = h * 0.10f
        val capX = (w - capW) / 2f
        val bodyW = w * 0.58f; val bodyX = (w - bodyW) / 2f
        val bodyTop = capH + h * 0.04f
        val bodyH = h - bodyTop - h * 0.04f
        val rad = CornerRadius(6.dp.toPx()); val bodyRad = CornerRadius(8.dp.toPx())

        drawRoundRect(Color(0xFF8FBDB8), Offset(capX, h * 0.02f), Size(capW, capH), rad)
        drawRoundRect(Color(0xFFCCDEDB), Offset(bodyX, bodyTop), Size(bodyW, bodyH), bodyRad)
        val fillH = bodyH * 0.62f
        drawRoundRect(Color(0xFF9DC5C0), Offset(bodyX, bodyTop + bodyH - fillH), Size(bodyW, fillH), bodyRad)
        drawRect(Color(0xFFB3D0CC), Offset(bodyX, bodyTop + bodyH * 0.15f), Size(bodyW, bodyH * 0.22f))
        drawArc(
            color      = Color(0xFF8FBDB8),
            startAngle = -90f, sweepAngle = 180f, useCenter = false,
            topLeft    = Offset(bodyX + bodyW - 2.dp.toPx(), bodyTop + bodyH * 0.25f),
            size       = Size(w * 0.20f, bodyH * 0.32f),
            style      = Stroke(width = 2.5.dp.toPx()),
        )
    }
}

// Portable 2-decimal formatter — avoids String.format() which is JVM-only
private val Double.fmt2: String get() {
    val scaled = (this * 100).toLong()
    val whole  = scaled / 100
    val frac   = (scaled % 100).toInt().let { if (it < 0) -it else it }
    return "$whole.${frac.toString().padStart(2, '0')}"
}
