package com.example.ruwia.ui.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import org.jetbrains.compose.resources.painterResource
import ruwia.shared.generated.resources.Res
import ruwia.shared.generated.resources.bottle_200ml
import ruwia.shared.generated.resources.bottle_500ml
import ruwia.shared.generated.resources.bottle_2l
import ruwia.shared.generated.resources.bottle_5l
import ruwia.shared.generated.resources.bottle_20l

private val productColors = mapOf(
    "200ML"    to Color(0xFF8B5CF6),
    "500ML"    to Color(0xFF3B82F6),
    "1 Litre"  to Color(0xFF10B981),
    "2 Litre"  to Color(0xFF0D9488),
    "5 Litre"  to Color(0xFFF97316),
    "20 Litre" to Color(0xFF1E4F58),
)

// ── Canvas fallback dimensions (for products without a photo) ─────────────────

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

// ── Screen ─────────────────────────────────────────────────────────────────────

@Composable
fun ProductManagementScreen(
    products: List<ProductCategory> = emptyList(),
    onAddProduct: (ProductCategory) -> Unit = {},
    onUpdateProduct: (ProductCategory) -> Unit = {},
    onDeleteProduct: (String) -> Unit = {},
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var showForm       by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductCategory?>(null) }
    var deletingProduct by remember { mutableStateOf<ProductCategory?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        Scaffold(
            containerColor = NTColors.Background,
            topBar = { ProductMgmtTopBar(onBack = onBack, count = products.size) },
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start  = NTDp.screenPad,
                    end    = NTDp.screenPad,
                    top    = NTDp.md,
                    bottom = NTDp.md,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NTColors.PrimaryLight, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Info, null, tint = NTColors.Primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Tap a card to edit prices. Toggle the switch to activate or deactivate a product. Use the trash icon to delete.",
                            fontSize = 12.sp, color = NTColors.Primary, lineHeight = 16.sp,
                        )
                    }
                }

                items(products, key = { it.id }) { product ->
                    ProductGridCard(
                        product  = product,
                        onEdit   = { editingProduct = product; showForm = true },
                        onToggle = { onUpdateProduct(product.copy(isActive = !product.isActive)) },
                        onDelete = { deletingProduct = product },
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 80.dp))
                }
            }
        }

        // FAB positioned above the floating nav bar
        FloatingActionButton(
            onClick        = { editingProduct = null; showForm = true },
            containerColor = NTColors.Primary,
            contentColor   = Color.White,
            shape          = CircleShape,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        ) {
            Icon(Icons.Rounded.Add, "Add product", modifier = Modifier.size(26.dp))
        }

        if (showForm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showForm = false },
            )
            ProductFormSheet(
                existing  = editingProduct,
                onDismiss = { showForm = false },
                onSave    = { updated ->
                    if (editingProduct == null) onAddProduct(updated) else onUpdateProduct(updated)
                    showForm = false
                },
                bottomInset = contentPadding.calculateBottomPadding(),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // Delete confirmation dialog — soft-deletes (sets is_active=false on the
        // server) so any historical sale_entries still resolve their product_id.
        deletingProduct?.let { p ->
            AlertDialog(
                onDismissRequest = { deletingProduct = null },
                icon  = { Icon(Icons.Rounded.Delete, null, tint = NTColors.Error) },
                title = { Text("Delete ${p.displayName}?", fontWeight = FontWeight.Bold) },
                text  = {
                    Text(
                        "This product will be hidden from all screens. " +
                        "Existing sales records keep working — only the product " +
                        "is removed from your catalogue.",
                        fontSize = 13.sp,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onDeleteProduct(p.id); deletingProduct = null },
                        colors  = ButtonDefaults.buttonColors(containerColor = NTColors.Error),
                    ) { Text("Delete", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deletingProduct = null }) { Text("Cancel") }
                },
                containerColor = NTColors.Surface,
            )
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun ProductMgmtTopBar(onBack: () -> Unit, count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Background)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp)
                .background(NTColors.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, NTColors.Border, RoundedCornerShape(10.dp))
                .clickable(onClick = onBack)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.ArrowBack, "Back", tint = NTColors.TextPrimary, modifier = Modifier.size(18.dp)) }

        Text(
            "Products", fontSize = 17.sp, fontWeight = FontWeight.Bold,
            color = NTColors.TextPrimary, modifier = Modifier.align(Alignment.Center),
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(NTDp.radFull))
                .background(NTColors.PrimaryLight)
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .align(Alignment.CenterEnd),
        ) {
            Text("$count SKUs", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NTColors.Primary)
        }
    }
}

// ── Product image resolver ─────────────────────────────────────────────────────

@Composable
private fun productPainter(sizeKey: String): Painter? = when (sizeKey) {
    "200ML"    -> painterResource(Res.drawable.bottle_200ml)
    "500ML"    -> painterResource(Res.drawable.bottle_500ml)
    "2 Litre"  -> painterResource(Res.drawable.bottle_2l)
    "5 Litre"  -> painterResource(Res.drawable.bottle_5l)
    "20 Litre" -> painterResource(Res.drawable.bottle_20l)
    else       -> null  // 1 Litre → Canvas fallback
}

// ── Product grid card ──────────────────────────────────────────────────────────

@Composable
private fun ProductGridCard(
    product: ProductCategory,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val color  = productColors[product.name] ?: NTColors.Primary
    val dimmed = !product.isActive
    val painter = productPainter(product.name)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NTColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, NTColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit),
    ) {
        // ── Product image area ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(if (painter != null) SolidColor(Color.White) else Brush.verticalGradient(
                    colors = listOf(Color.White, color.copy(alpha = 0.10f)),
                )),
        ) {
            if (painter != null) {
                // Real product photograph
                Image(
                    painter            = painter,
                    contentDescription = product.displayName,
                    contentScale       = ContentScale.Fit,
                    alpha              = if (dimmed) 0.30f else 1f,
                    modifier           = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                )
            } else {
                // Canvas illustration fallback (1 Litre)
                Canvas(modifier = Modifier.size(110.dp).align(Alignment.Center)) {
                    val cx = size.width / 2; val cy = size.height / 2
                    listOf(1f, 0.70f, 0.45f).forEach { r ->
                        drawCircle(color = color.copy(alpha = 0.05f), radius = size.minDimension / 2 * r, center = Offset(cx, cy))
                    }
                }
                Box(modifier = Modifier.align(Alignment.Center)) {
                    BottleSilhouette(sizeKey = product.name, color = if (dimmed) color.copy(alpha = 0.28f) else color)
                }
            }

            // Supplier badge — top left
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = if (dimmed) 0.08f else 0.14f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(
                    product.supplierGroup,
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (dimmed) color.copy(alpha = 0.4f) else color,
                )
            }

            // Active / Inactive badge — top right
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (product.isActive) NTColors.SuccessLight else NTColors.SurfaceVar)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(
                    if (product.isActive) "Active" else "Off",
                    fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    color = if (product.isActive) NTColors.Success else NTColors.TextTertiary,
                )
            }
        }

        // ── Product details ────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                product.name,
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = if (dimmed) NTColors.TextDisabled else NTColors.TextPrimary,
            )
            Text(
                "Packaged Drinking Water",
                fontSize = 10.sp, color = NTColors.TextTertiary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(7.dp))

            // Sell price + margin badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "₹${product.defaultSellPrice.toInt()}",
                    fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (dimmed) NTColors.TextDisabled else NTColors.TextPrimary,
                )
                if (!dimmed && product.purchasePriceGC > 0) {
                    val margin = product.defaultSellPrice - product.purchasePriceGC
                    if (margin > 0) {
                        Spacer(Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NTColors.SuccessLight)
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        ) {
                            Text("+₹${margin.toInt()}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NTColors.Success)
                        }
                    }
                }
            }

            if (product.purchasePriceGC > 0 || product.purchasePriceMB > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "GC ₹${fmtPrice(product.purchasePriceGC)}  ·  MB ₹${fmtPrice(product.purchasePriceMB)}",
                    fontSize = 10.sp, color = NTColors.TextTertiary,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NTColors.PrimaryLight)
                            .clickable(onClick = onEdit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Edit, "Edit", tint = NTColors.Primary, modifier = Modifier.size(14.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NTColors.ErrorLight)
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Delete, "Delete", tint = NTColors.Error, modifier = Modifier.size(14.dp))
                    }
                }
                Switch(
                    checked  = product.isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = color,
                        uncheckedThumbColor = NTColors.TextDisabled,
                        uncheckedTrackColor = NTColors.SurfaceVar,
                    ),
                    modifier = Modifier.height(22.dp),
                )
            }
        }
    }
}

private fun fmtPrice(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

// ── Canvas bottle illustration (fallback for products without a photo) ─────────

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
    var supplier    by remember(existing) { mutableStateOf(existing?.supplierGroup ?: "GC") }
    var priceGC     by remember(existing) { mutableStateOf(existing?.purchasePriceGC?.let { if (it > 0) it.toString() else "" } ?: "") }
    var priceMB     by remember(existing) { mutableStateOf(existing?.purchasePriceMB?.let { if (it > 0) it.toString() else "" } ?: "") }
    var sellPrice   by remember(existing) { mutableStateOf(existing?.defaultSellPrice?.let { if (it > 0) it.toString() else "" } ?: "") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(NTColors.Surface)
            // Absorb taps on the sheet so they don't fall through to the scrim
            // behind it (which dismisses the sheet).
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {}
            // Surface is drawn before padding, so it extends to the very bottom edge,
            // while the content (incl. Save/Cancel) is lifted clear of the floating
            // bottom navigation bar. bottomInset already includes the system nav inset.
            .padding(
                start  = 20.dp,
                end    = 20.dp,
                top    = 20.dp,
                bottom = 20.dp + bottomInset.coerceAtLeast(12.dp),
            ),
    ) {
        Box(modifier = Modifier.width(40.dp).height(4.dp).background(NTColors.Border, RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        Text(if (existing == null) "Add New Product" else "Edit Product", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
        Spacer(Modifier.height(20.dp))

        FormInput("Product SKU (e.g. 200ML)", name) { name = it }
        Spacer(Modifier.height(12.dp))
        FormInput("Display Name (e.g. 200ML Water Bottle)", displayName) { displayName = it }
        Spacer(Modifier.height(12.dp))

        Text("Supplier Group", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NTColors.TextSecondary)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp)
                .background(NTColors.SurfaceVar, RoundedCornerShape(12.dp))
                .border(1.dp, NTColors.Border, RoundedCornerShape(12.dp))
                .padding(4.dp),
        ) {
            listOf("GC" to "Global Creators", "MB" to "Multi Brands").forEach { (tag, label) ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .background(if (supplier == tag) NTColors.Primary else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { supplier = tag },
                    contentAlignment = Alignment.Center,
                ) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (supplier == tag) Color.White else NTColors.TextSecondary) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("GC Purchase Price (₹)", fontSize = 11.sp, color = NTColors.TextTertiary)
                Spacer(Modifier.height(4.dp))
                PriceInput(priceGC) { priceGC = it }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("MB Purchase Price (₹)", fontSize = 11.sp, color = NTColors.TextTertiary)
                Spacer(Modifier.height(4.dp))
                PriceInput(priceMB) { priceMB = it }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column {
            Text("Default Sell Price (₹)", fontSize = 11.sp, color = NTColors.TextTertiary)
            Spacer(Modifier.height(4.dp))
            PriceInput(sellPrice) { sellPrice = it }
        }
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
            Button(
                onClick = {
                    val product = (existing ?: ProductCategory(id = "", name = name, displayName = displayName, supplierGroup = supplier)).copy(
                        name             = name.trim(),
                        displayName      = displayName.trim(),
                        supplierGroup    = supplier,
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
                Text(if (existing == null) "Add product" else "Save changes", fontWeight = FontWeight.Bold)
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
