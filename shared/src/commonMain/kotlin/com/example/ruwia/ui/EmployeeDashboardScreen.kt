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
import com.example.ruwia.presentation.EmployeeViewModel
import com.example.ruwia.theme.RuwiaColor
import com.example.ruwia.ui.dashboard.NTBottomNavigation
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
    amount   = "+$qty",
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
    var screen by remember { mutableStateOf<EmpScreen>(EmpScreen.Home) }
    var selectedTab by remember { mutableStateOf(0) }

    // Push the parsed shop name down to the VM so its periodic refresh can
    // pull shop-scoped stock movements without re-asking the UI on every tick.
    val parsedShopName = remember(shopInfo) { shopInfo.split("·").getOrNull(0)?.trim().orEmpty() }
    LaunchedEffect(parsedShopName) { vm.setAssignedShop(parsedShopName) }

    LaunchedEffect(Unit) { vm.loadDashboard() }

    // ── Full-screen overlays (no bottom nav) ──────────────────────────────────
    when (screen) {
        EmpScreen.AddInward -> {
            AddStockPurchaseScreen(
                stockItems = emptyList(),
                products   = state.productCategories,
                suppliers  = state.suppliers,
                onBack     = { screen = EmpScreen.Home },
                onClose    = { screen = EmpScreen.Home },
                isEmployee = true,
                onSave     = { supplier, _, _, quantities, _ ->
                    val shopPart = shopInfo.split("·").getOrNull(0)?.trim() ?: ""
                    vm.addInwardStock(supplier, quantities, shopPart)
                    screen = EmpScreen.Home
                },
            )
            return
        }

        EmpScreen.AddSale -> {
            AddSaleScreen(
                products      = state.productCategories,
                customers     = state.customers,
                onNewCustomer = { vm.addCustomer(it) },
                onBack        = { screen = EmpScreen.Home },
                onSave        = { customerName, items, emptyCans ->
                    // Persist the sale: each line item creates one sale_entries
                    // row and one outward stock movement; any empties picked up
                    // from the customer become an inward movement (visible to
                    // the admin on the stock dashboard).
                    val shopPart = shopInfo.split("·").getOrNull(0)?.trim() ?: ""
                    val lines = items.mapNotNull { item ->
                        val product = state.productCategories.getOrNull(item.productIdx) ?: return@mapNotNull null
                        val sellPrice = item.sellPriceText.toDoubleOrNull() ?: product.defaultSellPrice
                        com.example.ruwia.data.EmployeeRepository.SaleLine(
                            productId            = product.id,
                            productName          = product.displayName.ifBlank { product.name },
                            qty                  = item.qty,
                            sellingPricePerUnit  = sellPrice,
                            purchasePricePerUnit = product.purchasePriceGC,
                        )
                    }
                    if (lines.isNotEmpty()) {
                        vm.addOutwardSale(
                            customerName       = customerName,
                            shopName           = shopPart,
                            lines              = lines,
                            emptyCansCollected = emptyCans,
                        )
                    }
                    screen = EmpScreen.Home
                },
            )
            return
        }

        EmpScreen.Profile -> {
            EmployeeProfileScreen(
                employeeName = employeeName,
                email        = userEmail,
                shopInfo     = shopInfo,
                // Real-time stats from the loaded dashboard state — these are the
                // employee's own movements for today, filtered server-side by
                // employee_id (see EmployeeRepository.getDailyCansSummary).
                todayInward  = state.todayInward,
                todayOutward = state.todayOutward,
                todaySales   = state.dailyEarnings,
                customerCount = state.customers.size,
                supplierCount = state.suppliers.size,
                onAddCustomer = { vm.addCustomer(it) },
                onAddSupplier = { name, loc -> vm.addSupplier(name, loc) },
                onBack       = {
                    // Return to whichever tab the user was on before opening Profile.
                    screen = EmpScreen.Home
                },
                onLogout     = onLogout,
            )
            return
        }

        EmpScreen.Home -> { /* Fall through to scaffold below. */ }
    }

    // ── Tabbed home (Home / Entries / Stock) ──────────────────────────────────
    Scaffold(
        containerColor = RuwiaColor.Background,
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
                shopInfo       = shopInfo,
                employeeName   = employeeName,
                onLogout       = onLogout,
                onAddInward    = { screen = EmpScreen.AddInward },
                onAddSale      = { screen = EmpScreen.AddSale },
                contentPadding = padding,
            )
        }
    }
}

// ── Home tab content ─────────────────────────────────────────────────────────
//   Pulled out of the main composable so each bottom-nav tab can render its
//   own LazyColumn without duplicating the scaffold/bottom-bar boilerplate.

@Composable
private fun EmployeeHomeContent(
    state: com.example.ruwia.presentation.EmployeeState,
    shopInfo: String,
    employeeName: String,
    onLogout: () -> Unit,
    onAddInward: () -> Unit,
    onAddSale: () -> Unit,
    contentPadding: PaddingValues,
) {
    val entries = when {
        state.tasks.isNotEmpty()         -> state.tasks.map { it.toEntryDisplay() }
        state.recentEntries.isNotEmpty() -> state.recentEntries.map { it.toEntryDisplay() }
        else                             -> emptyList()
    }

    val totalEntries  = entries.size
    val inwardCount   = entries.count { it.isInward }
    val outwardCount  = entries.count { !it.isInward }
    val salesTotal    = state.dailyEarnings
    val unitsInward   = state.todayInward
    val unitsOutward  = state.todayOutward

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        // ── Header ──────────────────────────────────
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                EmpHeader(
                    name      = employeeName,
                    shopInfo  = shopInfo,
                    onLogout  = onLogout,
                )
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = "Today's stock",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 26.sp,
                        color      = RuwiaColor.TextPrimary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(RuwiaColor.Orange, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Inventory2, null,
                            tint = Color.White, modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val shopParts = shopInfo.split("·").map { it.trim() }
                val shopName  = shopParts.getOrNull(0) ?: shopInfo
                val location  = shopParts.getOrNull(1) ?: ""
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Rounded.Event, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(12.dp))
                    Text(state.currentDate, fontSize = 12.sp, color = RuwiaColor.TextMuted)
                    if (shopName.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Store, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(12.dp))
                        Text(shopName, fontSize = 12.sp, color = RuwiaColor.TextMuted)
                    }
                    if (location.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.LocationOn, null, tint = RuwiaColor.TextMuted, modifier = Modifier.size(12.dp))
                        Text(location, fontSize = 12.sp, color = RuwiaColor.TextMuted)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Activity card ────────────────────────────
        item {
            EmpActivityCard(
                totalEntries = totalEntries,
                inwardCount  = inwardCount,
                outwardCount = outwardCount,
                salesTotal   = salesTotal,
                unitsInward  = unitsInward,
                unitsOutward = unitsOutward,
                dateLabel    = state.currentDate,
                modifier     = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(14.dp))
        }

        // ── Action buttons ────────────────────────────
        item {
            Row(Modifier.padding(horizontal = 20.dp)) {
                EmpActionCard(
                    label    = "FROM SUPPLIER",
                    title    = "Add Inward",
                    subtitle = "Record new stock\nfrom supplier",
                    isInward = true,
                    onClick  = onAddInward,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                EmpActionCard(
                    label    = "TO CUSTOMER",
                    title    = "Add Outward",
                    subtitle = "Record new stock\nto customer",
                    isInward = false,
                    onClick  = onAddSale,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(26.dp))
        }

        // ── Recent entries header ─────────────────────
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text       = "Recent entries",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = RuwiaColor.TextPrimary,
                )
                Row(
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
            Spacer(Modifier.height(12.dp))
        }

        // ── Entry list ────────────────────────────────
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
        } else {
            items(entries) { entry ->
                EmpEntryItem(
                    entry    = entry,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── EMP stock chip ────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            RuwiaColor.TextPrimary.copy(alpha = 0.06f),
                            RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text         = "EMP  ·  ${unitsInward + unitsOutward}",
                        fontSize     = 11.sp,
                        fontWeight   = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color        = RuwiaColor.TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun EmpHeader(name: String, shopInfo: String = "", onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar with online dot
        Box(modifier = Modifier.size(48.dp)) {
            val initials = name.split(" ")
                .take(2).joinToString("") { it.firstOrNull()?.toString() ?: "" }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(RuwiaColor.TealPrimary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = initials,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                )
            }
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .background(Color(0xFF4CAF7C), CircleShape)
                    .border(2.dp, RuwiaColor.Background, CircleShape)
                    .align(Alignment.BottomEnd),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text         = "VANAKKAM",
                fontSize     = 9.sp,
                letterSpacing = 1.8.sp,
                fontWeight   = FontWeight.Medium,
                color        = RuwiaColor.TextMuted,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text       = name,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = RuwiaColor.TextPrimary,
            )
            if (shopInfo.isNotEmpty()) {
                Text(
                    text     = shopInfo,
                    fontSize = 12.sp,
                    color    = RuwiaColor.TextSecondary,
                )
            }
        }

        // Search
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(1.2.dp, RuwiaColor.Divider, CircleShape)
                .clickable {},
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "Search",
                tint     = RuwiaColor.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        // Bell with badge
        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.2.dp, RuwiaColor.Divider, CircleShape)
                    .clickable(onClick = onLogout),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = "Logout",
                    tint     = RuwiaColor.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .background(RuwiaColor.Orange, CircleShape)
                    .border(1.5.dp, RuwiaColor.Background, CircleShape)
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("2", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Activity card ─────────────────────────────────────────────────────────────

@Composable
private fun EmpActivityCard(
    totalEntries: Int,
    inwardCount: Int,
    outwardCount: Int,
    salesTotal: Double,
    unitsInward: Int,
    unitsOutward: Int,
    dateLabel: String = "",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RuwiaColor.TealPrimary),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color  = Color.White.copy(alpha = 0.06f),
                radius = size.height * 1.0f,
                center = Offset(size.width * 0.5f, size.height * 1.1f),
            )
        }
        Column(modifier = Modifier.padding(20.dp)) {
            // Title row + date chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text         = "TODAY'S ACTIVITY",
                    fontSize     = 10.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight   = FontWeight.SemiBold,
                    color        = Color.White.copy(alpha = 0.75f),
                )
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Event, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(dateLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Number + "entries" inline + bar chart on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text       = "$totalEntries",
                            fontSize   = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White,
                            lineHeight = 54.sp,
                            letterSpacing = (-1.5).sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = "entries",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White.copy(alpha = 0.90f),
                            modifier   = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text         = "$inwardCount Inward  ·  $outwardCount Outward  ·  ₹${salesTotal.toInt()} Sales",
                        fontSize     = 12.sp,
                        letterSpacing = 0.2.sp,
                        color        = Color.White.copy(alpha = 0.80f),
                    )
                }
                // Bar chart illustration
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    listOf(0.42f, 0.68f, 1.00f).forEach { frac ->
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height((64.dp * frac))
                                .background(
                                    Color.White.copy(alpha = 0.28f),
                                    RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.8.dp)
            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                EmpStatBox(Icons.Rounded.ArrowDownward, "$unitsInward",  "Units Inward",  Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                EmpStatBox(Icons.Rounded.ArrowUpward,   "$unitsOutward", "Units Outward", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmpStatBox(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(
                text         = label,
                fontSize     = 10.sp,
                letterSpacing = 0.8.sp,
                fontWeight   = FontWeight.Medium,
                color        = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}

// ── Action buttons ─────────────────────────────────────────────────────────────

@Composable
private fun EmpActionCard(
    label: String,
    title: String,
    subtitle: String,
    isInward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardBg  = if (isInward) RuwiaColor.OrangeSurface else RuwiaColor.TealExtraLight
    val iconBg  = if (isInward) RuwiaColor.Orange         else RuwiaColor.TealDark
    val accent  = if (isInward) RuwiaColor.Orange         else RuwiaColor.TealDark

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .clickable(onClick = onClick),
    ) {
        // Watermark silhouette
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint     = accent.copy(alpha = 0.07f),
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 22.dp, y = 14.dp),
        )
        Column(modifier = Modifier.padding(16.dp)) {
            // Large circle icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(22.dp).rotate(if (isInward) 45f else -135f),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text         = label,
                fontSize     = 9.sp,
                letterSpacing = 1.2.sp,
                fontWeight   = FontWeight.SemiBold,
                color        = accent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = title,
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = RuwiaColor.TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text       = subtitle,
                fontSize   = 12.sp,
                color      = RuwiaColor.TextSecondary,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ── Entry list item ────────────────────────────────────────────────────────────

@Composable
private fun EmpEntryItem(entry: EntryDisplay, modifier: Modifier = Modifier) {
    val iconBg   = if (entry.isInward) RuwiaColor.OrangeLight else RuwiaColor.IconTealBg
    val iconTint = if (entry.isInward) RuwiaColor.Orange      else RuwiaColor.TealPrimary
    val amtColor = if (entry.isInward) RuwiaColor.Orange      else Color(0xFF1BAF70)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RuwiaColor.Surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(iconBg, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = null,
                tint     = iconTint,
                modifier = Modifier.size(17.dp).rotate(if (entry.isInward) 45f else -135f),
            )
        }

        Spacer(Modifier.width(12.dp))

        // Name + chip + detail
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = entry.name,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = RuwiaColor.TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                EntryStatusChip(entry.isInward)
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text     = entry.detail,
                fontSize = 11.sp,
                color    = RuwiaColor.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(10.dp))

        // Amount + status
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text       = entry.amount,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = amtColor,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text     = entry.status,
                fontSize = 11.sp,
                color    = RuwiaColor.TextMuted,
            )
        }

        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Rounded.ChevronRight, null,
            tint     = RuwiaColor.TextMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun EntryStatusChip(isInward: Boolean) {
    val bg    = if (isInward) RuwiaColor.OrangeSurface    else RuwiaColor.TealExtraLight
    val fg    = if (isInward) RuwiaColor.Orange            else RuwiaColor.TealPrimary
    val label = if (isInward) "INWARD"                    else "OUTWARD"
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text         = label,
            fontSize     = 8.sp,
            fontWeight   = FontWeight.ExtraBold,
            color        = fg,
            letterSpacing = 0.5.sp,
        )
    }
}

