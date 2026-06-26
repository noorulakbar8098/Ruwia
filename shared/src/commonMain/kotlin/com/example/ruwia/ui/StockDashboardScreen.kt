package com.example.ruwia.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.unitsPerCase
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockItem
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.dashboard.*

// ─────────────────────────────────────────────────────────────
//  Stock Dashboard Screen
// ─────────────────────────────────────────────────────────────

private val FullChipBg    = Color(0xFFDFF5F1)
private val FullNumColor  = Color(0xFF0D9488)
private val EmptyChipBg   = Color(0xFFFFF1E6)
private val EmptyNumColor = Color(0xFFF97316)
private val CustChipBg    = Color(0xFFFFE8E8)
private val CustNumColor  = Color(0xFFEF4444)
private val InwardBg      = Color(0xFFDFF5F1)
private val InwardFg      = Color(0xFF0D9488)
private val OutwardBg     = Color(0xFFF0E0FF)
private val OutwardFg     = Color(0xFF9B3CC7)
private val DelivBg       = Color(0xFFE0EEFF)
private val DelivFg       = Color(0xFF3B7BE0)

@Composable
fun StockDashboardScreen(
    state: AdminState,
    onBack: () -> Unit,
    onRefresh: () -> Unit = {},
    onAddStock: () -> Unit = {},
    onAdjust: () -> Unit = {},
    onViewInventory: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        when {
            state.loading       -> NTDashboardSkeleton(contentPadding)
            state.error != null -> NTErrorState(
                message        = state.error,
                onRetry        = onRefresh,
                contentPadding = contentPadding,
            )
            else -> StockDashboardContent(
                shopStocks        = state.shopStocks,
                stockItems        = state.stockItems,
                movements         = state.recentMovements,
                onBack            = onBack,
                onRefresh         = onRefresh,
                onAddStock        = onAddStock,
                onAdjust          = onAdjust,
                onViewInventory   = onViewInventory,
                contentPadding    = contentPadding,
            )
        }
    }
}

@Composable
private fun StockDashboardContent(
    shopStocks: List<ShopStockInfo>,
    stockItems: List<StockItem>,
    movements: List<StockMovement>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAddStock: () -> Unit,
    onAdjust: () -> Unit,
    onViewInventory: () -> Unit = {},
    contentPadding: PaddingValues,
) {
    // ── Derive live shop totals from stock movements ─────────────────────────
    //
    // The `shop_stocks` table only stores the static `(name, location, is_live)`
    // metadata in this codebase — nothing currently maintains the
    // `total_cans / full_cans / empty_cans / cans_with_customers` columns when
    // movements happen. So instead of trusting those (always-zero) columns,
    // we recompute them client-side from the latest movement log.
    //
    //   • Inward + product (e.g. "Aqua Pure Plant" supplier)  → full cans IN
    //   • Inward + "Empty cans · …"                            → empty cans IN
    //   • Outward                                              → cans went to
    //                                                            customer
    //
    // `full_cans` = full_in − outward, `empty_cans` = empty_in,
    // `cans_with_customers` = outward, `total_cans` = sum of the three.
    val derivedShops = remember(shopStocks, movements, stockItems) {
        deriveShopStockTotals(shopStocks, movements, stockItems)
    }

    val totalCans  = derivedShops.sumOf { it.totalCans }
    val totalFull  = derivedShops.sumOf { it.fullCans }
    val totalEmpty = derivedShops.sumOf { it.emptyCans }
    val totalCust  = derivedShops.sumOf { it.cansWithCustomers }

    val inwardByShop = movements
        .filter { it.type == "inward" }
        .groupBy { shopMatchKey(it.shopName) }
        .mapValues { (_, v) -> v.sumOf { it.qty } }

    val outwardByShop = movements
        .filter { it.type == "outward" }
        .groupBy { shopMatchKey(it.shopName) }
        .mapValues { (_, v) -> v.sumOf { it.qty } }

    Column(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        StockTopBar(onBack = onBack, onRefresh = onRefresh)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            CombinedStockCard(
                totalCans       = totalFull,
                emptyCans       = totalEmpty,
                withCustomers   = totalCust,
                onAddStock      = onAddStock,
                onAdjust        = onAdjust,
                onViewInventory = onViewInventory,
            )
        }

        item { Spacer(modifier = Modifier.height(NTDp.xl)) }

        item {
            Text(
                "PER SHOP OVERVIEW",
                modifier      = Modifier.padding(horizontal = NTDp.screenPad),
                color         = NTColors.Primary,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
        }

        item { Spacer(modifier = Modifier.height(NTDp.md)) }

        if (derivedShops.isEmpty()) {
            // No shop rows configured — fall back to a single combined card
            // showing the per-product stock so the admin still sees the
            // catalogue-level breakdown immediately.
            if (stockItems.isNotEmpty()) {
                item {
                    AllProductsStockCard(
                        items    = stockItems,
                        modifier = Modifier.padding(horizontal = NTDp.screenPad),
                    )
                    Spacer(modifier = Modifier.height(NTDp.md))
                }
            } else {
                item {
                    NTEmptyState(
                        icon     = Icons.Rounded.Inventory2,
                        title    = "No Shops Configured",
                        subtitle = "Add shops in settings to see stock breakdown.",
                        modifier = Modifier.padding(NTDp.lg),
                    )
                }
            }
        } else {
            items(items = derivedShops, key = { it.id }) { shop ->
                val shopKey = shopMatchKey(shop.name)
                val liveIn = inwardByShop[shopKey] ?: 0
                val liveOut = outwardByShop[shopKey] ?: 0
                val shopMovements = movements.filter { shopMatchKey(it.shopName) == shopKey }
                ShopStockCard(
                    shop        = shop,
                    shopMovements = shopMovements,
                    productItems = stockItems,
                )
                Spacer(modifier = Modifier.height(NTDp.md))
            }
        }

        item { Spacer(modifier = Modifier.height(NTDp.sm)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = NTDp.screenPad),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "RECENT MOVEMENTS",
                    color         = NTColors.Primary,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "View full log",
                        color      = NTColors.Primary,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint     = NTColors.Primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(NTDp.md)) }

        if (movements.isEmpty()) {
            item {
                NTEmptyState(
                    icon     = Icons.Rounded.SwapVert,
                    title    = "No Movements Yet",
                    subtitle = "Stock inward and outward movements will appear here.",
                    modifier = Modifier.padding(NTDp.lg),
                )
            }
        } else {
            items(items = movements, key = { it.id }) { movement ->
                MovementRow(movement = movement)
                Spacer(modifier = Modifier.height(NTDp.sm))
            }
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }
    }
    }
}

// ── Top bar ───────────────────────────────────────────────────

@Composable
private fun StockTopBar(onBack: () -> Unit, onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Surface)
            .statusBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(NTColors.SurfaceVar, RoundedCornerShape(10.dp))
                    .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ArrowBack, null, tint = NTColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Stock Dashboard",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = NTColors.TextPrimary,
                )
                Text(
                    "Real-time stock overview across all shops",
                    fontSize = 12.sp,
                    color    = NTColors.TextTertiary,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(NTColors.SurfaceVar, RoundedCornerShape(10.dp))
                    .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
                    .clickable(onClick = onRefresh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Refresh, null, tint = NTColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Combined stock card ───────────────────────────────────────

@Composable
private fun CombinedStockCard(
    totalCans: Double,
    emptyCans: Double,
    withCustomers: Double,
    onAddStock: () -> Unit,
    onAdjust: () -> Unit,
    onViewInventory: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad)
            .background(NTColors.Surface, RoundedCornerShape(NTDp.radXxl))
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radXxl))
            .padding(NTDp.cardPad),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            // Left: icon label + count + breakdown
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(NTColors.PrimaryDark, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Inventory2,
                            contentDescription = null,
                            tint     = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "COMBINED · ALL SHOPS",
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = NTColors.Primary,
                        letterSpacing = 0.8.sp,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatCases(totalCans),
                        fontSize      = 38.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = NTColors.TextPrimary,
                        letterSpacing = (-1).sp,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "cans/cases",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = NTColors.TextSecondary,
                        modifier   = Modifier.padding(bottom = 5.dp),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "In Stock (Cases & Cans)",
                    fontSize   = 12.sp,
                    color      = NTColors.TextTertiary,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Right: sparkline + trend badge
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                StockSparkline()
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(NTDp.radFull))
                        .background(NTColors.SuccessLight)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint     = NTColors.SuccessText,
                        modifier = Modifier.size(12.dp),
                    )
                    Text("+12.6%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NTColors.SuccessText)
                }
                Text("vs last week", fontSize = 9.sp, color = NTColors.TextTertiary, modifier = Modifier.padding(top = 2.dp))
            }
        }

        Spacer(modifier = Modifier.height(NTDp.lg))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = NTDp.md),
            horizontalArrangement = Arrangement.spacedBy(NTDp.sm),
        ) {
            CanStatChip(totalCans, "AVAILABLE", Icons.Rounded.Inventory2, FullNumColor, FullChipBg, Modifier.weight(1f))
            CanStatChip(emptyCans, "EMPTY CANS", Icons.Rounded.Undo, EmptyNumColor, EmptyChipBg, Modifier.weight(1f))
            CanStatChip(withCustomers, "WITH CUSTOMERS", Icons.Rounded.Group, CustNumColor, CustChipBg, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(NTDp.sm)) {
            ActionTile(
                title     = "Add Stock",
                subtitle  = "Add new stock",
                icon      = Icons.Rounded.Add,
                iconBg    = NTColors.PrimaryDark,
                tileBg    = NTColors.PrimaryLight,
                textColor = NTColors.PrimaryDark,
                onClick   = onAddStock,
                modifier  = Modifier.weight(1f),
            )
            ActionTile(
                title     = "Inventory",
                subtitle  = "Full catalogue",
                icon      = Icons.Rounded.List,
                iconBg    = Color(0xFF8B5CF6),
                tileBg    = Color(0xFFEDE9FE),
                textColor = Color(0xFF5B21B6),
                onClick   = onViewInventory,
                modifier  = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StockSparkline() {
    val lineColor = NTColors.Primary
    Canvas(modifier = Modifier.size(width = 84.dp, height = 46.dp)) {
        val w = size.width
        val h = size.height
        val pts = listOf(0.72f, 0.52f, 0.68f, 0.42f, 0.60f, 0.38f, 0.78f, 1.00f)
        val step = w / (pts.size - 1)

        // Gradient fill
        val fill = Path().apply {
            moveTo(0f, h)
            pts.forEachIndexed { i, v -> lineTo(i * step, h - v * h * 0.82f) }
            lineTo(w, h)
            close()
        }
        drawPath(fill, Brush.verticalGradient(
            colors = listOf(lineColor.copy(alpha = 0.22f), Color.Transparent),
            startY = 0f, endY = h,
        ))

        // Line
        val line = Path().apply {
            pts.forEachIndexed { i, v ->
                val x = i * step; val y = h - v * h * 0.82f
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(line, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // Endpoint dot
        val ex = (pts.size - 1) * step
        val ey = h - pts.last() * h * 0.82f
        drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(ex, ey))
        drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(ex, ey))
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    tileBg: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tileBg)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            Text(subtitle, fontSize = 10.sp, color = textColor.copy(alpha = 0.65f))
        }
    }
}

// ── Per-shop card ─────────────────────────────────────────────

@Composable
private fun ShopStockCard(
    shop: ShopStockInfo,
    shopMovements: List<StockMovement>,
    productItems: List<StockItem> = emptyList(),
) {
    val expanded = remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad)
            .background(NTColors.Surface, RoundedCornerShape(NTDp.radXxl))
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radXxl))
            .clickable { expanded.value = !expanded.value }
            .padding(NTDp.cardPad),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(NTColors.PrimaryDark),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Store, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(NTDp.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    shop.name,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = NTColors.TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(shop.location, fontSize = 12.sp, color = NTColors.TextTertiary)
            }
            if (shop.isLive) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(NTDp.radFull))
                        .background(NTColors.SuccessLight)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NTColors.Success))
                    Text("LIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NTColors.SuccessText, letterSpacing = 0.5.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = if (expanded.value) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded.value) "Collapse" else "Expand",
                tint = NTColors.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(NTDp.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NTDp.sm),
        ) {
            CanStatChip(shop.fullCans, "AVAILABLE", Icons.Rounded.Inventory2, FullNumColor, FullChipBg, Modifier.weight(1f))
            CanStatChip(shop.emptyCans, "EMPTY CANS", Icons.Rounded.Undo, EmptyNumColor, EmptyChipBg, Modifier.weight(1f))
            CanStatChip(shop.cansWithCustomers, "WITH CUSTOMERS", Icons.Rounded.Group, CustNumColor, CustChipBg, Modifier.weight(1f))
        }

        if (expanded.value && productItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NTDp.md))
            HorizontalDivider(color = NTColors.Border)
            Spacer(modifier = Modifier.height(NTDp.md))
            Text("PRODUCT BREAKDOWN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NTColors.TextTertiary, letterSpacing = 0.8.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                productItems.forEach { product ->
                    val productRows = shopMovements.filter { it.productId == product.id }
                    val fullIn = productRows.filter { it.type == "inward" && !it.source.trim().startsWith("Empty cans", ignoreCase = true) }.sumOf { it.qty }
                    val sentOut = productRows.filter { it.type == "outward" }.sumOf { it.qty }
                    val totalUnits = (fullIn - sentOut).coerceAtLeast(0)
                    val cases = totalUnits / product.unitsPerCase
                    val remainder = totalUnits % product.unitsPerCase
                    
                    if (cases > 0 || remainder > 0 || totalUnits > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(product.name, fontSize = 13.sp, color = NTColors.TextSecondary)
                            val unitLabel = if (product.unitsPerCase > 1) "cases" else "cans"
                            val valText = buildString {
                                append("$cases $unitLabel")
                                if (remainder > 0 && product.unitsPerCase > 1) {
                                    append(" + $remainder units")
                                }
                            }
                            Text(valText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextPrimary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(NTDp.md))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Rounded.Bookmark, null, tint = NTColors.TextTertiary, modifier = Modifier.size(14.dp))
            Text(
                "${shop.totalCans} TOTAL CANS/CASES",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = NTColors.TextTertiary,
                letterSpacing = 0.4.sp,
            )
        }

        // ── Per-product breakdown ─────────────────────────────
        // Shows the product catalogue with their current stock counts so the
        // admin sees what's in stock by SKU/litre, not just aggregate totals.
        // The product table is global (not yet shop-scoped), so we surface
        // the same list under each shop card with a clear caption.
        if (expanded.value && productItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NTDp.md))
            HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(NTDp.sm))
            Text(
                "PRODUCTS · BY LITRE",
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp, color = NTColors.TextTertiary,
            )
            Spacer(modifier = Modifier.height(NTDp.sm))
            productItems.forEach { item ->
                ProductStockRow(item = item)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

// ── Per-product stock row ──────────────────────────────────────
//   Used both inside per-shop cards and the all-products fallback card so the
//   look stays consistent across both presentations.

@Composable
private fun ProductStockRow(item: StockItem) {
    val (badgeBg, badgeFg) = when {
        item.stockAvailable <= 0  -> NTColors.ErrorLight   to NTColors.Error
        item.stockAvailable <= 20 -> Color(0xFFFFF1E6)     to Color(0xFFF97316)
        else                       -> NTColors.SuccessLight to NTColors.Success
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.SurfaceVar, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                .background(NTColors.PrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Water, null, tint = NTColors.Primary, modifier = Modifier.size(15.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = NTColors.TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val capacityLabel = if (item.capacityLiters > 0) "${item.capacityLiters}L" else "—"
            val isCan = item.unitsPerCase == 1
            val unit = if (isCan) "can" else "case"
            Text(
                "$capacityLabel · ₹${item.pricePerCan.toInt()}/$unit",
                fontSize = 11.sp, color = NTColors.TextTertiary,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(NTDp.radFull))
                .background(badgeBg)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            val isCan = item.unitsPerCase == 1
            val displayCount = if (isCan) item.stockAvailable else item.stockAvailable / item.unitsPerCase.coerceAtLeast(1)
            val remUnits     = if (!isCan) item.stockAvailable % item.unitsPerCase.coerceAtLeast(1) else 0
            val units = if (isCan) "cans" else "cases"
            val label = if (!isCan && remUnits > 0) "$displayCount $units +$remUnits" else "$displayCount $units"
            Text(
                label,
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = badgeFg,
            )
        }
    }
}

// ── All-products card (no shops configured) ───────────────────
//   Fallback when `shop_stocks` is empty so the admin still sees the SKU-level
//   breakdown immediately after launching the app.

@Composable
private fun AllProductsStockCard(
    items: List<StockItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NTColors.Surface, RoundedCornerShape(NTDp.radXxl))
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radXxl))
            .padding(NTDp.cardPad),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(NTColors.PrimaryLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Inventory2, null, tint = NTColors.Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(NTDp.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ALL PRODUCTS",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp, color = NTColors.Primary,
                )
                Text(
                    "${items.size} SKUs · ${items.sumOf { it.stockAvailable }} cans/cases on hand",
                    fontSize = 12.sp, color = NTColors.TextTertiary,
                )
            }
        }
        Spacer(modifier = Modifier.height(NTDp.md))
        items.forEach { item ->
            ProductStockRow(item = item)
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CanStatChip(
    value: Double,
    label: String,
    icon: ImageVector,
    numColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = numColor, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            formatCases(value),
            fontSize      = 20.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = numColor,
            letterSpacing = (-0.3).sp,
        )
        Text(
            label,
            fontSize      = 8.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = numColor.copy(alpha = 0.75f),
            textAlign     = TextAlign.Center,
            letterSpacing = 0.3.sp,
            lineHeight    = 11.sp,
        )
    }
}

// ── Movement row ──────────────────────────────────────────────

@Composable
private fun MovementRow(movement: StockMovement) {
    val (iconBg, iconFg, icon) = when (movement.type) {
        "inward"  -> Triple(InwardBg,  InwardFg,  Icons.Rounded.ArrowDownward)
        "outward" -> Triple(OutwardBg, OutwardFg, Icons.Rounded.ArrowUpward)
        else      -> Triple(DelivBg,   DelivFg,   Icons.Rounded.Person)
    }
    val (amountText, amountColor) = when (movement.type) {
        "inward"  -> "+${movement.qty}" to NTColors.Success
        "outward" -> "-${movement.qty}" to NTColors.Error
        else      -> "-${movement.qty}" to NTColors.Primary
    }
    val typeDesc = when (movement.type) {
        "inward"  -> "Stock inward"
        "outward" -> "Stock outward"
        else      -> "Customer delivery"
    }
    val dateLabel = movement.createdAt?.take(10)?.let { d ->
        val parts = d.split("-")
        if (parts.size == 3) {
            val mon = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                .getOrNull((parts[1].toIntOrNull() ?: 1) - 1) ?: parts[1]
            "${parts[2]} $mon"
        } else d
    } ?: "—"

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = NTDp.screenPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconFg, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(NTDp.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                movement.source,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = NTColors.TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                "${movement.shopName} · $typeDesc",
                fontSize = 12.sp,
                color    = NTColors.TextTertiary,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(amountText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = amountColor)
            Text(dateLabel, fontSize = 11.sp, color = NTColors.TextTertiary)
        }
    }
}

// ── Shop-stock derivation helpers ─────────────────────────────────────────────
//
// The DB's `shop_stocks` columns aren't kept in sync with `stock_movements`
// (no triggers, and the repos only write the movement itself). Instead of
// teaching every write site to also bump those columns — which is fragile and
// race-prone — we derive the live numbers from the movement log every time
// the dashboard renders. This guarantees the screen always shows the truth
// even if `shop_stocks` is stale or freshly-seeded with zeros.

/**
 * Returns each shop with its [ShopStockInfo] columns recomputed from the
 * movement log. The original metadata fields (`id`, `name`, `location`,
 * `isLive`) are preserved.
 */
internal fun deriveShopStockTotals(
    rawShops: List<ShopStockInfo>,
    movements: List<StockMovement>,
    stockItems: List<StockItem>,
): List<ShopStockInfo> {
    if (rawShops.isEmpty()) return emptyList()
    val productMap = stockItems.associateBy { it.id }

    return rawShops.map { shop ->
        val key = shopMatchKey(shop.name)
        val rows = movements.filter { shopMatchKey(it.shopName) == key }
        
        val productNetFull = mutableMapOf<String, Int>()
        var unknownProductFull = 0
        
        rows.forEach { m ->
            val isFullIn = m.type == "inward" && !m.source.trim().startsWith("Empty cans", ignoreCase = true)
            val isSentOut = m.type == "outward"
            if (isFullIn || isSentOut) {
                val delta = if (isFullIn) m.qty else -m.qty
                if (m.productId != null) {
                    productNetFull[m.productId] = (productNetFull[m.productId] ?: 0) + delta
                } else {
                    unknownProductFull += delta
                }
            }
        }
        
        var totalCases = unknownProductFull.toDouble().coerceAtLeast(0.0)
        productNetFull.forEach { (pid, units) ->
            val p = productMap[pid]
            val actualUnits = units.coerceAtLeast(0)
            if (p != null) {
                totalCases += actualUnits.toDouble() / p.unitsPerCase.coerceAtLeast(1)
            } else {
                totalCases += actualUnits.toDouble()
            }
        }
        
        // Convert empty cans to case-equivalent units (20L & 5L cans stay as 1 = 1,
        // but smaller sizes use their case size so the total is in consistent "case" units)
        var emptyCases = 0.0
        rows.filter { it.source.isEmptyCansSource() }.forEach { m ->
            val delta = if (m.type == "inward") m.qty else -m.qty
            if (m.productId != null) {
                val p = productMap[m.productId]
                if (p != null) emptyCases += delta.toDouble() / p.unitsPerCase.coerceAtLeast(1)
                else emptyCases += delta.toDouble()
            } else {
                // No product linked — count each movement's qty individually.
                emptyCases += delta.toDouble()
            }
        }
        
        val productOutward = mutableMapOf<String, Int>()
        var unknownProductOutward = 0
        rows.filter { it.type == "outward" && !it.source.isEmptyCansSource() }.forEach { m ->
            if (m.productId != null) {
                productOutward[m.productId] = (productOutward[m.productId] ?: 0) + m.qty
            } else {
                unknownProductOutward += m.qty
            }
        }
        var withCust = unknownProductOutward.toDouble()
        productOutward.forEach { (pid, units) ->
            val p = productMap[pid]
            if (p != null) withCust += units.toDouble() / p.unitsPerCase.coerceAtLeast(1) else withCust += units.toDouble()
        }

        val fullCans = totalCases
        val totalCans = fullCans + emptyCases + withCust

        shop.copy(
            totalCans = totalCans,
            fullCans = fullCans,
            emptyCans = emptyCases,
            cansWithCustomers = withCust,
        )
    }
}

/**
 * Normalises a shop name like "Shop 1 · Main" / "Shop 1" / " shop 1 " into
 * the same lowercase comparison key. Shop names entered in the employee app
 * (`shopInfo.split("·")[0].trim()`) only carry the first segment, so we
 * strip everything after the bullet so they match the seeded names.
 */
internal fun shopMatchKey(name: String): String =
    name.split("·", limit = 2).firstOrNull()?.trim()?.lowercase() ?: name.trim().lowercase()

/** True if a movement source string represents returned empty cans. */
private fun String.isEmptyCansSource(): Boolean =
    trim().startsWith("Empty cans", ignoreCase = true)

/** Format a Double case count for display: whole numbers as "5", fractions as "5.5" etc. */
private fun formatCases(value: Double): String {
    val floored = kotlin.math.floor(value)
    return if (value == floored && !value.isInfinite()) {
        floored.toInt().toString()
    } else {
        val tenths = kotlin.math.round(value * 10).toInt()
        "${tenths / 10}.${tenths % 10}"
    }
}
