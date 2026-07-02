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
import com.example.ruwia.domain.shopMatchKey
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.example.ruwia.presentation.EmployeeViewModel
import com.example.ruwia.theme.RuwiaColor
import com.example.ruwia.ui.components.SaaSLoadingOverlay
import com.example.ruwia.ui.dashboard.NTBottomNavigation
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ruwia.ui.dashboard.NTNavTab
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
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
    detail   = "$qty units · ${formatCreatedAtTime(createdAt)}",
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
    shopInfo: String = "Shop 1  ·  Saibaba",
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
            title   = { Text("Logout?") },
            text    = { Text("Are you sure you want to log out of your account?") },
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
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            },
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

    Box(modifier = Modifier.fillMaxSize().background(RuwiaColor.Background)) {
        when (screen) {
            EmpScreen.AddInward -> {
                AddStockPurchaseScreen(
                    stockItems = emptyList(),
                    products   = state.productCategories,
                    onBack     = { screen = EmpScreen.Home },
                    onClose    = { screen = EmpScreen.Home },
                    isEmployee = true,
                    onSave     = { productId, sku, brandName, purchasePrice, sellingPrice, qty, shopName, dateTimeIso, emptyCans ->
                        val shopPart = resolvedShopInfo.split("·").getOrNull(0)?.trim() ?: ""
                        isSubmittingAction = true
                        vm.addInwardStockEntry(
                            sku = sku,
                            brandName = brandName,
                            purchasePrice = purchasePrice,
                            sellingPrice = sellingPrice,
                            qty = qty,
                            shopName = shopPart,
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
                    errorMessage  = state.error,
                    onNewCustomer = { vm.addCustomer(it) },
                    onClearError  = vm::clearError,
                    onBack        = { screen = EmpScreen.Home },
                    shopName      = shopPart,
                    onSave        = { customerName, items, emptyCans, saleDate ->
                        // Persist the sale: each line item creates one sale_entries
                        // row and one outward stock movement; any empties picked up
                        // from the customer become an inward movement (visible to
                        // the admin on the stock dashboard).
                        val shopPart = resolvedShopInfo.split("·").getOrNull(0)?.trim() ?: ""
                        // Parse display date "25 Jun 2026" → "2026-06-25" for the DB.
                        val parsedSaleDate = run {
                            val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                            val parts = saleDate.split(" ")
                            if (parts.size == 3) {
                                val d = parts[0].padStart(2, '0')
                                val m = (months.indexOf(parts[1]) + 1).toString().padStart(2, '0')
                                val y = parts[2]
                                "$y-$m-$d"
                            } else null
                        }
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
                                saleDate           = parsedSaleDate,
                            )
                        }
                    },
                )
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
                    todaySales   = state.dailyEarnings,
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
                    containerColor = RuwiaColor.Background,
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
                    },
                ) { padding ->
                    when (selectedTab) {
                        1 -> EmployeeEntriesScreen(
                            movements      = state.recentEntries,
                            todayInward    = state.todayInward,
                            todayOutward   = state.todayOutward,
                            todayEmptyCans = state.todayEmptyCans,
                            dailyEarnings  = state.dailyEarnings,
                            currentDate    = state.currentDate,
                            isLoading      = state.loading,
                            products       = state.productCategories,
                            onRefresh      = { vm.loadDashboard() },
                            contentPadding = padding,
                        )

                        2 -> EmployeeStockScreen(
                            shopStocks     = state.shopStocks,
                            products       = state.productCategories,
                            // Shop-wide movements (not just this employee's) so the live
                            // can balances reflect every colleague's activity at the shop.
                            movements      = state.shopMovements,
                            isLoading      = state.loading,
                            onRefresh      = { vm.loadDashboard() },
                            shopName       = state.assignedShop,
                            contentPadding = padding,
                        )

                        // Tab 0 (Home) — and the fallback. Tab 3 (Profile) is handled above
                        // via the `screen` overlay state.
                        else -> EmployeeHomeContent(
                            state          = state,
                            shopInfo       = resolvedShopInfo,
                            employeeName   = employeeName,
                            onLogout       = { showLogoutDialog = true },
                            onAddInward    = { screen = EmpScreen.AddInward },
                            onAddSale      = { screen = EmpScreen.AddSale },
                            onProfileClick = { screen = EmpScreen.Profile },
                            onTabSelected  = { selectedTab = it },
                            contentPadding = padding,
                        )
                    }
                }
            }
        }

        if (state.loading) {
            SaaSLoadingOverlay(message = "Syncing Database")
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
    val time: String,
    val isSale: Boolean
)

private fun formatCreatedAtTime(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try {
        // Parse full ISO-8601 UTC string from Supabase and convert to local timezone
        // so the activity feed shows the correct wall-clock time for the device.
        val instant = Instant.parse(createdAt)
        val local   = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val h       = local.hour
        val hh      = if (h == 0) 12 else if (h > 12) h - 12 else h
        val ampm    = if (h >= 12) "PM" else "AM"
        val mm      = local.minute.toString().padStart(2, '0')
        "$hh:$mm $ampm"
    } catch (_: Exception) { "" }
}

private fun StockMovement.toHomeActivityItem(): HomeActivityItem {
    val isReturn = type == "inward" && source.trim().startsWith("Empty cans", ignoreCase = true)
    val cleanSource = if (isReturn) {
        source.replace("Empty Cases", "")
            .replace("·", "")
            .trim()
    } else {
        source.trim()
    }
    
    val timeStr = formatCreatedAtTime(createdAt)
    
    return if (isReturn) {
        HomeActivityItem(
            title = "Return from $cleanSource",
            detail = "$qty Empty Can${if (qty != 1) "s" else ""}",
            time = timeStr,
            isSale = false
        )
    } else {
        HomeActivityItem(
            title = "Sale to $cleanSource",
            detail = "$qty Can${if (qty != 1) "s" else ""}",
            time = timeStr,
            isSale = true
        )
    }
}

@Composable
private fun EmployeeHomeContent(
    state: com.example.ruwia.presentation.EmployeeState,
    shopInfo: String,
    employeeName: String,
    onLogout: () -> Unit,
    onAddInward: () -> Unit,
    onAddSale: () -> Unit,
    onProfileClick: () -> Unit,
    onTabSelected: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    val visibleShops = state.shopStocks
    val visibleMovements = state.shopMovements
    val stockItems = remember(state.productCategories) {
        state.productCategories.map { com.example.ruwia.domain.StockItem(it.id, it.name, it.stockAvailable, 0) }
    }
    val derivedShops = remember(visibleShops, visibleMovements, stockItems) {
        deriveShopStockTotals(visibleShops, visibleMovements, stockItems)
    }

    val totalFull = remember(visibleMovements) {
        visibleMovements
            .filter { !it.source.trim().startsWith("Empty cans", ignoreCase = true) }
            .sumOf { m ->
                when (m.type) {
                    "inward"  ->  m.qty.toDouble()
                    "outward" -> -m.qty.toDouble()
                    else      ->  0.0
                }
            }
            .coerceAtLeast(0.0)
    }
    val totalEmpty = remember(visibleMovements) {
        visibleMovements
            .filter { it.source.trim().startsWith("Empty cans", ignoreCase = true) }
            .sumOf { m ->
                if (m.type == "inward") m.qty.toDouble() else -m.qty.toDouble()
            }
            .coerceAtLeast(0.0)
    }
    val totalCust = remember(visibleMovements) {
        val sales = visibleMovements
            .filter { it.type == "outward" && !it.source.trim().startsWith("Empty cans", ignoreCase = true) }
            .sumOf { it.qty.toDouble() }
        val returns = visibleMovements
            .filter { it.type == "inward" && it.source.trim().startsWith("Empty cans", ignoreCase = true) }
            .sumOf { it.qty.toDouble() }
        (sales - returns).coerceAtLeast(0.0)
    }

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

//        // ── Today's Performance ─────────────────────
//        item {
//            TodayPerformanceCard(
//                earnings = state.dailyEarnings,
//                inward   = state.todayInward,
//                outward  = state.todayOutward,
//                modifier = Modifier.padding(horizontal = 20.dp)
//            )
//            Spacer(Modifier.height(24.dp))
//        }

        // ── Today's Stock Card ───────────────────────
        item {
            TodayStockCard(
                available    = totalFull,
                empty        = totalEmpty,
                withCustomer = totalCust,
                modifier     = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Quick Actions ───────────────────────────
        item {
            Text(
                text       = "Quick Actions",
                fontSize   = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = RuwiaColor.TextPrimary,
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
                    color      = RuwiaColor.TextPrimary,
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
                        color      = RuwiaColor.TealPrimary,
                    )
                    Icon(
                        Icons.Rounded.ArrowForward, null,
                        tint = RuwiaColor.TealPrimary, modifier = Modifier.size(14.dp),
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
                        color    = RuwiaColor.TealPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        } else if (activityItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No activity registered today",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = RuwiaColor.TextMuted
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.app_icon),
                    contentDescription = "Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(14.dp))

            Column {
                val greeting = remember {
                    try {
                        val hour = Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault()).hour
                        when {
                            hour < 12 -> "Good Morning"
                            hour < 17 -> "Good Afternoon"
                            else      -> "Good Evening"
                        }
                    } catch (_: Exception) { "Good Morning" }
                }
                Text(
                    text = "$greeting,",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = RuwiaColor.TextSecondary,
                )
                Text(
                    text = name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RuwiaColor.TextPrimary,
                )
                if (shopInfo.isNotEmpty()) {
                    val formattedShop = remember(shopInfo) { shopInfo.replace("·", "•") }
                    Text(
                        text = formattedShop,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RuwiaColor.TealPrimary,
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Notification Icon inside Elevated Container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(RuwiaColor.Surface, RoundedCornerShape(12.dp))
                    .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(12.dp))
                    .clickable { /* Notifications */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    tint = RuwiaColor.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Today's Performance Card ───────────────────────────────────────────────────

@Composable
private fun TodayPerformanceCard(
    earnings: Double,
    inward: Int,
    outward: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = RuwiaColor.TealPrimary.copy(alpha = 0.1f), spotColor = RuwiaColor.TealPrimary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = RuwiaColor.TealDark)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "TODAY'S EARNINGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "₹${earnings.toInt()}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Payments, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PerformanceStat(
                    label = "Full in",
                    value = "$inward",
                    icon = Icons.Rounded.ArrowDownward,
                    color = RuwiaColor.TealLight
                )
                PerformanceStat(
                    label = "Sold out",
                    value = "$outward",
                    icon = Icons.Rounded.ArrowUpward,
                    color = Color(0xFFFCA5A5) // Soft red
                )
                PerformanceStat(
                    label = "Deliveries",
                    value = "${outward}", // Simplified
                    icon = Icons.Rounded.LocalShipping,
                    color = Color(0xFFC7D2FE) // Soft indigo
                )
            }
        }
    }
}

@Composable
private fun PerformanceStat(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(28.dp).background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
        }
    }
}

// ── Today's Stock Card ────────────────────────────────────────────────────────

@Composable
private fun TodayStockCard(
    available: Double,
    empty: Double,
    withCustomer: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = RuwiaColor.Surface)
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
                        color = RuwiaColor.TealPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${formatCases(available)} Cases",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = RuwiaColor.TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(RuwiaColor.TealExtraLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Inventory2, null, tint = RuwiaColor.TealPrimary, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = RuwiaColor.Divider.copy(alpha = 0.6f))
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StockItemSmallCol(
                    value = formatCases(empty),
                    label = "Empty Cases",
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    color = Color(0xFFF59E0B)
                )
                VerticalDivider(modifier = Modifier.height(32.dp), color = RuwiaColor.Divider)
                StockItemSmallCol(
                    value = formatCases(withCustomer),
                    label = "With Customer",
                    icon = Icons.Rounded.Group,
                    color = Color(0xFF6366F1)
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
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RuwiaColor.TextPrimary)
            Text(label, fontSize = 11.sp, color = RuwiaColor.TextMuted, fontWeight = FontWeight.Medium)
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
                color = RuwiaColor.TealPrimary
            )
            QuickActionCard(
                icon = Icons.Rounded.Autorenew,
                title = "Return Cases",
                onClick = onReturnEmptyCans,
                modifier = Modifier.weight(1f),
                color = Color(0xFFF59E0B)
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
                color = Color(0xFF6366F1)
            )
            QuickActionCard(
                icon = Icons.Rounded.History,
                title = "History",
                onClick = onHistory,
                modifier = Modifier.weight(1f),
                color = Color(0xFFEC4899)
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
    color: Color = RuwiaColor.TealPrimary
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
            .shadow(2.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.03f), spotColor = Color.Black.copy(alpha = 0.03f))
            .background(RuwiaColor.Surface, RoundedCornerShape(24.dp))
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
                color = RuwiaColor.TextPrimary
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
            .shadow(2.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.02f), spotColor = Color.Black.copy(alpha = 0.02f))
            .background(RuwiaColor.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Icon Container (36dp)
        val iconColor = if (item.isSale) RuwiaColor.TealPrimary else Color(0xFFF59E0B)
        val icon = if (item.isSale) Icons.Rounded.ArrowUpward else Icons.AutoMirrored.Rounded.Undo

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
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
                color = RuwiaColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (item.isSale) "Delivered • ${item.time}" else "Returned • ${item.time}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = RuwiaColor.TextMuted
            )
        }

        // Right: Quantity
        Text(
            text = item.detail,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = if (item.isSale) RuwiaColor.TealPrimary else RuwiaColor.TextPrimary
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
