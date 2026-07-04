package com.example.ruwia.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.StockItem
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.isEmptyCansSource
import com.example.ruwia.data.getCurrentDateTimeIso
import com.example.ruwia.theme.RuwiaColor

private val fallbackShops = listOf(
    "Shop 1" to "SAIBABA",
    "Shop 2" to "RS PURAM",
)

private val skuSuggestions = listOf("20L", "2L", "1L", "500ml", "250ml")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockPurchaseScreen(
    stockItems: List<StockItem> = emptyList(),
    products: List<ProductCategory> = emptyList(),
    productToRestock: ProductCategory? = null,
    currentStock: Int = 0,
    movements: List<StockMovement> = emptyList(),
    suppliers: List<String> = emptyList(),
    shops: List<Pair<String, String>> = fallbackShops,
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
        emptyCans: Int
    ) -> Unit,
) {
    val isEditMode = productToRestock != null
    val effectiveShops = shops.ifEmpty { fallbackShops }

    var selectedShop by remember { mutableStateOf(effectiveShops.firstOrNull()?.first ?: "Shop 1") }
    var brandName by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var purchasePriceStr by remember { mutableStateOf("") }
    var sellingPriceStr by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf(1) }
    var emptyCans by remember { mutableStateOf(0) }

    // DateTime Iso
    val defaultDateTime = remember { getCurrentDateTimeIso() }
    var dateTimeIso by remember { mutableStateOf(defaultDateTime) }

    var showShopDropdown by remember { mutableStateOf(false) }
    var showProductSelectDialog by remember { mutableStateOf(false) }
    var showErrorAlert by remember { mutableStateOf<String?>(null) }

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
            
            // Dynamically calculate stock for the selected shop
            val shopKeyVal = selectedShop.split("·", limit = 2).firstOrNull()?.trim()?.lowercase() ?: selectedShop.trim().lowercase()
            val rows = movements.filter {
                val itemShopKey = it.shopName.split("·", limit = 2).firstOrNull()?.trim()?.lowercase() ?: it.shopName.trim().lowercase()
                it.productId == productToRestock.id && itemShopKey == shopKeyVal
            }
            val inward = rows.filter { it.type == "inward" && !it.source.isEmptyCansSource() }.sumOf { it.qty }
            val outward = rows.filter { it.type == "outward" }.sumOf { it.qty }
            qty = (inward - outward).coerceAtLeast(0)
        }
    }

    if (showProductSelectDialog) {
        ProductSelectDialog(
            products = products,
            onDismiss = { showProductSelectDialog = false },
            onSelect = { p ->
                brandName = p.brandName ?: ""
                sku = p.name
                purchasePriceStr = p.purchasePrice.toString()
                sellingPriceStr = p.defaultSellPrice.toString()
                showProductSelectDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RuwiaColor.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = RuwiaColor.Surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(RuwiaColor.LightGray, RoundedCornerShape(10.dp))
                            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(10.dp))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = RuwiaColor.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = if (isEditMode) "Edit Product" else "Add Inward Stock",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RuwiaColor.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(RuwiaColor.LightGray, RoundedCornerShape(10.dp))
                            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(10.dp))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = RuwiaColor.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                // Quick Picker Button
                if (products.isNotEmpty() && productToRestock == null) {
                    OutlinedButton(
                        onClick = { showProductSelectDialog = true },
                        border = BorderStroke(1.dp, RuwiaColor.TealPrimary),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RuwiaColor.TealPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pick Existing Product", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Shop Location Dropdown
                Column {
                    Text(
                        text = "Shop Location",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RuwiaColor.TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(RuwiaColor.Surface)
                            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
                            .clickable { showShopDropdown = true }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedShop,
                                color = RuwiaColor.TextPrimary,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = RuwiaColor.TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = showShopDropdown,
                            onDismissRequest = { showShopDropdown = false },
                            modifier = Modifier.background(RuwiaColor.Surface)
                        ) {
                            effectiveShops.forEach { shop ->
                                DropdownMenuItem(
                                    text = { Text(shop.first, color = RuwiaColor.TextPrimary) },
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
                Column {
                    Text(
                        text = "Brand Name",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RuwiaColor.TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = brandName,
                        onValueChange = { brandName = it },
                        placeholder = { Text("e.g. Kinley, Aquafina", color = RuwiaColor.TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RuwiaColor.TealPrimary,
                            unfocusedBorderColor = RuwiaColor.Divider,
                            focusedTextColor = RuwiaColor.TextPrimary,
                            unfocusedTextColor = RuwiaColor.TextPrimary,
                            focusedContainerColor = RuwiaColor.Surface,
                            unfocusedContainerColor = RuwiaColor.Surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // SKU / Size Input
                Column {
                    Text(
                        text = "SKU / Size",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RuwiaColor.TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { if (!isEditMode) sku = it },
                        enabled = !isEditMode,
                        placeholder = { Text("e.g. 20L, 1L, 250ml", color = RuwiaColor.TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RuwiaColor.TealPrimary,
                            unfocusedBorderColor = RuwiaColor.Divider,
                            focusedTextColor = RuwiaColor.TextPrimary,
                            unfocusedTextColor = RuwiaColor.TextPrimary,
                            focusedContainerColor = RuwiaColor.Surface,
                            unfocusedContainerColor = RuwiaColor.Surface,
                            disabledTextColor = RuwiaColor.TextMuted,
                            disabledBorderColor = RuwiaColor.Divider.copy(alpha = 0.5f),
                            disabledContainerColor = RuwiaColor.LightGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!isEditMode) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            skuSuggestions.forEach { sug ->
                                SuggestionChip(
                                    onClick = { sku = sug },
                                    label = { Text(sug, color = RuwiaColor.TextPrimary) },
                                    border = BorderStroke(1.dp, RuwiaColor.Divider),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Pricing Inputs Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Purchase Price (₹)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RuwiaColor.TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = purchasePriceStr,
                            onValueChange = { purchasePriceStr = it },
                            placeholder = { Text("0.00", color = RuwiaColor.TextMuted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RuwiaColor.TealPrimary,
                                unfocusedBorderColor = RuwiaColor.Divider,
                                focusedTextColor = RuwiaColor.TextPrimary,
                                unfocusedTextColor = RuwiaColor.TextPrimary,
                                focusedContainerColor = RuwiaColor.Surface,
                                unfocusedContainerColor = RuwiaColor.Surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Selling Price (₹)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RuwiaColor.TextSecondary
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = sellingPriceStr,
                            onValueChange = { sellingPriceStr = it },
                            placeholder = { Text("0.00", color = RuwiaColor.TextMuted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RuwiaColor.TealPrimary,
                                unfocusedBorderColor = RuwiaColor.Divider,
                                focusedTextColor = RuwiaColor.TextPrimary,
                                unfocusedTextColor = RuwiaColor.TextPrimary,
                                focusedContainerColor = RuwiaColor.Surface,
                                unfocusedContainerColor = RuwiaColor.Surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Quantity Select Stepper Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RuwiaColor.Surface)
                        .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Quantity",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = RuwiaColor.TextPrimary
                        )
                        Text(
                            text = "Number of cases/units",
                            fontSize = 11.sp,
                            color = RuwiaColor.TextMuted
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = { if (qty > 1) qty-- },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RuwiaColor.LightGray)
                        ) {
                            Icon(Icons.Rounded.Remove, null, tint = RuwiaColor.TextPrimary)
                        }
                        Text(
                            text = qty.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RuwiaColor.TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(36.dp)
                        )
                        IconButton(
                            onClick = { qty++ },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RuwiaColor.LightGray)
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = RuwiaColor.TextPrimary)
                        }
                    }
                }

                // Empty Cans Collection Row (hidden in edit mode)
                if (!isEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RuwiaColor.Surface)
                            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Empty Cans Returned",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RuwiaColor.TextPrimary
                            )
                            Text(
                                text = "Record collected empty cans",
                                fontSize = 11.sp,
                                color = RuwiaColor.TextMuted
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconButton(
                                onClick = { if (emptyCans > 0) emptyCans-- },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RuwiaColor.LightGray)
                            ) {
                                Icon(Icons.Rounded.Remove, null, tint = RuwiaColor.TextPrimary)
                            }
                            Text(
                                text = emptyCans.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RuwiaColor.TextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(36.dp)
                            )
                            IconButton(
                                onClick = { emptyCans++ },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RuwiaColor.LightGray)
                            ) {
                                Icon(Icons.Rounded.Add, null, tint = RuwiaColor.TextPrimary)
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar
            Surface(
                color = RuwiaColor.Surface,
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
                            onSave(
                                productToRestock?.id,
                                sku,
                                brandName,
                                purchasePrice,
                                sellingPrice,
                                qty,
                                selectedShop,
                                dateTimeIso,
                                emptyCans
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RuwiaColor.TealPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(if (isEditMode) "Update Product" else "Save Stock Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Error Alert Dialog
        if (showErrorAlert != null) {
            AlertDialog(
                onDismissRequest = { showErrorAlert = null },
                title = { Text("Validation Error", fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary) },
                text = { Text(showErrorAlert ?: "An unknown error occurred.", color = RuwiaColor.TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = { showErrorAlert = null },
                        colors = ButtonDefaults.buttonColors(containerColor = RuwiaColor.TealPrimary)
                    ) {
                        Text("OK", color = Color.White)
                    }
                },
                containerColor = RuwiaColor.Surface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun ProductSelectDialog(
    products: List<ProductCategory>,
    onDismiss: () -> Unit,
    onSelect: (ProductCategory) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = RuwiaColor.Surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            Column {
                Text(
                    text = "Select a Product",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(20.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(products) { product ->
                        ListItem(
                            headlineContent = { Text(product.displayName, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("${product.brandName} • ${product.name}", color = RuwiaColor.TextSecondary) },
                            modifier = Modifier.clickable { onSelect(product) }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}