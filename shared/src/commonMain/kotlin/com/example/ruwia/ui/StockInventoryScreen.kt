package com.example.ruwia.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.unitsPerCase
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ruwia.shared.generated.resources.*
import kotlin.math.*
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private object SaaSColors {
    val Primary       get() = NTColors.Primary
    val PrimaryDark   get() = NTColors.PrimaryDark
    val PrimaryLight  get() = NTColors.PrimaryLight
    val Background    get() = NTColors.Background
    val Surface       get() = NTColors.Surface
    val SurfaceVar    get() = NTColors.SurfaceVar
    val TextPrimary   get() = NTColors.TextPrimary
    val TextSecondary get() = NTColors.TextSecondary
    val TextMuted     get() = NTColors.TextTertiary
    val Border        get() = NTColors.Border
    val CardShadow    get() = if (NTColors.isDarkMode) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f)

    // Status colors
    val Healthy       get() = NTColors.Success
    val HealthyLight  get() = NTColors.SuccessLight
    val LowStock      get() = NTColors.Warning
    val LowStockLight get() = NTColors.WarningLight
    val OutOfStock    get() = NTColors.Error
    val OutOfStockLight get() = NTColors.ErrorLight
    val Critical      get() = NTColors.Error
    val CriticalLight get() = NTColors.ErrorLight
}

private enum class InvFilter { ALL, HEALTHY, LOW, CRITICAL, OUT }
private enum class InvSort { NAME, QTY_DESC, QTY_ASC, VALUE_DESC }

@Composable
fun StockInventoryScreen(
    state: AdminState,
    onBack: () -> Unit,
    onAddMovement: (source: String, qty: Int, type: String, shopName: String, productId: String?) -> Unit = { _, _, _, _, _ -> },
    onAddStock: () -> Unit = {},
    onAddProduct: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val products = state.productCategories
    val movements = state.recentMovements
    val shops = state.shopStocks

    var selectedShopIndex by remember { mutableStateOf(0) } // 0 = All Shops, i = shops[i-1]
    
    val selectedShop = if (selectedShopIndex == 0) null else shops[selectedShopIndex - 1]
    val selectedShopName = selectedShop?.name ?: "All Shops"
    val selectedShopLocation = selectedShop?.location ?: "Combined Inventory"
    val currentShopKey = selectedShop?.let { shopKey(it.name) }

    // ── Live inventory counts filtered by shop ────────────────────────────────
    val liveStockMap = remember(movements, products, currentShopKey) {
        products.associate { p ->
            val rows = movements.filter {
                it.productId == p.id && (currentShopKey == null || shopKey(it.shopName) == currentShopKey)
            }
            val inward = rows.filter { it.type == "inward" && !it.source.trim().startsWith("Empty cans", ignoreCase = true) }.sumOf { it.qty }
            val outward = rows.filter { it.type == "outward" }.sumOf { it.qty }
            p.id to (inward - outward).coerceAtLeast(0)
        }
    }

    val effectiveStockMap = remember(liveStockMap) {
        liveStockMap
    }

    // ── Metrics ──────────────────────────────────────────────────────────────
    val totalInventory = products.sumOf { p ->
        val units = effectiveStockMap[p.id] ?: 0
        val upc = p.unitsPerCase.coerceAtLeast(1)
        units.toDouble() / upc
    }
    val totalValue = products.sumOf { p ->
        (effectiveStockMap[p.id] ?: 0) * p.defaultSellPrice
    }
    val productsAvailable = products.count { (effectiveStockMap[it.id] ?: 0) > 0 }
    val lowStockCount = products.count { p ->
        val units = effectiveStockMap[p.id] ?: 0
        val upc = p.unitsPerCase.coerceAtLeast(1)
        val cases = if (upc > 1) (units.toDouble() / upc) else units.toDouble()
        cases > 0.0 && cases <= 5.0
    }

    // ── UI Control States ────────────────────────────────────────────────────
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(InvFilter.ALL) }
    var selectedSort by remember { mutableStateOf(InvSort.NAME) }
    
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    var activeTransferProduct by remember { mutableStateOf<ProductCategory?>(null) }
    var activeAdjustProduct by remember { mutableStateOf<ProductCategory?>(null) }

    // ── Filtered & Sorted products list ──────────────────────────────────────
    val filteredSortedProducts = remember(products, effectiveStockMap, searchQuery, selectedFilter, selectedSort) {
        products
            .filter { p ->
                val matchesSearch = p.displayName.contains(searchQuery, ignoreCase = true) || p.name.contains(searchQuery, ignoreCase = true)
                val units = effectiveStockMap[p.id] ?: 0
                val upc = p.unitsPerCase.coerceAtLeast(1)
                val cases = units.toDouble() / upc
                
                val matchesFilter = when (selectedFilter) {
                    InvFilter.ALL -> true
                    InvFilter.HEALTHY -> cases > 5.0
                    InvFilter.LOW -> cases > 2.0 && cases <= 5.0
                    InvFilter.CRITICAL -> cases > 0.0 && cases <= 2.0
                    InvFilter.OUT -> cases <= 0.0
                }
                matchesSearch && matchesFilter
            }
            .sortedWith { p1, p2 ->
                val u1 = effectiveStockMap[p1.id] ?: 0
                val u2 = effectiveStockMap[p2.id] ?: 0
                val upc1 = p1.unitsPerCase.coerceAtLeast(1)
                val upc2 = p2.unitsPerCase.coerceAtLeast(1)
                val c1 = u1.toDouble() / upc1
                val c2 = u2.toDouble() / upc2
                val val1 = u1 * p1.defaultSellPrice
                val val2 = u2 * p2.defaultSellPrice
                
                when (selectedSort) {
                    InvSort.NAME -> p1.displayName.compareTo(p2.displayName, ignoreCase = true)
                    InvSort.QTY_DESC -> c2.compareTo(c1)
                    InvSort.QTY_ASC -> c1.compareTo(c2)
                    InvSort.VALUE_DESC -> val2.compareTo(val1)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SaaSColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            InventoryHeader(
                shopName = selectedShopName,
                location = selectedShopLocation,
                onBack = onBack,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedFilter = selectedFilter,
                selectedSort = selectedSort,
                showFilterMenu = showFilterMenu,
                onShowFilterMenuChange = { showFilterMenu = it },
                showSortMenu = showSortMenu,
                onShowSortMenuChange = { showSortMenu = it },
                onFilterSelect = { selectedFilter = it },
                onSortSelect = { selectedSort = it }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 32.dp,
                )
            ) {
                // ── Shop Selection Tabs ───────────────────────────────────────────
                item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ShopTabChip(
                            name = "All Shops",
                            isSelected = selectedShopIndex == 0,
                            onClick = { selectedShopIndex = 0 }
                        )
                    }
                    items(shops.size) { index ->
                        val shop = shops[index]
                        ShopTabChip(
                            name = shop.name,
                            isSelected = selectedShopIndex == index + 1,
                            onClick = { selectedShopIndex = index + 1 }
                        )
                    }
                }
            }

            // ── Summary KPI Card ──────────────────────────────────────────────
            item {
                Spacer(Modifier.height(20.dp))
                ShopSummaryCard(
                    totalInventory = totalInventory,
                    totalValue = totalValue,
                    productsCount = productsAvailable,
                    lowStockCount = lowStockCount,
                    unitLabel = if (products.any { it.unitsPerCase > 1 }) "Cases" else "Cans"
                )
            }

            // ── Low Stock Attention Alert Section ─────────────────────────────
            val lowStockProducts = products.filter { p ->
                val units = effectiveStockMap[p.id] ?: 0
                val upc = p.unitsPerCase.coerceAtLeast(1)
                val cases = if (upc > 1) (units.toDouble() / upc) else units.toDouble()
                cases <= 5.0
            }
            if (lowStockProducts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    LowStockAttentionCard(
                        lowProducts = lowStockProducts,
                        liveStock = effectiveStockMap,
                        onRestock = onAddStock
                    )
                }
            }

            // ── Analytics Insights Section ────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                InventoryAnalyticsRow(
                    products = products,
                    liveStock = effectiveStockMap,
                    movements = movements,
                    currentShopKey = currentShopKey
                )
            }

            // ── Donut Chart Distribution Section ──────────────────────────────
            if (totalInventory > 0.0) {
                item {
                    Spacer(Modifier.height(24.dp))
                    InventoryDistributionCard(
                        products = products,
                        liveStock = effectiveStockMap,
                        totalInventory = totalInventory
                    )
                }
            }

            // ── Product Breakdown List ────────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "PRODUCT BREAKDOWN",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = SaaSColors.Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(10.dp))
            }

            if (filteredSortedProducts.isEmpty()) {
                item {
                    EmptySearchState()
                }
            } else {
                items(filteredSortedProducts, key = { it.id }) { product ->
                    val units = effectiveStockMap[product.id] ?: 0
                    ProductBreakdownCard(
                        product = product,
                        liveUnits = units,
                        onQuickAdd = {
                            val upc = product.unitsPerCase
                            onAddMovement("Quick Adjust (Add)", upc, "inward", selectedShopName, product.id)
                        },
                        onQuickRemove = {
                            val upc = product.unitsPerCase
                            if (units >= upc) {
                                onAddMovement("Quick Adjust (Remove)", upc, "outward", selectedShopName, product.id)
                            }
                        },
                        onTransferClick = { activeTransferProduct = product },
                        onAdjustClick = { activeAdjustProduct = product }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Recent Activity Section ───────────────────────────────────────
            val shopMovements = movements.filter { currentShopKey == null || shopKey(it.shopName) == currentShopKey }.take(8)
            if (shopMovements.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "RECENT ACTIVITY",
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = SaaSColors.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    RecentActivityFeed(movements = shopMovements, products = products)
                }
            }
        }
    }

        // ── Dialog Modals ─────────────────────────────────────────────────────
        activeTransferProduct?.let { product ->
            TransferStockDialog(
                product = product,
                shops = shops,
                currentShop = selectedShop,
                liveUnits = effectiveStockMap[product.id] ?: 0,
                onDismiss = { activeTransferProduct = null },
                onConfirm = { fromShopName, toShopName, casesCount ->
                    val totalQty = casesCount * product.unitsPerCase
                    onAddMovement("Transfer to $toShopName", totalQty, "outward", fromShopName, product.id)
                    onAddMovement("Transfer from $fromShopName", totalQty, "inward", toShopName, product.id)
                    activeTransferProduct = null
                }
            )
        }

        activeAdjustProduct?.let { product ->
            val units = effectiveStockMap[product.id] ?: 0
            val upc = product.unitsPerCase
            val currentCases = if (upc > 1) units / upc else units
            AdjustStockDialog(
                product = product,
                currentCases = currentCases,
                shopName = selectedShopName,
                onDismiss = { activeAdjustProduct = null },
                onConfirm = { targetCases, reason ->
                    val diff = (targetCases - currentCases) * upc
                    if (diff > 0) {
                        onAddMovement(reason, diff, "inward", selectedShopName, product.id)
                    } else if (diff < 0) {
                        onAddMovement(reason, -diff, "outward", selectedShopName, product.id)
                    }
                    activeAdjustProduct = null
                }
            )
        }

        // ── Floating Action Button (FAB) ──────────────────────────────────
        FloatingActionButton(
            onClick = onAddProduct,
            containerColor = SaaSColors.Primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp)
                .shadow(8.dp, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add Product",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ── Shop Selector Tab ─────────────────────────────────────────────────────────
@Composable
private fun ShopTabChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(32.dp))
            .background(if (isSelected) SaaSColors.Primary else SaaSColors.Surface)
            .border(1.dp, if (isSelected) SaaSColors.Primary else SaaSColors.Border, RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.White else SaaSColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Header Component ──────────────────────────────────────────────────────────
@Composable
private fun InventoryHeader(
    shopName: String,
    location: String,
    onBack: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: InvFilter,
    selectedSort: InvSort,
    showFilterMenu: Boolean,
    onShowFilterMenuChange: (Boolean) -> Unit,
    showSortMenu: Boolean,
    onShowSortMenuChange: (Boolean) -> Unit,
    onFilterSelect: (InvFilter) -> Unit,
    onSortSelect: (InvSort) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SaaSColors.Surface)
            .statusBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        Column {
            // Top Title Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(SaaSColors.SurfaceVar, RoundedCornerShape(10.dp))
                            .border(1.dp, SaaSColors.Border, RoundedCornerShape(10.dp))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = SaaSColors.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = shopName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SaaSColors.TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = location,
                                fontSize = 12.sp,
                                color = SaaSColors.TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(SaaSColors.TextMuted, CircleShape)
                            )
                            Text(
                                text = "Last updated 2m ago",
                                fontSize = 12.sp,
                                color = SaaSColors.TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Live Status Indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(SaaSColors.HealthyLight)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(SaaSColors.Healthy, CircleShape)
                        )
                        Text(
                            text = "Live",
                            color = SaaSColors.Healthy,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Search & Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input Box
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SaaSColors.SurfaceVar)
                        .border(1.dp, SaaSColors.Border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = SaaSColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search product...",
                                color = SaaSColors.TextMuted,
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            textStyle = TextStyle(
                                color = SaaSColors.TextPrimary,
                                fontSize = 14.sp
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = SaaSColors.TextMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onSearchQueryChange("") }
                        )
                    }
                }

                // Filter Dropdown Anchor
                Box {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedFilter != InvFilter.ALL) SaaSColors.PrimaryLight else SaaSColors.SurfaceVar)
                            .border(1.dp, if (selectedFilter != InvFilter.ALL) SaaSColors.Primary else SaaSColors.Border, RoundedCornerShape(12.dp))
                            .clickable { onShowFilterMenuChange(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "Filter",
                            tint = if (selectedFilter != InvFilter.ALL) SaaSColors.Primary else SaaSColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { onShowFilterMenuChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Products") },
                            onClick = { onFilterSelect(InvFilter.ALL); onShowFilterMenuChange(false) },
                            leadingIcon = { Icon(Icons.Rounded.Category, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Healthy Stock (>5)") },
                            onClick = { onFilterSelect(InvFilter.HEALTHY); onShowFilterMenuChange(false) },
                            leadingIcon = { Icon(Icons.Rounded.CheckCircle, null, tint = SaaSColors.Healthy) }
                        )
                        DropdownMenuItem(
                            text = { Text("Low Stock (3-5)") },
                            onClick = { onFilterSelect(InvFilter.LOW); onShowFilterMenuChange(false) },
                            leadingIcon = { Icon(Icons.Rounded.Warning, null, tint = SaaSColors.LowStock) }
                        )
                        DropdownMenuItem(
                            text = { Text("Critical (1-2)") },
                            onClick = { onFilterSelect(InvFilter.CRITICAL); onShowFilterMenuChange(false) },
                            leadingIcon = { Icon(Icons.Rounded.Dangerous, null, tint = SaaSColors.Critical) }
                        )
                        DropdownMenuItem(
                            text = { Text("Out of Stock (0)") },
                            onClick = { onFilterSelect(InvFilter.OUT); onShowFilterMenuChange(false) },
                            leadingIcon = { Icon(Icons.Rounded.Cancel, null, tint = SaaSColors.OutOfStock) }
                        )
                    }
                }

                // Sort Dropdown Anchor
                Box {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SaaSColors.SurfaceVar)
                            .border(1.dp, SaaSColors.Border, RoundedCornerShape(12.dp))
                            .clickable { onShowSortMenuChange(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = "Sort",
                            tint = SaaSColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { onShowSortMenuChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by Name") },
                            onClick = { onSortSelect(InvSort.NAME); onShowSortMenuChange(false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Quantity: High to Low") },
                            onClick = { onSortSelect(InvSort.QTY_DESC); onShowSortMenuChange(false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Quantity: Low to High") },
                            onClick = { onSortSelect(InvSort.QTY_ASC); onShowSortMenuChange(false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Value: High to Low") },
                            onClick = { onSortSelect(InvSort.VALUE_DESC); onShowSortMenuChange(false) }
                        )
                    }
                }
            }
        }
    }
}

// ── Shop Summary Card Component ──────────────────────────────────────────────
@Composable
private fun ShopSummaryCard(
    totalInventory: Double,
    totalValue: Double,
    productsCount: Int,
    lowStockCount: Int,
    unitLabel: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = SaaSColors.CardShadow, spotColor = SaaSColors.CardShadow),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SaaSColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Inventory",
                        color = SaaSColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${formatCases(totalInventory)} $unitLabel",
                        color = SaaSColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                // Sparkline Trend Widget
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, null, tint = SaaSColors.Healthy, modifier = Modifier.size(14.dp))
                        Text("12%", color = SaaSColors.Healthy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("vs last week", color = SaaSColors.TextMuted, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Canvas Mini Sparkline Graph
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                val width = size.width
                val height = size.height
                val points = listOf(0.2f, 0.4f, 0.35f, 0.6f, 0.55f, 0.8f, 0.95f) // visual upward trend
                val path = Path()
                
                points.forEachIndexed { idx, value ->
                    val x = idx * (width / (points.size - 1))
                    val y = height - (value * height * 0.8f) - (height * 0.1f)
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = SaaSColors.Primary,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw filled gradient underneath
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(SaaSColors.Primary.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = SaaSColors.Border)
            Spacer(Modifier.height(16.dp))

            // Sub Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Inventory Value", color = SaaSColors.TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("₹${formatInventoryAmount(totalValue)}", color = SaaSColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SKUs Available", color = SaaSColors.TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("$productsCount", color = SaaSColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Low Stock", color = SaaSColors.TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$lowStockCount Items",
                        color = if (lowStockCount > 0) SaaSColors.LowStock else SaaSColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Low Stock Alert Card Component ────────────────────────────────────────────
@Composable
private fun LowStockAttentionCard(
    lowProducts: List<ProductCategory>,
    liveStock: Map<String, Int>,
    onRestock: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SaaSColors.CriticalLight.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, SaaSColors.Critical.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Alert",
                    tint = SaaSColors.Critical
                )
                Text(
                    text = "Attention Required",
                    color = SaaSColors.Critical,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            lowProducts.forEach { p ->
                val units = liveStock[p.id] ?: 0
                val upc = p.unitsPerCase.coerceAtLeast(1)
                val qtyVal = if (upc > 1) units / upc else units
                val unitWord = if (upc > 1) "cases" else "cans"
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = p.displayName.ifBlank { p.name },
                        fontSize = 13.sp,
                        color = SaaSColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (qtyVal <= 0) "Out of Stock" else "$qtyVal $unitWord low",
                        fontSize = 13.sp,
                        color = SaaSColors.Critical,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRestock,
                colors = ButtonDefaults.buttonColors(containerColor = SaaSColors.Critical),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Restock Items", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── Inventory Analytics Highlights Component ──────────────────────────────────
@Composable
private fun InventoryAnalyticsRow(
    products: List<ProductCategory>,
    liveStock: Map<String, Int>,
    movements: List<StockMovement>,
    currentShopKey: String?
) {
    // 1. Top Product by Stock Qty
    val topProduct = products.maxByOrNull { liveStock[it.id] ?: 0 }
    val topUnits = topProduct?.let { liveStock[it.id] ?: 0 } ?: 0
    val topCases = topProduct?.let { if (it.unitsPerCase > 1) topUnits / it.unitsPerCase else topUnits } ?: 0
    val topLabel = topProduct?.displayName?.split(" ")?.firstOrNull() ?: "-"

    // 2. Most Moving Product
    val shopMovements = movements.filter { currentShopKey == null || shopKey(it.shopName) == currentShopKey }
    val outwardMvtGroup = shopMovements.filter { it.type == "outward" }.groupBy { it.productId }
    val mostMovingId = outwardMvtGroup.maxByOrNull { (_, mvts) -> mvts.sumOf { it.qty } }?.key
    val mostMovingProduct = products.find { it.id == mostMovingId }
    val mostMovingLabel = mostMovingProduct?.displayName?.split(" ")?.firstOrNull() ?: "20L Water"

    // 3. Low stock product
    val criticalProduct = products.filter { (liveStock[it.id] ?: 0) <= 0 }.firstOrNull() ?: products.minByOrNull { liveStock[it.id] ?: 0 }
    val criticalLabel = criticalProduct?.displayName?.split(" ")?.firstOrNull() ?: "-"

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AnalyticTile(
                title = "Top Product",
                value = "$topCases Cases",
                subtext = "$topLabel Water",
                icon = Icons.Rounded.Star,
                iconBg = Color(0xFFFFECE5),
                iconFg = Color(0xFFFF6D3B)
            )
        }
        item {
            AnalyticTile(
                title = "Most Moving",
                value = "High Sales",
                subtext = "$mostMovingLabel",
                icon = Icons.Rounded.TrendingUp,
                iconBg = Color(0xFFEBF7FF),
                iconFg = Color(0xFF1D9BF0)
            )
        }
        item {
            AnalyticTile(
                title = "Fastest Selling",
                value = "20L Can",
                subtext = "High Velocity",
                icon = Icons.Rounded.Bolt,
                iconBg = Color(0xFFFFFBE6),
                iconFg = Color(0xFFD4A700)
            )
        }
        item {
            AnalyticTile(
                title = "Needs Restock",
                value = "0 Units",
                subtext = "$criticalLabel Water",
                icon = Icons.Rounded.Warning,
                iconBg = SaaSColors.CriticalLight,
                iconFg = SaaSColors.Critical
            )
        }
    }
}

@Composable
private fun AnalyticTile(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    iconBg: Color,
    iconFg: Color
) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SaaSColors.Surface)
            .border(1.dp, SaaSColors.Border, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconFg, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = SaaSColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(value, color = SaaSColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtext, color = SaaSColors.TextSecondary, fontSize = 11.sp)
        }
    }
}

// ── Donut Chart Component ─────────────────────────────────────────────────────
@Composable
private fun InventoryDistributionCard(
    products: List<ProductCategory>,
    liveStock: Map<String, Int>,
    totalInventory: Double
) {
    val nonZeroProducts = products
        .map { p ->
            val units = liveStock[p.id] ?: 0
            val upc = p.unitsPerCase.coerceAtLeast(1)
            val cases = units.toDouble() / upc
            p to cases
        }
        .filter { it.second > 0.0 }
        .sortedByDescending { it.second }

    val chartData = remember(nonZeroProducts, totalInventory) {
        val total = totalInventory.toFloat()
        var cumPercent = 0f
        val list = mutableListOf<Triple<ProductCategory, Float, Color>>()
        val colors = listOf(
            SaaSColors.Primary,
            Color(0xFF8B5CF6),
            Color(0xFF3B82F6),
            Color(0xFFF59E0B),
            Color(0xFFEC4899),
            Color(0xFF10B981)
        )
        nonZeroProducts.forEachIndexed { index, (prod, qty) ->
            val percent = qty.toFloat() / total
            val color = colors[index % colors.size]
            list.add(Triple(prod, percent, color))
            cumPercent += percent
        }
        list
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = SaaSColors.CardShadow, spotColor = SaaSColors.CardShadow),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SaaSColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Inventory Distribution",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SaaSColors.TextPrimary
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Donut Chart Canvas
                Canvas(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(4.dp)
                ) {
                    val stroke = Stroke(width = 16.dp.toPx())
                    var startAngle = -90f
                    if (chartData.isEmpty()) {
                        drawCircle(color = SaaSColors.Border, style = stroke)
                    } else {
                        chartData.forEach { (_, fraction, color) ->
                            val sweepAngle = fraction * 360f
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = stroke
                            )
                            startAngle += sweepAngle
                        }
                    }
                }

                // Legend Details List
                Column(
                    modifier = Modifier.weight(1f).padding(start = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chartData.take(4).forEach { (prod, fraction, color) ->
                        val percentText = "${(fraction * 100).toInt()}%"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                                Text(
                                    text = prod.displayName.ifBlank { prod.name },
                                    fontSize = 12.sp,
                                    color = SaaSColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = percentText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SaaSColors.TextPrimary
                            )
                        }
                    }
                    if (chartData.size > 4) {
                        val otherPercent = ((chartData.drop(4).sumOf { it.second.toDouble() }) * 100).toInt()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(Color.Gray, CircleShape))
                                Text("Others", fontSize = 12.sp, color = SaaSColors.TextSecondary)
                            }
                            Text("$otherPercent%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

private data class StatusConfig(
    val label: String,
    val color: Color,
    val bg: Color,
    val fraction: Float
)

// ── Product Breakdown Card Component ──────────────────────────────────────────
@Composable
private fun ProductBreakdownCard(
    product: ProductCategory,
    liveUnits: Int,
    onQuickAdd: () -> Unit,
    onQuickRemove: () -> Unit,
    onTransferClick: () -> Unit,
    onAdjustClick: () -> Unit
) {
    val upc = product.unitsPerCase.coerceAtLeast(1)
    val isCan = upc == 1
    val cases = if (isCan) liveUnits else liveUnits / upc
    val remUnits = if (!isCan) liveUnits % upc else 0
    val totalVal = liveUnits * product.defaultSellPrice
    
    var showActions by remember { mutableStateOf(false) }

    // Status config
    val config = when {
        cases <= 0 -> StatusConfig("Out of Stock", SaaSColors.OutOfStock, SaaSColors.OutOfStockLight, 0f)
        cases <= 2 -> StatusConfig("Critical Stock", SaaSColors.Critical, SaaSColors.CriticalLight, 0.15f)
        cases <= 5 -> StatusConfig("Low Stock", SaaSColors.LowStock, SaaSColors.LowStockLight, 0.45f)
        else -> StatusConfig("Healthy Stock", SaaSColors.Healthy, SaaSColors.HealthyLight, (cases.toFloat() / 50f).coerceAtMost(1f))
    }

    // Color theme
    val (iconBg, iconFg) = when {
        product.name.contains("300", true) -> Color(0xFFEDE9FE) to Color(0xFF8B5CF6)
        product.name.contains("500", true) -> Color(0xFFDBEAFE) to Color(0xFF3B82F6)
        product.name.contains("1", true) && upc == 12 -> Color(0xFFCCFBF1) to Color(0xFF0D9488)
        product.name.contains("2", true) && upc == 9 -> Color(0xFFD1FAE5) to Color(0xFF10B981)
        product.name.contains("5", true) -> Color(0xFFFFF9DB) to Color(0xFFF59E0B)
        else -> Color(0xFFFEE2E2) to Color(0xFFEF4444)
    }

    val painter = when {
        product.name.contains("300", true) -> painterResource(Res.drawable.bottle_200ml)
        product.name.contains("500", true) -> painterResource(Res.drawable.bottle_500ml)
        product.name.contains("2", true) && upc == 9 -> painterResource(Res.drawable.bottle_2l)
        product.name.contains("1", true) -> painterResource(Res.drawable.bottle_2l)
        product.name.contains("5", true) -> painterResource(Res.drawable.bottle_5l)
        product.name.contains("20", true) -> painterResource(Res.drawable.bottle_20l)
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp), ambientColor = SaaSColors.CardShadow, spotColor = SaaSColors.CardShadow),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SaaSColors.Surface),
        onClick = { showActions = !showActions }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Product Thumbnail
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (painter != null) Color.White else iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (painter != null) {
                        Image(
                            painter = painter,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            tint = iconFg,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Detail Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.displayName.ifBlank { product.name },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaaSColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(config.bg)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = config.label,
                                color = config.color,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val packWord = if (isCan) "Single Can" else "$upc Units / Case"
                    Text(packWord, fontSize = 11.sp, color = SaaSColors.TextMuted)
                    
                    Spacer(Modifier.height(8.dp))

                    // Count and Value Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedContent(
                                targetState = cases,
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
                            ) { targetCases ->
                                Text(
                                    text = "$targetCases",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = config.color,
                                    style = TextStyle(letterSpacing = (-1).sp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isCan) "Cans" else "Cases",
                                fontSize = 13.sp,
                                color = SaaSColors.TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            if (!isCan && remUnits > 0) {
                                Spacer(Modifier.width(6.dp))
                                AnimatedContent(
                                    targetState = remUnits,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                    }
                                ) { targetRem ->
                                    Text(
                                        text = "(+$targetRem loose bottles)",
                                        fontSize = 11.sp,
                                        color = SaaSColors.TextMuted,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "₹${formatInventoryAmount(totalVal)} Value",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaaSColors.TextPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Stock Health Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SaaSColors.Border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(config.fraction)
                        .background(
                            brush = Brush.horizontalGradient(listOf(config.color, config.color.copy(alpha = 0.6f))),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }

            // Quick Actions Panel (Collapsible)
            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = SaaSColors.Border)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick +/- adjustments
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val removeInteractionSource = remember { MutableInteractionSource() }
                            val isRemovePressed by removeInteractionSource.collectIsPressedAsState()
                            val removeScale by animateFloatAsState(
                                targetValue = if (isRemovePressed) 0.82f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            val removeBgColor by animateColorAsState(
                                targetValue = if (isRemovePressed) SaaSColors.Border else SaaSColors.Background,
                                animationSpec = tween(150)
                            )
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = removeScale
                                        scaleY = removeScale
                                    }
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(removeBgColor)
                                    .border(1.dp, SaaSColors.Border, RoundedCornerShape(10.dp))
                                    .clickable(
                                        onClick = onQuickRemove,
                                        interactionSource = removeInteractionSource,
                                        indication = androidx.compose.foundation.LocalIndication.current
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Remove, null, tint = SaaSColors.TextSecondary, modifier = Modifier.size(16.dp))
                            }

                            val addInteractionSource = remember { MutableInteractionSource() }
                            val isAddPressed by addInteractionSource.collectIsPressedAsState()
                            val addScale by animateFloatAsState(
                                targetValue = if (isAddPressed) 0.82f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            val addBgColor by animateColorAsState(
                                targetValue = if (isAddPressed) SaaSColors.Primary.copy(alpha = 0.25f) else SaaSColors.PrimaryLight,
                                animationSpec = tween(150)
                            )
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = addScale
                                        scaleY = addScale
                                    }
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(addBgColor)
                                    .border(1.dp, SaaSColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .clickable(
                                        onClick = onQuickAdd,
                                        interactionSource = addInteractionSource,
                                        indication = androidx.compose.foundation.LocalIndication.current
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Add, null, tint = SaaSColors.Primary, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Text actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onAdjustClick,
                                border = BorderStroke(1.dp, SaaSColors.Border),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SaaSColors.TextSecondary)
                            ) {
                                Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Adjust", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onTransferClick,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SaaSColors.Primary)
                            ) {
                                Icon(Icons.Rounded.SwapHoriz, null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("Transfer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Recent Activity Component ─────────────────────────────────────────────────
@Composable
private fun RecentActivityFeed(
    movements: List<StockMovement>,
    products: List<ProductCategory>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SaaSColors.Surface)
            .border(1.dp, SaaSColors.Border, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        movements.forEachIndexed { index, m ->
            val isReturn = m.source.trim().startsWith("Empty cans", ignoreCase = true)
            val product = products.find { it.id == m.productId }
            val prodName = if (isReturn) "Empty Cans" else (product?.displayName ?: "Water Bottle")
            val upc = if (isReturn) 1 else (product?.unitsPerCase ?: 1)
            val casesCount = if (upc > 1) m.qty / upc else m.qty
            val suffix = if (isReturn) "Cans" else if (upc > 1) "Cases" else "Cans"

            val isAdd = m.type == "inward"
            val titleText = when {
                isReturn -> if (isAdd) "Cans Collected" else "Cans Returned"
                isAdd    -> "Stock Added"
                else     -> "Stock Sold"
            }
            val qtyText = "${if (isAdd) "+" else "-"}$casesCount $suffix"
            val sourceText = if (isAdd) "From ${m.source}" else "To ${m.source}"
            
            val iconBg = if (isAdd) SaaSColors.HealthyLight else Color(0xFFF3E8FF)
            val iconFg = if (isAdd) SaaSColors.Healthy else Color(0xFF8B5CF6)
            val arrowIcon = if (isAdd) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle type icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(arrowIcon, null, tint = iconFg, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$titleText • $prodName",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaaSColors.TextPrimary
                    )
                    Text(
                        text = sourceText,
                        fontSize = 11.sp,
                        color = SaaSColors.TextMuted
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = qtyText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconFg
                    )
                    Text(
                        text = "Today",
                        fontSize = 11.sp,
                        color = SaaSColors.TextMuted
                    )
                }
            }
            if (index < movements.size - 1) {
                HorizontalDivider(color = SaaSColors.Border, modifier = Modifier.padding(start = 48.dp))
            }
        }
    }
}

// ── Search Empty State ────────────────────────────────────────────────────────
@Composable
private fun EmptySearchState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            tint = SaaSColors.TextMuted,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No matching products found",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = SaaSColors.TextSecondary
        )
        Text(
            text = "Try adjusting your filters or search keywords.",
            fontSize = 12.sp,
            color = SaaSColors.TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

// ── Transfer Dialog Component ──────────────────────────────────────────────────
@Composable
private fun TransferStockDialog(
    product: ProductCategory,
    shops: List<ShopStockInfo>,
    currentShop: ShopStockInfo?,
    liveUnits: Int,
    onDismiss: () -> Unit,
    onConfirm: (fromShop: String, toShop: String, qty: Int) -> Unit
) {
    val upc = product.unitsPerCase.coerceAtLeast(1)
    val maxCases = if (upc > 1) liveUnits / upc else liveUnits

    var fromShopName by remember { mutableStateOf(currentShop?.name ?: shops.firstOrNull()?.name ?: "") }
    var toShopName by remember { mutableStateOf(shops.find { it.name != fromShopName }?.name ?: "") }
    var transferCount by remember { mutableStateOf(1) }

    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SaaSColors.Surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Transfer Stock",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SaaSColors.TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = product.displayName,
                    fontSize = 13.sp,
                    color = SaaSColors.TextMuted
                )

                Spacer(Modifier.height(20.dp))

                // Source Shop Selector
                Text("Source Shop", fontSize = 12.sp, color = SaaSColors.TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SaaSColors.Background)
                        .border(1.dp, SaaSColors.Border, RoundedCornerShape(10.dp))
                        .clickable { if (currentShop == null) expandedFrom = true }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(fromShopName, fontSize = 13.sp, color = SaaSColors.TextPrimary)
                        if (currentShop == null) {
                            Icon(Icons.Rounded.ArrowDropDown, null, tint = SaaSColors.TextSecondary)
                        }
                    }
                    DropdownMenu(expanded = expandedFrom, onDismissRequest = { expandedFrom = false }) {
                        shops.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    fromShopName = s.name
                                    if (toShopName == s.name) {
                                        toShopName = shops.find { it.name != s.name }?.name ?: ""
                                    }
                                    expandedFrom = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Destination Shop Selector
                Text("Destination Shop", fontSize = 12.sp, color = SaaSColors.TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SaaSColors.Background)
                        .border(1.dp, SaaSColors.Border, RoundedCornerShape(10.dp))
                        .clickable { expandedTo = true }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(toShopName, fontSize = 13.sp, color = SaaSColors.TextPrimary)
                        Icon(Icons.Rounded.ArrowDropDown, null, tint = SaaSColors.TextSecondary)
                    }
                    DropdownMenu(expanded = expandedTo, onDismissRequest = { expandedTo = false }) {
                        shops.filter { it.name != fromShopName }.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = { toShopName = s.name; expandedTo = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Quantity selector
                val unitWord = if (upc > 1) "Cases" else "Cans"
                Text("Transfer Quantity ($unitWord)", fontSize = 12.sp, color = SaaSColors.TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { if (transferCount > 1) transferCount-- },
                        modifier = Modifier
                            .size(36.dp)
                            .background(SaaSColors.Background, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Remove, null, tint = SaaSColors.TextSecondary)
                    }
                    Text(
                        text = "$transferCount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaaSColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    IconButton(
                        onClick = { if (transferCount < maxCases) transferCount++ },
                        modifier = Modifier
                            .size(36.dp)
                            .background(SaaSColors.Background, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = SaaSColors.TextSecondary)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Confirm Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SaaSColors.TextSecondary),
                        border = BorderStroke(1.dp, SaaSColors.Border)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            if (fromShopName.isNotEmpty() && toShopName.isNotEmpty() && transferCount > 0) {
                                onConfirm(fromShopName, toShopName, transferCount)
                            }
                        },
                        enabled = transferCount <= maxCases && fromShopName != toShopName && fromShopName.isNotEmpty() && toShopName.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SaaSColors.Primary)
                    ) {
                        Text("Transfer", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ── Adjust Dialog Component ───────────────────────────────────────────────────
@Composable
private fun AdjustStockDialog(
    product: ProductCategory,
    currentCases: Int,
    shopName: String,
    onDismiss: () -> Unit,
    onConfirm: (targetCases: Int, reason: String) -> Unit
) {
    var targetCount by remember { mutableStateOf(currentCases) }
    var reason by remember { mutableStateOf("Manual stock count audit") }
    var expandedReason by remember { mutableStateOf(false) }

    val upc = product.unitsPerCase
    val unitWord = if (upc > 1) "Cases" else "Cans"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SaaSColors.Surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Adjust Stock Level",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SaaSColors.TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${product.displayName} • $shopName",
                    fontSize = 13.sp,
                    color = SaaSColors.TextMuted
                )

                Spacer(Modifier.height(20.dp))

                // Quantity selector
                Text("Target Quantity ($unitWord)", fontSize = 12.sp, color = SaaSColors.TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { if (targetCount > 0) targetCount-- },
                        modifier = Modifier
                            .size(36.dp)
                            .background(SaaSColors.Background, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Remove, null, tint = SaaSColors.TextSecondary)
                    }
                    Text(
                        text = "$targetCount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaaSColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    IconButton(
                        onClick = { targetCount++ },
                        modifier = Modifier
                            .size(36.dp)
                            .background(SaaSColors.Background, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = SaaSColors.TextSecondary)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Reason Selection
                Text("Reason for adjustment", fontSize = 12.sp, color = SaaSColors.TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SaaSColors.Background)
                        .border(1.dp, SaaSColors.Border, RoundedCornerShape(10.dp))
                        .clickable { expandedReason = true }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(reason, fontSize = 13.sp, color = SaaSColors.TextPrimary)
                        Icon(Icons.Rounded.ArrowDropDown, null, tint = SaaSColors.TextSecondary)
                    }
                    DropdownMenu(expanded = expandedReason, onDismissRequest = { expandedReason = false }) {
                        val reasons = listOf(
                            "Manual stock count audit",
                            "Reported leakage or damage",
                            "Free sample distribution",
                            "Supplier returns",
                            "Data entry correction"
                        )
                        reasons.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = { reason = r; expandedReason = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SaaSColors.TextSecondary),
                        border = BorderStroke(1.dp, SaaSColors.Border)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onConfirm(targetCount, reason) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SaaSColors.Primary)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ── Small format helper ───────────────────────────────────────────────────────
private fun formatInventoryAmount(value: Double): String = when {
    value >= 1_00_000.0 -> "${(value / 1_00_000).toInt()}L"
    value >= 1_000.0    -> "${(value / 1_000).toInt()}K"
    else                -> "${value.toInt()}"
}

private fun shopKey(name: String): String =
    name.split("·", limit = 2).firstOrNull()?.trim()?.lowercase() ?: name.trim().lowercase()

private fun formatCases(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        val rounded = (value * 10).toLong() / 10.0
        if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }
}
