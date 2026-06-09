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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.ui.dashboard.*

// ─────────────────────────────────────────────────────────────
//  Stock Dashboard Screen — matches design mockup exactly
// ─────────────────────────────────────────────────────────────

@Composable
fun StockDashboardScreen(
    state: AdminState,
    onBack: () -> Unit,
    onRefresh: () -> Unit = {},
    onAddStock: () -> Unit = {},
    onAdjust: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)
            .statusBarsPadding()) {
        when {
            state.loading       -> NTDashboardSkeleton()
            state.error != null -> NTErrorState(message = state.error, onRetry = onRefresh)
            else -> StockDashboardContent(
                shopStocks  = state.shopStocks,
                movements   = state.recentMovements,
                onBack      = onBack,
                onRefresh   = onRefresh,
                onAddStock  = onAddStock,
                onAdjust    = onAdjust
            )
        }
    }
}

@Composable
private fun StockDashboardContent(
    shopStocks: List<ShopStockInfo>,
    movements: List<StockMovement>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAddStock: () -> Unit,
    onAdjust: () -> Unit
) {
    val totalCans  = shopStocks.sumOf { it.totalCans }
    val totalFull  = shopStocks.sumOf { it.fullCans }
    val totalEmpty = shopStocks.sumOf { it.emptyCans }
    val totalCust  = shopStocks.sumOf { it.cansWithCustomers }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NTColors.Background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Top bar ───────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = NTDp.md, vertical = NTDp.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StockTopBarButton(
                    icon    = Icons.Rounded.ArrowBack,
                    onClick = onBack
                )
                Text(
                    text = "Stock dashboard",
                    color = NTColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                        .padding(horizontal = NTDp.md)
                )
                StockTopBarButton(
                    icon    = Icons.Rounded.Refresh,
                    onClick = onRefresh
                )
            }
        }

        item { Spacer(modifier = Modifier.height(NTDp.sm)) }

        // ── Combined summary card ─────────────────────────────
        item {
            CombinedStockCard(
                totalCans  = totalCans,
                fullCans   = totalFull,
                emptyCans  = totalEmpty,
                withCust   = totalCust,
                onAddStock = onAddStock,
                onAdjust   = onAdjust
            )
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // ── Per shop section header ───────────────────────────
        item {
            Text(
                text = "PER SHOP",
                color = NTColors.Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(horizontal = NTDp.screenPad)
            )
            Spacer(modifier = Modifier.height(NTDp.md))
        }

        // ── Shop cards ────────────────────────────────────────
        if (shopStocks.isEmpty()) {
            item {
                NTEmptyState(
                    icon     = Icons.Rounded.Inventory2,
                    title    = "No Shops Configured",
                    subtitle = "Add shops in settings to see stock breakdown.",
                    modifier = Modifier.padding(NTDp.lg)
                )
            }
        } else {
            items(items = shopStocks, key = { it.id }) { shop ->
                ShopStockCard(shop = shop)
                Spacer(modifier = Modifier.height(NTDp.md))
            }
        }

        item { Spacer(modifier = Modifier.height(NTDp.xs)) }

        // ── Recent movements header ───────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = NTDp.screenPad),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT MOVEMENTS",
                    color = NTColors.Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "FULL LOG →",
                    color = NTColors.Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(NTDp.md))
        }

        // ── Movement items ────────────────────────────────────
        if (movements.isEmpty()) {
            item {
                NTEmptyState(
                    icon     = Icons.Rounded.SwapVert,
                    title    = "No Movements Yet",
                    subtitle = "Stock inward and outward movements will appear here.",
                    modifier = Modifier.padding(NTDp.lg)
                )
            }
        } else {
            items(items = movements, key = { it.id }) { movement ->
                MovementCard(movement = movement)
                Spacer(modifier = Modifier.height(NTDp.sm))
            }
        }
    }
}

// ── Top bar circular button ───────────────────────────────────

@Composable
private fun StockTopBarButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null,
            tint = NTColors.TextPrimary, modifier = Modifier.size(NTDp.iconMd))
    }
}

// ── Combined stock summary card ───────────────────────────────

@Composable
private fun CombinedStockCard(
    totalCans: Int,
    fullCans: Int,
    emptyCans: Int,
    withCust: Int,
    onAddStock: () -> Unit,
    onAdjust: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = NTDp.screenPad),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(NTDp.cardPad)) {
            Text(
                text = "COMBINED · ALL SHOPS",
                color = NTColors.Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(NTDp.sm))
            Text(
                text = "$totalCans cans",
                color = NTColors.TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(NTDp.xs))
            Text(
                text = "$fullCans FULL · $emptyCans EMPTY · $withCust WITH CUSTOMERS",
                color = NTColors.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
            Spacer(modifier = Modifier.height(NTDp.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(NTDp.md)) {
                StockActionButton(
                    label   = "Add stock",
                    icon    = Icons.Rounded.Add,
                    bgColor = NTColors.PrimaryLight,
                    fgColor = NTColors.Primary,
                    onClick = onAddStock,
                    modifier = Modifier.weight(1f)
                )
                StockActionButton(
                    label   = "Adjust",
                    icon    = Icons.Rounded.Tune,
                    bgColor = NTColors.AccentLight,
                    fgColor = NTColors.AccentDark,
                    onClick = onAdjust,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StockActionButton(
    label: String,
    icon: ImageVector,
    bgColor: Color,
    fgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(NTDp.radFull))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fgColor,
                modifier = Modifier.size(16.dp))
            Text(label, color = fgColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Per-shop card ─────────────────────────────────────────────

@Composable
private fun ShopStockCard(shop: ShopStockInfo) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = NTDp.screenPad),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(NTDp.cardPad)) {
            // Shop identity row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dark teal shop icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(NTDp.radMd))
                        .background(NTColors.PrimaryDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Store,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(NTDp.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shop.name,
                        color = NTColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${shop.location} · ${shop.totalCans} CANS",
                        color = NTColors.TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                }
                // Live badge
                if (shop.isLive) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(NTDp.radFull))
                            .background(NTColors.SuccessLight)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(6.dp).clip(CircleShape)
                                .background(NTColors.Success)
                        )
                        Text(
                            text = "LIVE",
                            color = NTColors.SuccessText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(NTDp.md))

            // Stat chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NTDp.sm)
            ) {
                StockStatChip(
                    value    = shop.fullCans,
                    label    = "FULL",
                    numColor = Color(0xFF0D9488),
                    bgColor  = Color(0xFFDFF5F1),
                    modifier = Modifier.weight(1f)
                )
                StockStatChip(
                    value    = shop.emptyCans,
                    label    = "EMPTY",
                    numColor = Color(0xFFF97316),
                    bgColor  = Color(0xFFFFF1E6),
                    modifier = Modifier.weight(1f)
                )
                StockStatChip(
                    value    = shop.cansWithCustomers,
                    label    = "CUST",
                    numColor = Color(0xFFEF4444),
                    bgColor  = Color(0xFFFFE8E8),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StockStatChip(
    value: Int,
    label: String,
    numColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(NTDp.radLg))
            .background(bgColor)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$value",
                color = numColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = label,
                color = NTColors.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ── Movement card ─────────────────────────────────────────────

@Composable
private fun MovementCard(movement: StockMovement) {
    val (iconBg, iconFg, icon) = when (movement.type) {
        "inward"     -> Triple(NTColors.SaleIconBg, NTColors.SaleIconFg, Icons.Rounded.Download)
        "outward"    -> Triple(NTColors.StockIconBg, NTColors.StockIconFg, Icons.Rounded.Upload)
        else         -> Triple(NTColors.WarningLight, NTColors.Warning, Icons.Rounded.Tune)
    }
    val amountText = when (movement.type) {
        "inward"  -> "+${movement.qty}"
        "outward" -> "-${movement.qty}"
        else      -> "~${movement.qty}"
    }
    val amountColor = when (movement.type) {
        "inward"  -> NTColors.Success
        "outward" -> NTColors.Error
        else      -> NTColors.Warning
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = NTDp.screenPad),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(NTDp.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(NTDp.radMd))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconFg,
                    modifier = Modifier.size(NTDp.iconLg))
            }
            Spacer(modifier = Modifier.width(NTDp.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(movement.source, color = NTColors.TextPrimary,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${movement.shopName} · ${movement.createdAt?.take(10) ?: ""}",
                    color = NTColors.TextTertiary, fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amountText, color = amountColor,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("CANS", color = NTColors.TextTertiary,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
