package com.example.ruwia.ui.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.unitsPerCase
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ruwia.shared.generated.resources.*
import kotlin.math.absoluteValue

// ── Sorting Enum ───────────────────────────────────────────────────────────────
enum class ProductSort(val displayName: String) {
    Name("Alphabetical"),
    PriceHighToLow("Price: High to Low"),
    PriceLowToHigh("Price: Low to High"),
    StockHighToLow("Stock: High to Low"),
    StockLowToHigh("Stock: Low to High"),
    MarginHighToLow("Margin: High to Low"),
}

// ── Canvas Fallbacks ───────────────────────────────────────────────────────────
private fun getProductColor(name: String): Color = when {
    name.contains("300", true) -> Color(0xFF8B5CF6)
    name.contains("500", true) -> Color(0xFF3B82F6)
    name.contains("1",   true) -> Color(0xFF0D9488)
    name.contains("2",   true) -> Color(0xFF10B981)
    name.contains("5",   true) -> Color(0xFFF59E0B)
    else -> Color(0xFFEF4444) // 20L / Default
}

private data class BottleDims(
    val bodyW: Int,
    val bodyH: Int,
    val neckW: Int,
    val neckH: Int,
    val hasHandle: Boolean = false,
)

private val bottleDims = mapOf(
    "1 Litre"  to BottleDims(32, 48, 15, 13),
)

// Helper to match categories
private fun matchesCategory(product: ProductCategory, category: String): Boolean {
    if (category == "All") return true
    val name = product.displayName.lowercase()
    return when (category) {
        "300ML" -> name.contains("300")
        "500ML" -> name.contains("500")
        "1L" -> name.contains("1 l") || name.endsWith("1l") || (name.contains("1") && !name.contains("10") && !name.contains("20") && !name.contains("300") && !name.contains("500"))
        "2L" -> name.contains("2 l") || name.endsWith("2l") || (name.contains("2") && !name.contains("20"))
        "5L" -> name.contains("5 l") || name.endsWith("5l")
        "20L" -> name.contains("20 l") || name.endsWith("20l")
        else -> false
    }
}

// ── Main Screen ────────────────────────────────────────────────────────────────

@Composable
fun ProductManagementScreen(
    products: List<ProductCategory> = emptyList(),
    onAddProduct: (ProductCategory) -> Unit = {},
    onUpdateProduct: (ProductCategory) -> Unit = {},
    onDeleteProduct: (String) -> Unit = {},
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    // UI states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf(ProductSort.Name) }
    var filterStatus by remember { mutableStateOf("All") } // All, Active, Inactive, Out of Stock

    // Range filters
    var priceMinInput by remember { mutableStateOf("") }
    var priceMaxInput by remember { mutableStateOf("") }
    var marginMinInput by remember { mutableStateOf("") }
    var marginMaxInput by remember { mutableStateOf("") }
    var stockMinInput by remember { mutableStateOf("") }
    var stockMaxInput by remember { mutableStateOf("") }

    // Overlay/Dialog flags
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    var activeFabAlert by remember { mutableStateOf<String?>(null) }

    // Product operation states
    var showForm by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductCategory?>(null) }
    var updatingPriceProduct by remember { mutableStateOf<ProductCategory?>(null) }
    var adjustingInventoryProduct by remember { mutableStateOf<ProductCategory?>(null) }
    var deletingProduct by remember { mutableStateOf<ProductCategory?>(null) }

    // Derived product list metrics
    val totalProducts = products.size
    val activeProductsCount = products.count { it.isActive }
    val lowStockCount = products.count { prod ->
        val upc = prod.unitsPerCase.coerceAtLeast(1)
        val cases = if (upc == 1) prod.stockAvailable else prod.stockAvailable / upc
        cases <= 5
    }
    val totalInventoryValue = products.sumOf { it.stockAvailable * it.defaultSellPrice }

    // Average margin percent for active products
    val activeProducts = products.filter { it.isActive }
    val averageMargin = if (activeProducts.isNotEmpty()) {
        activeProducts.map { prod ->
            val cost = if (prod.supplierGroup == "GC") prod.purchasePriceGC else prod.purchasePriceMB
            val margin = prod.defaultSellPrice - cost
            if (prod.defaultSellPrice > 0) (margin / prod.defaultSellPrice) * 100 else 0.0
        }.average()
    } else 0.0

    // Filtering & Sorting Logic
    val filteredProducts = remember(
        products, searchQuery, selectedCategory, sortBy, filterStatus,
        priceMinInput, priceMaxInput, marginMinInput, marginMaxInput, stockMinInput, stockMaxInput
    ) {
        products.filter { prod ->
            // Category check
            val matchesCat = matchesCategory(prod, selectedCategory)

            // Search text check
            val matchesSearch = prod.displayName.contains(searchQuery, ignoreCase = true) ||
                    prod.name.contains(searchQuery, ignoreCase = true)

            // Status check
            val matchesStatus = when (filterStatus) {
                "Active" -> prod.isActive
                "Inactive" -> !prod.isActive
                "Out of Stock" -> prod.stockAvailable == 0
                else -> true
            }

            // Cost price helper
            val cost = if (prod.supplierGroup == "GC") prod.purchasePriceGC else prod.purchasePriceMB
            val margin = prod.defaultSellPrice - cost
            val marginPercent = if (prod.defaultSellPrice > 0) (margin / prod.defaultSellPrice) * 100 else 0.0

            // Price range check
            val priceMin = priceMinInput.toDoubleOrNull() ?: 0.0
            val priceMax = priceMaxInput.toDoubleOrNull() ?: Double.MAX_VALUE
            val matchesPrice = prod.defaultSellPrice in priceMin..priceMax

            // Margin range check
            val marginMin = marginMinInput.toDoubleOrNull() ?: -100.0
            val marginMax = marginMaxInput.toDoubleOrNull() ?: 100.0
            val matchesMargin = marginPercent in marginMin..marginMax

            // Stock range check (calculated in cases/cans)
            val upc = prod.unitsPerCase.coerceAtLeast(1)
            val casesCount = if (upc == 1) prod.stockAvailable else prod.stockAvailable / upc
            val stockMin = stockMinInput.toIntOrNull() ?: 0
            val stockMax = stockMaxInput.toIntOrNull() ?: Int.MAX_VALUE
            val matchesStock = casesCount in stockMin..stockMax

            matchesCat && matchesSearch && matchesStatus && matchesPrice && matchesMargin && matchesStock
        }.sortedWith { p1, p2 ->
            val cost1 = if (p1.supplierGroup == "GC") p1.purchasePriceGC else p1.purchasePriceMB
            val margin1 = p1.defaultSellPrice - cost1
            val marginPercent1 = if (p1.defaultSellPrice > 0) (margin1 / p1.defaultSellPrice) * 100 else 0.0

            val cost2 = if (p2.supplierGroup == "GC") p2.purchasePriceGC else p2.purchasePriceMB
            val margin2 = p2.defaultSellPrice - cost2
            val marginPercent2 = if (p2.defaultSellPrice > 0) (margin2 / p2.defaultSellPrice) * 100 else 0.0

            val upc1 = p1.unitsPerCase.coerceAtLeast(1)
            val upc2 = p2.unitsPerCase.coerceAtLeast(1)
            val c1 = if (upc1 == 1) p1.stockAvailable else p1.stockAvailable / upc1
            val c2 = if (upc2 == 1) p2.stockAvailable else p2.stockAvailable / upc2

            when (sortBy) {
                ProductSort.Name -> p1.displayName.compareTo(p2.displayName, ignoreCase = true)
                ProductSort.PriceHighToLow -> p2.defaultSellPrice.compareTo(p1.defaultSellPrice)
                ProductSort.PriceLowToHigh -> p1.defaultSellPrice.compareTo(p2.defaultSellPrice)
                ProductSort.StockHighToLow -> c2.compareTo(c1)
                ProductSort.StockLowToHigh -> c1.compareTo(c2)
                ProductSort.MarginHighToLow -> marginPercent2.compareTo(marginPercent1)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header Section ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NTColors.Surface)
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
                                    .background(NTColors.SurfaceVar, RoundedCornerShape(10.dp))
                                    .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
                                    .clickable(onClick = onBack),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowBack,
                                    "Back",
                                    tint = NTColors.TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Products",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NTColors.TextPrimary
                                )
                                Text(
                                    "Inventory Overview",
                                    fontSize = 12.sp,
                                    color = NTColors.TextTertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Advanced Search Bar & Sort/Filter Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search bar input
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(NTColors.SurfaceVar, RoundedCornerShape(12.dp))
                                .border(1.dp, NTColors.Border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                "Search",
                                tint = NTColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text("Search Products...", fontSize = 14.sp, color = NTColors.TextTertiary)
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = NTColors.TextPrimary
                                    ),
                                    cursorBrush = SolidColor(NTColors.Primary),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Rounded.Close,
                                    "Clear",
                                    tint = NTColors.TextSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Sort selector button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(NTColors.Surface, RoundedCornerShape(12.dp))
                                .border(1.dp, NTColors.Border, RoundedCornerShape(12.dp))
                                .clickable { showSortDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Sort,
                                "Sort",
                                tint = NTColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(6.dp))

                        // Filter details button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (filterStatus != "All" || priceMinInput.isNotEmpty() || priceMaxInput.isNotEmpty() ||
                                        marginMinInput.isNotEmpty() || marginMaxInput.isNotEmpty() ||
                                        stockMinInput.isNotEmpty() || stockMaxInput.isNotEmpty()
                                    ) NTColors.PrimaryLight else NTColors.Surface,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (filterStatus != "All" || priceMinInput.isNotEmpty() || priceMaxInput.isNotEmpty() ||
                                        marginMinInput.isNotEmpty() || marginMaxInput.isNotEmpty() ||
                                        stockMinInput.isNotEmpty() || stockMaxInput.isNotEmpty()
                                    ) NTColors.Primary else NTColors.Border,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { showFilterDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.FilterList,
                                "Filter",
                                tint = if (filterStatus != "All" || priceMinInput.isNotEmpty() || priceMaxInput.isNotEmpty() ||
                                    marginMinInput.isNotEmpty() || marginMaxInput.isNotEmpty() ||
                                    stockMinInput.isNotEmpty() || stockMaxInput.isNotEmpty()
                                ) NTColors.Primary else NTColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Category Pill Selector Row
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categories = listOf("All", "300ML", "500ML", "1L", "2L", "5L", "20L")
                        items(categories) { category ->
                            val isSelected = selectedCategory == category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) NTColors.Primary else NTColors.SurfaceVar)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.Transparent else NTColors.Border,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    category,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else NTColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // ── Scrollable Body ────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 90.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Product Catalog Summary Card
                item {
                    ProductCatalogSummaryCard(
                        totalValue = totalInventoryValue,
                        totalProducts = totalProducts,
                        activeProducts = activeProductsCount,
                        averageMargin = averageMargin
                    )
                }

                // 2. AI Insights Section
                item {
                    AIInsightsSection(products = products)
                }

                // 3. Products List Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Product SKU List (${filteredProducts.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NTColors.TextPrimary
                        )
                        if (filteredProducts.size < products.size) {
                            Text(
                                "Filters Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NTColors.Primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NTColors.PrimaryLight)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // 5. Product Cards
                if (filteredProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.Inventory2,
                                    "No Products",
                                    tint = NTColors.TextTertiary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No products found",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NTColors.TextSecondary
                                )
                                Text(
                                    "Try adjusting your filters or search terms.",
                                    fontSize = 13.sp,
                                    color = NTColors.TextTertiary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductManagementCard(
                            product = product,
                            onEdit = {
                                editingProduct = product
                                showForm = true
                            },
                            onUpdatePrice = {
                                updatingPriceProduct = product
                            },
                            onAdjustInventory = {
                                adjustingInventoryProduct = product
                            },
                            onDuplicate = {
                                val duplicate = product.copy(
                                    id = "",
                                    name = "${product.name}-COPY",
                                    displayName = "${product.displayName} (Copy)",
                                    stockAvailable = 0
                                )
                                onAddProduct(duplicate)
                            },
                            onToggleStatus = {
                                onUpdateProduct(product.copy(isActive = !product.isActive))
                            },
                            onDelete = {
                                deletingProduct = product
                            }
                        )
                    }
                }
            }
        }

        // ── Floating Action Button (FAB) Menu ───────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Expanded Action Items
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FABSubMenuOption(
                            label = "Add Product",
                            icon = Icons.Rounded.Add,
                            onClick = {
                                isFabExpanded = false
                                editingProduct = null
                                showForm = true
                            }
                        )
                        FABSubMenuOption(
                            label = "Bulk Import",
                            icon = Icons.Rounded.CloudUpload,
                            isComingSoon = true,
                            onClick = {
                                isFabExpanded = false
                                activeFabAlert = "Bulk Import Feature"
                            }
                        )
                        FABSubMenuOption(
                            label = "Scan Barcode",
                            icon = Icons.Rounded.QrCodeScanner,
                            isComingSoon = true,
                            onClick = {
                                isFabExpanded = false
                                activeFabAlert = "Barcode Scanner Feature"
                            }
                        )
                        FABSubMenuOption(
                            label = "Generate Report",
                            icon = Icons.Rounded.Assessment,
                            isComingSoon = true,
                            onClick = {
                                isFabExpanded = false
                                activeFabAlert = "Report Generator"
                            }
                        )
                        FABSubMenuOption(
                            label = "Export Products",
                            icon = Icons.Rounded.SimCardDownload,
                            isComingSoon = true,
                            onClick = {
                                isFabExpanded = false
                                activeFabAlert = "Data Exporter"
                            }
                        )
                    }
                }

                // Main FAB Button
                val rotation by animateFloatAsState(if (isFabExpanded) 45f else 0f)
                FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = NTColors.Primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        "Actions Menu",
                        modifier = Modifier.size(28.dp).rotate(rotation)
                    )
                }
            }
        }

        // ── Dialogs & Sheets ───────────────────────────────────

        // 1. Sort Selection Dialog
        if (showSortDialog) {
            AlertDialog(
                onDismissRequest = { showSortDialog = false },
                title = { Text("Sort Products By", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProductSort.values().forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        sortBy = option
                                        showSortDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    option.displayName,
                                    fontSize = 14.sp,
                                    color = if (sortBy == option) NTColors.Primary else NTColors.TextPrimary,
                                    fontWeight = if (sortBy == option) FontWeight.Bold else FontWeight.Medium
                                )
                                if (sortBy == option) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        "Selected",
                                        tint = NTColors.Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSortDialog = false }) {
                        Text("Close", color = NTColors.TextSecondary)
                    }
                },
                containerColor = Color.White
            )
        }

        // 2. Filter Customization Dialog
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filter Products", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Status Selector
                        Column {
                            Text("Availability Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .padding(2.dp)
                            ) {
                                listOf("All", "Active", "Inactive", "Out of Stock").forEach { status ->
                                    val isSelected = filterStatus == status
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) NTColors.Primary else Color.Transparent)
                                            .clickable { filterStatus = status },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            status,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else NTColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Price Limits
                        Column {
                            Text("Selling Price Range (₹)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FormDialogInput(placeholder = "Min", value = priceMinInput, onValueChange = { priceMinInput = it }, isNumeric = true)
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    FormDialogInput(placeholder = "Max", value = priceMaxInput, onValueChange = { priceMaxInput = it }, isNumeric = true)
                                }
                            }
                        }

                        // Stock Limits
                        Column {
                            Text("Inventory Stock Range", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FormDialogInput(placeholder = "Min units", value = stockMinInput, onValueChange = { stockMinInput = it }, isNumeric = true)
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    FormDialogInput(placeholder = "Max units", value = stockMaxInput, onValueChange = { stockMaxInput = it }, isNumeric = true)
                                }
                            }
                        }

                        // Margin Limits
                        Column {
                            Text("Margin % Range", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FormDialogInput(placeholder = "Min %", value = marginMinInput, onValueChange = { marginMinInput = it }, isNumeric = true)
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    FormDialogInput(placeholder = "Max %", value = marginMaxInput, onValueChange = { marginMaxInput = it }, isNumeric = true)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showFilterDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply Filters", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            filterStatus = "All"
                            priceMinInput = ""
                            priceMaxInput = ""
                            stockMinInput = ""
                            stockMaxInput = ""
                            marginMinInput = ""
                            marginMaxInput = ""
                            showFilterDialog = false
                        }
                    ) {
                        Text("Clear All", color = NTColors.Error)
                    }
                },
                containerColor = Color.White
            )
        }

        // 3. Price Adjust Dialogue
        updatingPriceProduct?.let { prod ->
            var newSellPrice by remember { mutableStateOf(prod.defaultSellPrice.toInt().toString()) }
            var newCostPrice by remember {
                val currentCost = if (prod.supplierGroup == "GC") prod.purchasePriceGC else prod.purchasePriceMB
                mutableStateOf(currentCost.toInt().toString())
            }

            AlertDialog(
                onDismissRequest = { updatingPriceProduct = null },
                title = { Text("Update Pricing", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(prod.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)

                        Column {
                                Text("Selling Price (₹)", fontSize = 13.sp, color = NTColors.TextSecondary)
                                Spacer(Modifier.height(4.dp))
                                FormDialogInput(placeholder = "Selling Price", value = newSellPrice, onValueChange = { newSellPrice = it }, isNumeric = true)
                            }

                            Column {
                                Text("Cost Price (₹)", fontSize = 13.sp, color = NTColors.TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            FormDialogInput(placeholder = "Cost Price", value = newCostPrice, onValueChange = { newCostPrice = it }, isNumeric = true)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val sell = newSellPrice.toDoubleOrNull() ?: prod.defaultSellPrice
                            val cost = newCostPrice.toDoubleOrNull() ?: 0.0
                            val updated = if (prod.supplierGroup == "GC") {
                                prod.copy(defaultSellPrice = sell, purchasePriceGC = cost)
                            } else {
                                prod.copy(defaultSellPrice = sell, purchasePriceMB = cost)
                            }
                            onUpdateProduct(updated)
                            updatingPriceProduct = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Price", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { updatingPriceProduct = null },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = Color.White
            )
        }

        // 4. Inventory Adjust Dialogue
        adjustingInventoryProduct?.let { prod ->
            val upc = prod.unitsPerCase.coerceAtLeast(1)
            val isCan = upc == 1
            val currentCases = if (isCan) prod.stockAvailable else prod.stockAvailable / upc
            val remUnits = if (!isCan) prod.stockAvailable % upc else 0
            val stockTypeLabel = if (isCan) "Cans" else "Cases"
            var newStock by remember(prod) { mutableStateOf(currentCases.toString()) }

            AlertDialog(
                onDismissRequest = { adjustingInventoryProduct = null },
                title = { Text("Adjust Stock Level", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(prod.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)

                        val currentStockText = buildString {
                            append("Current: $currentCases $stockTypeLabel")
                            if (!isCan && remUnits > 0) {
                                append(" (+$remUnits loose)")
                            }
                        }
                        Text(
                            text = currentStockText,
                            fontSize = 12.sp, color = NTColors.TextTertiary
                        )

                        Column {
                            Text("New Stock Count ($stockTypeLabel)", fontSize = 13.sp, color = NTColors.TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            FormDialogInput(placeholder = "Stock Available", value = newStock, onValueChange = { newStock = it }, isNumeric = true)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val targetCases = newStock.toIntOrNull() ?: currentCases
                            val diff = (targetCases - currentCases) * upc
                            val finalStock = prod.stockAvailable + diff
                            onUpdateProduct(prod.copy(stockAvailable = finalStock))
                            adjustingInventoryProduct = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Stock", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { adjustingInventoryProduct = null },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = Color.White
            )
        }

        // 5. FAB Feature alerts
        activeFabAlert?.let { title ->
            AlertDialog(
                onDismissRequest = { activeFabAlert = null },
                icon = { Icon(Icons.Rounded.AutoAwesome, null, tint = NTColors.Primary, modifier = Modifier.size(32.dp)) },
                title = { Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
                text = {
                    Text(
                        "This premium SaaS workflow module is currently simulated for local presentation. Connect to a production server to link backend actions.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { activeFabAlert = null },
                        colors = ButtonDefaults.buttonColors(containerColor = NTColors.Primary)
                    ) {
                        Text("Okay", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White
            )
        }

        // 6. Delete Dialog (soft-deletes)
        deletingProduct?.let { p ->
            AlertDialog(
                onDismissRequest = { deletingProduct = null },
                icon = { Icon(Icons.Rounded.Delete, null, tint = NTColors.Error) },
                title = { Text(stringResource(Res.string.delete_product_confirm, p.displayName), fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        stringResource(Res.string.delete_product_warning),
                        fontSize = 13.sp,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onDeleteProduct(p.id); deletingProduct = null },
                        colors = ButtonDefaults.buttonColors(containerColor = NTColors.Error),
                    ) { Text(stringResource(Res.string.action_delete), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deletingProduct = null }) { Text(stringResource(Res.string.action_cancel)) }
                },
                containerColor = Color.White,
            )
        }

        // 7. Add/Edit Product Full Scrim & Bottom Sheet
        if (showForm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showForm = false },
            )
            ProductFormSheet(
                existing = editingProduct,
                onDismiss = { showForm = false },
                onSave = { updated ->
                    if (editingProduct == null) onAddProduct(updated) else onUpdateProduct(updated)
                    showForm = false
                },
                bottomInset = contentPadding.calculateBottomPadding(),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

// ── Product Summary Card Component ───────────────────────────────────────────
@Composable
private fun ProductCatalogSummaryCard(
    totalValue: Double,
    totalProducts: Int,
    activeProducts: Int,
    averageMargin: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color(0xFF0F172A).copy(alpha = 0.04f), spotColor = Color(0xFF0F172A).copy(alpha = 0.04f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        border = BorderStroke(1.dp, NTColors.Border)
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
                        text = "Total Catalog Value",
                        color = NTColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "₹${formatPrice(totalValue)}",
                        color = NTColors.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NTColors.PrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = NTColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = NTColors.Divider)
            Spacer(Modifier.height(16.dp))

            // Sub Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total SKUs", color = NTColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text("$totalProducts Products", color = NTColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1.5f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Active SKUs", color = NTColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text("$activeProducts Active", color = NTColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Avg Margin", color = NTColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${averageMargin.toInt()}%",
                        color = NTColors.Primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── AI Insights Component ──────────────────────────────────────────────────────
@Composable
private fun AIInsightsSection(products: List<ProductCategory>) {
    val insight = remember(products) {
        if (products.isEmpty()) {
            "No products registered in the database yet. Add your first product category above."
        } else {
            val activeProds = products.filter { it.isActive }
            val outOfStockProds = activeProds.filter { it.stockAvailable == 0 }
            val criticalProds = activeProds.filter { prod ->
                val upc = prod.unitsPerCase.coerceAtLeast(1)
                val cases = if (upc == 1) prod.stockAvailable else prod.stockAvailable / upc
                cases in 1..5
            }

            when {
                outOfStockProds.isNotEmpty() -> {
                    val names = outOfStockProds.joinToString { it.displayName }
                    "OUT OF STOCK: $names is currently out of stock. Restock immediately to resume distribution."
                }
                criticalProds.isNotEmpty() -> {
                    val details = criticalProds.joinToString { prod ->
                        val upc = prod.unitsPerCase.coerceAtLeast(1)
                        val cases = if (upc == 1) prod.stockAvailable else prod.stockAvailable / upc
                        "${prod.displayName} ($cases cases)"
                    }
                    "CRITICAL STOCK: $details running low. Restock immediately to secure customer deliveries."
                }
                activeProds.isNotEmpty() -> {
                    // Try to calculate highest profit margin SKU
                    val prodsWithMargin = activeProds.filter { it.defaultSellPrice > 0 }
                    val bestMarginProd = if (prodsWithMargin.isNotEmpty()) {
                        prodsWithMargin.maxByOrNull { prod ->
                            val cost = if (prod.supplierGroup == "GC") prod.purchasePriceGC else prod.purchasePriceMB
                            val margin = prod.defaultSellPrice - cost
                            (margin / prod.defaultSellPrice) * 100
                        }
                    } else null

                    val totalVal = activeProds.sumOf { it.stockAvailable * it.defaultSellPrice }
                    val bestValueProd = if (totalVal > 0) {
                        activeProds.maxByOrNull { it.stockAvailable * it.defaultSellPrice }
                    } else null

                    if (bestMarginProd != null) {
                        val cost = if (bestMarginProd.supplierGroup == "GC") bestMarginProd.purchasePriceGC else bestMarginProd.purchasePriceMB
                        val margin = bestMarginProd.defaultSellPrice - cost
                        val marginPercent = (margin / bestMarginProd.defaultSellPrice) * 100
                        val formattedPercent = ((marginPercent * 10).toInt() / 10.0).toString()
                        "OPTIMIZATION: ${bestMarginProd.displayName} offers the highest margin in the active catalog at ${formattedPercent}% (₹${margin.toInt()} profit per unit)."
                    } else if (bestValueProd != null) {
                        val prodVal = bestValueProd.stockAvailable * bestValueProd.defaultSellPrice
                        val percent = (prodVal / totalVal) * 100
                        val formattedPercent = ((percent * 10).toInt() / 10.0).toString()
                        "INVENTORY VALUE: ${bestValueProd.displayName} represents the largest share of inventory value at ₹${prodVal.toInt()} (${formattedPercent}% of total active inventory)."
                    } else {
                        val totalCases = activeProds.sumOf { prod ->
                            val upc = prod.unitsPerCase.coerceAtLeast(1)
                            if (upc == 1) prod.stockAvailable else prod.stockAvailable / upc
                        }
                        "CATALOG STATUS: ${activeProds.size} active SKUs holding a total of $totalCases cases/cans in stock."
                    }
                }
                else -> {
                    "All registered product categories are currently set as inactive. Activate them to show on shop dashboards."
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF0F9D8A).copy(alpha = 0.04f), Color(0xFF0F9D8A).copy(alpha = 0.08f))
                )
            )
            .border(1.dp, Color(0xFF0F9D8A).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                "AI Insight",
                tint = NTColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = insight,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = NTColors.TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

// ── Redesigned High Density Product Card ───────────────────────────────────────
@Composable
private fun ProductManagementCard(
    product: ProductCategory,
    onEdit: () -> Unit,
    onUpdatePrice: () -> Unit,
    onAdjustInventory: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColor = getProductColor(product.displayName)
    val upc = product.unitsPerCase.coerceAtLeast(1)
    val isCan = upc == 1
    val casesCount = if (isCan) product.stockAvailable else product.stockAvailable / upc
    val remUnits = if (!isCan) product.stockAvailable % upc else 0
    val stockTypeLabel = if (isCan) "Cans" else "Cases"

    val costPrice = if (product.supplierGroup == "GC") product.purchasePriceGC else product.purchasePriceMB
    val marginVal = product.defaultSellPrice - costPrice
    val marginPercent = if (product.defaultSellPrice > 0) (marginVal / product.defaultSellPrice) * 100 else 0.0
    val painter = productPainter(product.displayName)

    // Actions Dropdown Menu State
    var showDropdownMenu by remember { mutableStateOf(false) }

    // Margin status text & colors
    val (badgeText, badgeBg, badgeTextClr) = when {
        marginPercent >= 40.0 -> Triple("Excellent Margin", NTColors.SuccessLight, NTColors.Success)
        marginPercent >= 25.0 -> Triple("Good Margin", NTColors.PrimaryLight, NTColors.Primary)
        marginPercent >= 15.0 -> Triple("Fair Margin", NTColors.WarningLight, NTColors.WarningText)
        else -> Triple("Low Margin", NTColors.ErrorLight, NTColors.ErrorText)
    }

    // Stock Health values
    val (healthLabel, healthColor, healthPercent) = when {
        casesCount <= 0 -> Triple("Out of Stock", NTColors.Error, 0f)
        casesCount <= 2 -> Triple("Critical", NTColors.Error, 0.15f)
        casesCount <= 5 -> Triple("Low Stock", NTColors.Warning, 0.45f)
        else -> Triple("Healthy", NTColors.Success, (casesCount.toFloat() / 50f).coerceIn(0f, 1f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp), ambientColor = Color(0xFF0F172A).copy(alpha = 0.04f), spotColor = Color(0xFF0F172A).copy(alpha = 0.04f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (product.isActive) NTColors.Surface else NTColors.Background),
        border = BorderStroke(1.dp, if (!product.isActive) NTColors.TextDisabled else NTColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Row 1: Product Thumbnail, Details, Action menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Product Thumbnail (80.dp size matching ProductBreakdownCard)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (painter != null) NTColors.Surface else NTColors.SurfaceVar)
                        .border(1.dp, NTColors.Border, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (painter != null) {
                        Image(
                            painter = painter,
                            contentDescription = product.displayName,
                            contentScale = ContentScale.Fit,
                            alpha = if (product.isActive) 1f else 0.4f,
                            modifier = Modifier.fillMaxSize().padding(6.dp)
                        )
                    } else {
                        Box(modifier = Modifier) {
                            BottleSilhouette(
                                sizeKey = product.displayName,
                                color = if (product.isActive) themeColor else themeColor.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Detail Column (Name, Category/SKU, Active badge)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (product.isActive) NTColors.TextPrimary else NTColors.TextDisabled,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))

                        // Active/Inactive Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (product.isActive) NTColors.SuccessLight else NTColors.TextDisabled)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (product.isActive) "Active" else "Inactive",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (product.isActive) NTColors.SuccessText else NTColors.TextSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "SKU: ${product.name} • ${if (isCan) "Cans Category" else "Cases Category"}",
                        fontSize = 12.sp,
                        color = NTColors.TextTertiary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Action Menu Dropdown Button
                Box {
                    IconButton(
                        onClick = { showDropdownMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            "Actions",
                            tint = NTColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false },
                        modifier = Modifier.background(NTColors.Surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Details", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showDropdownMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Update Price", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Rounded.Payment, null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showDropdownMenu = false
                                onUpdatePrice()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Adjust Inventory", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Rounded.Inventory2, null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showDropdownMenu = false
                                onAdjustInventory()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate SKU", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showDropdownMenu = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (product.isActive) "Archive SKU" else "Activate SKU", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(if (product.isActive) Icons.Rounded.Archive else Icons.Rounded.Unarchive, null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showDropdownMenu = false
                                onToggleStatus()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Product", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NTColors.Error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = NTColors.Error, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showDropdownMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Row 2: Price, Margin, Stock Level in clean 3 columns (No divider line)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Price Column
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "Price",
                        color = NTColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "₹${product.defaultSellPrice.toInt()}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NTColors.TextPrimary
                    )
                }

                // Margin Column
                Column(modifier = Modifier.weight(2f)) {
                    Text(
                        text = "Margin",
                        color = NTColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "₹${marginVal.toInt()} (${marginPercent.toInt()}%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextClr
                    )
                }

                // Stock Level Column
                Column(
                    modifier = Modifier.weight(2f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$casesCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (casesCount <= 5) NTColors.Warning else NTColors.Primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stockTypeLabel,
                            fontSize = 13.sp,
                            color = NTColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        if (!isCan && remUnits > 0) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "(+$remUnits loose bottles)",
                                fontSize = 11.sp,
                                color = NTColors.TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    // Stock Health Progress Bar & label
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { healthPercent },
                            color = healthColor,
                            trackColor = Color(0xFFF1F5F9),
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = healthLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = healthColor
                        )
                    }
                }
            }
        }
    }
}

// ── FAB Submenu Options ────────────────────────────────────────────────────────
@Composable
private fun FABSubMenuOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isComingSoon: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            if (isComingSoon) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .background(NTColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, NTColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "COMING SOON",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = NTColors.Primary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(8.dp))
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NTColors.TextPrimary
                )
            }
        }

        Box(
            modifier = Modifier
                .shadow(2.dp, CircleShape)
                .size(36.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                label,
                tint = if (isComingSoon) NTColors.TextDisabled else NTColors.Primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ── Form Inputs Inside Dialogs ──────────────────────────────────────────────────
@Composable
private fun FormDialogInput(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    isNumeric: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, fontSize = 12.sp, color = NTColors.TextTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = {
                    if (!isNumeric || it.all { c -> c.isDigit() || c == '.' }) {
                        onValueChange(it)
                    }
                },
                textStyle = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NTColors.TextPrimary
                ),
                cursorBrush = SolidColor(NTColors.Primary),
                keyboardOptions = if (isNumeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Form Helper Methods ────────────────────────────────────────────────────────

private fun formatPrice(v: Double): String =
    if (v == v.toLong().toDouble()) {
        formatNumberSeparator(v.toLong())
    } else {
        v.toString()
    }

private fun formatPrice(v: Int): String = formatNumberSeparator(v.toLong())

private fun formatNumberSeparator(num: Long): String {
    val str = num.toString()
    if (str.length <= 3) return str
    val result = StringBuilder()
    var count = 0
    for (i in str.length - 1 downTo 0) {
        result.append(str[i])
        count++
        if (count == 3 && i > 0) {
            result.append(",")
        } else if (count > 3 && (count - 3) % 2 == 0 && i > 0) {
            // Indian numbering format (split by 2 digits after 3 digits)
            result.append(",")
        }
    }
    return result.reverse().toString()
}

private fun formatNumberCompact(num: Double): String =
    if (num >= 100000.0) {
        "${(num / 100000.0).toInt()}L"
    } else if (num >= 1000.0) {
        "${(num / 1000.0).toInt()}k"
    } else {
        num.toInt().toString()
    }

// ── Add/Edit form sheet ────────────────────────────────────────────────────────

@Composable
private fun ProductFormSheet(
    existing: ProductCategory?,
    onDismiss: () -> Unit,
    onSave: (ProductCategory) -> Unit,
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    var name        by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var displayName by remember(existing) { mutableStateOf(existing?.displayName ?: "") }
    var priceGC     by remember(existing) { mutableStateOf(existing?.purchasePriceGC?.let { if (it > 0) it.toString() else "" } ?: "") }
    var priceMB     by remember(existing) { mutableStateOf(existing?.purchasePriceMB?.let { if (it > 0) it.toString() else "" } ?: "") }
    var sellPrice   by remember(existing) { mutableStateOf(existing?.defaultSellPrice?.let { if (it > 0) it.toString() else "" } ?: "") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(NTColors.Surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {}
            .padding(
                start  = 20.dp,
                end    = 20.dp,
                top    = 20.dp,
                bottom = 20.dp + bottomInset.coerceAtLeast(12.dp),
            ),
    ) {
        Box(modifier = Modifier.width(40.dp).height(4.dp).background(NTColors.Border, RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        Text(
            if (existing == null) stringResource(Res.string.title_add_new_product) else stringResource(Res.string.title_edit_product),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NTColors.TextPrimary
        )
        Spacer(Modifier.height(20.dp))

        FormInput(stringResource(Res.string.hint_product_sku), name) { name = it }
        Spacer(Modifier.height(12.dp))
        FormInput(stringResource(Res.string.hint_display_name), displayName) { displayName = it }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.label_gc_purchase_price), fontSize = 13.sp, color = NTColors.TextTertiary)
                Spacer(Modifier.height(4.dp))
                PriceInput(priceGC) { priceGC = it }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.label_mb_purchase_price), fontSize = 13.sp, color = NTColors.TextTertiary)
                Spacer(Modifier.height(4.dp))
                PriceInput(priceMB) { priceMB = it }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column {
            Text(stringResource(Res.string.label_default_sell_price), fontSize = 13.sp, color = NTColors.TextTertiary)
            Spacer(Modifier.height(4.dp))
            PriceInput(sellPrice) { sellPrice = it }
        }
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
            Button(
                onClick = {
                    val product = (existing ?: ProductCategory(id = "", name = name, displayName = displayName)).copy(
                        name             = name.trim(),
                        displayName      = displayName.trim(),
                        purchasePriceGC  = priceGC.toDoubleOrNull() ?: 0.0,
                        purchasePriceMB  = priceMB.toDoubleOrNull() ?: 0.0,
                        defaultSellPrice = sellPrice.toDoubleOrNull() ?: 0.0,
                    )
                    onSave(product)
                },
                enabled  = name.isNotBlank() && displayName.isNotBlank(),
                modifier = Modifier.weight(2f).height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = NTColors.Primary),
            ) {
                Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (existing == null) stringResource(Res.string.action_add_product) else stringResource(Res.string.action_save_changes), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FormInput(placeholder: String, value: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(NTColors.SurfaceVar, RoundedCornerShape(10.dp))
            .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, fontSize = 14.sp, color = NTColors.TextDisabled)
            BasicTextField(
                value = value, onValueChange = onChange,
                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NTColors.TextPrimary),
                cursorBrush = SolidColor(NTColors.Primary), singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PriceInput(value: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(NTColors.SurfaceVar, RoundedCornerShape(10.dp))
            .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("₹", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextSecondary)
        Spacer(Modifier.width(4.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text("0.00", fontSize = 15.sp, color = NTColors.TextDisabled)
            BasicTextField(
                value = value, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onChange(it) },
                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary),
                cursorBrush = SolidColor(NTColors.Primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Product Image Resolver fallback painter ────────────────────────────────────
@Composable
private fun productPainter(sizeKey: String): Painter? = when {
    sizeKey.contains("200", true) -> painterResource(Res.drawable.bottle_200ml)
    sizeKey.contains("300", true) -> painterResource(Res.drawable.bottle_200ml)
    sizeKey.contains("500", true) -> painterResource(Res.drawable.bottle_500ml)
    sizeKey.contains("1",   true) -> painterResource(Res.drawable.bottle_2l)
    sizeKey.contains("2",   true) -> painterResource(Res.drawable.bottle_2l)
    sizeKey.contains("5",   true) -> painterResource(Res.drawable.bottle_5l)
    sizeKey.contains("20L",  true) -> painterResource(Res.drawable.bottle_20l)
    else -> null
}

// ── Canvas Bottle Silhouette Falling-back ──────────────────────────────────────
@Composable
private fun BottleSilhouette(sizeKey: String, color: Color) {
    val dims   = bottleDims[sizeKey] ?: BottleDims(28, 48, 14, 13)
    val totalW = (dims.bodyW + if (dims.hasHandle) 20 else 12).dp
    val totalH = (dims.bodyH + dims.neckH + 22).dp

    val capColor = Color(
        red   = (color.red   * 0.72f).coerceAtMost(1f),
        green = (color.green * 0.72f).coerceAtMost(1f),
        blue  = (color.blue  * 0.72f).coerceAtMost(1f),
    )

    Canvas(modifier = Modifier.size(width = totalW, height = totalH)) {
        val w  = size.width
        val h  = size.height
        val cx = w / 2f

        val bw = dims.bodyW.dp.toPx()
        val bh = dims.bodyH.dp.toPx()
        val nw = dims.neckW.dp.toPx()
        val nh = dims.neckH.dp.toPx()
        val capH      = 9.dp.toPx()
        val shoulderH = 8.dp.toPx()

        val bodyTop     = h - bh
        val shoulderTop = bodyTop - shoulderH
        val neckTop     = shoulderTop - nh
        val capTop      = neckTop - capH + 1.dp.toPx()

        if (dims.hasHandle) {
            val hx  = cx + bw / 2 - 1.dp.toPx()
            val hy1 = neckTop + 2.dp.toPx()
            val hy2 = bodyTop + 18.dp.toPx()
            drawPath(
                path = Path().apply {
                    moveTo(hx, hy1)
                    cubicTo(hx + 18.dp.toPx(), hy1, hx + 18.dp.toPx(), hy2, hx, hy2)
                },
                color = capColor,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        drawRoundRect(color = color, topLeft = Offset(cx - bw / 2, bodyTop), size = Size(bw, bh), cornerRadius = CornerRadius(5.dp.toPx()))
        drawRoundRect(color = Color.White.copy(alpha = 0.28f), topLeft = Offset(cx - bw / 2 + 3.dp.toPx(), bodyTop + bh * 0.22f), size = Size(bw - 6.dp.toPx(), bh * 0.42f), cornerRadius = CornerRadius(2.dp.toPx()))
        drawPath(path = Path().apply { moveTo(cx - bw / 2 + 4.dp.toPx(), bodyTop); lineTo(cx - nw / 2, shoulderTop); lineTo(cx + nw / 2, shoulderTop); lineTo(cx + bw / 2 - 4.dp.toPx(), bodyTop); close() }, color = color)
        drawRect(color = color.copy(alpha = 0.88f), topLeft = Offset(cx - nw / 2, neckTop), size = Size(nw, nh))
        val capW = nw + 4.dp.toPx()
        drawRoundRect(color = capColor, topLeft = Offset(cx - capW / 2, capTop), size = Size(capW, capH), cornerRadius = CornerRadius(3.dp.toPx()))
        drawRoundRect(color = capColor.copy(alpha = 0.6f), topLeft = Offset(cx - capW / 2 + 2.dp.toPx(), capTop + capH * 0.30f), size = Size(capW - 4.dp.toPx(), capH * 0.38f), cornerRadius = CornerRadius(1.dp.toPx()))
    }
}

private fun formatCases(value: Double): String {
    val integerPart = value.toInt()
    val fractionalPart = value - integerPart
    return when {
        fractionalPart == 0.0 -> integerPart.toString()
        else -> {
            val roundedFraction = (fractionalPart * 10).toInt()
            if (roundedFraction == 0) integerPart.toString() else "$integerPart.$roundedFraction"
        }
    }
}
