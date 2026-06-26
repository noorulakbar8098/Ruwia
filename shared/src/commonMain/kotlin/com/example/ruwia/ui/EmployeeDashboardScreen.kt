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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.unitsPerCase
import kotlin.time.Clock
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
    detail   = "$canQty cans  ·  $etaText",
    amount   = if (status == "delivered") "+₹${canQty * 28}" else "+$canQty",
    status   = if (status == "delivered") "SOLD" else status.replace("_", " ").uppercase(),
    isInward = false,
)

private fun StockMovement.toEntryDisplay(): EntryDisplay = EntryDisplay(
    name     = source,
    detail   = "$qty units · ${createdAt?.take(10) ?: ""}",
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

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title   = { Text("Logout?") },
            text    = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                TextButton(
                    onClick = { showLogoutDialog = false; onLogout() },
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
                    suppliers  = state.suppliers,
                    onBack     = { screen = EmpScreen.Home },
                    onClose    = { screen = EmpScreen.Home },
                    isEmployee = true,
                    onSave     = { supplier, _, quantities, empties ->
                        val shopPart = resolvedShopInfo.split("·").getOrNull(0)?.trim() ?: ""
                        vm.addInwardStock(supplier, quantities, shopPart, empties)
                        screen = EmpScreen.Home
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
                            val upc = product.unitsPerCase.coerceAtLeast(1)
                            val sellPrice = item.sellPriceText.toDoubleOrNull() ?: product.defaultSellPrice
                            com.example.ruwia.data.EmployeeRepository.SaleLine(
                                productId            = product.id,
                                productName          = product.displayName.ifBlank { product.name },
                                qty                  = item.qty * upc,  // cases → units
                                sellingPricePerUnit  = sellPrice / upc,  // case price → unit price
                                purchasePricePerUnit = product.purchasePriceGC / upc,
                            )
                        }
                        if (lines.isNotEmpty()) {
                            vm.addOutwardSale(
                                customerName       = customerName,
                                shopName           = shopPart,
                                lines              = lines,
                                emptyCansCollected = emptyCans,
                                saleDate           = parsedSaleDate,
                            )
                        }
                        screen = EmpScreen.Home
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
                    supplierCount = state.suppliers.size,
                    errorMessage  = state.error,
                    onClearError  = vm::clearError,
                    onAddCustomer = { vm.addCustomer(it) },
                    onAddSupplier = { name, loc -> vm.addSupplier(name, loc) },
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
                            // Restrict the view to the employee's assigned shop. The
                            // shopInfo string carries "{shopName} · {location}" — strip
                            // the location segment so it matches the keys in shop_stocks.
                            shopName       = parsedShopName,
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
    if (createdAt == null) return ""
    // e.g. "2026-06-24T11:30:00Z" or "2026-06-24 11:30:00"
    val timePart = createdAt.split("T").getOrNull(1) ?: createdAt.split(" ").getOrNull(1) ?: return ""
    val parts = timePart.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return ""
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val hh = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val ampm = if (hour >= 12) "PM" else "AM"
    return "$hh:${minute.toString().padStart(2, '0')} $ampm"
}

private fun StockMovement.toHomeActivityItem(): HomeActivityItem {
    val isReturn = type == "inward" && source.trim().startsWith("Empty cans", ignoreCase = true)
    val cleanSource = if (isReturn) {
        source.replace("Empty cans", "")
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
    val parsedShopName = remember(shopInfo) { shopInfo.split("·").getOrNull(0)?.trim().orEmpty() }
    val visibleShops = remember(state.shopStocks, parsedShopName) {
        if (parsedShopName.isBlank()) state.shopStocks
        else {
            val key = shopMatchKey(parsedShopName)
            state.shopStocks.filter { shopMatchKey(it.name) == key }.ifEmpty { state.shopStocks }
        }
    }
    val visibleMovements = remember(state.shopMovements, parsedShopName) {
        if (parsedShopName.isBlank()) state.shopMovements
        else {
            val key = shopMatchKey(parsedShopName)
            state.shopMovements.filter { shopMatchKey(it.shopName) == key }
        }
    }
    val stockItems = remember(state.productCategories) {
        state.productCategories.map { com.example.ruwia.domain.StockItem(it.id, it.name, it.stockAvailable, 0) }
    }
    val derivedShops = remember(visibleShops, visibleMovements, stockItems) {
        deriveShopStockTotals(visibleShops, visibleMovements, stockItems)
    }

    val totalFull  = derivedShops.sumOf { it.fullCans }
    val totalEmpty = derivedShops.sumOf { it.emptyCans }
    val totalCust  = derivedShops.sumOf { it.cansWithCustomers }

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
                Spacer(Modifier.height(16.dp))
            }
        }

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
                text = greeting,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = RuwiaColor.TextSecondary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = name,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = RuwiaColor.TextPrimary,
            )
            if (shopInfo.isNotEmpty()) {
                val formattedShop = remember(shopInfo) { shopInfo.replace("·", "•") }
                Text(
                    text = formattedShop,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = RuwiaColor.TextMuted,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Notification Icon inside Elevated Container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(RuwiaColor.MintLight, RoundedCornerShape(10.dp))
                    .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(10.dp))
                    .clickable { /* Notifications */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    tint = RuwiaColor.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Profile Initials inside Elevated Container
            val initials = name.split(" ")
                .take(2).joinToString("") { it.firstOrNull()?.toString()?.uppercase() ?: "" }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(RuwiaColor.MintLight, RoundedCornerShape(10.dp))
                    .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = RuwiaColor.TealPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RuwiaColor.Surface)
            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StockItemCol(
                value = formatCases(available),
                label = "Available",
                modifier = Modifier.weight(1f)
            )
            StockItemDivider()
            StockItemCol(
                value = formatCases(empty),
                label = "Empty",
                modifier = Modifier.weight(1f)
            )
            StockItemDivider()
            StockItemCol(
                value = formatCases(withCustomer),
                label = "Customer",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StockItemCol(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RuwiaColor.TealPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = RuwiaColor.TextSecondary
        )
    }
}

@Composable
private fun StockItemDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(RuwiaColor.Divider)
    )
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Rounded.ShoppingCart,
                title = "Record Sale",
                onClick = onRecordSale,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Rounded.Autorenew,
                title = "Return Cans",
                onClick = onReturnEmptyCans,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Rounded.Inventory2,
                title = "View Stock",
                onClick = onViewStock,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Rounded.History,
                title = "History",
                onClick = onHistory,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "pressScale"
    )

    Box(
        modifier = modifier
            .height(85.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(20.dp))
            .background(RuwiaColor.Surface)
            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(RuwiaColor.TealLight, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = RuwiaColor.TealPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
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
            .clip(RoundedCornerShape(20.dp))
            .background(RuwiaColor.Surface)
            .border(1.dp, RuwiaColor.Divider, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Icon Container (36dp)
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(RuwiaColor.MintLight, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.isSale) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                contentDescription = null,
                tint = if (item.isSale) RuwiaColor.TealPrimary else RuwiaColor.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Middle: Customer & Type
        Column(modifier = Modifier.weight(1f)) {
            val customerName = remember(item.title) {
                item.title.replace("Sale to ", "").replace("Return from ", "")
            }
            Text(
                text = customerName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = RuwiaColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (item.isSale) "Sale" else "Return",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = RuwiaColor.TextSecondary
            )
        }

        // Right: Quantity & Time
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.detail,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.isSale) RuwiaColor.TealPrimary else RuwiaColor.TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.time,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = RuwiaColor.TextMuted
            )
        }
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
