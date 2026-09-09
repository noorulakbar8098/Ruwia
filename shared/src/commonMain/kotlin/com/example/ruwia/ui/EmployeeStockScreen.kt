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
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.deriveShopStockTotals
import com.example.ruwia.domain.isEmptyCansSource
import com.example.ruwia.domain.shopMatchKey
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp

@Composable
fun EmployeeStockScreen(
    shopStocks: List<ShopStockInfo>,
    products: List<ProductCategory>,
    movements: List<StockMovement> = emptyList(),
    isLoading: Boolean,
    onRefresh: () -> Unit,
    shopName: String = "",
    customerCount: Int = 0,
    emptyCansBaseline: Int = 0,
    contentPadding: PaddingValues = PaddingValues(),
) {
    // All shops for this business (deduplicated by their canonical key).
    val allShops = remember(shopStocks) {
        shopStocks.distinctBy { shopMatchKey(it.name) }
    }

    // Shop selection state: empty means "All shops".
    var selectedShop by remember { mutableStateOf("") }
    val selectedShopKey = remember(selectedShop) {
        if (selectedShop.isBlank()) "" else shopMatchKey(selectedShop)
    }

    // Filter the movement set that backs every number below.
    val scopedMovements = remember(selectedShopKey, movements) {
        if (selectedShopKey.isBlank()) movements
        else movements.filter { shopMatchKey(it.shopName) == selectedShopKey }
    }

    val stockItems = remember(products) {
        products.map { com.example.ruwia.domain.StockItem(it.id, it.name, it.stockAvailable, 0) }
    }
    val derivedShops = remember(shopStocks, movements, stockItems, emptyCansBaseline) {
        deriveShopStockTotals(shopStocks, movements, stockItems, emptyCansBaseline.toDouble())
    }

    // Per selected shop, each product's net on-hand over its own movements.
    // Counts are shop-specific: a product created/stocked in another shop is
    // not included here (no global fallback, so counts never leak across shops).
    val availableUnitsMap = remember(products, scopedMovements) {
        products.associate { p ->
            val rows = scopedMovements.filter { it.productId == p.id }
            val inward = rows.filter { it.type == "inward" && !it.source.isEmptyCansSource() }.sumOf { it.qty }
            val outward = rows.filter { it.type == "outward" }.sumOf { it.qty }
            p.id to (inward - outward).coerceAtLeast(0)
        }
    }

    val totalFull = remember(availableUnitsMap) {
        availableUnitsMap.values.sumOf { it.toDouble() }
    }
    // Live "Empty Cases" is reported relative to the admin-set reset baseline.
    val totalEmpty = remember(scopedMovements, emptyCansBaseline) {
        (scopedMovements
            .filter { it.source.isEmptyCansSource() }
            .sumOf { m ->
                if (m.type == "inward") m.qty.toDouble() else -m.qty.toDouble()
            }
            .coerceAtLeast(0.0) - emptyCansBaseline)
            .coerceAtLeast(0.0)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NTColors.Background),
        contentPadding = PaddingValues(
            top    = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { StockTabTopBar(onRefresh = onRefresh, shopName = shopName) }

        // ── Shop toggle ────────────────────────────────
        if (allShops.isNotEmpty()) {
            item {
                ShopToggleRow(
                    shops        = allShops,
                    selectedShop = selectedShop,
                    onSelect     = { shop -> selectedShop = shop },
                    modifier     = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        // ── Top Summary Card ──────────────────────────
        item {
            TodayStockCard(
                available     = totalFull,
                empty         = totalEmpty,
                customerCount = customerCount,
                modifier      = Modifier.padding(horizontal = 20.dp)
            )
        }

        // ── Loading state ─────────────────────────────
        if (isLoading && products.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NTColors.Primary, modifier = Modifier.size(28.dp))
                }
            }
        }

        // ── Product List ──────────────────────────────
        if (products.isNotEmpty()) {
            item {
                Text(
                    text = if (selectedShopKey.isBlank()) "PRODUCT CATALOG (ALL SHOPS)" else "PRODUCT CATALOG · ${selectedShop.uppercase()}",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = NTColors.Primary
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

// ── Shop toggle ───────────────────────────────────────────────────────────────

@Composable
private fun ShopToggleRow(
    shops: List<ShopStockInfo>,
    selectedShop: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = buildList {
        add("")
        addAll(shops.map { it.name })
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = if (option.isBlank()) selectedShop.isBlank() else shopMatchKey(option) == shopMatchKey(selectedShop)
            val label = if (option.isBlank()) "All" else option
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) NTColors.Primary else NTColors.Surface)
                    .border(1.dp, if (isSelected) NTColors.Primary else NTColors.Divider, RoundedCornerShape(50))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else NTColors.TextSecondary,
                )
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
                color      = NTColors.TextPrimary,
            )
            Text(
                if (shopName.isNotBlank())
                    "Live inventory for $shopName"
                else
                    "Live inventory across all shops",
                fontSize = 12.sp,
                color    = NTColors.TextTertiary,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(1.2.dp, NTColors.Divider, CircleShape)
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint     = NTColors.TextSecondary,
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
    val displayStock = availableUnits
    
    // Status Badge colors and labels
    val (statusLabel, badgeBg, badgeFg) = when {
        availableUnits <= 0 -> Triple("Out Of Stock", NTColors.ErrorLight, NTColors.ErrorText)
        availableUnits <= 2 -> Triple("Low Stock", NTColors.WarningLight, NTColors.WarningText)
        else -> Triple("In Stock", NTColors.PrimaryLight, NTColors.Primary)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NTDp.radXxl))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Divider, RoundedCornerShape(NTDp.radXxl))
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
                    color = NTColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                val unitLabel = "Case"
                val displayPrice = product.defaultSellPrice
                Text(
                    text = "₹${displayPrice.toInt()} / $unitLabel  ·  $displayStock $unitLabel${if (displayStock != 1) "s" else ""} Available",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NTColors.TextSecondary
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
    customerCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(NTDp.radXxl), ambientColor = NTColors.GradAccent, spotColor = NTColors.GradAccent),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Live Inventory",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NTColors.Primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${formatCases(available)} Cases",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = NTColors.TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(NTColors.PrimaryLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Inventory2, null, tint = NTColors.Primary, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.6f))
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StockItemSmallCol(
                    value = formatCases(empty),
                    label = "Empty Cases",
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    color = NTColors.Warning
                )
                VerticalDivider(modifier = Modifier.height(32.dp), color = NTColors.Divider)
                StockItemSmallCol(
                    value = "$customerCount",
                    label = "Customers",
                    icon = Icons.Rounded.Group,
                    color = NTColors.Info
                )
            }
        }
    }
}

@Composable
private fun StockItemSmallCol(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
            Text(label, fontSize = 11.sp, color = NTColors.TextTertiary, fontWeight = FontWeight.Medium)
        }
    }
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
                .background(NTColors.PrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Inventory2,
                contentDescription = null,
                tint = NTColors.Primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Stock not configured yet",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = NTColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Ask the admin to add shops and products. Once they do, live stock balances will show up here.",
            fontSize  = 13.sp,
            color     = NTColors.TextTertiary,
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
