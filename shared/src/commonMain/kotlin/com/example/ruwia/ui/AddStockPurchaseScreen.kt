package com.example.ruwia.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.StockItem
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.netStockPerProductInShop
import com.example.ruwia.domain.shopMatchKey
import com.example.ruwia.data.getCurrentDateTimeIso
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.util.capitalizeWords
import com.example.ruwia.util.isFutureTimestamp
import com.example.ruwia.util.isoToDisplayDate
import com.example.ruwia.util.isoToDisplayTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

// Deep teal shared with the home Business Overview card.
private val FormTeal = Color(0xFF0F2E2C)

private val skuSuggestions = listOf("20L", "2L", "1L", "500ml", "250ml")
private const val DEFAULT_LOW_STOCK_ALERT = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockPurchaseScreen(
    stockItems: List<StockItem> = emptyList(),
    products: List<ProductCategory> = emptyList(),
    productToRestock: ProductCategory? = null,
    currentStock: Int = 0,
    movements: List<StockMovement> = emptyList(),
    suppliers: List<String> = emptyList(),
    shops: List<Pair<String, String>> = emptyList(),
    onBack: () -> Unit,
    onClose: () -> Unit,
    isEmployee: Boolean = false,
    onSave: (
        productId: String?,
        sku: String,
        brandName: String,
        purchasePrice: Double,
        sellingPrice: Double,
        qty: Int,
        shopName: String,
        dateTimeIso: String,
        emptyCans: Int,
        lowStockAlert: Int,
        notes: String
    ) -> Unit,
) {
    val isEditMode = productToRestock != null
    val effectiveShops = shops
    val shopNameFromState = effectiveShops.firstOrNull()?.first ?: ""
    var selectedShop by remember { mutableStateOf(shopNameFromState) }
    var brandName by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var purchasePriceStr by remember { mutableStateOf("") }
    var sellingPriceStr by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf(1) }
    var emptyCans by remember { mutableStateOf(0) }
    var notes by remember { mutableStateOf("") }
    val skuFocus = remember { FocusRequester() }

    // Low-stock alert: ON/OFF toggle + numeric threshold (default 5).
    var alertEnabled by remember { mutableStateOf(true) }
    var alertThresholdText by remember { mutableStateOf(DEFAULT_LOW_STOCK_ALERT.toString()) }

    // DateTime Iso
    val defaultDateTime = remember { getCurrentDateTimeIso() }
    var dateTimeIso by remember { mutableStateOf(defaultDateTime) }
    var showEntryDatePicker by remember { mutableStateOf(false) }
    var showEntryTimePicker by remember { mutableStateOf(false) }

    var showShopDropdown by remember { mutableStateOf(false) }
    var showErrorAlert by remember { mutableStateOf<String?>(null) }

    // Apply a picked date/time as the entry timestamp. Past and present are
    // kept; future values are rejected with an error (never silently clamped).
    fun applyEntryDateTime(dateDisplay: String, timeDisplay: String) {
        val iso = buildEntryIsoOrNull(dateDisplay, timeDisplay)
        if (iso == null) {
            showErrorAlert = "Could not understand that date/time. Please pick again."
            return
        }
        if (isFutureTimestamp(iso)) {
            showErrorAlert = "Future date/time is not allowed. Pick today or a past date/time."
            return
        }
        dateTimeIso = iso
    }

    LaunchedEffect(productToRestock) {
        if (productToRestock != null) {
            val assigned = productToRestock.supplierGroup.trim()
            if (assigned.isNotBlank() && assigned != "GC") {
                selectedShop = assigned
            }
        }
    }

    LaunchedEffect(productToRestock, selectedShop, movements) {
        if (productToRestock != null) {
            brandName = productToRestock.brandName ?: ""
            sku = productToRestock.name
            purchasePriceStr = productToRestock.purchasePrice.toString()
            sellingPriceStr = productToRestock.defaultSellPrice.toString()

            // Pre-fill the product's own alert threshold (OFF when saved as 0).
            val saved = productToRestock.lowStockAlert.coerceIn(0, 999)
            alertEnabled = saved > 0
            alertThresholdText = if (saved > 0) saved.toString() else DEFAULT_LOW_STOCK_ALERT.toString()
            
            // Dynamically calculate stock for the selected shop
            qty = netStockPerProductInShop(movements, shopMatchKey(selectedShop))[productToRestock.id] ?: 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NTColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Light header: back · title + subtitle · scan action.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NTColors.Surface)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NTColors.SurfaceVar)
                        .border(1.dp, NTColors.Border, RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = "Back", onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = NTColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditMode) "Edit Product" else "Add Inward Stock",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        color = NTColors.TextPrimary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Add new stock or select an existing product",
                        fontSize = 12.sp,
                        color = NTColors.TextSecondary,
                        maxLines = 1,
                    )
                }
            }

            // Scrollable Form Fields
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Shop Location Dropdown
                Column {
                    FormLabel("Shop Location")
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NTColors.Surface)
                            .border(1.dp, NTColors.Border, RoundedCornerShape(14.dp))
                            .clickable(onClickLabel = "Select shop", onClick = { showShopDropdown = true })
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = FormTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = selectedShop.ifBlank { "Select shop" },
                                    color = if (selectedShop.isBlank()) NTColors.TextTertiary else NTColors.TextPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Dropdown",
                                tint = NTColors.TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = showShopDropdown,
                            onDismissRequest = { showShopDropdown = false },
                            modifier = Modifier.background(NTColors.Surface)
                        ) {
                            effectiveShops.forEach { shop ->
                                DropdownMenuItem(
                                    text = { Text(shop.first, color = NTColors.TextPrimary) },
                                    onClick = {
                                        selectedShop = shop.first
                                        showShopDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Brand Name Input
                FormLabel("Brand Name")
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = capitalizeWords(it) },
                    placeholder = { Text("e.g. Kinley, Aquafina", color = NTColors.TextTertiary) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Store,
                            contentDescription = null,
                            tint = NTColors.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FormTeal,
                        unfocusedBorderColor = NTColors.Border,
                        focusedTextColor = NTColors.TextPrimary,
                        unfocusedTextColor = NTColors.TextPrimary,
                        focusedContainerColor = NTColors.Surface,
                        unfocusedContainerColor = NTColors.Surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // SKU / Size Input
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        FormLabel("SKU / Size")
                        Spacer(Modifier.weight(1f))
                        if (!isEditMode) {
                            Text(
                                text = "Quick Select",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FormTeal,
                                modifier = Modifier.clickable(
                                    onClickLabel = "Focus SKU field",
                                    onClick = { skuFocus.requestFocus() }
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { if (!isEditMode) sku = it },
                        enabled = !isEditMode,
                        placeholder = { Text("e.g. 20L, 1L, 250ml", color = NTColors.TextTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FormTeal,
                            unfocusedBorderColor = NTColors.Border,
                            focusedTextColor = NTColors.TextPrimary,
                            unfocusedTextColor = NTColors.TextPrimary,
                            focusedContainerColor = NTColors.Surface,
                            unfocusedContainerColor = NTColors.Surface,
                            disabledTextColor = NTColors.TextTertiary,
                            disabledBorderColor = NTColors.Border.copy(alpha = 0.5f),
                            disabledContainerColor = NTColors.SurfaceVar
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(skuFocus)
                    )
                    if (!isEditMode) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            skuSuggestions.forEach { sug ->
                                val isSelected = sku.equals(sug, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) FormTeal else NTColors.SurfaceVar)
                                        .border(
                                            1.dp,
                                            if (isSelected) FormTeal else NTColors.Border,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(
                                            onClickLabel = "Use size $sug",
                                            onClick = { sku = sug }
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        sug,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else NTColors.TextPrimary,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }

                // Pricing Inputs Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        FormLabel("Purchase Price (₹)")
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = purchasePriceStr,
                            onValueChange = { purchasePriceStr = it },
                            placeholder = { Text("0.00", color = NTColors.TextTertiary) },
                            prefix = { Text("₹ ", color = NTColors.TextSecondary, fontWeight = FontWeight.Bold) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FormTeal,
                                unfocusedBorderColor = NTColors.Border,
                                focusedTextColor = NTColors.TextPrimary,
                                unfocusedTextColor = NTColors.TextPrimary,
                                focusedContainerColor = NTColors.Surface,
                                unfocusedContainerColor = NTColors.Surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        FormLabel("Selling Price (₹)")
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = sellingPriceStr,
                            onValueChange = { sellingPriceStr = it },
                            placeholder = { Text("0.00", color = NTColors.TextTertiary) },
                            prefix = { Text("₹ ", color = NTColors.TextSecondary, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(14.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FormTeal,
                                unfocusedBorderColor = NTColors.Border,
                                focusedTextColor = NTColors.TextPrimary,
                                unfocusedTextColor = NTColors.TextPrimary,
                                focusedContainerColor = NTColors.Surface,
                                unfocusedContainerColor = NTColors.Surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Low Stock Alert — ON/OFF toggle + compact numeric threshold.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NTColors.Surface)
                        .border(1.dp, NTColors.Border, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FormTeal.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = FormTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Low Stock Alert",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NTColors.TextPrimary
                            )
                            Text(
                                text = "Get notified when inventory is running low",
                                fontSize = 12.sp,
                                color = NTColors.TextSecondary
                            )
                        }
                        Switch(
                            checked = alertEnabled,
                            onCheckedChange = { alertEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FormTeal,
                                uncheckedThumbColor = NTColors.TextTertiary,
                                uncheckedTrackColor = NTColors.Border,
                                uncheckedBorderColor = Color.Transparent,
                            )
                        )
                    }
                    if (alertEnabled) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Alert threshold",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NTColors.TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = alertThresholdText,
                                onValueChange = { alertThresholdText = it.filter { c -> c.isDigit() }.take(3) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FormTeal,
                                    unfocusedBorderColor = NTColors.Border,
                                    focusedTextColor = NTColors.TextPrimary,
                                    unfocusedTextColor = NTColors.TextPrimary,
                                    focusedContainerColor = NTColors.SurfaceVar,
                                    unfocusedContainerColor = NTColors.SurfaceVar
                                ),
                                modifier = Modifier.width(96.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "cases/units",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = NTColors.TextSecondary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "You’ll be notified when available stock reaches this level.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = NTColors.TextTertiary
                        )
                    }
                }

                // Quantity card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NTColors.Surface)
                        .border(1.dp, NTColors.Border, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FormTeal.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Inventory2,
                                contentDescription = null,
                                tint = FormTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Quantity",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NTColors.TextPrimary
                            )
                            Text(
                                text = "Number of cases/units",
                                fontSize = 12.sp,
                                color = NTColors.TextSecondary
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { if (qty > 1) qty-- },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NTColors.SurfaceVar)
                        ) {
                            Icon(Icons.Rounded.Remove, null, tint = NTColors.TextPrimary)
                        }
                        Text(
                            text = qty.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NTColors.TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(min = 40.dp)
                        )
                        IconButton(
                            onClick = { qty++ },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(FormTeal)
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = Color.White)
                        }
                    }
                }

                // Transaction date & time — any past moment or now; future is
                // blocked in the pickers and re-validated on save + in the
                // data layer. The chosen stamp becomes the entry's created_at.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NTColors.Surface)
                        .border(1.dp, NTColors.Border, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(FormTeal.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = FormTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Transaction Date & Time",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NTColors.TextPrimary
                            )
                            Text(
                                text = "${isoToDisplayDate(dateTimeIso)} · ${isoToDisplayTime(dateTimeIso).ifBlank { "—" }}",
                                fontSize = 12.sp,
                                color = NTColors.TextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showEntryDatePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Rounded.CalendarToday, null, tint = FormTeal, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Date", color = NTColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { showEntryTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Rounded.Schedule, null, tint = FormTeal, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Time", color = NTColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Empty Cases card — amber accent to distinguish from quantity.
                if (!isEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NTColors.Surface)
                            .border(1.dp, NTColors.Border, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF3E8)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Undo,
                                    contentDescription = null,
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Empty Cases Returned",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NTColors.TextPrimary
                                )
                                Text(
                                    text = "Record collected empty cans",
                                    fontSize = 12.sp,
                                    color = NTColors.TextSecondary
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { if (emptyCans > 0) emptyCans-- },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(NTColors.SurfaceVar)
                            ) {
                                Icon(Icons.Rounded.Remove, null, tint = NTColors.TextPrimary)
                            }
                            Text(
                                text = emptyCans.toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NTColors.TextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(min = 40.dp)
                            )
                            IconButton(
                                onClick = { emptyCans++ },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF97316))
                            ) {
                                Icon(Icons.Rounded.Add, null, tint = Color.White)
                            }
                        }
                    }
                }

                // Notes card — optional, visually secondary (new entries only).
                if (!isEditMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NTColors.SurfaceVar)
                            .border(1.dp, NTColors.Border, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Text(
                            text = "Notes (Optional)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NTColors.TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it.take(200) },
                            placeholder = { Text("Add any additional notes…", color = NTColors.TextTertiary) },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FormTeal,
                                unfocusedBorderColor = NTColors.Border,
                                focusedTextColor = NTColors.TextPrimary,
                                unfocusedTextColor = NTColors.TextPrimary,
                                focusedContainerColor = NTColors.Surface,
                                unfocusedContainerColor = NTColors.Surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Bottom Action Bar
            Surface(
                color = NTColors.Surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    Button(
                        onClick = {
                            val purchasePrice = purchasePriceStr.toDoubleOrNull()
                            val sellingPrice = sellingPriceStr.toDoubleOrNull()
                            if (brandName.isBlank() || sku.isBlank() || purchasePrice == null || sellingPrice == null) {
                                showErrorAlert = "Please fill all fields correctly."
                                return@Button
                            }
                            // Last line of defence: a future timestamp must
                            // never be saved even if it slipped past the pickers.
                            if (isFutureTimestamp(dateTimeIso)) {
                                showErrorAlert = "Future date/time is not allowed. Pick today or a past date/time."
                                return@Button
                            }
                            val alertLevel = if (alertEnabled) {
                                alertThresholdText.toIntOrNull()?.coerceIn(0, 999)
                                    ?: DEFAULT_LOW_STOCK_ALERT
                            } else {
                                0
                            }
                            onSave(
                                productToRestock?.id,
                                sku,
                                brandName,
                                purchasePrice,
                                sellingPrice,
                                qty,
                                selectedShop,
                                dateTimeIso,
                                emptyCans,
                                alertLevel,
                                notes.trim()
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(FormTeal)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.16f),
                                        Color.White.copy(alpha = 0.04f),
                                        Color.Transparent,
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Save,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isEditMode) "Update Product" else "Save Stock Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Entry date picker — today and past only.
        if (showEntryDatePicker) {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = runCatching { kotlinx.datetime.Instant.parse(dateTimeIso).toEpochMilliseconds() }.getOrNull(),
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val picked = kotlinx.datetime.Instant.fromEpochMilliseconds(utcTimeMillis)
                            .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
                        val today = kotlin.time.Clock.System.now()
                            .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
                        return picked <= today
                    }
                }
            )
            DatePickerDialog(
                onDismissRequest = { showEntryDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val d = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
                            val dd = d.dayOfMonth.toString().padStart(2, '0')
                            val mm = d.monthNumber.toString().padStart(2, '0')
                            applyEntryDateTime(
                                "$dd/$mm/${d.year}",
                                isoToDisplayTime(dateTimeIso).ifBlank { "12:00 AM" }
                            )
                        }
                        showEntryDatePicker = false
                    }) { Text("OK", color = FormTeal, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showEntryDatePicker = false }) {
                        Text("Cancel", color = NTColors.TextSecondary)
                    }
                }
            ) {
                DatePicker(state = pickerState)
            }
        }

        // Entry time picker — combined with the chosen date; future rejected.
        if (showEntryTimePicker) {
            val nowLocal = runCatching {
                kotlinx.datetime.Instant.parse(dateTimeIso)
                    .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            }.getOrNull()
            val timeState = rememberTimePickerState(
                initialHour = nowLocal?.hour ?: 12,
                initialMinute = nowLocal?.minute ?: 0,
                is24Hour = false,
            )
            AlertDialog(
                onDismissRequest = { showEntryTimePicker = false },
                title = { Text("Entry time", fontWeight = FontWeight.Bold, color = NTColors.TextPrimary) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TimePicker(state = timeState)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val h24 = timeState.hour
                        val hh = when {
                            h24 == 0 -> 12
                            h24 > 12 -> h24 - 12
                            else -> h24
                        }
                        val ampm = if (h24 >= 12) "PM" else "AM"
                        val mm = timeState.minute.toString().padStart(2, '0')
                        applyEntryDateTime(isoToDisplayDate(dateTimeIso), "$hh:$mm $ampm")
                        showEntryTimePicker = false
                    }) { Text("OK", color = FormTeal, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showEntryTimePicker = false }) {
                        Text("Cancel", color = NTColors.TextSecondary)
                    }
                },
                containerColor = NTColors.Surface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Error Alert Dialog
        if (showErrorAlert != null) {
            AlertDialog(
                onDismissRequest = { showErrorAlert = null },
                title = { Text("Validation Error", fontWeight = FontWeight.Bold, color = NTColors.TextPrimary) },
                text = { Text(showErrorAlert ?: "An unknown error occurred.", color = NTColors.TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = { showErrorAlert = null },
                        colors = ButtonDefaults.buttonColors(containerColor = FormTeal)
                    ) {
                        Text("OK", color = Color.White)
                    }
                },
                containerColor = NTColors.Surface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
        color = NTColors.TextSecondary
    )
}

/**
 * Builds a TZ-aware ISO instant from a "DD/MM/YYYY" date and a "h:mm AM/PM"
 * time at the device zone. Null when unparseable. Does NOT clamp — callers
 * reject future results via [isFutureTimestamp].
 */
private fun buildEntryIsoOrNull(dateDisplay: String, timeDisplay: String): String? {
    return try {
        val dp = dateDisplay.trim().split("/")
        if (dp.size != 3) return null
        val d = dp[0].toIntOrNull() ?: return null
        val m = dp[1].toIntOrNull() ?: return null
        val y = dp[2].toIntOrNull() ?: return null
        val tp = timeDisplay.trim().split(" ", limit = 2)
        if (tp.size != 2) return null
        val hm = tp[0].split(":")
        if (hm.size != 2) return null
        var h = hm[0].toIntOrNull() ?: return null
        val min = hm[1].toIntOrNull() ?: return null
        val ampm = tp[1].trim().uppercase()
        if (ampm != "AM" && ampm != "PM") return null
        if (h !in 1..12 || min !in 0..59 || m !in 1..12 || d !in 1..31) return null
        h = when {
            ampm == "AM" && h == 12 -> 0
            ampm == "PM" && h < 12 -> h + 12
            else -> h
        }
        kotlinx.datetime.LocalDateTime(y, m, d, h, min, 0, 0)
            .toInstant(kotlinx.datetime.TimeZone.currentSystemDefault())
            .toString()
    } catch (_: Exception) {
        null
    }
}
