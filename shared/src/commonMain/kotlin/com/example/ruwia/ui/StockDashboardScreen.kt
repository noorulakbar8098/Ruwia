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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ruwia.domain.ShopStockInfo
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
    contentPadding: PaddingValues = PaddingValues(),
) {
    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background).statusBarsPadding()) {
        when {
            state.loading       -> NTDashboardSkeleton(contentPadding)
            state.error != null -> NTErrorState(
                message        = state.error,
                onRetry        = onRefresh,
                contentPadding = contentPadding,
            )
            else -> StockDashboardContent(
                shopStocks     = state.shopStocks,
                movements      = state.recentMovements,
                onBack         = onBack,
                onRefresh      = onRefresh,
                onAddStock     = onAddStock,
                onAdjust       = onAdjust,
                contentPadding = contentPadding,
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
    onAdjust: () -> Unit,
    contentPadding: PaddingValues,
) {
    val totalCans  = shopStocks.sumOf { it.totalCans }
    val totalFull  = shopStocks.sumOf { it.fullCans }
    val totalEmpty = shopStocks.sumOf { it.emptyCans }
    val totalCust  = shopStocks.sumOf { it.cansWithCustomers }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NTColors.Background),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        item { StockTopBar(onBack = onBack, onRefresh = onRefresh) }

        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            CombinedStockCard(
                totalCans  = totalCans,
                fullCans   = totalFull,
                emptyCans  = totalEmpty,
                withCust   = totalCust,
                onAddStock = onAddStock,
                onAdjust   = onAdjust,
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

        if (shopStocks.isEmpty()) {
            item {
                NTEmptyState(
                    icon     = Icons.Rounded.Inventory2,
                    title    = "No Shops Configured",
                    subtitle = "Add shops in settings to see stock breakdown.",
                    modifier = Modifier.padding(NTDp.lg),
                )
            }
        } else {
            items(items = shopStocks, key = { it.id }) { shop ->
                ShopStockCard(shop = shop)
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

// ── Top bar ───────────────────────────────────────────────────

@Composable
private fun StockTopBar(onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad, vertical = NTDp.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(NTColors.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.ArrowBack, null, tint = NTColors.TextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(NTDp.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Stock Dashboard",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = NTColors.TextPrimary,
            )
            Text(
                "Real-time stock overview across all shops",
                fontSize = 12.sp,
                color    = NTColors.TextTertiary,
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(NTColors.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Refresh, null, tint = NTColors.TextPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Combined stock card ───────────────────────────────────────

@Composable
private fun CombinedStockCard(
    totalCans: Int,
    fullCans: Int,
    emptyCans: Int,
    withCust: Int,
    onAddStock: () -> Unit,
    onAdjust: () -> Unit,
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
                        "$totalCans",
                        fontSize      = 38.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = NTColors.TextPrimary,
                        letterSpacing = (-1).sp,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "cans",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = NTColors.TextSecondary,
                        modifier   = Modifier.padding(bottom = 5.dp),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$fullCans Full · $emptyCans Empty · $withCust With Customers",
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

        Row(horizontalArrangement = Arrangement.spacedBy(NTDp.md)) {
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
                title     = "Adjust Stock",
                subtitle  = "Modify existing",
                icon      = Icons.Rounded.Tune,
                iconBg    = Color(0xFFF97316),
                tileBg    = Color(0xFFFFF1E6),
                textColor = Color(0xFFB94508),
                onClick   = onAdjust,
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
private fun ShopStockCard(shop: ShopStockInfo) {
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
            Icon(Icons.Rounded.ChevronRight, null, tint = NTColors.TextTertiary, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.height(NTDp.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NTDp.sm),
        ) {
            CanStatChip(shop.fullCans,          "FULL",          Icons.Rounded.Water,  FullNumColor,  FullChipBg,  Modifier.weight(1f))
            CanStatChip(shop.emptyCans,          "EMPTY",         Icons.Rounded.Water,  EmptyNumColor, EmptyChipBg, Modifier.weight(1f))
            CanStatChip(shop.cansWithCustomers, "WITH\nCUST.",   Icons.Rounded.Groups, CustNumColor,  CustChipBg,  Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(NTDp.md))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Rounded.Bookmark, null, tint = NTColors.TextTertiary, modifier = Modifier.size(14.dp))
            Text(
                "${shop.totalCans} TOTAL CANS",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = NTColors.TextTertiary,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

@Composable
private fun CanStatChip(
    value: Int,
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
            "$value",
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
