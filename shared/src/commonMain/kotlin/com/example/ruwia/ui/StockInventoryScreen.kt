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
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
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
import androidx.compose.ui.window.DialogProperties
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.CustomerProductPrice
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.isEmptyCansSource
import com.example.ruwia.domain.netStockPerProduct
import com.example.ruwia.domain.netStockPerProductInShop
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
    onAddStock: (product: ProductCategory?, currentStock: Int) -> Unit = { _, _ -> },
    onAddProduct: () -> Unit = {},
    onDeleteProduct: (String) -> Unit = {},
    onToggleProductStatus: (ProductCategory) -> Unit = {},
    onOpenStockHistory: () -> Unit = {},
    onSaveCustomerPrice: (CustomerProductPrice) -> Unit = {},
    onLoadCustomerPrices: (productId: String) -> Unit = {},
    onDeleteCustomerPrice: (customerId: String, productId: String) -> Unit = { _, _ -> },
    /** View-only mode (employee app): same UI, every editing affordance removed. */
    readOnly: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val products = state.productCategories
    val movements = state.recentMovements
    val shops = state.shopStocks

    // Derive the two shops to show, preferring the admin-configured display names
    // (from Settings). Falls back to names seen in the movement history so data is
    // never hidden even before a shop is configured.
    val displayShops = remember(shops, movements, state.shopNames) {
        val configured = state.shopNames.take(2).map { it.trim() }
        val configuredList = configured.mapIndexedNotNull { index, name ->
            if (name.isNotBlank()) ShopStockInfo(
                id = "config_$index",
                name = name,
                location = if (index == 0) "Primary Shop" else "Secondary Shop"
            ) else null
        }
        if (configuredList.size >= 2) return@remember configuredList

        val fromMovements = movements.map { it.shopName }.distinct()
            .mapNotNull { raw ->
                val clean = raw.trim()
                if (clean.isNotBlank() && clean.lowercase() != "all shops") clean else null
            }
            .filter { m -> configuredList.none { it.name.equals(m, ignoreCase = true) } }
            .take(2 - configuredList.size)
            .mapIndexed { index, name ->
                ShopStockInfo(id = "temp_$index", name = name, location = "Assigned Shop")
            }

        (configuredList + fromMovements).ifEmpty {
            state.shopNames.take(2).mapIndexedNotNull { index, name ->
                val clean = name.trim()
                if (clean.isBlank()) null else ShopStockInfo(
                    id = "default_$index",
                    name = clean,
                    location = if (index == 0) "Primary Shop" else "Secondary Shop"
                )
            }
        }
    }

    var selectedShopName by remember { mutableStateOf("All Shops") }

    val selectedShop = displayShops.find { it.name == selectedShopName }
    val selectedShopLocation = selectedShop?.location ?: if (selectedShopName == "All Shops") "Combined Inventory" else "Assigned Shop"
    val currentShopKey = if (selectedShopName == "All Shops") null else shopKey(selectedShopName)

    // ── Live inventory counts filtered by shop ────────────────────────────────
    // The "All Shops" view must agree with the Home screen and the ViewModel,
    // which compute per-product net stock from EVERY movement regardless of the
    // shop label it carries. Only the per-shop tabs scope to one shop, so
    // movements recorded under an unconfigured/renamed shop never get orphaned.
    val liveStockMap = remember(movements, products, currentShopKey) {
        if (currentShopKey != null) {
            // Per-shop tab: only that shop's movements count. No global fallback,
            // so stock recorded for another shop never leaks into this tab.
            val perShop = netStockPerProductInShop(movements, currentShopKey)
            products.associate { p ->
                p.id to (perShop[p.id] ?: 0)
            }
        } else {
            // "All Shops": every movement counts regardless of its shop label.
            val global = netStockPerProduct(movements)
            products.associate { p ->
                p.id to (global[p.id] ?: 0)
            }
        }
    }

    val effectiveStockMap = remember(liveStockMap) {
        liveStockMap
    }

    // ── Metrics ──────────────────────────────────────────────────────────────
    val totalInventory = products.sumOf { p ->
        val units = effectiveStockMap[p.id] ?: 0
        units.toDouble()
    }
    val totalValue = products.sumOf { p ->
        (effectiveStockMap[p.id] ?: 0) * p.defaultSellPrice
    }
    val productsAvailable = products.count { (effectiveStockMap[it.id] ?: 0) > 0 }
    val lowStockCount = products.count { p ->
        val units = effectiveStockMap[p.id] ?: 0
        units > 0 && units <= 5
    }

    // ── UI Control States ────────────────────────────────────────────────────
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(InvFilter.ALL) }
    var selectedSort by remember { mutableStateOf(InvSort.NAME) }

    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    var activeTransferProduct by remember { mutableStateOf<ProductCategory?>(null) }
    var activeAdjustProduct by remember { mutableStateOf<ProductCategory?>(null) }
    var deletingProduct by remember { mutableStateOf<ProductCategory?>(null) }
    var activeCustomerPriceProduct by remember { mutableStateOf<ProductCategory?>(null) }
    var showCustomerPriceDialog by remember { mutableStateOf(false) }

    // ── Filtered & Sorted products list ──────────────────────────────────────
    val filteredSortedProducts = remember(products, effectiveStockMap, searchQuery, selectedFilter, selectedSort) {
        products
            .filter { p ->
                val matchesSearch = p.displayName.contains(searchQuery, ignoreCase = true) || p.name.contains(searchQuery, ignoreCase = true)
                val units = effectiveStockMap[p.id] ?: 0

                val matchesFilter = when (selectedFilter) {
                    InvFilter.ALL -> true
                    InvFilter.HEALTHY -> units > 5
                    InvFilter.LOW -> units in 3..5
                    InvFilter.CRITICAL -> units in 1..2
                    InvFilter.OUT -> units <= 0
                }
                matchesSearch && matchesFilter
            }
            .sortedWith { p1, p2 ->
                val u1 = effectiveStockMap[p1.id] ?: 0
                val u2 = effectiveStockMap[p2.id] ?: 0
                val val1 = u1 * p1.defaultSellPrice
                val val2 = u2 * p2.defaultSellPrice

                when (selectedSort) {
                    InvSort.NAME -> p1.displayName.compareTo(p2.displayName, ignoreCase = true)
                    InvSort.QTY_DESC -> u2.compareTo(u1)
                    InvSort.QTY_ASC -> u1.compareTo(u2)
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
                            isSelected = selectedShopName == "All Shops",
                            onClick = { selectedShopName = "All Shops" }
                        )
                    }
                    items(displayShops) { shop ->
                        ShopTabChip(
                            name = shop.name,
                            isSelected = selectedShopName == shop.name,
                            onClick = { selectedShopName = shop.name }
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
                        unitLabel = "Units",
                        readOnly = readOnly
                    )
                }

                // ── Low Stock Attention Alert Section ─────────────────────────────
                // Hidden in read-only (employee) mode.
                if (!readOnly) {
                    val lowStockProducts = products.filter { p ->
                        val units = effectiveStockMap[p.id] ?: 0
                        units in 1..5
                    }
                    if (lowStockProducts.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(20.dp))
                            LowStockAttentionCard(
                                lowProducts = lowStockProducts,
                                liveStock = effectiveStockMap,
                                readOnly = readOnly,
                                onRestock = { product -> activeAdjustProduct = product }
                            )
                        }
                    }
                }

                // ── Analytics Insights Section ────────────────────────────────────
                // (Top Product / Most Moving / Fastest Selling — hidden in
                // read-only (employee) mode.)
                if (!readOnly) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        InventoryAnalyticsRow(
                            products = products,
                            liveStock = effectiveStockMap,
                            movements = movements,
                            currentShopKey = currentShopKey
                        )
                    }
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
                        val assignedShop = remember(product, movements, state.shopNames) {
                            getAssignedShop(product, movements, state.shopNames)
                        }
                        ProductBreakdownCard(
                            product = product,
                            liveUnits = units,
                            assignedShop = assignedShop,
                            readOnly = readOnly,
                            onEditPrice = { onAddStock(product, units) },
                            onTransferClick = { activeTransferProduct = product },
                            onAdjustClick = { activeAdjustProduct = product },
                            onDeleteClick = { deletingProduct = product },
                            onToggleStatus = { onToggleProductStatus(product) },
                            onSetCustomerPrice = { activeCustomerPriceProduct = product; showCustomerPriceDialog = true }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // ── Stock History Entry ──────────────────────────────────────────
                // Hidden in read-only (employee) mode.
                if (!readOnly && state.recentMovements.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SaaSColors.Surface)
                                .border(1.dp, SaaSColors.Border, RoundedCornerShape(20.dp))
                                .clickable(onClick = onOpenStockHistory)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SaaSColors.PrimaryLight),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = SaaSColors.Primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "STOCK HISTORY",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SaaSColors.TextPrimary,
                                    letterSpacing = 0.4.sp,
                                )
                                Text(
                                    text = "View all movements · ${state.recentMovements.size} entries",
                                    fontSize = 11.sp,
                                    color = SaaSColors.TextMuted,
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowRight,
                                contentDescription = "Open stock history",
                                tint = SaaSColors.TextMuted,
                            )
                        }
                    }
                }

                // ── Recent Activity Section ───────────────────────────────────────
                // Hidden in read-only (employee) mode.
                if (!readOnly) {
                    val recentMovements = movements.take(12)
                    if (recentMovements.isNotEmpty()) {
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
                        RecentActivityFeed(
                            movements = recentMovements,
                            products = products,
                            employees = state.employees,
                        )
                    }
                    }
                }
            }
        }

        // ── Dialog Modals ─────────────────────────────────────────────────────
        activeTransferProduct?.let { product ->
            TransferStockDialog(
                product = product,
                shops = displayShops,
                currentShop = selectedShop,
                liveUnits = effectiveStockMap[product.id] ?: 0,
                onDismiss = { activeTransferProduct = null },
                onConfirm = { fromShopName, toShopName, casesCount ->
                    val totalQty = casesCount
                    onAddMovement("Transfer to $toShopName", totalQty, "outward", fromShopName, product.id)
                    onAddMovement("Transfer from $fromShopName", totalQty, "inward", toShopName, product.id)
                    activeTransferProduct = null
                }
            )
        }

        activeAdjustProduct?.let { product ->
            val targetShopName = if (selectedShopName == "All Shops") {
                getAssignedShop(product, movements, state.shopNames)
            } else {
                selectedShopName
            }
            // Start the dialog on the exact live figure shown next to this
            // product (business-wide net on the All-Shops tab, that shop's net
            // on a shop tab). Adjustments are diff-based, so the +/- steppers
            // move from this base and only the delta is recorded.
            val currentCases = effectiveStockMap[product.id] ?: 0

            AdjustStockDialog(
                product = product,
                currentCases = currentCases,
                shopName = targetShopName,
                onDismiss = { activeAdjustProduct = null },
                onConfirm = { targetCases, reason ->
                    if (targetCases > currentCases) {
                        onAddMovement("Manual adjustment", targetCases - currentCases, "inward", targetShopName, product.id)
                    } else if (targetCases < currentCases) {
                        onAddMovement("Manual adjustment", currentCases - targetCases, "outward", targetShopName, product.id)
                    }
                    activeAdjustProduct = null
                }
            )
        }

        deletingProduct?.let { p ->
            AlertDialog(
                onDismissRequest = { deletingProduct = null },
                icon = { Icon(Icons.Rounded.Delete, null, tint = SaaSColors.Critical) },
                title = { Text(stringResource(Res.string.delete_product_confirm, p.displayName), fontWeight = FontWeight.Bold, color = SaaSColors.TextPrimary) },
                text = {
                    Text(
                        stringResource(Res.string.delete_product_warning),
                        fontSize = 13.sp,
                        color = SaaSColors.TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onDeleteProduct(p.id); deletingProduct = null },
                        colors = ButtonDefaults.buttonColors(containerColor = SaaSColors.Critical, contentColor = Color.White),
                    ) { Text(stringResource(Res.string.action_delete), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { deletingProduct = null },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SaaSColors.TextSecondary),
                        border = BorderStroke(1.dp, SaaSColors.Border)
                    ) { Text(stringResource(Res.string.action_cancel)) }
                },
                containerColor = SaaSColors.Surface,
            )
        }

        activeCustomerPriceProduct?.let { product ->
            // Load any previously saved custom prices so the dialog opens
            // with real values instead of defaults.
            LaunchedEffect(product.id) { onLoadCustomerPrices(product.id) }
            val savedPrices = remember(state.customerPrices, product.id) {
                state.customerPrices[product.id]
                    ?.associate { it.customerId to it.sellingPrice }
                    ?: emptyMap()
            }
            CustomerPricePickerDialog(
                product = product,
                customers = state.customers,
                savedPrices = savedPrices,
                onDeletePrice = { customerId -> onDeleteCustomerPrice(customerId, product.id) },
                onDismiss = {
                    activeCustomerPriceProduct = null
                    showCustomerPriceDialog = false
                },
                onPriceSave = { customerId, purchasePrice, sellingPrice ->
                    // Persist the customer-specific price via the repository.
                    // The dialog stays open so the admin can keep editing
                    // other customers; it tracks its own per-row saved state.
                    onSaveCustomerPrice(
                        CustomerProductPrice(
                            customerId = customerId,
                            productId = product.id,
                            sellingPrice = sellingPrice,
                            isActive = true
                        )
                    )
                }
            )
        }

        // ── Floating Action Button (FAB) ──────────────────────────────────
        // Hidden in read-only (employee) mode.
        if (!readOnly) {
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
                    .height(64.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(SaaSColors.SurfaceVar, RoundedCornerShape(10.dp))
                            .border(1.dp, SaaSColors.Border, RoundedCornerShape(10.dp))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Inventory",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SaaSColors.TextPrimary
                            )
                            // Live Status Indicator
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(SaaSColors.HealthyLight)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(SaaSColors.Healthy, CircleShape)
                                    )
                                    Text(
                                        text = "Live",
                                        color = SaaSColors.Healthy,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (shopName.isNotBlank() && shopName != "All Shops") "$shopName • $location" else "All Shops overview",
                                fontSize = 11.sp,
                                color = SaaSColors.TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(SaaSColors.TextMuted, CircleShape)
                            )
                            Text(
                                text = "Last updated 2m ago",
                                fontSize = 11.sp,
                                color = SaaSColors.TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

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
    unitLabel: String,
    readOnly: Boolean = false
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

            // Premium Animated Water Bottle Inventory Visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentAlignment = Alignment.Center
            ) {
                // We use a dynamic scale: if stock is very low, max is small.
                // This ensures "2 Units" still looks meaningful in the bottle.
                val maxCapacity = remember(totalInventory) {
                    max(10.0, totalInventory * 1.2).coerceAtMost(100.0)
                }
                PremiumWaterBottle(
                    level = (totalInventory / maxCapacity).coerceIn(0.0, 1.0).toFloat(),
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = SaaSColors.Border)
            Spacer(Modifier.height(16.dp))

            // Sub Stats Grid (Inventory Value is hidden in read-only mode)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!readOnly) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Inventory Value", color = SaaSColors.TextMuted, fontSize = 11.sp)
                        Spacer(Modifier.height(2.dp))
                        Text("₹${formatInventoryAmount(totalValue)}", color = SaaSColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
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
    readOnly: Boolean = false,
    onRestock: (ProductCategory) -> Unit
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SaaSColors.SurfaceVar)
                        .border(1.dp, SaaSColors.Border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .then(if (readOnly) Modifier else Modifier.clickable { onRestock(p) })
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = p.displayName.ifBlank { p.name },
                            fontSize = 13.sp,
                            color = SaaSColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = if (units <= 0) "Out of Stock" else "$units Units available",
                            fontSize = 11.sp,
                            color = SaaSColors.Critical,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    if (!readOnly) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SaaSColors.Critical)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Restock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (readOnly) "Low stock items need attention"
                else "Tap a product to adjust its stock level",
                fontSize = 11.sp,
                color = SaaSColors.TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
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
                value = "$topUnits Units",
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
            .height(115.dp) // Fixed height for uniformity in horizontal scroll
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
                contentAlignment = Alignment.Center,
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
            p to units.toDouble()
        }
        .filter { (_, qty) -> qty > 0.0 }
        .sortedByDescending { (_, qty) -> qty }

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
    assignedShop: String,
    readOnly: Boolean = false,
    onEditPrice: () -> Unit,
    onTransferClick: () -> Unit,
    onAdjustClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onToggleStatus: () -> Unit = {},
    onSetCustomerPrice: () -> Unit = {} // New parameter for customer price setting
) {

    val cases = liveUnits
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
        product.name.contains("1", true)   -> Color(0xFFCCFBF1) to Color(0xFF0D9488)
        product.name.contains("2", true)   -> Color(0xFFD1FAE5) to Color(0xFF10B981)
        product.name.contains("5", true)   -> Color(0xFFFFF9DB) to Color(0xFFF59E0B)
        else                               -> Color(0xFFFEE2E2) to Color(0xFFEF4444)
    }

    val painter = when {
        product.name.contains("300", true)  -> painterResource(Res.drawable.bottle_200ml)
        product.name.contains("500", true)  -> painterResource(Res.drawable.bottle_500ml)
        product.name.contains("2", true)    -> painterResource(Res.drawable.bottle_2l)
        product.name.contains("1", true)    -> painterResource(Res.drawable.bottle_2l)
        product.name.contains("5", true)    -> painterResource(Res.drawable.bottle_5l)
        product.name.contains("20", true)   -> painterResource(Res.drawable.bottle_20l)
        else                                -> null
    }

    val cardAlpha = if (product.isActive) 1f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp), ambientColor = SaaSColors.CardShadow, spotColor = SaaSColors.CardShadow),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SaaSColors.Surface),
        // Read-only cards don't expand: there are no actions to reveal.
        onClick = { if (!readOnly && product.isActive) showActions = !showActions }
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
                        .size(60.dp)
                        .graphicsLayer(alpha = cardAlpha)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (painter != null) Color.White else iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (painter != null) {
                        Image(
                            painter = painter,
                            contentDescription = product.displayName.ifBlank { product.name },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = product.displayName.ifBlank { product.name },
                            tint = iconFg,
                            modifier = Modifier.size(26.dp)
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
                            modifier = Modifier.weight(1f).graphicsLayer(alpha = cardAlpha)
                        )
                        Spacer(Modifier.width(6.dp))
                        // Status Badge
                        if (product.isActive) {
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
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF3F4F6))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Hidden",
                                        color = Color(0xFF6B7280),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = onToggleStatus,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFECFDF5),
                                        contentColor = Color(0xFF10B981)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Icon(Icons.Rounded.Visibility, null, modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Unhide", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.graphicsLayer(alpha = cardAlpha)
                    ) {
                        Text(
                            text = "₹${product.defaultSellPrice.toInt()} / unit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SaaSColors.TextSecondary
                        )
                        Text(
                            text = "·",
                            fontSize = 12.sp,
                            color = SaaSColors.TextMuted
                        )
                        Text(
                            text = "Cost ₹${product.purchasePrice.toInt()}",
                            fontSize = 12.sp,
                            color = SaaSColors.TextMuted
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Count and Value Row
                    Row(
                        modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = cardAlpha),
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
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = config.color,
                                    style = TextStyle(letterSpacing = (-1).sp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Units",
                                fontSize = 13.sp,
                                color = SaaSColors.TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
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
                        .graphicsLayer(alpha = cardAlpha)
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
                    visible = showActions && product.isActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = SaaSColors.Border)
                        Spacer(Modifier.height(16.dp))

                        // Primary actions: 2 x 2 grid so every label stays
                        // readable and each target meets the 44dp minimum.
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onEditPrice,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SaaSColors.Primary),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Edit Price", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = onSetCustomerPrice,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SaaSColors.PrimaryLight,
                                        contentColor = SaaSColors.Primary
                                    ),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.AttachMoney, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Customer Price", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onAdjustClick,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SaaSColors.Border),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SaaSColors.TextSecondary),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Tune, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Adjust Stock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onTransferClick,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SaaSColors.Primary.copy(alpha = 0.35f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SaaSColors.Primary),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.SwapHoriz, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Transfer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Row 2: Secondary Actions (Hide, Delete)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onToggleStatus,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFF7ED),
                                    contentColor = Color(0xFFF97316)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFDBA74)),
                                modifier = Modifier.weight(1f).height(46.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Rounded.VisibilityOff, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Hide Product", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (onDeleteClick != null) {
                                Button(
                                    onClick = onDeleteClick,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFEE2E2),
                                        contentColor = Color(0xFFEF4444)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Delete Product", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    products: List<ProductCategory>,
    employees: List<EmployeeInfo>,
) {
    // Accordion state: only one entry is open at a time. Clicking an open entry
    // collapses it; clicking another switches the expanded detail to that one.
    var expandedId by remember { mutableStateOf<String?>(null) }

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
            val isExpanded = expandedId == m.id
            val isReturn = m.source.isNotEmpty() && m.source.isEmptyCansSource()
            val product = products.find { it.id == m.productId }
            val prodName = if (isReturn) "Empty Units" else (product?.displayName ?: "Water Bottle")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                StockActivityRow(
                    movement   = m,
                    productName = prodName,
                    isReturn   = isReturn,
                    isExpanded = isExpanded,
                    onClick    = { expandedId = if (isExpanded) null else m.id },
                )

                if (isExpanded) {
                    ActivityDetailSheet(
                        movement = m,
                        employees = employees,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (index < movements.size - 1) {
                HorizontalDivider(color = SaaSColors.Border, modifier = Modifier.padding(start = 48.dp))
            }
        }
    }
}

/** A tappable recent-activity row shared by the inventory feed & stock history. */
@Composable
internal fun StockActivityRow(
    movement: StockMovement,
    productName: String,
    isReturn: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val upc = 1
    val casesCount = if (upc > 1) movement.qty / upc else movement.qty
    val suffix = "Units"

    val isAdd = movement.type == "inward"
    val titleText = when {
        isReturn -> if (isAdd) "Units Collected" else "Units Returned"
        isAdd    -> "Stock Added"
        else     -> "Stock Sold"
    }
    val qtyText = "${if (isAdd) "+" else "-"}$casesCount $suffix"
    val sourceText = if (isAdd) "From ${movement.source}" else "To ${movement.source}"

    val iconBg = if (isAdd) SaaSColors.HealthyLight else Color(0xFFF3E8FF)
    val iconFg = if (isAdd) SaaSColors.Healthy else Color(0xFF8B5CF6)
    val arrowIcon = if (isAdd) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circle type icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(arrowIcon, null, tint = iconFg, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$titleText • $productName",
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
                text = movement.createdAt?.let { com.example.ruwia.util.isoToDisplayDate(it) } ?: "—",
                fontSize = 11.sp,
                color = SaaSColors.TextMuted
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse details" else "Expand details",
            tint = SaaSColors.TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Expandable detail sheet rendered under a movement row. Shared by the
 *  inventory recent-activity feed and the stock-history screen. */
@Composable
internal fun ActivityDetailSheet(
    movement: StockMovement,
    employees: List<EmployeeInfo>,
    bg: Color = SaaSColors.SurfaceVar,
    border: Color = SaaSColors.Border,
    textPrimary: Color = SaaSColors.TextPrimary,
    textMuted: Color = SaaSColors.TextMuted,
) {
    val isAdd = movement.type == "inward"
    val isReturn = movement.source.isNotEmpty() && movement.source.isEmptyCansSource()

    val employeeName = employees.find { it.id == movement.employeeId }?.name
        ?: if (movement.employeeId.isNullOrBlank()) "Admin" else "—"
    val shopName = movement.shopName.ifBlank { "—" }
    val date = com.example.ruwia.util.isoToDisplayDate(movement.createdAt)
    val time = com.example.ruwia.util.isoToDisplayTime(movement.createdAt).ifBlank { "—" }

    // Customer is embedded in the source recorded for sales / empty-can returns:
    // "Sale · <name>", "Empty cans · <name>".
    val customerName = remember(movement.source) {
        val s = movement.source.trim()
        when {
            s.startsWith("Sale ·", ignoreCase = true)      -> s.substringAfter("·").trim()
            s.startsWith("Empty cans ·", ignoreCase = true) -> s.substringAfter("·").trim()
            s.startsWith("Empty cases ·", ignoreCase = true) -> s.substringAfter("·").trim()
            else -> null
        }
    }

    val rows = buildList {
        if (isAdd) {
            add("Added by" to (employeeName ?: "Admin"))
            add("Shop" to shopName)
            add("Date" to date)
            add("Time" to time)
            add("Total items" to "${movement.qty} Units")
            if (customerName != null && isReturn) add("Customer" to customerName)
        } else {
            add("Employee" to (employeeName ?: "Admin"))
            add("Shop" to shopName)
            add("Date" to date)
            add("Time" to time)
            add("Quantity" to "${movement.qty} Units")
            if (customerName != null) add("Customer" to customerName)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        rows.forEachIndexed { i, (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = textMuted,
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    textAlign = TextAlign.End,
                )
            }
            if (i < rows.lastIndex) Spacer(Modifier.height(6.dp))
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

// ── Customer Price Picker Dialog Component ────────────────────────────────────
@Composable
private fun CustomerPricePickerDialog(
    product: ProductCategory,
    customers: List<Customer> = emptyList(),
    /** Previously saved custom selling prices, keyed by customer id. */
    savedPrices: Map<String, Double> = emptyMap(),
    onDeletePrice: (customerId: String) -> Unit = {},
    onDismiss: () -> Unit,
    onPriceSave: (String, Double, Double) -> Unit // customerId, purchasePrice, sellingPrice
) {
    val isLoading by remember { mutableStateOf(false) }
    val errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember(product.id) { mutableStateOf("") }

    val defaultPurchase = product.purchasePrice.toInt()
    val defaultSelling = product.defaultSellPrice.toInt()

    // Rows saved during this session (optimistic; failures surface through
    // the global admin error state).
    var savedIds by remember(product.id) { mutableStateOf(setOf<String>()) }

    // Price fields per customer: raw input text. Blank / zero values fall back
    // to the product defaults when saving.
    var customerPriceFields by remember(product.id) {
        mutableStateOf<Map<String, Pair<String, String>>>(emptyMap())
    }
    // Rows the admin has typed into. Only these are preserved across
    // refreshes — everything else always reflects the latest saved values,
    // so prices arriving late from the database still fill in correctly.
    var dirtyIds by remember(product.id) { mutableStateOf(setOf<String>()) }

    // Pre-fill: previously saved custom prices first, product defaults
    // otherwise. Only user-edited rows are preserved.
    LaunchedEffect(customers, savedPrices) {
        val current = customerPriceFields
        customerPriceFields = customers.mapNotNull { customer ->
            val id = customer.id ?: return@mapNotNull null
            val keep = current[id]
            if (keep != null && dirtyIds.contains(id)) {
                id to keep
            } else {
                val selling = savedPrices[id]?.toInt() ?: defaultSelling
                id to Pair(defaultPurchase.toString(), selling.toString())
            }
        }.toMap()
    }

    val visibleCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                (it.phone?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    // Rows with a saved custom price or unsaved edits differing from defaults.
    val customizedCount = remember(customerPriceFields, savedPrices) {
        val ids = (customerPriceFields.keys + savedPrices.keys).toSet()
        ids.count { id ->
            if (savedPrices.containsKey(id)) true
            else {
                val pair = customerPriceFields[id]
                pair != null &&
                    ((pair.first.toIntOrNull() ?: defaultPurchase) != defaultPurchase ||
                        (pair.second.toIntOrNull() ?: defaultSelling) != defaultSelling)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SaaSColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SaaSColors.PrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AttachMoney,
                            contentDescription = null,
                            tint = SaaSColors.Primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Customer Pricing",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaaSColors.TextPrimary
                        )
                        Text(
                            text = "${product.displayName.ifBlank { product.name }} · Default ₹${product.defaultSellPrice.toInt()}",
                            fontSize = 11.sp,
                            color = SaaSColors.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = SaaSColors.TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Default-price reference strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriceStatTile(
                        label = "Cost",
                        value = "₹$defaultPurchase",
                        modifier = Modifier.weight(1f)
                    )
                    PriceStatTile(
                        label = "Default sell",
                        value = "₹$defaultSelling",
                        modifier = Modifier.weight(1f)
                    )
                    PriceStatTile(
                        label = "Margin",
                        value = "₹${defaultSelling - defaultPurchase}",
                        modifier = Modifier.weight(1f),
                        highlight = true
                    )
                }
                Spacer(Modifier.height(12.dp))

                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search customers...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = SaaSColors.TextMuted
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = SaaSColors.TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(10.dp))

                // Section label with customized counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CUSTOMERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = SaaSColors.Primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (customizedCount > 0) {
                        Text(
                            text = "$customizedCount custom",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaaSColors.Primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(SaaSColors.PrimaryLight)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                val dialogError = errorMessage
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = SaaSColors.Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else if (dialogError != null) {
                    Text(
                        text = dialogError,
                        color = SaaSColors.Critical,
                        fontSize = 14.sp
                    )
                } else if (visibleCustomers.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            contentDescription = null,
                            tint = SaaSColors.TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (customers.isEmpty()) "No customers found" else "No matches",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SaaSColors.TextSecondary
                        )
                        Text(
                            text = if (customers.isEmpty()) "Add customers in Customer Management first."
                            else "Try a different search.",
                            fontSize = 12.sp,
                            color = SaaSColors.TextMuted
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        visibleCustomers.forEach { customer ->
                            val customerId = customer.id ?: return@forEach
                            val (purchaseText, sellingText) = customerPriceFields[customerId]
                                ?: Pair(product.purchasePrice.toInt().toString(), product.defaultSellPrice.toInt().toString())
                            val purchaseValue = purchaseText.toIntOrNull()?.takeIf { it > 0 }
                                ?: defaultPurchase
                            val sellingValue = sellingText.toIntOrNull()?.takeIf { it > 0 }
                                ?: defaultSelling
                            val margin = sellingValue - purchaseValue
                            val hasDbCustom = savedPrices.containsKey(customerId)
                            val isCustom = hasDbCustom ||
                                purchaseValue != defaultPurchase || sellingValue != defaultSelling
                            val isSaved = savedIds.contains(customerId)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SaaSColors.SurfaceVar),
                                border = BorderStroke(1.dp, SaaSColors.Border)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(SaaSColors.PrimaryLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = customer.name.firstOrNull()?.uppercase() ?: "?",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SaaSColors.Primary
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = customer.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SaaSColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val customerPhone = customer.phone?.takeIf { it.isNotBlank() }
                                            if (customerPhone != null) {
                                                Text(
                                                    text = customerPhone,
                                                    fontSize = 11.sp,
                                                    color = SaaSColors.TextMuted
                                                )
                                            }
                                        }
                                        if (isCustom) {
                                            Text(
                                                text = "Custom",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SaaSColors.Primary,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(SaaSColors.PrimaryLight)
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = "Margin ₹$margin",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SaaSColors.Healthy,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(SaaSColors.HealthyLight)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = purchaseText,
                                            onValueChange = { newValue ->
                                                val digitsOnly = newValue.filter { it.isDigit() }
                                                val updated = customerPriceFields.toMutableMap()
                                                updated[customerId] = Pair(digitsOnly, sellingText)
                                                customerPriceFields = updated
                                                dirtyIds = dirtyIds + customerId
                                                savedIds = savedIds - customerId
                                            },
                                            modifier = Modifier.weight(1f),
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SaaSColors.TextPrimary
                                            ),
                                            label = { Text("Purchase", fontSize = 11.sp) },
                                            prefix = { Text("₹") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        OutlinedTextField(
                                            value = sellingText,
                                            onValueChange = { newValue ->
                                                val digitsOnly = newValue.filter { it.isDigit() }
                                                val updated = customerPriceFields.toMutableMap()
                                                updated[customerId] = Pair(purchaseText, digitsOnly)
                                                customerPriceFields = updated
                                                dirtyIds = dirtyIds + customerId
                                                savedIds = savedIds - customerId
                                            },
                                            modifier = Modifier.weight(1f),
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SaaSColors.TextPrimary
                                            ),
                                            label = { Text("Selling", fontSize = 11.sp) },
                                            prefix = { Text("₹") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Blank or 0 uses the product default.",
                                        fontSize = 10.sp,
                                        color = SaaSColors.TextMuted
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isCustom) {
                                            TextButton(
                                                onClick = {
                                                    if (hasDbCustom) onDeletePrice(customerId)
                                                    val updated = customerPriceFields.toMutableMap()
                                                    updated[customerId] = Pair(
                                                        defaultPurchase.toString(),
                                                        defaultSelling.toString()
                                                    )
                                                    customerPriceFields = updated
                                                    dirtyIds = dirtyIds - customerId
                                                    savedIds = savedIds - customerId
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Refresh,
                                                    null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Reset",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Spacer(Modifier.width(1.dp))
                                        }
                                        Button(
                                            onClick = {
                                                onPriceSave(customerId, purchaseValue.toDouble(), sellingValue.toDouble())
                                                savedIds = savedIds + customerId
                                            },
                                            modifier = Modifier.height(38.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSaved) SaaSColors.Healthy else SaaSColors.Primary
                                            )
                                        ) {
                                            Icon(
                                                if (isSaved) Icons.Rounded.CheckCircle else Icons.Rounded.Check,
                                                null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                if (isSaved) "Saved"
                                                else if (hasDbCustom) "Update" else "Save",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${visibleCustomers.size} customer${if (visibleCustomers.size == 1) "" else "s"}",
                            fontSize = 12.sp,
                            color = SaaSColors.TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismiss) {
                            Text("Close", fontWeight = FontWeight.Bold, color = SaaSColors.Primary)
                        }
                    }
                }
            }
        }
    }
}

// ── Small stat tile used by the customer pricing header ───────────────────────
@Composable
private fun PriceStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlight) SaaSColors.PrimaryLight else SaaSColors.SurfaceVar)
            .border(
                1.dp,
                if (highlight) SaaSColors.Primary.copy(alpha = 0.3f) else SaaSColors.Border,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) SaaSColors.Primary else SaaSColors.TextPrimary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = SaaSColors.TextMuted
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

    val maxCases = liveUnits

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
                Text("Transfer Quantity (Units)", fontSize = 12.sp, color = SaaSColors.TextSecondary, fontWeight = FontWeight.Bold)
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


    val unitWord = "Units"

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
                var textVal by remember(targetCount) { mutableStateOf(targetCount.toString()) }

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

                    BasicTextField(
                        value = textVal,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                textVal = newValue
                                targetCount = newValue.toIntOrNull() ?: 0
                            } else if (newValue.isEmpty()) {
                                textVal = ""
                                targetCount = 0
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SaaSColors.TextPrimary,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .width(80.dp)
                            .background(SaaSColors.Background, RoundedCornerShape(8.dp))
                            .border(1.dp, SaaSColors.Border, RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp, horizontal = 12.dp)
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

                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { targetCount = 0 },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(12.dp), tint = SaaSColors.Primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Reset to 0", fontSize = 11.sp, color = SaaSColors.Primary, fontWeight = FontWeight.Bold)
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

private fun getAssignedShop(
    product: ProductCategory,
    movements: List<StockMovement>,
    configuredShops: List<String>,
): String {
    val group = product.supplierGroup.trim()
    if (group.isNotBlank() && group != "GC" && group != "MB") {
        return group
    }
    val firstMov = movements.firstOrNull { it.productId == product.id }
    return when {
        firstMov != null ->
            firstMov.shopName.split("·", limit = 2).firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
                ?: configuredShops.firstOrNull().orEmpty()
        else -> configuredShops.firstOrNull().orEmpty()
    }
}

@Composable
private fun PremiumWaterBottle(
    level: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val animatedLevel by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Horizontal Bottle Dimensions
        val bottleWidth = width * 0.75f
        val bottleHeight = height * 0.7f
        val left = (width - bottleWidth - 40.dp.toPx()) / 2
        val top = (height - bottleHeight) / 2

        val neckWidth = 20.dp.toPx()
        val neckHeight = bottleHeight * 0.45f
        val capWidth = 10.dp.toPx()
        val capHeight = neckHeight * 1.1f

        // Single organic bottle path
        val bottlePath = Path().apply {
            val corner = 14.dp.toPx()
            // Body
            moveTo(left + corner, top)
            lineTo(left + bottleWidth - corner * 2, top)
            // Taper to neck
            quadraticTo(
                left + bottleWidth - corner, top,
                left + bottleWidth, top + (bottleHeight - neckHeight) / 2
            )
            // Neck
            lineTo(left + bottleWidth + neckWidth, top + (bottleHeight - neckHeight) / 2)
            // Cap
            lineTo(left + bottleWidth + neckWidth, top + (bottleHeight - capHeight) / 2)
            lineTo(left + bottleWidth + neckWidth + capWidth, top + (bottleHeight - capHeight) / 2)
            lineTo(left + bottleWidth + neckWidth + capWidth, top + (bottleHeight + capHeight) / 2)
            lineTo(left + bottleWidth + neckWidth, top + (bottleHeight + capHeight) / 2)
            lineTo(left + bottleWidth + neckWidth, top + (bottleHeight + capHeight) / 2)
            lineTo(left + bottleWidth + neckWidth, top + (bottleHeight + neckHeight) / 2)
            // Back to body
            lineTo(left + bottleWidth, top + (bottleHeight + neckHeight) / 2)
            quadraticTo(
                left + bottleWidth - corner, top + bottleHeight,
                left + bottleWidth - corner * 2, top + bottleHeight
            )
            lineTo(left + corner, top + bottleHeight)
            // Left rounded end
            quadraticTo(left, top + bottleHeight, left, top + bottleHeight - corner)
            lineTo(left, top + corner)
            quadraticTo(left, top, left + corner, top)
            close()
        }

        // 1. Outer Bloom/Glow (#3EF7F5)
        drawPath(
            path = bottlePath,
            color = Color(0xFF3EF7F5).copy(alpha = 0.15f),
            style = Stroke(width = 12.dp.toPx())
        )

        // 2. Glass Base Tint
        drawPath(
            path = bottlePath,
            color = Color(0xFFFFFFFF).copy(alpha = 0.05f)
        )

        // 3. Water Rendering
        clipPath(bottlePath) {
            val effectiveLevel = animatedLevel.coerceIn(0.01f, 0.99f)
            val baseWaterY = top + bottleHeight - (bottleHeight * effectiveLevel)

            // Background Wave (slightly different phase and darker)
            val backWavePath = Path()
            backWavePath.moveTo(left - 50f, top + bottleHeight + 50f)
            backWavePath.lineTo(left + bottleWidth + 100f, top + bottleHeight + 50f)
            for (i in 0..100) {
                val x = left + bottleWidth + 100f - (i.toFloat() / 100) * (bottleWidth + 150f)
                val relX = i.toFloat() / 100
                val y = baseWaterY + 3.dp.toPx() * sin(relX * 2 * PI.toFloat() + waveOffset * 0.8f + 0.5f)
                backWavePath.lineTo(x, y.toFloat())
            }
            backWavePath.close()
            drawPath(backWavePath, Color(0xFF16A5B2).copy(alpha = 0.4f))

            // Middle Wave (Highlight streak)
            val midWavePath = Path()
            midWavePath.moveTo(left - 50f, top + bottleHeight + 50f)
            midWavePath.lineTo(left + bottleWidth + 100f, top + bottleHeight + 50f)
            for (i in 0..100) {
                val x = left + bottleWidth + 100f - (i.toFloat() / 100) * (bottleWidth + 150f)
                val relX = i.toFloat() / 100
                val y = baseWaterY + 2.dp.toPx() * sin(relX * 2.5 * PI.toFloat() + waveOffset * 1.2f)
                midWavePath.lineTo(x, y.toFloat())
            }
            midWavePath.close()
            drawPath(midWavePath, Color(0xFF59F3F0).copy(alpha = 0.2f))

            // Main Liquid Gradient
            val waterPath = Path()
            waterPath.moveTo(left - 50f, top + bottleHeight + 50f)
            waterPath.lineTo(left + bottleWidth + 100f, top + bottleHeight + 50f)
            for (i in 0..100) {
                val x = left + bottleWidth + 100f - (i.toFloat() / 100) * (bottleWidth + 150f)
                var relX = i.toFloat() / 100
                val amplitude = 5.dp.toPx() * (1f - abs(effectiveLevel - 0.5f))
                val y = baseWaterY + amplitude * sin(relX * 1.8 * PI.toFloat() + waveOffset)
                waterPath.lineTo(x, y.toFloat())
            }
            waterPath.close()

            drawPath(
                path = waterPath,
                brush = Brush.verticalGradient(
                    0.0f to Color(0xFF59F3F0).copy(alpha = 0.9f),
                    0.4f to Color(0xFF2ED8D3).copy(alpha = 0.95f),
                    1.0f to Color(0xFF0E7F94),
                    startY = baseWaterY - 10.dp.toPx(),
                    endY = top + bottleHeight
                )
            )

            // Surface Reflection Line
            drawPath(
                path = waterPath,
                color = Color(0xFFBFFFFF).copy(alpha = 0.5f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Bubbles (Submerged and Surface)
            val bubbleRandom = kotlin.random.Random(42)
            repeat(15) { i ->
                val bRelX = bubbleRandom.nextFloat()
                val bX = left + bottleWidth * bRelX
                val bY = top + bottleHeight - (bottleHeight * effectiveLevel * bubbleRandom.nextFloat())

                // Only draw if submerged enough
                if (bY > baseWaterY - 5.dp.toPx()) {
                    val size = (1f + bubbleRandom.nextFloat() * 2f).dp.toPx()
                    val alpha = 0.2f + 0.3f * sin(waveOffset + i).coerceAtLeast(0f)
                    drawCircle(
                        color = Color(0xFFBFFFFF).copy(alpha = alpha),
                        radius = size,
                        center = Offset(bX, bY)
                    )
                    // Tiny glow for bubbles
                    drawCircle(
                        color = Color(0xFF59F3F0).copy(alpha = alpha * 0.5f),
                        radius = size * 2,
                        center = Offset(bX, bY)
                    )
                }
            }

            // 4. Glass Detail
            // Thick edges
            drawPath(
                path = bottlePath,
                color = Color(0xFF6EEEF4).copy(alpha = 0.25f),
                style = Stroke(width = 2.5.dp.toPx())
            )
            // Internal highlight
            drawPath(
                path = bottlePath,
                color = Color(0xFFC8FFFF).copy(alpha = 0.1f),
                style = Stroke(width = 0.8.dp.toPx())
            )

            // Top Glossy Reflection
            val glossPath = Path().apply {
                moveTo(left + 20.dp.toPx(), top + 4.dp.toPx())
                lineTo(left + bottleWidth - 40.dp.toPx(), top + 4.dp.toPx())
            }
            drawPath(
                path = glossPath,
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color(0xFFC8FFFF).copy(alpha = 0.3f), Color.Transparent)
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}