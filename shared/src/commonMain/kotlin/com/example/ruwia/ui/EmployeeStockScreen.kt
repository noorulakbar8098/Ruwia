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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.theme.RuwiaColor

// ─────────────────────────────────────────────────────────────────────────────
//  Employee Stock Tab
//  Read-only view of the live can balances for the shop this employee is
//  assigned to + the product catalog with running stock counts. Filters
//  shop_stocks by the employee's [shopName] so they only see their own shop.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmployeeStockScreen(
    shopStocks: List<ShopStockInfo>,
    products: List<ProductCategory>,
    movements: List<StockMovement> = emptyList(),
    isLoading: Boolean,
    onRefresh: () -> Unit,
    /** The shop the employee is assigned to (e.g. "Shop 1"). When non-blank
     *  the stock view is restricted to just that shop. Empty/blank → fall
     *  back to showing every shop (admin-style overview). */
    shopName: String = "",
    contentPadding: PaddingValues = PaddingValues(),
) {
    // Restrict shop list to the employee's assigned shop. We compare with the
    // same key normaliser the admin dashboard uses so "Shop 1", " shop 1 "
    // and "Shop 1 · Saibaba" all resolve to the same shop row.
    val visibleShops = remember(shopStocks, shopName) {
        if (shopName.isBlank()) shopStocks
        else {
            val key = shopMatchKey(shopName)
            shopStocks.filter { shopMatchKey(it.name) == key }
                // Defensive: if no match (e.g. typo in employees.shop_name)
                // fall back to the full list so the screen isn't empty.
                .ifEmpty { shopStocks }
        }
    }
    val visibleMovements = remember(movements, shopName) {
        if (shopName.isBlank()) movements
        else {
            val key = shopMatchKey(shopName)
            movements.filter { shopMatchKey(it.shopName) == key }
        }
    }

    // Live-derived shop totals — see [deriveShopStockTotals] in
    // StockDashboardScreen.kt for the rationale. The DB columns are stale
    // because nothing writes to them; we recompute from movements every
    // render so the employee sees the same numbers the admin does.
    val derivedShops = remember(visibleShops, visibleMovements) {
        deriveShopStockTotals(visibleShops, visibleMovements)
    }

    val totalCans  = derivedShops.sumOf { it.totalCans }
    val totalFull  = derivedShops.sumOf { it.fullCans }
    val totalEmpty = derivedShops.sumOf { it.emptyCans }
    val totalCust  = derivedShops.sumOf { it.cansWithCustomers }

    val heroTitle = if (shopName.isNotBlank() && derivedShops.size == 1)
        derivedShops.first().name.uppercase()
    else "ALL SHOPS · COMBINED"
    val heroSubtitle = if (shopName.isNotBlank()) "Your shop · live total" else "Live total"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RuwiaColor.Background),
        contentPadding = PaddingValues(
            top    = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
    ) {
        item { StockTabTopBar(onRefresh = onRefresh, shopName = shopName) }

        // ── Combined totals card ──────────────────────────────────────────
        item {
            CombinedStockHero(
                totalCans = totalCans,
                fullCans  = totalFull,
                emptyCans = totalEmpty,
                withCust  = totalCust,
                title     = heroTitle,
                subtitle  = heroSubtitle,
                modifier  = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(18.dp))
        }

        // ── Loading state ─────────────────────────────────────────────────
        if (isLoading && derivedShops.isEmpty() && products.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = RuwiaColor.TealPrimary, modifier = Modifier.size(28.dp))
                }
            }
            return@LazyColumn
        }

        // ── Per-shop section ──────────────────────────────────────────────
        if (derivedShops.isNotEmpty()) {
            val sectionLabel = if (shopName.isNotBlank() && derivedShops.size == 1)
                "YOUR SHOP" else "PER SHOP"
            item { StockSectionHeader(sectionLabel, modifier = Modifier.padding(horizontal = 20.dp)) }
            item { Spacer(Modifier.height(10.dp)) }

            items(items = derivedShops, key = { it.id }) { shop ->
                EmpShopStockCard(shop = shop, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(10.dp))
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        // ── Per-product section ───────────────────────────────────────────
        if (products.isNotEmpty()) {
            item { StockSectionHeader("PRODUCTS · BY LITRE", modifier = Modifier.padding(horizontal = 20.dp)) }
            item { Spacer(Modifier.height(10.dp)) }

            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .background(RuwiaColor.Surface, RoundedCornerShape(16.dp))
                        .padding(vertical = 4.dp),
                ) {
                    products.forEachIndexed { idx, product ->
                        ProductStockListRow(product = product)
                        if (idx < products.lastIndex) {
                            HorizontalDivider(
                                modifier  = Modifier.padding(horizontal = 14.dp),
                                color     = RuwiaColor.Divider.copy(alpha = 0.5f),
                                thickness = 0.6.dp,
                            )
                        }
                    }
                }
            }
        }

        // ── Empty state if nothing loaded at all ──────────────────────────
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
                    "Live can balances for $shopName"
                else
                    "Live can balances across all shops",
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

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun StockSectionHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        modifier      = modifier,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color         = RuwiaColor.TealPrimary,
    )
}

// ── Combined hero card ────────────────────────────────────────────────────────

@Composable
private fun CombinedStockHero(
    totalCans: Int,
    fullCans: Int,
    emptyCans: Int,
    withCust: Int,
    title: String = "ALL SHOPS · COMBINED",
    subtitle: String = "Live total",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RuwiaColor.TealDark)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Color.White.copy(alpha = 0.78f),
                )
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "$totalCans",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-1.5).sp,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "cans",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.18f), thickness = 0.8.dp)
        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            HeroStatCell("$fullCans",  "FULL",          Modifier.weight(1f))
            HeroStatDivider()
            HeroStatCell("$emptyCans", "EMPTY",         Modifier.weight(1f))
            HeroStatDivider()
            HeroStatCell("$withCust",  "WITH CUST.",    Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroStatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = Color.White.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun HeroStatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Color.White.copy(alpha = 0.15f)),
    )
}

// ── Per-shop card ─────────────────────────────────────────────────────────────

@Composable
private fun EmpShopStockCard(shop: ShopStockInfo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RuwiaColor.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        // Header row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(RuwiaColor.TealExtraLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Store,
                    contentDescription = null,
                    tint = RuwiaColor.TealPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    shop.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RuwiaColor.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (shop.location.isNotBlank()) {
                    Text(
                        shop.location,
                        fontSize = 12.sp,
                        color = RuwiaColor.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (shop.isLive) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(RuwiaColor.TealExtraLight)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(RuwiaColor.TealPrimary),
                    )
                    Text(
                        "LIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = RuwiaColor.TealPrimary,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Stat row — full / empty / with-cust
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShopStatChip(
                value    = shop.fullCans,
                label    = "FULL",
                icon     = Icons.Rounded.Water,
                bg       = RuwiaColor.TealExtraLight,
                fg       = RuwiaColor.TealPrimary,
                modifier = Modifier.weight(1f),
            )
            ShopStatChip(
                value    = shop.emptyCans,
                label    = "EMPTY",
                icon     = Icons.Rounded.Water,
                bg       = RuwiaColor.OrangeSurface,
                fg       = RuwiaColor.Orange,
                modifier = Modifier.weight(1f),
            )
            ShopStatChip(
                value    = shop.cansWithCustomers,
                label    = "WITH CUST.",
                icon     = Icons.Rounded.Groups,
                bg       = Color(0xFFFFE8E8),
                fg       = Color(0xFFEF4444),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Total chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Rounded.Bookmark,
                contentDescription = null,
                tint = RuwiaColor.TextMuted,
                modifier = Modifier.size(13.dp),
            )
            Text(
                "${shop.totalCans} TOTAL CANS",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = RuwiaColor.TextMuted,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

@Composable
private fun ShopStatChip(
    value: Int,
    label: String,
    icon: ImageVector,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "$value",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            letterSpacing = (-0.3).sp,
        )
        Text(
            label,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg.copy(alpha = 0.75f),
            letterSpacing = 0.3.sp,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp,
        )
    }
}

// ── Per-product row ──────────────────────────────────────────────────────────

@Composable
private fun ProductStockListRow(product: ProductCategory) {
    val (badgeBg, badgeFg) = when {
        product.stockAvailable <= 0  -> Color(0xFFFFE8E8)        to Color(0xFFCC3333)
        product.stockAvailable <= 20 -> RuwiaColor.OrangeSurface to RuwiaColor.Orange
        else                          -> RuwiaColor.TealExtraLight to RuwiaColor.TealPrimary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(RuwiaColor.TealExtraLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.WaterDrop,
                contentDescription = null,
                tint = RuwiaColor.TealPrimary,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                product.displayName.ifBlank { product.name },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = RuwiaColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${product.supplierGroup} · ₹${product.defaultSellPrice.toInt()}/can",
                fontSize = 11.sp,
                color = RuwiaColor.TextMuted,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(badgeBg)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                "${product.stockAvailable} cans",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = badgeFg,
            )
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
            "Ask the admin to add shops and products. Once they do, live can balances will show up here.",
            fontSize  = 13.sp,
            color     = RuwiaColor.TextMuted,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 12.dp),
            lineHeight = 18.sp,
        )
    }
}

