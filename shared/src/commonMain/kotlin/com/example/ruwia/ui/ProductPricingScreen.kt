package com.example.ruwia.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.StockItem
import com.example.ruwia.ui.dashboard.*

// ─────────────────────────────────────────────────────────────
//  Product Pricing Screen
// ─────────────────────────────────────────────────────────────

// Palette per product index: bg color, can body color, can fill color
private val canPalette = listOf(
    Triple(Color(0xFFCCEFEB), Color(0xFFB2D8D4), Color(0xFF9AC6C2)), // 20L teal
    Triple(Color(0xFFDEF5F3), Color(0xFFC8ECE8), Color(0xFFB0E0DC)), // 10L light mint
    Triple(Color(0xFFFEE4CE), Color(0xFFF4C9A0), Color(0xFFEBB07A)), // 5L  warm orange
)

@Composable
fun ProductPricingScreen(
    stockItems: List<StockItem>,
    onBack: () -> Unit,
    onAddProduct: () -> Unit = {},
    onEditProduct: (StockItem) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)
            .statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Top bar ───────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = NTDp.md, vertical = NTDp.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PricingIconButton(icon = Icons.Rounded.ArrowBack, onClick = onBack)
                    Text(
                        "Product pricing",
                        color = NTColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(horizontal = NTDp.md)
                    )
                    PricingIconButton(icon = Icons.Rounded.Add, onClick = onAddProduct)
                }
            }

            // ── Info banner ───────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = NTDp.screenPad),
                    shape = RoundedCornerShape(NTDp.radXxl),
                    colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(NTDp.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NTDp.md)
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp)
                                .clip(CircleShape).background(NTColors.PrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null,
                                tint = NTColors.Primary, modifier = Modifier.size(NTDp.iconLg))
                        }
                        Column {
                            Text("${stockItems.size} active products",
                                color = NTColors.TextPrimary, fontSize = 15.sp,
                                fontWeight = FontWeight.Bold)
                            Text("UPDATE PRICES ANY TIME · APPLIES TO NEW ORDERS",
                                color = NTColors.TextTertiary, fontSize = 11.sp,
                                fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(NTDp.md)) }

            // ── Product cards ─────────────────────────────────
            itemsIndexed(items = stockItems, key = { _, s -> s.id ?: s.name }) { index, item ->
                ProductPriceCard(
                    item      = item,
                    palette   = canPalette.getOrElse(index) { canPalette.last() },
                    onEdit    = { onEditProduct(item) }
                )
                Spacer(modifier = Modifier.height(NTDp.md))
            }

            // ── Bulk pricing section ──────────────────────────
            item {
                Text(
                    "BULK PRICING",
                    color = NTColors.Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = NTDp.screenPad)
                )
                Spacer(modifier = Modifier.height(NTDp.sm))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = NTDp.screenPad),
                    shape = RoundedCornerShape(NTDp.radXxl),
                    colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(NTDp.cardPad)) {
                        BulkPricingRow("10+ cans (20L)",  "₹55 · save ₹5",   isDiscount = false)
                        HorizontalDivider(color = NTColors.Divider,
                            modifier = Modifier.padding(vertical = NTDp.md))
                        BulkPricingRow("25+ cans (20L)",  "₹50 · save ₹10",  isDiscount = false)
                        HorizontalDivider(color = NTColors.Divider,
                            modifier = Modifier.padding(vertical = NTDp.md))
                        BulkPricingRow("Subscription monthly", "–15% off", isDiscount = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductPriceCard(
    item: StockItem,
    palette: Triple<Color, Color, Color>,
    onEdit: () -> Unit
) {
    val margin = item.pricePerCan - item.costPrice

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = NTDp.screenPad),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(NTDp.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Can illustration
            Box(
                modifier = Modifier.size(width = 80.dp, height = 100.dp)
                    .clip(RoundedCornerShape(NTDp.radLg))
                    .background(palette.first),
                contentAlignment = Alignment.Center
            ) {
                PricingCanCanvas(
                    bodyColor = palette.second,
                    fillColor = palette.third,
                    label = "${item.capacityLiters}L",
                    modifier = Modifier.size(width = 44.dp, height = 80.dp)
                )
            }

            Spacer(modifier = Modifier.width(NTDp.md))

            // Name + tags + chips
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${item.capacityLiters}L Water\ncan",
                    color = NTColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.sp
                )
                if (item.tags.isNotEmpty()) {
                    Text(item.tags, color = NTColors.TextTertiary,
                        fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PriceChip(label = "COST", amount = "₹${item.costPrice.toInt()}",
                        bg = NTColors.SurfaceVar, fg = NTColors.TextSecondary)
                    PriceChip(label = "MARGIN", amount = "₹${margin.toInt()}",
                        bg = NTColors.PrimaryLight, fg = NTColors.Primary)
                }
            }

            Spacer(modifier = Modifier.width(NTDp.sm))

            // Price + edit button
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${item.pricePerCan.toInt()}",
                    color = NTColors.TextPrimary, fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                Text("PER CAN", color = NTColors.TextTertiary, fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(NTDp.sm))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(NTDp.radMd))
                        .background(NTColors.TextPrimary)
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(13.dp))
                        Text("EDIT", color = Color.White, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PricingCanCanvas(
    bodyColor: Color,
    fillColor: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val capW = w * 0.4f; val capX = (w - capW) / 2f; val capH = h * 0.09f
        val bodyW = w * 0.62f; val bodyX = (w - bodyW) / 2f
        val bodyTop = capH + h * 0.03f; val bodyH = h - bodyTop - h * 0.03f
        val r = CornerRadius(7.dp.toPx())
        drawRoundRect(color = bodyColor.copy(alpha = 0.6f),
            topLeft = Offset(capX, h * 0.01f), size = Size(capW, capH),
            cornerRadius = CornerRadius(3.dp.toPx()))
        drawRoundRect(color = bodyColor, topLeft = Offset(bodyX, bodyTop),
            size = Size(bodyW, bodyH), cornerRadius = r)
        val fillH = bodyH * 0.55f
        drawRoundRect(color = fillColor, topLeft = Offset(bodyX, bodyTop + bodyH - fillH),
            size = Size(bodyW, fillH), cornerRadius = r)
        drawArc(color = bodyColor.copy(alpha = 0.8f), startAngle = -90f, sweepAngle = 180f,
            useCenter = false, topLeft = Offset(bodyX + bodyW - 2.dp.toPx(), bodyTop + bodyH * 0.28f),
            size = Size(w * 0.18f, bodyH * 0.3f), style = Stroke(width = 2.5.dp.toPx()))
    }
}

@Composable
private fun PriceChip(label: String, amount: String, bg: Color, fg: Color) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(NTDp.radSm))
            .background(bg).padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(label, color = fg.copy(alpha = 0.7f), fontSize = 9.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        Text(amount, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BulkPricingRow(label: String, value: String, isDiscount: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = NTColors.TextSecondary, fontSize = 14.sp)
        Text(value, color = if (isDiscount) NTColors.Error else NTColors.Primary,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PricingIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = NTColors.TextPrimary,
            modifier = Modifier.size(NTDp.iconMd))
    }
}
