package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.Order
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.presentation.AdminViewModel
import com.example.ruwia.presentation.EmployeeCreationState
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.ui.dashboard.*

// ─────────────────────────────────────────────────────────────
//  Admin Dashboard Screen — Neer Thuli
//  Premium SaaS Water Delivery Management Dashboard
// ─────────────────────────────────────────────────────────────

@Composable
fun AdminDashboardScreen(
    vm: AdminViewModel,
    onLogout: () -> Unit
) {
    val state by vm.state.collectAsState()
    var selectedTab    by remember { mutableStateOf(0) }
    var showStock      by remember { mutableStateOf(false) }
    var showAddStock   by remember { mutableStateOf(false) }
    var showSettings   by remember { mutableStateOf(false) }
    var showEmployees    by remember { mutableStateOf(false) }
    var showAddEmployee  by remember { mutableStateOf(false) }
    var showPricing      by remember { mutableStateOf(false) }

    // ── Deepest level ─────────────────────────────────────────
    if (showPricing) {
        SystemBackHandler { showPricing = false }
        ProductPricingScreen(
            stockItems = state.stockItems,
            onBack     = { showPricing = false }
        )
        return
    }
    if (showAddEmployee) {
        val creation = state.employeeCreation

        // ── Credentials dialog — only after Supabase confirms ─────────────
        if (creation is EmployeeCreationState.Success) {
            SystemBackHandler { /* block back while dialog is open */ }
            EmployeeCreatedDialog(
                creation = creation,
                onDone   = {
                    vm.clearEmployeeCreation()
                    showAddEmployee = false
                }
            )
            return
        }

        // ── Creation form ─────────────────────────────────────────────────
        SystemBackHandler {
            vm.clearEmployeeCreation()
            showAddEmployee = false
        }
        AddEmployeeScreen(
            isSaving   = creation is EmployeeCreationState.Loading,
            saveError  = (creation as? EmployeeCreationState.Error)?.message,
            onBack     = { vm.clearEmployeeCreation(); showAddEmployee = false },
            onClose    = { vm.clearEmployeeCreation(); showAddEmployee = false; showEmployees = false },
            onSave     = { name, phone, role, shop, salary, email, password ->
                vm.addEmployee(name, phone, role, shop, salary, email, password)
            }
        )
        return
    }
    if (showEmployees) {
        SystemBackHandler { showEmployees = false }
        EmployeesScreen(
            employees     = state.employees,
            onBack        = { showEmployees = false },
            onAddEmployee = { showAddEmployee = true }
        )
        return
    }

    // ── Settings ──────────────────────────────────────────────
    if (showSettings) {
        SystemBackHandler { showSettings = false }
        SettingsScreen(
            state                  = state,
            onBack                 = { showSettings = false },
            onNavigateToEmployees  = { showEmployees = true },
            onNavigateToPricing    = { showPricing = true },
            onLogout               = onLogout
        )
        return
    }

    // ── Add stock purchase ────────────────────────────────────
    if (showAddStock) {
        SystemBackHandler { showAddStock = false }
        AddStockPurchaseScreen(
            stockItems = state.stockItems,
            onBack     = { showAddStock = false },
            onClose    = { showAddStock = false; showStock = false; selectedTab = 0 },
            onSave     = { _, _, _, _, _ ->
                showAddStock = false
                vm.loadData()
            }
        )
        return
    }

    // ── Stock dashboard ───────────────────────────────────────
    if (showStock) {
        SystemBackHandler { showStock = false; selectedTab = 0 }
        StockDashboardScreen(
            state      = state,
            onBack     = { showStock = false; selectedTab = 0 },
            onRefresh  = vm::loadData,
            onAddStock = { showAddStock = true }
        )
        return
    }

    Scaffold(
        containerColor = NTColors.Background,
        bottomBar = {
            NTBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab == 1) showStock = true else selectedTab = tab
                },
                onFabClick = { /* open new-order sheet */ },
                tabs = listOf(
                    NTNavTab(0, "Home",      Icons.Rounded.Home),
                    NTNavTab(1, "Stock",     Icons.Rounded.Inventory2),
                    NTNavTab(2, "Customers", Icons.Rounded.Group),
                    NTNavTab(3, "Reports",   Icons.Rounded.BarChart),
                )
            )
        }
    ) { contentPadding ->
        when (selectedTab) {
            0 -> AdminHomeTab(state, vm, contentPadding, onLogout,
                    onAddStock      = { showAddStock = true },
                    onOpenSettings  = { showSettings = true })
            2 -> AdminCustomersTab(state.customers, contentPadding)
            3 -> AdminReportsTab(state, contentPadding)
        }
    }
}

// ── Home tab ──────────────────────────────────────────────────

@Composable
private fun AdminHomeTab(
    state: AdminState,
    vm: AdminViewModel,
    contentPadding: PaddingValues,
    onLogout: () -> Unit,
    onAddStock: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    when {
        state.loading  -> NTDashboardSkeleton(contentPadding)
        state.error != null -> NTErrorState(
            message = state.error,
            onRetry  = vm::loadData,
            contentPadding = contentPadding
        )
        else -> AdminDashboardContent(state, contentPadding, onLogout, onAddStock, onOpenSettings)
    }
}

@Composable
private fun AdminDashboardContent(
    state: AdminState,
    contentPadding: PaddingValues,
    @Suppress("UNUSED_PARAMETER") onLogout: () -> Unit,
    onAddStock: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    // ── Derive dashboard metrics from real AdminState ────────
    val totalStock     = state.stockItems.sumOf { it.stockAvailable }
    val pendingOrders  = state.orders.count { it.status == "pending" }
    val deliveredToday = state.orders.count { it.status == "delivered" }
    val dueAmount      = state.customers.filter { it.balance > 0 }.sumOf { it.balance }
    val dueCount       = state.customers.count { it.balance > 0 }
    val activeStaff    = state.employees.count { it.status != "inactive" }
    val emptyCans      = state.customers.sumOf { it.cansHeld }

    val (greeting, subtext) = buildGreeting(
        mrr         = state.mrr,
        pending     = pendingOrders,
        delivered   = deliveredToday
    )

    val kpiItems = buildKpis(
        totalStock    = totalStock,
        mrr           = state.mrr,
        emptyCans     = emptyCans,
        dueAmount     = dueAmount,
        dueCount      = dueCount,
        pendingOrders = pendingOrders,
        customers     = state.customers.size
    )

    val quickActions = listOf(
        NTQuickAction("a1", "New order",       Icons.Rounded.AddShoppingCart, NTColors.PrimaryDark),
        NTQuickAction("a2", "Add customer",    Icons.Rounded.PersonAdd,       NTColors.PrimaryMid),
        NTQuickAction("a3", "Add stock",       Icons.Rounded.LocalShipping,   NTColors.Accent),
        NTQuickAction("a4", "View reports",    Icons.Rounded.Analytics,       NTColors.TextPrimary),
        NTQuickAction("a5", "Collect payment", Icons.Rounded.Payments,        NTColors.Info, badgeCount = dueCount.coerceAtMost(99)),
        NTQuickAction("a6", "Assign driver",   Icons.Rounded.DirectionsCar,   NTColors.AvatarPurple),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NTColors.Background),
        contentPadding = PaddingValues(
            top    = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        )
    ) {
        // Header
        item {
            NTDashboardHeader(
                shopName            = "Neer Thuli",
                adminName           = "Admin",
                notificationCount   = pendingOrders,
                onAvatarClick       = onOpenSettings, // tap avatar → Settings
                onSearchClick       = {},
                onNotificationClick = {}
            )
        }

        // Greeting
        item {
            NTGreetingSection(greeting = greeting, subtext = subtext)
        }

        // Revenue hero card
        item {
            NTRevenueHeroCard(
                shopName       = "Neer Thuli",
                mrr            = state.mrr,
                totalOrders    = state.orders.size,
                totalCustomers = state.customers.size,
                activeStaff    = activeStaff,
                fleetActive    = state.fleetActiveCount,
                growthPercent  = 8.2,
                dateLabel      = "This Month"
            )
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // KPI grid
        item {
            NTKpiGrid(items = kpiItems)
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // Quick actions
        item {
            NTQuickActionsRow(
                actions = quickActions,
                onActionClick = { action ->
                    when (action.id) {
                        "a3" -> onAddStock()  // Add stock → opens AddStockPurchaseScreen
                        else -> { /* other actions handled later */ }
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // Analytics section — line chart
        if (state.weeklyRevenuePoints.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
                    NTSectionHeader(label = "ANALYTICS", title = "Revenue trend")
                    Spacer(modifier = Modifier.height(NTDp.md))
                    NTDateRangePicker(
                        selected = NTDateRange.WEEKLY,
                        onSelect = {}
                    )
                    Spacer(modifier = Modifier.height(NTDp.md))
                    NTLineChartCard(
                        title         = "WEEKLY REVENUE",
                        valueLabel    = state.weeklyRevenueLabel,
                        growthPercent = 18.0,
                        points        = state.weeklyRevenuePoints,
                        labels        = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(NTDp.lg)) }
        }

        // Stock performance chart
        if (state.stockItems.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
                    val stockPoints = state.stockItems
                        .map { it.stockAvailable.toFloat() }
                        .let { pts ->
                            val max = pts.maxOrNull() ?: 1f
                            pts.map { it / max }
                        }
                    NTLineChartCard(
                        title         = "STOCK LEVELS",
                        valueLabel    = "$totalStock cans on hand",
                        growthPercent = -3.0,
                        points        = if (stockPoints.size >= 2) stockPoints else List(7) { 0.5f },
                        labels        = state.stockItems.map { it.name.take(3).uppercase() }
                            .let { l -> if (l.size < 2) listOf("5L", "10L", "20L", "Bulk") else l }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(NTDp.lg)) }
        }

        // Activity feed
        item {
            NTActivityFeedSection(
                orders    = state.orders,
                customers = state.customers,
                onViewAll = {}
            )
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // Staff overview row
        if (state.employees.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
                    NTSectionHeader(label = "TEAM", title = "Staff overview")
                    Spacer(modifier = Modifier.height(NTDp.md))
                }
            }
            items(items = state.employees.take(3)) { emp ->
                NTStaffCard(employee = emp, modifier = Modifier.padding(horizontal = NTDp.screenPad))
                Spacer(modifier = Modifier.height(NTDp.sm))
            }
            item { Spacer(modifier = Modifier.height(NTDp.sm)) }
        }
    }
}

// ── Staff card ────────────────────────────────────────────────

@Composable
private fun NTStaffCard(employee: EmployeeInfo, modifier: Modifier = Modifier) {
    val (statusColor, statusLabel) = when (employee.status) {
        "on_delivery" -> NTColors.Success to "On Delivery"
        "active"      -> NTColors.Primary to "Active"
        else          -> NTColors.TextTertiary to "Inactive"
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(NTDp.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(NTDp.radMd))
                    .background(NTColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null,
                    tint = NTColors.Primary, modifier = Modifier.size(NTDp.iconLg))
            }
            Spacer(modifier = Modifier.width(NTDp.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(employee.name, color = NTColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${employee.completedDeliveries} deliveries · ${employee.rating} rating", color = NTColors.TextTertiary, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(NTDp.radFull))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Orders tab ────────────────────────────────────────────────

@Composable
private fun NTOrderCard(order: Order, @Suppress("UNUSED_PARAMETER") onAssign: (String) -> Unit) {
    val (statusColor, statusBg) = when (order.status) {
        "delivered" -> NTColors.Success to NTColors.SuccessLight
        "approved"  -> NTColors.Primary to NTColors.PrimaryLight
        "cancelled" -> NTColors.Error   to NTColors.ErrorLight
        else        -> NTColors.Warning to NTColors.WarningLight
    }
    Card(
        shape = RoundedCornerShape(NTDp.radXxl),
        colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(NTDp.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(NTDp.radMd)).background(statusBg),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Inventory2, contentDescription = null,
                tint = statusColor, modifier = Modifier.size(NTDp.iconLg)) }
            Spacer(modifier = Modifier.width(NTDp.md))
            Column(modifier = Modifier.weight(1f)) {
                Text("Order #${order.id?.take(6) ?: "—"}", color = NTColors.TextPrimary,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Qty: ${order.qty} cans · ${order.createdAt?.take(10) ?: ""}",
                    color = NTColors.TextTertiary, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(NTDp.radFull))
                    .background(statusBg).padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text(order.status.replaceFirstChar { it.uppercase() }, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// ── Customers tab ─────────────────────────────────────────────

@Composable
fun AdminCustomersTab(customers: List<Customer>, contentPadding: PaddingValues) {
    if (customers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(NTColors.Background).padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            NTEmptyState(
                icon     = Icons.Rounded.Group,
                title    = "No Customers Yet",
                subtitle = "Customers will appear here once they sign up.",
                ctaLabel = "Add Customer"
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NTColors.Background),
        contentPadding = PaddingValues(
            top   = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
            start = NTDp.screenPad, end = NTDp.screenPad
        ),
        verticalArrangement = Arrangement.spacedBy(NTDp.sm)
    ) {
        item {
            Text("Customers", color = NTColors.TextPrimary, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = NTDp.sm))
        }
        items(items = customers, key = { it.id ?: it.hashCode().toString() }) { cust ->
            Card(
                shape = RoundedCornerShape(NTDp.radXxl),
                colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(NTDp.md), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(NTDp.radMd))
                            .background(NTColors.PrimaryLight),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Person, contentDescription = null,
                        tint = NTColors.Primary, modifier = Modifier.size(NTDp.iconLg)) }
                    Spacer(modifier = Modifier.width(NTDp.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cust.name, color = NTColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${cust.cansHeld} cans held · ${cust.phone ?: ""}",
                            color = NTColors.TextTertiary, fontSize = 12.sp)
                    }
                    if (cust.balance > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${cust.balance.toInt()}", color = NTColors.Error,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("DUE", color = NTColors.Error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── Reports tab ───────────────────────────────────────────────

@Composable
fun AdminReportsTab(state: AdminState, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NTColors.Background),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
            start = NTDp.screenPad, end = NTDp.screenPad
        ),
        verticalArrangement = Arrangement.spacedBy(NTDp.md)
    ) {
        item {
            Text("Reports", color = NTColors.TextPrimary, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = NTDp.xs))
        }
        // Revenue chart
        if (state.weeklyRevenuePoints.isNotEmpty()) {
            item {
                NTLineChartCard(
                    title = "WEEKLY REVENUE", valueLabel = state.weeklyRevenueLabel,
                    growthPercent = 18.0, points = state.weeklyRevenuePoints,
                    labels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                )
            }
        }
        // CSAT card
        item {
            Card(
                shape = RoundedCornerShape(NTDp.radXxl),
                colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(NTDp.cardPad), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(NTDp.radMd))
                        .background(NTColors.SuccessLight), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.StarRate, contentDescription = null,
                            tint = NTColors.Success, modifier = Modifier.size(NTDp.iconLg))
                    }
                    Spacer(modifier = Modifier.width(NTDp.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CSAT SCORE", color = NTColors.TextTertiary, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                        Text("${state.csat} / 5.0", color = NTColors.TextPrimary, fontSize = 22.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    NTGrowthBadge(percent = 3.5)
                }
            }
        }
        // Fleet summary
        item {
            Card(
                shape = RoundedCornerShape(NTDp.radXxl),
                colors = CardDefaults.cardColors(containerColor = NTColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(NTDp.cardPad), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(NTDp.radMd))
                        .background(NTColors.DelivIconBg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.LocalShipping, contentDescription = null,
                            tint = NTColors.DelivIconFg, modifier = Modifier.size(NTDp.iconLg))
                    }
                    Spacer(modifier = Modifier.width(NTDp.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FLEET ACTIVE", color = NTColors.TextTertiary, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                        Text("${state.fleetActiveCount} vehicles", color = NTColors.TextPrimary,
                            fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Helper functions ──────────────────────────────────────────

private fun buildGreeting(mrr: Double, pending: Int, delivered: Int): Pair<String, String> {
    val greeting = "Good morning"  // Platform time can override via ViewModel if needed
    val subtext = when {
        pending > 5   -> "$pending orders are pending dispatch."
        delivered > 0 -> "$delivered deliveries completed today."
        mrr > 20_000  -> "Revenue is on track this month."
        else          -> "Here's how your business is doing today."
    }
    return greeting to subtext
}

private fun buildKpis(
    totalStock: Int,
    mrr: Double,
    emptyCans: Int,
    dueAmount: Double,
    dueCount: Int,
    pendingOrders: Int,
    customers: Int
): List<NTKpiItem> = listOf(
    NTKpiItem(
        title = "STOCK ON HAND",   value = "$totalStock",
        subtitle = "${totalStock / 10 + 1} SKUs · live",
        icon = Icons.Rounded.Inventory2,
        iconBg = NTColors.SuccessLight, iconFg = NTColors.Success,
        statusColor = NTColors.Success, subtitleColor = NTColors.SuccessText
    ),
    NTKpiItem(
        title = "MONTHLY REVENUE", value = formatAmount(mrr),
        subtitle = "+8.2% vs last month",
        icon = Icons.Rounded.AccountBalanceWallet,
        iconBg = NTColors.SuccessLight, iconFg = NTColors.Success,
        statusColor = NTColors.Success, subtitleColor = NTColors.SuccessText
    ),
    NTKpiItem(
        title = "EMPTY CANS",      value = "$emptyCans",
        subtitle = "With customers",
        icon = Icons.Rounded.Water,
        iconBg = NTColors.SurfaceVar, iconFg = NTColors.TextSecondary,
        statusColor = NTColors.TextTertiary, subtitleColor = NTColors.TextSecondary
    ),
    NTKpiItem(
        title = "DUE PAYMENTS",    value = formatAmount(dueAmount),
        subtitle = "$dueCount customers",
        icon = Icons.Rounded.Warning,
        iconBg = NTColors.ErrorLight, iconFg = NTColors.Error,
        statusColor = NTColors.Error, subtitleColor = NTColors.ErrorText
    ),
    NTKpiItem(
        title = "PENDING ORDERS",  value = "$pendingOrders",
        subtitle = "Awaiting dispatch",
        icon = Icons.Rounded.PendingActions,
        iconBg = NTColors.WarningLight, iconFg = NTColors.Warning,
        statusColor = NTColors.Warning, subtitleColor = NTColors.WarningText
    ),
    NTKpiItem(
        title = "TOTAL CUSTOMERS", value = "$customers",
        subtitle = "Active accounts",
        icon = Icons.Rounded.Group,
        iconBg = NTColors.PrimaryLight, iconFg = NTColors.Primary,
        statusColor = NTColors.Primary, subtitleColor = NTColors.PrimaryDark
    ),
)

private fun formatAmount(amount: Double): String = when {
    amount >= 1_00_000 -> "₹${(amount / 1_00_000 * 10).toLong() / 10.0}L"
    amount >= 1_000    -> "₹${amount.toLong()}"
    else               -> "₹${amount.toLong()}"
}

// ── Employee created — credentials dialog ─────────────────────
//   Fires ONLY when Supabase confirms the auth user was created.

@Composable
fun EmployeeCreatedDialog(
    creation: EmployeeCreationState.Success,
    onDone: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NTDp.radXxl))
                .background(NTColors.Surface)
                .padding(NTDp.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success icon
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(NTColors.SuccessLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null,
                    tint = NTColors.Success, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(NTDp.md))

            Text("Account created!",
                color = NTColors.TextPrimary, fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold)
            Text("Share these credentials with ${creation.name}",
                color = NTColors.TextSecondary, fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp))

            Spacer(modifier = Modifier.height(NTDp.lg))

            // Credentials card
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(NTDp.radLg))
                    .background(NTColors.SurfaceVar)
                    .padding(NTDp.md),
                verticalArrangement = Arrangement.spacedBy(NTDp.md)
            ) {
                EmpCredRow("Name",     creation.name,     Icons.Rounded.Person)
                EmpCredRow("Role",     creation.role.replaceFirstChar { it.uppercaseChar() }, Icons.Rounded.Badge)
                EmpCredRow("Shop",     creation.shop,     Icons.Rounded.Store)
                HorizontalDivider(color = NTColors.Border)
                EmpCredRow("Email",    creation.email,    Icons.Rounded.Email)
                EmpCredRow("Password", creation.password, Icons.Rounded.Key)
            }

            // Warning
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = NTDp.md)
                    .clip(RoundedCornerShape(NTDp.radMd))
                    .background(NTColors.WarningLight)
                    .padding(NTDp.sm),
                horizontalArrangement = Arrangement.spacedBy(NTDp.sm)
            ) {
                Icon(Icons.Rounded.Warning, contentDescription = null,
                    tint = NTColors.Warning,
                    modifier = Modifier.size(14.dp).padding(top = 2.dp))
                Text(
                    "Ask the employee to change their password after first login.",
                    color = NTColors.WarningText, fontSize = 11.sp, lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(NTDp.lg))

            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp)
                    .clip(RoundedCornerShape(NTDp.radFull))
                    .background(NTColors.Primary)
                    .clickable(onClick = onDone),
                contentAlignment = Alignment.Center
            ) {
                Text("Done", color = Color.White, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmpCredRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NTDp.sm)
    ) {
        Icon(icon, contentDescription = null, tint = NTColors.TextTertiary,
            modifier = Modifier.size(16.dp))
        Text(label, color = NTColors.TextTertiary, fontSize = 12.sp,
            modifier = Modifier.width(64.dp))
        Text(value, color = NTColors.TextPrimary, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold)
    }
}
