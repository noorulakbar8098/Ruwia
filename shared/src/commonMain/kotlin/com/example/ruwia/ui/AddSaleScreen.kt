package com.example.ruwia.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ruwia.domain.CanBalancePreview
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.SaleEntry
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.canBalancePreview
import com.example.ruwia.domain.effectiveStockMap
import com.example.ruwia.domain.is20LitreCan
import com.example.ruwia.domain.shopMatchKey
import com.example.ruwia.theme.RuwiaColor
import com.example.ruwia.util.dbToDisplayDate
import com.example.ruwia.util.isoToDisplayTime
import com.example.ruwia.ui.components.formKeyboardActions
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ── Multi-product line item ───────────────────────────────────────────────────

data class OutwardLineItem(
    val productIdx: Int,
    val qty: Int,
    val sellPriceText: String,
)

// ── Public entry-point ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, kotlin.uuid.ExperimentalUuidApi::class)
@Composable
fun AddSaleScreen(
    products: List<ProductCategory> = emptyList(),
    customers: List<Customer> = emptyList(),
    /** Shop-scoped stock movements used to compute live per-product available
     *  stock. Pass the employee's `shopMovements` from the ViewModel state. */
    stockMovements: List<StockMovement> = emptyList(),
    /** The currently selected customer for whom customer-specific pricing may apply. */
    selectedCustomer: Customer? = null,
    /** Callback invoked when the customer selection changes. */
    onCustomerChange: (Customer) -> Unit = {},
    /** Function to get the effective selling price for a customer and product.
     *  Returns the customer-specific price if available, otherwise the product's default sell price. */
    getEffectivePrice: (customerId: String, productId: String) -> Double = { _, _ -> 0.0 },
    /** Live custom-price cache (product id -> price) for the selected customer.
     *  Lines re-price whenever this map arrives/updates. */
    customPrices: Map<String, Double> = emptyMap(),
    errorMessage: String? = null,
    onNewCustomer: (Customer) -> Unit = {},
    onClearError: () -> Unit = {},
    onBack: () -> Unit,
    /** True while a save is in flight — the Save button locks to prevent
     *  double-tap duplicate sales. */
    isSaving: Boolean = false,
    /** Called when the employee taps "Save sale". The second arg is the
     *  selected customer's database id (required for the 20L can ledger).
     *  The fourth arg is the number of empty 20L cans the employee collected
     *  from this customer at delivery. The fifth arg is the selected sale date
     *  in display format (e.g. "29/09/2026"). The sixth arg is the selected
     *  sale time in display format (e.g. "2:30 PM"). The seventh arg is this
     *  draft's idempotency key: stable for the screen's lifetime, so retried/
     *  double-tapped saves with the same key can never deduct stock twice.
     *  The eighth arg is the shop the employee picked in the shop selector —
     *  the movement lands in that shop's bucket. */
    onSave: (customerName: String, customerId: String, items: List<OutwardLineItem>, emptyCans: Int, saleDate: String, saleTime: String, saleKey: String, saleShop: String, paymentReceived: String, paymentMode: String) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    /** The employee's assigned shop (default selection). */
    shopName: String = "",
    /** All shops of the business, from the backend — drives the shop
     *  selector, so renames flow through automatically. */
    shops: List<String> = emptyList(),
    /** Admin-only edit mode. Product, quantity, shop, date and payment stay
     *  locked; only the customer may change. */
    editingSale: SaleEntry? = null,
    /** All line rows belonging to the sale being edited (same client key). */
    editingSaleLines: List<SaleEntry> = emptyList(),
    onSaveEdit: (customerName: String, customerId: String) -> Unit = { _, _ -> },
) {
    val isEditing = editingSale != null
    val snackbarHostState = remember { SnackbarHostState() }
    /** Idempotency key for this sale draft (see onSave). */
    val saleKey = remember { Uuid.random().toString() }
    var errorDialogText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            errorDialogText = it
            snackbarHostState.showSnackbar(it)
        }
    }

    if (errorDialogText != null) {
        AlertDialog(
            onDismissRequest = { errorDialogText = null; onClearError() },
            title = { Text("Database Error", fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary) },
            text = { Text(errorDialogText ?: "", color = RuwiaColor.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { errorDialogText = null; onClearError() }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = RuwiaColor.TealPrimary)
                }
            },
            containerColor = RuwiaColor.Surface,
            shape = RoundedCornerShape(20.dp),
        )
    }

    val defaultProductIdx = if (products.isNotEmpty()) products.indices.last else 0
    val editingLines = remember(editingSale?.id, editingSaleLines, products) {
        editingSaleLines.ifEmpty { listOfNotNull(editingSale) }.mapNotNull { sale ->
            val idx = products.indexOfFirst { it.id == sale.productId }
                .takeIf { it >= 0 }
                ?: products.indexOfFirst { it.displayName == sale.productName }
                    .takeIf { it >= 0 }
                ?: return@mapNotNull null
            val price = sale.sellingPricePerUnit.takeIf { it > 0 }
                ?: products[idx].defaultSellPrice
            OutwardLineItem(
                productIdx = idx,
                qty = sale.qty.coerceAtLeast(1),
                sellPriceText = price.takeIf { it > 0 }?.toInt()?.toString().orEmpty(),
            )
        }
    }
    var lineItems by remember(products, editingLines) {
        // Only seed an initial line item if at least one product exists.
        // Otherwise the line-item renderer would crash trying to look up a
        // product in an empty list.
        mutableStateOf(
            when {
                isEditing -> editingLines
                products.isNotEmpty() -> {
                    val p = products.last()
                    listOf(
                        OutwardLineItem(
                            defaultProductIdx, 1,
                            p.defaultSellPrice.let {
                                if (it > 0) it.toInt().toString() else ""
                            }
                        )
                    )
                }
                else -> emptyList()
            }
        )
    }
    val customerProp = selectedCustomer
    val editingCustomer = remember(editingSale?.id, editingSale?.customerId, editingSale?.customerName, customers) {
        editingSale?.let { sale ->
            customers.firstOrNull { sale.customerId != null && it.id == sale.customerId }
                ?: customers.firstOrNull { it.name.equals(sale.customerName, ignoreCase = true) }
        }
    }
    var selectedCustomer   by remember(editingSale?.id) {
        mutableStateOf(editingCustomer ?: selectedCustomer)
    }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var editingLineIdx     by remember { mutableStateOf<Int?>(null) }
    var showProductPicker  by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }
    /** Number of empty 20L cans the employee received from the customer at
     *  delivery. Feeds both the shop's inward movement and the customer's
     *  20L can ledger. */
    var emptyCansText      by remember { mutableStateOf("") }
    var paymentReceivedText by remember { mutableStateOf("") }
    var paymentMode        by remember { mutableStateOf("cash") }

    // The picker optimistically selects a freshly created customer with a
    // local id. Replace it with the real DB row as soon as it arrives so the
    // can ledger always has a stable customer id to write against.
    LaunchedEffect(customerProp?.id) {
        val incoming = customerProp ?: return@LaunchedEffect
        val current  = selectedCustomer
        val incomingIsReal = incoming.id != null && !incoming.id.startsWith("local_")
        val currentIsLocal = current?.id == null || current.id.startsWith("local_")
        if (incomingIsReal && currentIsLocal &&
            current?.name?.equals(incoming.name, ignoreCase = true) == true
        ) {
            selectedCustomer = incoming
        }
    }

    LaunchedEffect(editingCustomer?.id) {
        if (isEditing && editingCustomer != null) selectedCustomer = editingCustomer
    }

    // A return belongs to one customer; never carry the typed count across a
    // customer switch.
    LaunchedEffect(selectedCustomer?.id) {
        emptyCansText = ""
        paymentReceivedText = ""
        paymentMode = "cash"
    }

    val liveSelectedCustomer = remember(selectedCustomer?.id, selectedCustomer?.name, customers) {
        val selected = selectedCustomer ?: return@remember null
        customers.firstOrNull { it.id != null && it.id == selected.id }
            ?: customers.firstOrNull { it.name.equals(selected.name, ignoreCase = true) }
            ?: selected
    }
    val previouslyOutstanding = liveSelectedCustomer?.cansHeld ?: 0
    val issued20LCans = remember(lineItems, products) {
        lineItems.sumOf { item ->
            val product = products.getOrNull(item.productIdx)
            if (product?.is20LitreCan() == true) item.qty else 0
        }
    }
    val invoiceTotal = remember(lineItems, products, selectedCustomer?.id, customPrices) {
        lineItems.sumOf { item ->
            val product = products.getOrNull(item.productIdx) ?: return@sumOf 0.0
            val custom = selectedCustomer?.id?.takeIf { it.isNotBlank() }?.let { cid ->
                customPrices[product.id]?.takeIf { it > 0 } ?: getEffectivePrice(cid, product.id)
            } ?: 0.0
            val fallback = if (custom > 0) custom else product.defaultSellPrice
            (item.sellPriceText.toDoubleOrNull() ?: fallback) * item.qty
        }
    }
    val paidNow = paymentReceivedText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    // Previous unpaid balance the customer already owes (from the DB row).
    val customerPreviousBalance = (liveSelectedCustomer?.balance ?: 0.0).coerceAtLeast(0.0)
    // Total the customer owes = old balance + this invoice.
    val totalDue = customerPreviousBalance + invoiceTotal
    val amountDue = (totalDue - paidNow).coerceAtLeast(0.0)
    val emptyCansReceived = emptyCansText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val canPreview = canBalancePreview(
        previouslyOutstanding = previouslyOutstanding,
        newCansIssued = issued20LCans,
        emptyCansReceived = emptyCansReceived,
    )

    /** Effective per-unit price text for a product and the selected customer:
     *  the customer's custom price when one exists, else the product default. */
    fun priceTextFor(product: ProductCategory): String {
        val custom = selectedCustomer?.id?.takeIf { it.isNotBlank() }?.let { cid ->
            customPrices[product.id]?.takeIf { it > 0 } ?: getEffectivePrice(cid, product.id)
        } ?: 0.0
        val price = if (custom > 0) custom else product.defaultSellPrice
        return if (price > 0) price.toInt().toString() else ""
    }

    // Whenever the customer changes — or their custom prices finish loading
    // from the database — re-price every line so a custom price is reflected
    // immediately in each sale count.
    LaunchedEffect(selectedCustomer?.id, customPrices) {
        val cid = selectedCustomer?.id
        if (!cid.isNullOrBlank() && products.isNotEmpty()) {
            lineItems = lineItems.map { item ->
                val product = products.getOrNull(item.productIdx) ?: return@map item
                item.copy(sellPriceText = priceTextFor(product))
            }
        }
    }

    // Sale timestamp is always the present moment — the employee cannot edit
    // it. Computed fresh at save time so it reflects the actual entry moment.
    fun nowSaleDate(): String = try {
        com.example.ruwia.util.toDisplayDate(Clock.System.now())
    } catch (_: Exception) { "" }
    fun nowSaleTime(): String = try {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hh = if (now.hour == 0) 12 else if (now.hour > 12) now.hour - 12 else now.hour
        val ampm = if (now.hour >= 12) "PM" else "AM"
        "$hh:${now.minute.toString().padStart(2, '0')} $ampm"
    } catch (_: Exception) { "" }

    val totalQty   = lineItems.sumOf { it.qty }
    val isSaveEnabled = !isSaving && selectedCustomer != null && lineItems.isNotEmpty() &&
        (isEditing || lineItems.all { it.qty > 0 })
    val displayDate = editingSale?.date?.takeIf { it.isNotBlank() }
        ?.let(::dbToDisplayDate) ?: nowSaleDate()
    val displayTime = editingSale?.createdAt?.takeIf { it.isNotBlank() }
        ?.let(::isoToDisplayTime)?.takeIf { it.isNotBlank() } ?: nowSaleTime()

    // ── Shop selector ─────────────────────────────────────────────────────
    // The sale deducts the picked shop's bucket, so the employee chooses
    // Shop 1 / Shop 2 here instead of a static assignment. Options come from
    // the backend, so admin renames flow through; matching is key-based
    // (see shopMatchKey), so a rename never orphans the selection.
    val shopOptions = remember(shopName, shops, editingSale?.shopId) {
        val source = if (isEditing) {
            listOf(editingSale?.shopId.orEmpty().ifBlank { shopName })
        } else {
            listOf(shopName) + shops
        }
        source
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { shopMatchKey(it) }
            .take(2)
            .takeIf { it.isNotEmpty() } ?: listOf("Shop 1")
    }
    // Keyless remember: background refreshes rebuild the options list every
    // 15 s, and re-keying on it would snap the employee's pick back to the
    // default (selection looks stuck). The effect below only assigns when
    // there is no valid selection yet (first load / renamed away).
    var selectedShop by remember { mutableStateOf("") }
    LaunchedEffect(shopName, shopOptions, editingSale?.id) {
        if (isEditing) {
            selectedShop = shopOptions.firstOrNull().orEmpty()
            return@LaunchedEffect
        }
        val currentValid = selectedShop.isNotBlank() &&
            shopOptions.any { shopMatchKey(it) == shopMatchKey(selectedShop) }
        if (!currentValid) {
            selectedShop = shopOptions.firstOrNull { shopMatchKey(it) == shopMatchKey(shopName) }
                ?: shopOptions.firstOrNull().orEmpty()
        }
    }

    // ── Per-product available stock, scoped to the picked shop ──────────────
    // Same formula as that shop's inventory tab (see effectiveStockMap), so
    // the "X Cases available" figures and the over-stock block always agree
    // with what the Stock tab shows for the selected shop.
    val availableUnitsMap: Map<String, Int> = remember(stockMovements, products, selectedShop) {
        effectiveStockMap(products, stockMovements, shopMatchKey(selectedShop).ifBlank { null })
    }

    fun saveSale() {
        if (!isSaveEnabled || isSaving) return
        if (isEditing) {
            val customer = selectedCustomer ?: return
            val customerId = liveSelectedCustomer?.id.orEmpty()
            if (customerId.isBlank() || customerId.startsWith("local_")) {
                errorDialogText = "Select a saved customer before saving this sale."
                return
            }
            onSaveEdit(customer.name, customerId)
            return
        }
        val empties = emptyCansText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val overStockLine = lineItems.firstOrNull { item ->
            val product = products.getOrNull(item.productIdx) ?: return@firstOrNull false
            val availRaw = availableUnitsMap[product.id] ?: product.stockAvailable
            item.qty > availRaw
        }
        if (overStockLine != null) {
            val product = products.getOrNull(overStockLine.productIdx)
            val availRaw = product?.let { availableUnitsMap[it.id] ?: it.stockAvailable } ?: 0
            errorDialogText = "Not enough stock for ${product?.displayName ?: "this product"}.\n\nRequested: ${overStockLine.qty} Cases\nAvailable: $availRaw Cases\n\nPlease reduce the quantity and try again."
            return
        }
        if (!canPreview.isWithinLimit) {
            errorDialogText = buildString {
                append("Cannot receive $emptyCansReceived empty 20L cans.\n\n")
                append("Previously outstanding: $previouslyOutstanding\n")
                append("New 20L cans in this sale: $issued20LCans\n")
                append("Maximum you can receive: ${canPreview.maxReturnable}")
            }
            return
        }
        val customerId = liveSelectedCustomer?.id.orEmpty()
        if ((issued20LCans > 0 || empties > 0 || paidNow > 0.0) &&
            (customerId.isBlank() || customerId.startsWith("local_"))
        ) {
            errorDialogText = "This customer is still saving. Wait a moment, then tap Save Sale again."
            return
        }
        if (paidNow > totalDue + 0.001) {
            errorDialogText = "Payment received (₹${paidNow.toInt()}) cannot exceed the total amount due of ₹${totalDue.toInt()}."
            return
        }
        val saleShop = selectedShop.ifBlank { shopOptions.firstOrNull().orEmpty() }
        onSave(
            selectedCustomer!!.name,
            customerId,
            lineItems,
            empties,
            nowSaleDate(),
            nowSaleTime(),
            saleKey,
            saleShop,
            paymentReceivedText,
            paymentMode,
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(RuwiaColor.Background)) {
        Scaffold(
            containerColor = RuwiaColor.Background,
            topBar    = { SaleTopBar(onBack = onBack) },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                SaleBottomBar(
                    isSaveEnabled = isSaveEnabled,
                    isSaving = isSaving,
                    lineCount = lineItems.size,
                    totalQty = totalQty,
                    onCancel = onBack,
                    saveLabel = if (isEditing) "Save Changes" else "Save Sale",
                    onSave = ::saveSale,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(14.dp))
                SaleHeroCard()
                Spacer(Modifier.height(14.dp))

                // ── Shop (which bucket this sale deducts) ──
                SaleFormSection(title = "Shop") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        shopOptions.forEach { option ->
                            val isSelected = shopMatchKey(option) == shopMatchKey(selectedShop)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) RuwiaColor.TealPrimary
                                        else RuwiaColor.Background
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) RuwiaColor.TealPrimary else RuwiaColor.Divider,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable(
                                        enabled = !isEditing,
                                        onClickLabel = "Sell from $option",
                                    ) {
                                        selectedShop = option
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    option,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else RuwiaColor.TextSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                // ── Customer ───────────────────────────────────
                SaleFormSection(title = "Customer") {
                    CustomerPickerRow(
                        selectedCustomer = selectedCustomer,
                        onClick          = { showCustomerPicker = true },
                    )
                }
                Spacer(Modifier.height(10.dp))

                // ── Products (multi) ─────────────────────────
                MultiProductSection(
                    products      = products,
                    lineItems     = lineItems,
                    availableUnitsMap = availableUnitsMap,
                    readOnly      = isEditing,
                    onChangeProduct = { lineIdx ->
                        editingLineIdx = lineIdx
                        showProductPicker  = true
                    },
                    onQtyChange   = { lineIdx, newQty ->
                        lineItems = lineItems.mapIndexed { i, item ->
                            if (i == lineIdx) item.copy(qty = newQty) else item
                        }
                    },
                    onRemove      = { lineIdx ->
                        lineItems = lineItems.filterIndexed { i, _ -> i != lineIdx }
                    },
                    onAdd         = {
                        // Guard against an empty products list — adding a
                        // line item with no product to bind to would crash
                        // the renderer.
                        val firstProduct = products.firstOrNull() ?: return@MultiProductSection
                        lineItems = lineItems + OutwardLineItem(
                            productIdx    = 0,
                            qty           = 1,
                            sellPriceText = priceTextFor(firstProduct),
                        )
                    },
                )
                Spacer(Modifier.height(10.dp))

                // ── Entry time (locked to now, not editable) ──
                SaleFormSection(title = "Entry time") {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SaleTimestampView(
                            icon    = Icons.Rounded.DateRange,
                            label   = displayDate,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        SaleTimestampView(
                            icon    = Icons.Rounded.Schedule,
                            label   = displayTime,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isEditing) "Original entry time · not editable"
                        else "Auto-recorded at entry time · not editable",
                        fontSize = 11.sp,
                        color = RuwiaColor.TextMuted,
                    )
                }
                Spacer(Modifier.height(20.dp))

                // ── Section 4: Customer 20L can ledger ──────────
                // Shows the customer's live outstanding balance, the 20L cans
                // issued in this sale, and the empties received. The returned
                // count is applied only after a successful save.
                if (!isEditing) {
                    CanTrackingSection(
                        customerName  = liveSelectedCustomer?.name ?: selectedCustomer?.name.orEmpty(),
                        preview       = canPreview,
                        value         = emptyCansText,
                        onValueChange = { newVal ->
                            if (newVal.length <= 5 && newVal.all { it.isDigit() }) {
                                emptyCansText = newVal
                            }
                        },
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // ── Section 5: Payment received ───────────────────
                // Captures the amount of payment received from the customer
                // at sale time.
                if (!isEditing) {
                    PaymentReceivedSection(
                        value = paymentReceivedText,
                        paid = paidNow,
                        due = amountDue,
                        totalDue = totalDue,
                        previousBalance = customerPreviousBalance,
                        invoiceTotal = invoiceTotal,
                        mode = paymentMode,
                        onValueChange = { newVal ->
                            if (newVal.all { it.isDigit() } || newVal.isEmpty()) {
                                // Clamp: never allow typing more than totalDue.
                                val entered = newVal.toDoubleOrNull() ?: 0.0
                                if (entered <= totalDue + 0.001) {
                                    paymentReceivedText = newVal
                                } else {
                                    paymentReceivedText = totalDue.toInt().toString()
                                }
                            }
                        },
                        onModeChange = { paymentMode = it },
                        onImeDone = ::saveSale,
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
        }

        // ── Customer picker overlay ────────────────────────────
        if (showCustomerPicker) {
            CustomerPickerOverlay(
                initialCustomers = customers,
                selectedCustomer = selectedCustomer,
                onDismiss        = { showCustomerPicker = false },
                onSelect         = { selectedCustomer = it; onCustomerChange(it) },
                onNewCustomer    = onNewCustomer,
            )
        }

        // ── Product picker overlay ─────────────────────────────
        if (showProductPicker) {
            val filteredProducts = remember(products, productSearchQuery) {
                if (productSearchQuery.isBlank()) products
                else products.filter {
                    it.displayName.contains(productSearchQuery, ignoreCase = true) ||
                    it.name.contains(productSearchQuery, ignoreCase = true)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showProductPicker = false; editingLineIdx = null; productSearchQuery = "" },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(RuwiaColor.Surface)
                    .navigationBarsPadding()
                    .padding(20.dp),
            ) {
                Text(
                    "Change product",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RuwiaColor.LightGray, RoundedCornerShape(10.dp))
                        .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    if (productSearchQuery.isEmpty()) {
                        Text("Search product...", fontSize = 13.sp, color = RuwiaColor.TextMuted)
                    }
                    BasicTextField(
                        value = productSearchQuery,
                        onValueChange = { productSearchQuery = it },
                        textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = RuwiaColor.TextPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = formKeyboardActions(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(14.dp))
                val currentIdx = editingLineIdx?.let { lineItems.getOrNull(it)?.productIdx } ?: -1
                Column(
                    modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filteredProducts.forEachIndexed { fIdx, p ->
                        val originalIdx = products.indexOf(p)
                        val selected = originalIdx == currentIdx
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) RuwiaColor.TealExtraLight else Color.Transparent)
                                .clickable {
                                    editingLineIdx?.let { lineIdx ->
                                        lineItems = lineItems.mapIndexed { i, item ->
                                            if (i == lineIdx) {
                                                item.copy(
                                                    productIdx    = originalIdx,
                                                    sellPriceText = priceTextFor(p),
                                                )
                                            } else item
                                        }
                                    }
                                    showProductPicker = false
                                    editingLineIdx    = null
                                    productSearchQuery = ""
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (selected) RuwiaColor.TealPrimary else RuwiaColor.TealExtraLight,
                                            RoundedCornerShape(8.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.WaterDrop, null,
                                        tint = if (selected) Color.White else RuwiaColor.TealPrimary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(p.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary)
                                    val limitText = availableUnitsMap[p.id]?.let {
                                        "$it Cases available"
                                    } ?: "Available stock: ${p.stockAvailable} Cases"
                                    Text(limitText, fontSize = 11.sp, color = RuwiaColor.TextMuted)
                                }
                            }
                            if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(18.dp))
                        }
                        if (fIdx < filteredProducts.lastIndex) HorizontalDivider(color = RuwiaColor.Divider)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun SaleTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.Background)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(RuwiaColor.Surface, RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = RuwiaColor.TextPrimary, modifier = Modifier.size(18.dp))
            }
            Text(
                text = "Add sale", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = RuwiaColor.TextPrimary, modifier = Modifier.align(Alignment.Center),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .border(1.2.dp, RuwiaColor.Divider, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.LocalShipping, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("EMP", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, color = RuwiaColor.TextSecondary)
            }
        }
    }
}

// ── Hero card ────────────────────────────────────────────────────────────────────

@Composable
private fun SaleHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RuwiaColor.TealDark),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.06f), size.height * 1.5f, Offset(size.width * 0.82f, size.height * 0.5f))
        }
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White,
                    modifier = Modifier.size(20.dp).rotate(-45f))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("RECORD SALE  ·  OUTWARD", fontSize = 10.sp, letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.70f))
                Spacer(Modifier.height(3.dp))
                Text("Case-Wise Entry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── Generic section card ───────────────────────────────────────────────────────

@Composable
private fun SaleFormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary)
        Spacer(Modifier.height(14.dp))
        content()
    }
}

// ── Multi-product section ────────────────────────────────────────────────────

@Composable
private fun MultiProductSection(
    products: List<ProductCategory>,
    lineItems: List<OutwardLineItem>,
    availableUnitsMap: Map<String, Int> = emptyMap(),
    readOnly: Boolean = false,
    onChangeProduct: (lineIdx: Int) -> Unit,
    onQtyChange: (lineIdx: Int, qty: Int) -> Unit,
    onRemove: (lineIdx: Int) -> Unit,
    onAdd: () -> Unit,
) {
    val totalQty = lineItems.sumOf { it.qty }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Products", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary, modifier = Modifier.weight(1f))
            if (totalQty > 0) {
                Box(
                    modifier = Modifier
                        .background(RuwiaColor.TealExtraLight, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text("$totalQty units", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TealPrimary)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // ── Empty state when no products are available ─────────────
        // Without this guard the line-item renderer below would crash
        // trying to look up a product in an empty list.
        if (products.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RuwiaColor.Background, RoundedCornerShape(12.dp))
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = RuwiaColor.TextMuted,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "No products available",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RuwiaColor.TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ask the admin to add products in Product Management before recording a sale.",
                    fontSize = 11.sp,
                    color = RuwiaColor.TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        lineItems.forEachIndexed { idx, item ->
            // Resolve the product safely. If the cached productIdx is out of
            // range (e.g. the product list shrank since the line was added),
            // fall back to the first product instead of crashing.
            val product = products.getOrNull(item.productIdx) ?: products.first()
            val availableUnitsRaw = availableUnitsMap[product.id] ?: product.stockAvailable

            if (idx > 0) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = RuwiaColor.Divider.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
            }
            SaleLineCard(
                lineNumber   = idx + 1,
                product      = product,
                qty          = item.qty,
                availableUnits = availableUnitsRaw,
                showRemove   = lineItems.size > 1 && !readOnly,
                readOnly     = readOnly,
                onProductTap = { onChangeProduct(idx) },
                onQtyChange  = { onQtyChange(idx, it) },
                onRemove     = { onRemove(idx) },
            )
        }

        if (!readOnly) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, RuwiaColor.TealLight, RoundedCornerShape(12.dp))
                    .clickable(onClick = onAdd)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add another product", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TealPrimary)
            }
        }
    }
}

@Composable
private fun SaleLineCard(
    lineNumber: Int,
    product: ProductCategory,
    qty: Int,
    availableUnits: Int,
    showRemove: Boolean,
    readOnly: Boolean = false,
    onProductTap: () -> Unit,
    onQtyChange: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val isOverStock = !readOnly && qty > availableUnits
    val cardBorder  = if (isOverStock) Color(0xFFEF4444) else RuwiaColor.Divider
    val cardBg      = if (isOverStock) Color(0xFF3B1616) else RuwiaColor.Background
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(12.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        // ── Product selector row ───────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(RuwiaColor.TealExtraLight, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$lineNumber", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TealPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(RuwiaColor.Surface, RoundedCornerShape(8.dp))
                    .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(8.dp))
                    .clickable(enabled = !readOnly, onClick = onProductTap)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.WaterDrop, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    product.displayName,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (!readOnly) {
                    Icon(Icons.Rounded.SwapHoriz, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(14.dp))
                }
            }
            if (showRemove) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color(0xFF3B1616), RoundedCornerShape(8.dp))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Qty stepper ────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Stepper
            Row(
                modifier = Modifier
                    .background(RuwiaColor.Surface, RoundedCornerShape(24.dp))
                    .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(24.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SaleStepBtn(Icons.Rounded.Remove, !readOnly && qty > 1) { onQtyChange(qty - 1) }
                Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = qty,
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
                    ) { targetQty ->
                        Text(
                            text = "$targetQty",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverStock) Color(0xFFCC2222) else RuwiaColor.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                SaleStepBtn(Icons.Rounded.Add, !readOnly && qty < availableUnits) { onQtyChange(qty + 1) }
            }
        }

        val availLabel = if (readOnly) "Quantity is locked while editing"
        else "$availableUnits Cases available"
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isOverStock) Color(0xFF3B1616) else RuwiaColor.TealExtraLight,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isOverStock) Icons.Rounded.Warning else Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = if (isOverStock) Color(0xFFEF4444) else RuwiaColor.TealPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    if (isOverStock) "Over limit! $availLabel" else availLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOverStock) Color(0xFFFCA5A5) else RuwiaColor.TealPrimary,
                )
            }
        }


    }
}

// ── Timestamp view (locked, non-interactive) ────────────────────────────────────

@Composable
private fun SaleTimestampView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextSecondary)
    }
}

// ── Bottom bar ────────────────────────────────────────────────────────────────

@Composable
private fun SaleBottomBar(
    isSaveEnabled: Boolean,
    isSaving: Boolean = false,
    lineCount: Int,
    totalQty: Int,
    saveLabel: String = "Save Sale",
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(totalQty) {
        if (totalQty > 0) {
            scale.animateTo(
                targetValue = 1.03f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .background(RuwiaColor.Surface)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = totalQty,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) { height -> height } + fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) { height -> -height } + fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))) togetherWith
                            (slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    }
                ) { targetQty ->
                    val itemsLabel = if (lineCount == 1) "1 Item" else "$lineCount Items"
                    val unitsLabel = if (targetQty == 1) "1 Unit" else "$targetQty Units"
                    Text(
                        text = "$itemsLabel, $unitsLabel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = RuwiaColor.TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RuwiaColor.TextSecondary),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(RuwiaColor.Divider))
                ) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onSave,
                    enabled = isSaveEnabled,
                    modifier = Modifier.weight(2f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RuwiaColor.TealPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = RuwiaColor.LightGray,
                        disabledContentColor = RuwiaColor.TextMuted
                    )
                ) {
                    Text(
                        if (isSaving) "Saving…" else saveLabel,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
private fun SaleStepBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) RuwiaColor.TealExtraLight else RuwiaColor.Background,
        animationSpec = tween(150)
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(32.dp)
            .background(bgColor, CircleShape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) RuwiaColor.TextSecondary else RuwiaColor.TextMuted, modifier = Modifier.size(16.dp)) }
}

// ── Customer 20L can ledger section ───────────────────────────────────────────
//   Shows the customer's live outstanding 20L cans, the cans issued in this
//   sale, and the empties received. The returned count is only applied to the
//   customer balance after the sale saves successfully.

@Composable
private fun CanTrackingSection(
    customerName: String,
    preview: CanBalancePreview,
    value: String,
    onValueChange: (String) -> Unit,
    onImeDone: (() -> Unit)? = null,
) {
    val canEdit = preview.maxReturnable > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(RuwiaColor.TealExtraLight, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Inventory2, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("20L Can Summary", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary)
                Text(customerName, fontSize = 11.sp, color = RuwiaColor.TextMuted)
            }
        }
        Spacer(Modifier.height(14.dp))

        CanSummaryRow("Previously outstanding", "${preview.previouslyOutstanding} cans")
        Spacer(Modifier.height(6.dp))
        CanSummaryRow("New 20L cans issued", "${preview.newCansIssued} cans")
        Spacer(Modifier.height(14.dp))

        Text("Empty 20L Cans Received", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RuwiaColor.LightGray, RoundedCornerShape(12.dp))
                .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Recycling,
                null,
                tint = if (canEdit) RuwiaColor.TextPrimary else RuwiaColor.TextMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text("0", fontSize = 16.sp, color = RuwiaColor.TextMuted, fontWeight = FontWeight.Bold)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = canEdit,
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = RuwiaColor.TextPrimary),
                    cursorBrush = SolidColor(RuwiaColor.TealPrimary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = formKeyboardActions(onDone = onImeDone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text("Cans", fontSize = 11.sp, color = RuwiaColor.TextSecondary)
        }

        if (!preview.isWithinLimit) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Only ${preview.maxReturnable} can(s) can be returned for this customer.",
                fontSize = 11.sp,
                color = Color(0xFFC0392B),
                fontWeight = FontWeight.SemiBold,
            )
        } else if (!canEdit) {
            Spacer(Modifier.height(8.dp))
            Text(
                "No outstanding 20L cans to return.",
                fontSize = 11.sp,
                color = RuwiaColor.TextMuted,
            )
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = RuwiaColor.Divider.copy(alpha = 0.7f))
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Outstanding after this transaction",
                fontSize = 12.sp,
                color = RuwiaColor.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${preview.outstandingAfter} cans",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = RuwiaColor.TealDark,
            )
        }
    }
}

@Composable
private fun CanSummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = RuwiaColor.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary)
    }
}
// ── Payment Received section ────────────────────────────────────────
//   Asks the employee how much payment they received from the customer at
//   sale time. Stored in the sale_entries.payment_received column.

@Composable
private fun PaymentReceivedSection(
    value: String,
    paid: Double,
    due: Double,
    totalDue: Double,
    previousBalance: Double,
    invoiceTotal: Double,
    mode: String,
    onValueChange: (String) -> Unit,
    onModeChange: (String) -> Unit,
    onImeDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RuwiaColor.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Payment Received", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RuwiaColor.TextPrimary)
                Text("Amount received from customer at sale time", fontSize = 11.sp, color = RuwiaColor.TextMuted)
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── Total due breakdown ──────────────────────────────────────────
        // Show previous balance + this sale = total due, so the employee
        // knows the full amount the customer owes before recording payment.
        if (previousBalance > 0.01) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3E0), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Previous balance", fontSize = 12.sp, color = RuwiaColor.TextSecondary, modifier = Modifier.weight(1f))
                    Text("₹${previousBalance.toInt()}", fontSize = 12.sp, color = Color(0xFFC05000), fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("This sale", fontSize = 12.sp, color = RuwiaColor.TextSecondary, modifier = Modifier.weight(1f))
                    Text("₹${invoiceTotal.toInt()}", fontSize = 12.sp, color = RuwiaColor.TextSecondary, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = RuwiaColor.Divider.copy(alpha = 0.5f))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Total due", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary, modifier = Modifier.weight(1f))
                    Text("₹${totalDue.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD9534F))
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RuwiaColor.LightGray, RoundedCornerShape(12.dp))
                .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Money, null, tint = RuwiaColor.TextPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text("0", fontSize = 16.sp, color = RuwiaColor.TextMuted, fontWeight = FontWeight.Bold)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = RuwiaColor.TextPrimary),
                    cursorBrush = SolidColor(RuwiaColor.TealPrimary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = formKeyboardActions(onDone = onImeDone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text("Amount", fontSize = 11.sp, color = RuwiaColor.TextSecondary)
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentSummaryCell("Paid Now", "₹${paid.toInt()}", RuwiaColor.TealPrimary, Modifier.weight(1f))
            PaymentSummaryCell("Total Due", "₹${due.toInt()}", Color(0xFFD9534F), Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("cash" to "Cash", "upi" to "UPI", "card" to "Card").forEach { (key, label) ->
                val selected = mode == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) RuwiaColor.TealPrimary else RuwiaColor.LightGray)
                        .clickable { onModeChange(key) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else RuwiaColor.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentSummaryCell(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(RuwiaColor.LightGray, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 10.sp, color = RuwiaColor.TextMuted, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
    }
}