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
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.deriveShopStockTotals
import com.example.ruwia.domain.isEmptyCansSource
import com.example.ruwia.domain.shopMatchKey
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.presentation.EmployeeState
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.example.ruwia.presentation.EmployeeViewModel
import com.example.ruwia.ui.components.SaaSLoadingOverlay
import com.example.ruwia.ui.dashboard.NTBottomNavigation
import com.example.ruwia.ui.dashboard.NTColors
import com.example.ruwia.ui.dashboard.NTDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ruwia.ui.dashboard.NTNavTab
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ruwia.domain.Customer
import org.jetbrains.compose.resources.painterResource
import ruwia.shared.generated.resources.Res
import ruwia.shared.generated.resources.app_icon

// ── Entry display model ──────────────────────────────────────────────────────

private data class EntryDisplay(
    val name: String,
    val detail: String,
    val amount: String,
    val status: String,
    val isInward: Boolean,
)

private fun DeliveryTask.toEntryDisplay(): EntryDisplay = EntryDisplay(
    name     = customerName,
    detail   = "$canQty Cases  ·  $etaText",
    amount   = if (status == "delivered") "+₹${canQty * 28}" else "+$canQty",
    status   = if (status == "delivered") "SOLD" else status.replace("_", " ").uppercase(),
    isInward = false,
)

private fun StockMovement.toEntryDisplay(): EntryDisplay = EntryDisplay(
    name     = source,
    detail   = "$qty units · ${formatCreatedAtDateTime(createdAt)}",
    amount   = if (type == "inward") "+$qty" else "-$qty",
    status   = type.uppercase(),
    isInward = type == "inward",
)

// ── Screen navigation ─────────────────────────────────────────────────────────
//
// Three of these are full-screen overlays (no bottom nav):
//   • [EmpScreen.AddInward] — record a stock purchase from a supplier
//   • [EmpScreen.AddSale]   — record an outward sale to a customer
//   • [EmpScreen.Profile]   — the employee's profile + logout dialog
//
// The home / entries / stock tabs all stay inside the scaffold and are
// switched by the bottom-nav `selectedTab` index instead so the bottom bar
// never disappears between them.

private sealed class EmpScreen {
    object Home      : EmpScreen()
    object AddInward : EmpScreen()
    object AddSale   : EmpScreen()
    object Profile   : EmpScreen()
}

// ── Main composable ───────────────────────────────────────────────────────────

@Composable
fun EmployeeDashboardScreen(
    vm: EmployeeViewModel,
    onLogout: () -> Unit,
    employeeName: String = "Employee",
    shopInfo: String = "",
    userEmail: String = "",
) {
    val state by vm.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.loadDashboard()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }
    var screen by remember { mutableStateOf<EmpScreen>(EmpScreen.Home) }
    var selectedTab by remember { mutableStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }
    var isSubmittingAction by remember { mutableStateOf(false) }
    var showAddEmptyCasesDialog by remember { mutableStateOf(false) }
    var selectedCustomerForSale by remember { mutableStateOf<Customer?>(null) }

    LaunchedEffect(state.loading, state.error) {
        if (isSubmittingAction && !state.loading) {
            if (state.error == null) {
                screen = EmpScreen.Home
                isSubmittingAction = false
            } else {
                isSubmittingAction = false
            }
        }
    }

    if (isLoggingOut) {
        SaaSLoadingOverlay(message = "Signing Out")
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1000L)
            onLogout()
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title   = { Text("Logout?", fontWeight = FontWeight.Bold, color = Color.White) },
            text    = { Text("Are you sure you want to log out of your account?", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        isLoggingOut = true
                    },
                    colors  = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCC3333)),
                ) { Text("Logout", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
                ) { Text("Cancel") }
            },
            containerColor = Color(0xFF111C1F), // Match Employee App background
            shape = RoundedCornerShape(20.dp),
        )
    }

    val resolvedShopInfo = remember(state.assignedShop, state.shopStocks, shopInfo) {
        val matched = state.shopStocks.find { shopMatchKey(it.name) == shopMatchKey(state.assignedShop) }
        if (matched != null) {
            "${matched.name}  ·  ${matched.location}"
        } else {
            state.assignedShop.takeIf { it.isNotBlank() } ?: shopInfo
        }
    }

    // Push the parsed shop name down to the VM so its periodic refresh can
    // pull shop-scoped stock movements without re-asking the UI on every tick.
    val parsedShopName = remember(resolvedShopInfo) { resolvedShopInfo.split("·").getOrNull(0)?.trim().orEmpty() }
    LaunchedEffect(parsedShopName) { vm.setAssignedShop(parsedShopName) }

    LaunchedEffect(Unit) { vm.loadDashboard() }

    Box(modifier = Modifier.fillMaxSize().background(NTColors.Background)) {
        when (screen) {
            EmpScreen.AddInward -> {
                val shopPart = resolvedShopInfo.split("·").getOrNull(0)?.trim().orEmpty()
                val shopLoc  = resolvedShopInfo.split("·").getOrNull(1)?.trim().orEmpty()
                AddStockPurchaseScreen(
                    stockItems = emptyList(),
                    products   = state.productCategories,
                    shops      = listOf(shopPart to shopLoc).filter { it.first.isNotBlank() },
                    onBack     = { screen = EmpScreen.Home },
                    onClose    = { screen = EmpScreen.Home },
                    isEmployee = true,
                    onSave     = { productId, sku, brandName, purchasePrice, sellingPrice, qty, shopName, dateTimeIso, emptyCans ->
                        val saveShop = resolvedShopInfo.split("·").getOrNull(0)?.trim() ?: ""
                        isSubmittingAction = true
                        vm.addInwardStockEntry(
                            sku = sku,
                            brandName = brandName,
                            purchasePrice = purchasePrice,
                            sellingPrice = sellingPrice,
                            qty = qty,
                            shopName = saveShop,
                            createdAt = dateTimeIso,
                            emptyCans = emptyCans
                        )
                    },
                )
            }

            EmpScreen.AddSale -> {
                val shopPart = resolvedShopInfo.split("·").getOrNull(0)?.trim() ?: ""
                AddSaleScreen(
                    products      = state.productCategories,
                    customers     = state.customers,
                    stockMovements = state.shopMovements,
                    customPrices  = state.customPrices,
                    selectedCustomer = selectedCustomerForSale,
                    onCustomerChange = {
                        selectedCustomerForSale = it
                        vm.loadCustomPrices(it.id ?: "")
                    },
                    getEffectivePrice = { _, productId ->
                        state.customPrices[productId]
                            ?: state.productCategories.find { it.id == productId }?.defaultSellPrice
                            ?: 0.0
                    },
                    errorMessage  = state.error,
                    onNewCustomer = { vm.addCustomer(it) },
                    onClearError  = vm::clearError,
                    onBack        = { screen = EmpScreen.Home },
                    shopName      = shopPart,
                    onSave        = { customerName, items, emptyCans, saleDate, saleTime ->
                        // Persist the sale: each line item creates one sale_entries
                        // row and one outward stock movement; any empties picked up
                        // from the customer become an inward movement (visible to
                        // the admin on the stock dashboard).
                        val shopPart = resolvedShopInfo.split("·").getOrNull(0)?.trim() ?: ""
                        val parsedSaleDate = com.example.ruwia.util.displayDateToDb(saleDate)
                        // Combine date and time for proper timestamp
                        val saleDateTime = "$parsedSaleDate $saleTime"
                        val lines = items.mapNotNull { item ->
                            val product = state.productCategories.getOrNull(item.productIdx) ?: return@mapNotNull null
                            val sellPricePerUnit = item.sellPriceText.toDoubleOrNull() ?: product.defaultSellPrice
                            com.example.ruwia.data.EmployeeRepository.SaleLine(
                                productId            = product.id,
                                productName          = product.displayName.ifBlank { product.name },
                                qty                  = item.qty,
                                sellingPricePerUnit  = sellPricePerUnit,
                                purchasePricePerUnit = if (product.purchasePrice > 0) product.purchasePrice else product.purchasePriceGC,
                            )
                        }
                        if (lines.isNotEmpty()) {
                            isSubmittingAction = true
                            vm.addOutwardSale(
                                customerName       = customerName,
                                shopName           = shopPart,
                                lines              = lines,
                                emptyCansCollected = emptyCans,
                                saleDate           = saleDateTime,
                            )
                        }
                    },
                )
                // Loader while the sale is being saved. The overlay previously
                // only rendered on the Home tab, so Save appeared to do nothing.
                if (isSubmittingAction && state.loading) {
                    SaaSLoadingOverlay(message = "Saving Sale")
                }
            }

            EmpScreen.Profile -> {
                EmployeeProfileScreen(
                    employeeName = employeeName,
                    email        = userEmail,
                    shopInfo     = resolvedShopInfo,
                    // Real-time stats from the loaded dashboard state — these are the
                    // employee's own movements for today, filtered server-side by
                    // employee_id (see EmployeeRepository.getDailyCansSummary).
                    todayInward  = state.todayInward,
                    todayOutward = state.todayOutward,
                    customerCount = state.customers.size,
                    supplierCount = 0,
                    errorMessage  = state.error,
                    onClearError  = vm::clearError,
                    onAddCustomer = { vm.addCustomer(it) },
                    onBack       = {
                        // Return to whichever tab the user was on before opening Profile.
                        screen = EmpScreen.Home
                    },
                    onLogout     = onLogout,
                )
            }

            EmpScreen.Home -> {
                Scaffold(
                    containerColor = NTColors.Background,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        NTBottomNavigation(
                            selectedTab   = selectedTab,
                            onTabSelected = { tab ->
                                when (tab) {
                                    3    -> screen = EmpScreen.Profile
                                    else -> selectedTab = tab
                                }
                            },
                            tabs = listOf(
                                NTNavTab(0, "Home",    Icons.Rounded.Home),
                                NTNavTab(1, "Entries", Icons.Rounded.History),
                                NTNavTab(2, "Stock",   Icons.Rounded.Inventory2),
                                NTNavTab(3, "Profile", Icons.Rounded.Person),
                            ),
                        )
                    }
                ) { padding ->
                    when (selectedTab) {
                        1 -> EmployeeEntriesScreen(
                            movements      = state.recentEntries,
                            currentDate    = state.currentDate,
                            isLoading      = state.loading,
                            emptyCansTotal = state.emptyCansTotal,
                            products       = state.productCategories,
                            onRefresh      = { vm.loadDashboard() },
                            errorMessage   = state.error,
                            contentPadding = padding,
                        )

                        2 -> {
                            // Same Inventory UI as the admin side, but strictly
                            // view-only: every editing affordance is removed.
                            val inventoryState = remember(state) {
                                AdminState(
                                    productCategories = state.productCategories,
                                    // Shop-wide movements (not just this employee's)
                                    // so the live balances reflect every
                                    // colleague's activity at the shop.
                                    recentMovements = state.shopMovements,
                                    shopStocks = state.shopStocks,
                                    shopNames = state.shopStocks.map { it.name }.distinct().take(2),
                                    loading = state.loading,
                                )
                            }
                            StockInventoryScreen(
                                state = inventoryState,
                                onBack = {},
                                readOnly = true,
                                contentPadding = padding,
                            )
                        }

                        // Tab 0 (Home) — and the fallback. Tab 3 (Profile) is handled above
                        // via the `screen` overlay state.
                        else -> EmployeeHomeContent(
                            state          = state,
                            shopInfo       = resolvedShopInfo,
                            employeeName   = employeeName,
                            onLogout       = { showLogoutDialog = true },
                            onAddInward    = { screen = EmpScreen.AddInward },
                            onAddSale      = { screen = EmpScreen.AddSale },
                            onAddEmptyCases = { showAddEmptyCasesDialog = true },
                            onProfileClick = { screen = EmpScreen.Profile },
                            onTabSelected  = { selectedTab = it },
                            contentPadding = padding,
                        )
                    }
                }

                if (showAddEmptyCasesDialog) {
                    EmptyCasesStepperDialog(
                        currentCount = liveEmptyCases(state).toInt(),
                        onDismiss    = { showAddEmptyCasesDialog = false },
                        onConfirm    = { qty ->
                            showAddEmptyCasesDialog = false
                            vm.addEmptyCases(qty)
                        },
                    )
                }

                if (state.loading) {
                    SaaSLoadingOverlay(message = "Syncing Database")
                }
            }
        }
    }
}

// ── Home tab content ─────────────────────────────────────────────────────────
//   Pulled out of the main composable so each bottom-nav tab can render its
//   own LazyColumn without duplicating the scaffold/bottom-bar boilerplate.

// ── Recent activity display model ─────────────────────────────────────────────

private data class HomeActivityItem(
    val title: String,
    val detail: String,
    val dateTime: String,
    val isSale: Boolean
)

private fun formatCreatedAtDateTime(createdAt: String?): String =
    com.example.ruwia.util.isoToDisplayDateTime(createdAt)

private fun StockMovement.toHomeActivityItem(): HomeActivityItem {
    val isReturn = type == "inward" && source.isEmptyCansSource()
    val cleanSource = if (isReturn) {
        source.replace("Empty Cases", "")
            .replace("·", "")
            .trim()
    } else {
        source.trim()
    }

    val dateTimeStr = formatCreatedAtDateTime(createdAt)

    return if (isReturn) {
        HomeActivityItem(
            title = "Return from $cleanSource",
            detail = "$qty Empty Case${if (qty != 1) "s" else ""}",
            dateTime = dateTimeStr,
            isSale = false
        )
    } else {
        HomeActivityItem(
            title = "Sale to $cleanSource",
            detail = "$qty Case${if (qty != 1) "s" else ""}",
            dateTime = dateTimeStr,
            isSale = true
        )
    }
}

/** Live "Empty Cases" figure: admin-wide empty-cans movements minus the
 *  admin-set reset baseline, floored at 0. */
private fun liveEmptyCases(state: EmployeeState): Double {
    return (state.shopMovements
        .filter { it.source.isEmptyCansSource() }
        .sumOf { m ->
            if (m.type == "inward") m.qty.toDouble() else -m.qty.toDouble()
        }
        .coerceAtLeast(0.0) - state.emptyCansBaseline)
        .coerceAtLeast(0.0)
}

@Composable
private fun EmployeeHomeContent(
    state: com.example.ruwia.presentation.EmployeeState,
    shopInfo: String,
    employeeName: String,
    onLogout: () -> Unit,
    onAddInward: () -> Unit,
    onAddSale: () -> Unit,
    onAddEmptyCases: () -> Unit,
    onProfileClick: () -> Unit,
    onTabSelected: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    val visibleShops = state.shopStocks
    val visibleMovements = state.shopMovements
    val stockItems = remember(state.productCategories) {
        state.productCategories.map { com.example.ruwia.domain.StockItem(it.id, it.name, it.stockAvailable, 0) }
    }
    val derivedShops = remember(visibleShops, visibleMovements, stockItems, state.emptyCansBaseline) {
        deriveShopStockTotals(visibleShops, visibleMovements, stockItems, state.emptyCansBaseline.toDouble())
    }

    // The "Live Inventory" total must exactly match the Stock tab and the
    // per-product cards. We therefore derive it from the per-product on-hand
    // counts (already computed by the ViewModel from stock movements) rather
    // than a raw global movement sum, so the Home and Stock screens always agree.
    val totalFull = remember(state.productCategories) {
        state.productCategories.sumOf { it.stockAvailable.coerceAtLeast(0).toDouble() }
    }
    // Live "Empty Cases" is reported relative to the admin-set reset baseline.
    val totalEmpty = liveEmptyCases(state)
    val customerCount = state.customers.size

    val activityItems = remember(state.recentEntries) {
        state.recentEntries.map { it.toHomeActivityItem() }.take(5)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // ── Header ──────────────────────────────────
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                EmpHeader(
                    name           = employeeName,
                    shopInfo       = shopInfo,
                    onLogout       = onLogout,
                    onProfileClick = onProfileClick,
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Today's Stock Card ───────────────────────
        item {
            TodayStockCard(
                available     = totalFull,
                empty         = totalEmpty,
                customerCount = customerCount,
                modifier      = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Quick Actions ───────────────────────────
        item {
            Text(
                text       = "Quick Actions",
                fontSize   = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = NTColors.TextPrimary,
                modifier   = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        item {
            QuickActionGrid(
                onRecordSale      = onAddSale,
                onReturnEmptyCans = onAddInward,
                onViewStock       = { onTabSelected(2) },
                onHistory         = { onTabSelected(1) },
                onAddEmptyCases   = onAddEmptyCases,
                modifier          = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Recent Activity header ───────────────────
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text       = "Recent Activity",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = NTColors.TextPrimary,
                )
                Row(
                    modifier = Modifier.clickable { onTabSelected(1) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text       = "View all",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = NTColors.Primary,
                    )
                    Icon(
                        Icons.Rounded.ArrowForward, null,
                        tint = NTColors.Primary, modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Activity list ────────────────────────────
        if (state.loading) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color    = NTColors.Primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        } else if (activityItems.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No activity registered today",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = NTColors.TextTertiary
                    )
                }
            }
        } else {
            items(activityItems) { item ->
                RecentActivityItemRow(
                    item     = item,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun EmpHeader(
    name: String,
    shopInfo: String = "",
    onLogout: () -> Unit,
    onProfileClick: () -> Unit
) {
    val greeting = remember {
        try {
            val hour = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).hour
            when {
                hour < 12 -> "Good morning"
                hour < 17 -> "Good afternoon"
                else      -> "Good evening"
            }
        } catch (_: Exception) { "Good morning" }
    }
    val initials = remember(name) {
        name.split(" ").take(2)
            .mapNotNull { it.firstOrNull() }
            .joinToString("") { it.uppercase() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NTDp.radXxl))
            .background(
                Brush.linearGradient(
                    listOf(NTColors.PrimaryDeep, NTColors.GradStart, NTColors.GradEnd),
                    start = Offset.Zero,
                    end = Offset(1200f, 400f),
                )
            )
            .padding(NTDp.lg),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(NTDp.avatarLg)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                            .clickable(onClick = onProfileClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (initials.isBlank()) {
                            Image(
                                painter = painterResource(Res.drawable.app_icon),
                                contentDescription = "App logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = initials,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NTColors.TextOnPrimary,
                            )
                        }
                    }

                    Spacer(Modifier.width(NTDp.md))

                    Column {
                        Text(
                            text = greeting,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                        Text(
                            text = name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(NTDp.radMd))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(NTDp.radMd))
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(NTDp.iconMd),
                    )
                }
            }

            if (shopInfo.isNotEmpty()) {
                Spacer(Modifier.height(NTDp.lg))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(NTDp.radFull))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Store, null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = shopInfo.replace("·", "•"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

// ── Today's Stock Card ────────────────────────────────────────────────────────

@Composable
private fun TodayStockCard(
    available: Double,
    empty: Double,
    customerCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(NTDp.radXxl), ambientColor = NTColors.GradAccent, spotColor = NTColors.GradAccent),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Live Inventory",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NTColors.Primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${formatCases(available)} Cases",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = NTColors.TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(NTColors.PrimaryLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Inventory2, null, tint = NTColors.Primary, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = NTColors.Divider.copy(alpha = 0.6f))
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StockItemSmallCol(
                    value = formatCases(empty),
                    label = "Empty Cases",
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    color = NTColors.Warning
                )
                VerticalDivider(modifier = Modifier.height(32.dp), color = NTColors.Divider)
                StockItemSmallCol(
                    value = "$customerCount",
                    label = "Customers",
                    icon = Icons.Rounded.Group,
                    color = NTColors.Info
                )
            }
        }
    }
}

@Composable
private fun StockItemSmallCol(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NTColors.TextPrimary)
            Text(label, fontSize = 11.sp, color = NTColors.TextTertiary, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Quick Actions ─────────────────────────────────────────────────────────────

@Composable
private fun QuickActionGrid(
    onRecordSale: () -> Unit,
    onReturnEmptyCans: () -> Unit,
    onViewStock: () -> Unit,
    onHistory: () -> Unit,
    onAddEmptyCases: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuickActionCard(
                icon = Icons.Rounded.ShoppingCart,
                title = "Record Sale",
                onClick = onRecordSale,
                modifier = Modifier.weight(1f),
                color = NTColors.Primary
            )
            QuickActionCard(
                icon = Icons.Rounded.Autorenew,
                title = "Return Cases",
                onClick = onReturnEmptyCans,
                modifier = Modifier.weight(1f),
                color = NTColors.Warning
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuickActionCard(
                icon = Icons.Rounded.Inventory2,
                title = "View Stock",
                onClick = onViewStock,
                modifier = Modifier.weight(1f),
                color = NTColors.Info
            )
            QuickActionCard(
                icon = Icons.Rounded.History,
                title = "History",
                onClick = onHistory,
                modifier = Modifier.weight(1f),
                color = NTColors.Accent
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            QuickActionCard(
                icon = Icons.Rounded.Add,
                title = "Add Empty Cases",
                onClick = onAddEmptyCases,
                modifier = Modifier.weight(1f),
                color = NTColors.Success
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NTColors.Primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "pressScale"
    )

    Box(
        modifier = modifier
            .height(100.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(2.dp, RoundedCornerShape(NTDp.radXxl), ambientColor = NTColors.GradAccent, spotColor = NTColors.GradAccent)
            .background(NTColors.Surface, RoundedCornerShape(NTDp.radXxl))
            .border(1.dp, NTColors.Divider, RoundedCornerShape(NTDp.radXxl))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary
            )
        }
    }
}

// ── Recent Activity Row ────────────────────────────────────────────────────────

@Composable
private fun RecentActivityItemRow(
    item: HomeActivityItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(NTDp.radXxl), ambientColor = NTColors.GradAccent, spotColor = NTColors.GradAccent)
            .background(NTColors.Surface, RoundedCornerShape(NTDp.radXxl))
            .border(1.dp, NTColors.Divider, RoundedCornerShape(NTDp.radXxl))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Icon Container (36dp)
        val iconColor = if (item.isSale) NTColors.Primary else NTColors.Warning
        val icon = if (item.isSale) Icons.Rounded.ArrowUpward else Icons.AutoMirrored.Rounded.Undo

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        // Middle: Customer & Type
        Column(modifier = Modifier.weight(1f)) {
            val customerName = remember(item.title) {
                item.title.replace("Sale to ", "").replace("Return from ", "")
            }
            Text(
                text = customerName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NTColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (item.isSale) "Delivered • ${item.dateTime}" else "Returned • ${item.dateTime}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = NTColors.TextTertiary
            )
        }

        // Right: Quantity
        Text(
            text = item.detail,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = if (item.isSale) NTColors.Primary else NTColors.TextPrimary
        )
    }
}

/** Format a Double case count: whole numbers as "5", fractions as "5.5". */
private fun formatCases(value: Double): String {
    val floored = kotlin.math.floor(value)
    return if (value == floored && !value.isInfinite()) {
        floored.toInt().toString()
    } else {
        val tenths = kotlin.math.round(value * 10).toInt()
        "${tenths / 10}.${tenths % 10}"
    }
}