package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.unitsPerCase
import com.example.ruwia.theme.RuwiaColor

@Composable
fun EmployeeStockScreen(
    shopStocks: List<ShopStockInfo>,
    products: List<ProductCategory>,
    movements: List<StockMovement> = emptyList(),
    isLoading: Boolean,
    onRefresh: () -> Unit,
    shopName: String = "",
    contentPadding: PaddingValues = PaddingValues(),
) {
    val visibleShops = remember(shopStocks, shopName) {
        if (shopName.isBlank()) shopStocks
        else {
            val key = shopMatchKey(shopName)
            shopStocks.filter { shopMatchKey(it.name) == key }.ifEmpty { shopStocks }
        }
    }
    val visibleMovements = remember(movements, shopName) {
        if (shopName.isBlank()) movements
        else {
            val key = shopMatchKey(shopName)
            movements.filter { shopMatchKey(it.shopName) == key }
        }
    }

    val stockItems = remember(products) {
        products.map { com.example.ruwia.domain.StockItem(it.id, it.name, it.stockAvailable, 0) }
    }
    val derivedShops = remember(visibleShops, visibleMovements, stockItems) {
        deriveShopStockTotals(visibleShops, visibleMovements, stockItems)
    }

    val cleanShop = shopName.trim().lowercase().substringBefore("·").trim()
    val isMainShop = cleanShop.startsWith("shop 1") ||
                     cleanShop.contains("main") ||
                     cleanShop.contains("warehouse") ||
                     cleanShop.contains("primary") ||
                     cleanShop.isBlank()

    val availableUnitsMap = remember(visibleMovements, products, shopName) {
        products.associate { product ->
            if (isMainShop) {
                product.id to product.stockAvailable
            } else {
                val rows = visibleMovements.filter { it.productId == product.id }
                val inward  = rows.filter { it.type == "inward"  && !it.source.trim().startsWith("Empty cans", ignoreCase = true) }.sumOf { it.qty }
                val outward = rows.filter { it.type == "outward" }.sumOf { it.qty }
                product.id to (inward - outward).coerceAtLeast(0)
            }
        }
    }

    val totalFull  = derivedShops.sumOf { it.fullCans }
    val totalEmpty = derivedShops.sumOf { it.emptyCans }
    val totalCust  = derivedShops.sumOf { it.cansWithCustomers }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RuwiaColor.Background),
        contentPadding = PaddingValues(
            top    = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { StockTabTopBar(onRefresh = onRefresh, shopName = shopName) }

        // ── Top Summary Card ──────────────────────────
        item {
            TodayStockCard(
                available    = totalFull,
                empty        = totalEmpty,
                withCustomer = totalCust,
                modifier     = Modifier.padding(horizontal = 20.dp)
            )
        }

        // ── Loading state ─────────────────────────────
        if (isLoading && products.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = RuwiaColor.TealPrimary, modifier = Modifier.size(28.dp))
                }
            }
        }

        // ── Product List ──────────────────────────────
        if (products.isNotEmpty()) {
            item {
                Text(
                    text = "PRODUCT CATALOG",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = RuwiaColor.TealPrimary
                )
            }

            items(products) { product ->
                val availUnits = availableUnitsMap[product.id] ?: product.stockAvailable
                ProductStockCard(
                    product = product,
                    availableUnits = availUnits,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        // ── Empty state if nothing loaded at all ────────
        if (derivedShops.isEmpty() && products.isEmpty() && !isLoading) {
            item {
                StockEmptyState(modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp))
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun StockTabTopBar(onRefresh: () -> Unit, shopName: String = "") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Stock",
                fontSize   = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = RuwiaColor.TextPrimary,
            )
            Text(
                if (shopName.isNotBlank())
                    "Live inventory for $shopName"
                else
                    "Live inventory across all shops",
                fontSize = 12.sp,
                color    = RuwiaColor.TextMuted,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(1.2.dp, RuwiaColor.Divider, CircleShape)
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint     = RuwiaColor.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Product Card ──────────────────────────────────────────────────────────────

@Composable
private fun ProductStockCard(
    product: ProductCategory,
    availableUnits: Int,
    modifier: Modifier = Modifier
) {
    val upc = product.unitsPerCase.coerceAtLeast(1)
    val isCan = upc == 1
    val displayStock = if (isCan) availableUnits else availableUnits / upc
    
    // Status Badge colors and labels
    val (statusLabel, badgeBg, badgeFg) = when {
        availableUnits <= 0 -> Triple("Out Of Stock", Color(0xFFFFE8E8), Color(0xFFCC3333))
        availableUnits <= (2 * upc) -> Triple("Low Stock", Color(0xFFFFF3E0), Color(0xFFE65100))
        else -> Triple("In Stock", RuwiaColor.TealExtraLight, RuwiaColor.TealPrimary)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RuwiaColor.Surface)
            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.displayName.ifBlank { product.name },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = RuwiaColor.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                val unitLabel = if (isCan) "Can" else "Case"
                Text(
                    text = "₹${product.defaultSellPrice.toInt()} / $unitLabel  ·  $displayStock $unitLabel${if (displayStock != 1) "s" else ""} Available",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = RuwiaColor.TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeFg
                )
            }
        }
    }
}

// ── Today's Stock Card ────────────────────────────────────────────────────────

@Composable
private fun TodayStockCard(
    available: Double,
    empty: Double,
    withCustomer: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RuwiaColor.TealExtraLight)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StockItemCol(
                value = "${formatCases(available)} Cans",
                label = "Available Stock",
                modifier = Modifier.weight(1f)
            )
            StockItemDivider()
            StockItemCol(
                value = formatCases(empty),
                label = "Empty Cans",
                modifier = Modifier.weight(1f)
            )
            StockItemDivider()
            StockItemCol(
                value = formatCases(withCustomer),
                label = "With Customer",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StockItemCol(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = RuwiaColor.TealPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = RuwiaColor.TextSecondary
        )
    }
}

@Composable
private fun StockItemDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(RuwiaColor.Divider)
    )
}

// ── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun StockEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(RuwiaColor.TealExtraLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Inventory2,
                contentDescription = null,
                tint = RuwiaColor.TealPrimary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Stock not configured yet",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = RuwiaColor.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Ask the admin to add shops and products. Once they do, live stock balances will show up here.",
            fontSize  = 13.sp,
            color     = RuwiaColor.TextMuted,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 12.dp),
            lineHeight = 18.sp,
        )
    }
}

/** Format a Double case count: whole numbers as "5", fractions as "5.5". */
private fun formatCases(value: Double): String {
    val floored = kotlin.math.floor(value)
    return if (value == floored && !value.isInfinite()) {
        floored.toInt().toString()
    } else {
        val tenths = kotlin.math.round(value * 10).toInt()
        "${tenths / 10}.${tenths % 10}"
    }
}
