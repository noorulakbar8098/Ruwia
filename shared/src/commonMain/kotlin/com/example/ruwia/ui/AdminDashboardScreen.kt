package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.Order
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.presentation.AdminViewModel
import com.example.ruwia.presentation.DashboardMetrics
import com.example.ruwia.presentation.DashboardRange
import com.example.ruwia.presentation.EmployeeCreationState
import com.example.ruwia.presentation.toDashboardMetrics
import com.example.ruwia.SystemBackHandler
import com.example.ruwia.ui.admin.ProductDetailScreen
import com.example.ruwia.ui.admin.ProductFormSheet
import com.example.ruwia.ui.admin.ProductManagementScreen
import com.example.ruwia.ui.admin.ProfitDashboardScreen
import com.example.ruwia.ui.admin.InventoryScreen
import com.example.ruwia.ui.dashboard.*
import com.example.ruwia.ui.components.SaaSLoadingOverlay
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock


// ─────────────────────────────────────────────────────────────
//  Admin Dashboard Screen — Neer Thuli
//  Premium SaaS Water Delivery Management Dashboard
// ─────────────────────────────────────────────────────────────

@Composable
fun AdminDashboardScreen(
    vm: AdminViewModel,
    onLogout: () -> Unit,
    adminName: String = "Admin",
    adminEmail: String = "",
) {
    val state by vm.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Long)
            vm.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AdminDashboardContentSwitcher(
            state = state,
            vm = vm,
            onLogout = onLogout,
            adminName = adminName,
            adminEmail = adminEmail,
            snackbarHostState = snackbarHostState
        )

        if (state.loading) {
            SaaSLoadingOverlay(message = "Syncing Database")
        }
    }
}

@Composable
private fun AdminDashboardContentSwitcher(
    state: AdminState,
    vm: AdminViewModel,
    onLogout: () -> Unit,
    adminName: String,
    adminEmail: String,
    snackbarHostState: SnackbarHostState,
) {
    var selectedTab       by remember { mutableStateOf(0) }
    var showAddStock      by remember { mutableStateOf(false) }
    var showSettings      by remember { mutableStateOf(false) }
    var showEmployees     by remember { mutableStateOf(false) }
    var showAddEmployee   by remember { mutableStateOf(false) }
    var showPricing       by remember { mutableStateOf(false) }
    var showCustomers     by remember { mutableStateOf(false) }
    var productToRestock by remember { mutableStateOf<ProductCategory?>(null) }
    var editModeStock by remember { mutableStateOf(0) }

    var productDetailName by remember { mutableStateOf<String?>(null) }

    // ── Full-screen overlays ──────────────────────────────────
    productDetailName?.let { pName ->
        SystemBackHandler { productDetailName = null }
        ProductDetailScreen(
            productName  = pName,
            transactions = state.saleEntries.filter { it.productName.contains(pName, ignoreCase = true) },
            onBack       = { productDetailName = null },
        )
        return
    }

    // ── Deepest level ─────────────────────────────────────────
    if (showCustomers) {
        SystemBackHandler { showCustomers = false }
        com.example.ruwia.ui.admin.CustomerManagementScreen(
            customers        = state.customers,
            errorMessage     = state.error,
            onAddCustomer    = vm::addCustomer,
            onDeleteCustomer = vm::deleteCustomer,
            onClearError     = vm::clearError,
            onBack           = { showCustomers = false },
        )
        return
    }

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

    // Settings overlay removed (moved to bottom navigation tab 4)

    // ── Add stock purchase ────────────────────────────────────
    if (showAddStock) {
        SystemBackHandler { showAddStock = false }
        AddStockPurchaseScreen(
            stockItems = state.stockItems,
            products   = state.productCategories,
            productToRestock = productToRestock,
            currentStock = editModeStock,
            movements  = state.recentMovements,
            shops      = state.shopStocks
                .filter { it.name.trim().lowercase().let { name -> name == "shop 1" || name == "shop 2" } }
                .map { it.name to it.location },
            onBack     = { productToRestock = null; editModeStock = 0; showAddStock = false },
            onClose    = { productToRestock = null; editModeStock = 0; showAddStock = false },
            onSave     = { productId, sku, brandName, purchasePrice, sellingPrice, qty, shopName, dateTimeIso, emptyCans ->
                if (productToRestock != null) {
                    // Edit mode: update product details + stock delta
                    vm.updateProductWithStockDelta(
                        productId = productToRestock!!.id,
                        brandName = brandName,
                        purchasePrice = purchasePrice,
                        sellingPrice = sellingPrice,
                        newStock = qty,
                        previousStock = editModeStock,
                        shopName = shopName
                    )
                } else {
                    vm.addInwardStockEntry(
                        sku = sku,
                        brandName = brandName,
                        purchasePrice = purchasePrice,
                        sellingPrice = sellingPrice,
                        qty = qty,
                        shopName = shopName,
                        createdAt = dateTimeIso
                    )
                }
                productToRestock = null
                editModeStock = 0
                showAddStock = false
            }
        )
        return
    }





    Scaffold(
        containerColor = NTColors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NTBottomNavigation(
                selectedTab   = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }
    ) { contentPadding ->
        if (selectedTab != 0) {
            SystemBackHandler { selectedTab = 0 }
        }
        when (selectedTab) {
            0 -> AdminHomeTab(
                     state          = state,
                     vm             = vm,
                     contentPadding = contentPadding,
                     adminName      = adminName,
                     onLogout       = onLogout,
                     onOpenSettings = { selectedTab = 3 },
                 )
            1 -> InventoryScreen(
                     state          = state,
                     onBack         = { selectedTab = 0 },
                     onAddProductRequest = {
                         productToRestock = null
                         showAddStock = true
                     },
                     onAddProductCategory = vm::addProductCategory,
                     onUpdateProduct = vm::updateProductCategory,
                     onDeleteProduct = vm::deleteProductCategory,
                     onAddMovement   = vm::addStockMovement,
                     onAddStockPurchase = { product, currentStock ->
                          productToRestock = product
                          editModeStock = currentStock
                          showAddStock = true
                      },
                     onToggleProductStatus = { product ->
                         vm.updateProductCategory(product.copy(isActive = !product.isActive))
                     },
                     contentPadding = contentPadding,
                 )
            2 -> ProfitDashboardScreen(
                     state           = state,
                     onBack          = {},
                     onExpenseMonthSelected = vm::loadMonthlyExpense,
                     onExpenseSave     = vm::saveMonthlyExpense,
                     onProductClick    = { pName -> productDetailName = pName },
                     onAddStock        = { showAddStock = true },
                     onAddProduct      = { selectedTab = 1 },
                     onClearData       = vm::clearStockAndRevenue,
                     contentPadding    = contentPadding,
                 )
            3 -> SettingsScreen(
                     state                  = state,
                     adminName              = adminName,
                     adminEmail             = adminEmail,
                     onBack                 = { selectedTab = 0 },
                     onNavigateToEmployees  = { showEmployees = true },
                     onNavigateToPricing    = { showPricing = true },
                     onNavigateToCustomers  = { showCustomers = true },
                     onLogout               = onLogout,
                     onDeleteAllData        = {
                         vm.deleteAllData()
                         selectedTab = 0
                     },
                     contentPadding         = contentPadding
                 )
        }
    }
}

// ── Home tab ──────────────────────────────────────────────────

@Composable
private fun AdminHomeTab(
    state: AdminState,
    vm: AdminViewModel,
    contentPadding: PaddingValues,
    adminName: String,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    when {
        state.loading  -> NTDashboardSkeleton(contentPadding)
        state.error != null -> NTErrorState(
            message = state.error,
            onRetry  = vm::loadData,
            contentPadding = contentPadding
        )
        else -> AdminDashboardContent(state, contentPadding, adminName, onLogout, onOpenSettings)
    }
}

@Composable
private fun AdminDashboardContent(
    state: AdminState,
    contentPadding: PaddingValues,
    adminName: String,
    @Suppress("UNUSED_PARAMETER") onLogout: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    // ── Range picker state — drives BOTH the hero chart and the
    // analytics chart below it. Defaults to MONTH so the home screen
    // opens on the most useful "current month" view.
    var selectedRange by remember { mutableStateOf(NTDateRange.MONTHLY) }

    // Recompute metrics whenever state OR the selected range changes.
    val metrics = remember(state, selectedRange) {
        state.toDashboardMetrics(selectedRange.toDashboardRange())
    }

    val totalStock     = metrics.totalStockUnits
    val pendingOrders  = state.orders.count { it.status == "pending" }
    val deliveredToday = state.orders.count { it.status == "delivered" }
    val activeStaff    = state.employees.count { it.status != "inactive" }

    val (greeting, subtext) = buildGreeting(
        profit    = metrics.thisMonthProfit,
        pending   = pendingOrders,
        delivered = deliveredToday,
        revGrowth = metrics.revenueGrowthPercent,
    )

    val kpiItems = buildKpis(state = state, metrics = metrics)

    // Decide which figure drives the hero card.
    // If we have any expense data for either month, show NET PROFIT (the
    // primary owner concern). Otherwise fall back to the range total so
    // brand-new tenants without expenses configured still see a meaningful
    // number — and showing ₹0 is FINE when there are no sales yet, that's
    // the correct truth for the day.
    val hasExpenseData = state.currentMonthExpense != null || state.lastMonthExpense != null
    val isMonthRange   = selectedRange == NTDateRange.MONTHLY
    val headlineLabel  = when {
        hasExpenseData && isMonthRange -> "NET PROFIT"
        isMonthRange                   -> "TOTAL REVENUE"
        else                           -> "RANGE REVENUE"
    }
    val headlineValue  = when {
        hasExpenseData && isMonthRange -> metrics.thisMonthProfit
        isMonthRange                   -> metrics.thisMonthRevenue
        else                           -> metrics.rangeRevenue
    }
    val lastValue      = if (hasExpenseData && isMonthRange) metrics.lastMonthProfit else metrics.lastMonthRevenue
    val growth         = when {
        hasExpenseData && isMonthRange -> metrics.profitGrowthPercent
        isMonthRange                   -> metrics.revenueGrowthPercent
        else                           -> metrics.weeklyGrowthPercent  // best non-month proxy
    }

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
                adminName           = adminName.ifBlank { "Admin" },
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

        // Revenue / Profit hero card — fully dynamic; chart, axes & headline
        // all swap when the range picker below the KPI grid changes.
        item {
            NTRevenueHeroCard(
                shopName       = "Neer Thuli",
                headlineLabel  = headlineLabel,
                headlineValue  = headlineValue,
                lastMonthValue = lastValue,
                growthPercent  = growth,
                totalOrders    = state.orders.size,
                totalCustomers = state.customers.size,
                activeStaff    = activeStaff,
                fleetActive    = state.fleetActiveCount,
                chartPoints    = metrics.chartPoints,
                yAxisLabels    = metrics.yAxisLabels,
                xAxisLabels    = metrics.xAxisLabels,
                dateLabel      = metrics.rangeLabel,
            )
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // KPI grid
        item {
            NTKpiGrid(items = kpiItems)
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // Analytics — chart that REACTS to the range picker (week / month /
        // quarter / year). Both the data series and the X-axis labels
        // recompute on every selection change.
        item {
            Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
                NTSectionHeader(label = "ANALYTICS", title = "Revenue trend")
                Spacer(modifier = Modifier.height(NTDp.md))
                NTDateRangePicker(
                    selected = selectedRange,
                    onSelect = { selectedRange = it },
                )
                Spacer(modifier = Modifier.height(NTDp.md))
                NTLineChartCard(
                    title         = rangeAnalyticsTitle(selectedRange),
                    valueLabel    = formatAmount(metrics.rangeRevenue),
                    growthPercent = metrics.weeklyGrowthPercent ?: 0.0,
                    points        = metrics.chartPoints,
                    labels        = analyticsAxisLabels(metrics.xAxisLabels, metrics.chartPoints.size),
                    rawValues     = metrics.chartRaw,
                    xRawLabels    = metrics.xAxisLabels
                )
            }
        }
        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // Stock performance chart — REAL stock growth %
        if (state.stockItems.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
                    val stockPoints = state.stockItems
                        .map { it.stockAvailable.toFloat() }
                        .let { pts ->
                            val max = pts.maxOrNull() ?: 1f
                            if (max <= 0f) pts else pts.map { it / max }
                        }
                    val stockRaw = state.stockItems.map { it.stockAvailable.toDouble() }
                    val stockRawLabels = state.stockItems.map { it.name }
                    NTLineChartCard(
                        title         = "STOCK LEVELS",
                        valueLabel    = "${formatCases(totalStock)} units on hand",
                        growthPercent = metrics.stockGrowthPercent ?: 0.0,
                        points        = if (stockPoints.size >= 2) stockPoints else List(7) { 0.5f },
                        labels        = state.stockItems.map { it.name.take(3).uppercase() }
                            .let { l -> if (l.size < 2) listOf("5L", "10L", "20L", "Bulk") else l },
                        rawValues     = if (stockRaw.size >= 2) stockRaw else List(7) { 10.0 },
                        xRawLabels    = if (stockRawLabels.size >= 2) stockRawLabels else listOf("5L", "10L", "20L", "Bulk"),
                        valueFormatter = { "${it.toInt()} cans" }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(NTDp.lg)) }
        }

//        // Activity feed
//        item {
//            NTActivityFeedSection(
//                orders    = state.orders,
//                customers = state.customers,
//                onViewAll = {}
//            )
//        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

//        // Staff overview row
//        if (state.employees.isNotEmpty()) {
//            item {
//                Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
//                    NTSectionHeader(label = "TEAM", title = "Staff overview")
//                    Spacer(modifier = Modifier.height(NTDp.md))
//                }
//            }
//            items(items = state.employees.take(3)) { emp ->
//                NTStaffCard(employee = emp, modifier = Modifier.padding(horizontal = NTDp.screenPad))
//                Spacer(modifier = Modifier.height(NTDp.sm))
//            }
//            item { Spacer(modifier = Modifier.height(NTDp.sm)) }
//        }
    }
}

/** Maps the visible date-range tab to the metrics-layer enum. */
private fun NTDateRange.toDashboardRange(): DashboardRange = when (this) {
    NTDateRange.WEEKLY    -> DashboardRange.WEEK
    NTDateRange.MONTHLY   -> DashboardRange.MONTH
    NTDateRange.QUARTERLY -> DashboardRange.QUARTER
    NTDateRange.YEARLY    -> DashboardRange.YEAR
}

private fun rangeAnalyticsTitle(range: NTDateRange): String = when (range) {
    NTDateRange.WEEKLY    -> "WEEKLY REVENUE"
    NTDateRange.MONTHLY   -> "MONTHLY REVENUE"
    NTDateRange.QUARTERLY -> "QUARTERLY REVENUE"
    NTDateRange.YEARLY    -> "YEARLY REVENUE"
}

/**
 * The line-chart card paints labels evenly across the bottom — too many tick
 * marks turn into illegible mush, so we down-sample to ~7 markers maximum.
 */
private fun analyticsAxisLabels(source: List<String>, pointCount: Int): List<String> {
    if (source.isEmpty()) return List(pointCount.coerceAtLeast(1)) { "" }
    if (source.size <= 7) return source
    val step = (source.size - 1) / 6.0
    return (0..6).map { i -> source[(i * step).toInt().coerceAtMost(source.size - 1)] }
}

// ── Staff card ────────────────────────────────────────────────

@Composable
private fun NTStaffCard(employee: EmployeeInfo, modifier: Modifier = Modifier) {
    val (statusColor, statusLabel) = when (employee.status) {
        "on_delivery" -> NTColors.Success to "On Delivery"
        "active"      -> NTColors.Primary to "Active"
        else          -> NTColors.TextTertiary to "Inactive"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NTColors.Surface, RoundedCornerShape(NTDp.radLg))
            .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radLg))
            .padding(NTDp.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(NTDp.radMd))
                .background(NTColors.PrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Person, null, tint = NTColors.Primary, modifier = Modifier.size(NTDp.iconMd))
        }
        Spacer(modifier = Modifier.width(NTDp.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(employee.name, color = NTColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${employee.completedDeliveries} deliveries · ${employee.rating} rating",
                color = NTColors.TextTertiary, fontSize = 12.sp,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(NTDp.radFull))
                .background(statusColor.copy(alpha = 0.10f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NTColors.Surface, RoundedCornerShape(NTDp.radLg))
                    .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radLg))
                    .padding(NTDp.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(NTDp.radMd))
                        .background(NTColors.PrimaryLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Person, null, tint = NTColors.Primary, modifier = Modifier.size(NTDp.iconMd))
                }
                Spacer(modifier = Modifier.width(NTDp.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(cust.name, color = NTColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${cust.cansHeld} cans held · ${cust.phone ?: ""}",
                        color = NTColors.TextTertiary, fontSize = 12.sp,
                    )
                }
                if (cust.balance > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(NTDp.radFull))
                            .background(NTColors.ErrorLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "₹${cust.balance.toInt()} DUE",
                            color = NTColors.Error, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        )
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
                    labels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
                    rawValues = state.weeklyRevenueRaw.map { it.toDouble() },
                    xRawLabels = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                )
            }
        }
        // CSAT card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NTColors.Surface, RoundedCornerShape(NTDp.radLg))
                    .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radLg))
                    .padding(NTDp.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(NTDp.radMd))
                        .background(NTColors.SuccessLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.StarRate, null, tint = NTColors.Success, modifier = Modifier.size(NTDp.iconMd))
                }
                Spacer(modifier = Modifier.width(NTDp.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "CSAT SCORE", color = NTColors.TextTertiary, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                    )
                    Text("${state.csat} / 5.0", color = NTColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
                NTGrowthBadge(percent = 3.5)
            }
        }
        // Fleet summary
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NTColors.Surface, RoundedCornerShape(NTDp.radLg))
                    .border(1.dp, NTColors.Border, RoundedCornerShape(NTDp.radLg))
                    .padding(NTDp.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(NTDp.radMd))
                        .background(NTColors.DelivIconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.LocalShipping, null, tint = NTColors.DelivIconFg, modifier = Modifier.size(NTDp.iconMd))
                }
                Spacer(modifier = Modifier.width(NTDp.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "FLEET ACTIVE", color = NTColors.TextTertiary, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                    )
                    Text("${state.fleetActiveCount} vehicles", color = NTColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ── Helper functions ──────────────────────────────────────────

private fun getGreeting(): String {
    val hour = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .hour

    return when (hour) {
        in 6..11 -> "Good Morning ☀️"
        in 12..16 -> "Good Afternoon 🌤️"
        in 17..20 -> "Good Evening 🌇"
        else -> "Good Night 🌙"
    }
}

private fun buildGreeting(
    profit: Double,
    pending: Int,
    delivered: Int,
    revGrowth: Double?,
): Pair<String, String> {

    val greeting = getGreeting()

    val subtext = when {
        pending > 5 ->
            "$pending orders are pending dispatch."

        delivered > 0 ->
            "$delivered deliveries completed today."

        profit < 0 ->
            "Heads up — this month is running at a loss so far."

        revGrowth != null && revGrowth >= 10.0 ->
            "Revenue is up ${formatPctShort(revGrowth)}% vs last month — nice work."

        revGrowth != null && revGrowth <= -5.0 ->
            "Revenue is down ${formatPctShort(-revGrowth)}% vs last month."

        profit > 0 ->
            "You're in the green this month."

        else ->
            "Here's how your business is doing today."
    }

    return greeting to subtext
}

private fun formatPctShort(p: Double): String {
    val rounded = (kotlin.math.abs(p) * 10).toLong() / 10.0
    return rounded.toString()
}

/**
 * Builds the four KPI cards entirely from real numbers — every "+/− X% vs last
 * month" footer reflects actual data, every up/down icon flips according to
 * the sign, and every accent colour matches the direction (green = up,
 * red = down, neutral grey when there's no last-month baseline).
 */
private fun buildKpis(state: AdminState, metrics: DashboardMetrics): List<NTKpiItem> {
    val totalStock = metrics.totalStockUnits

    // ── 1. Stock on hand ─────────────────────────────────────
    val stockKpi = run {
        val pct = metrics.stockGrowthPercent
        val (footerText, footerIcon, accent) = trendCell(pct, "vs last month")
        NTKpiItem(
            title         = "STOCK ON HAND",
            value         = formatCases(totalStock),
            subtitle      = "Units across all shops",
            subtitleColor = accent.text,
            icon          = Icons.Rounded.Inventory2,
            iconBg        = accent.bgLight,
            iconFg        = accent.iconFg,
            accentColor   = accent.iconFg,
            footerText    = "",
            footerIcon    = footerIcon,
            footerColor   = accent.text,
            cardIndex     = 0,
        )
    }

    // ── 2. Monthly revenue ───────────────────────────────────
    val revenueKpi = run {
        val pct = metrics.revenueGrowthPercent
        val (footerText, footerIcon, accent) = trendCell(pct, "vs last month")
        NTKpiItem(
            title         = "MONTHLY REVENUE",
            value         = formatAmount(metrics.thisMonthRevenue),
            subtitle      = pctSubtitle(pct, "vs last month"),
            subtitleColor = accent.text,
            icon          = Icons.Rounded.AccountBalanceWallet,
            iconBg        = NTColors.PrimaryLight,
            iconFg        = NTColors.Primary,
            accentColor   = NTColors.Primary,
            footerText    = "/*vs ${formatAmount(metrics.lastMonthRevenue)} last month*/",
            footerIcon    = footerIcon,
            footerColor   = accent.text,
            cardIndex     = 1,
        )
    }

    // ── 3. Empty cans (live derivation from movements) ──
    val cansKpi = NTKpiItem(
        title         = "EMPTY CANS",
        value         = "${metrics.emptyCansAtShop}",
        subtitle      = "Available at shop",
        subtitleColor = NTColors.TextSecondary,
        icon          = Icons.AutoMirrored.Rounded.Undo,
        iconBg        = Color(0xFFFFF3E8),
        iconFg        = Color(0xFFF97316),
        accentColor   = Color(0xFFF97316),
        footerText    = "${metrics.emptyCansOut} with customers",
        footerIcon    = Icons.Rounded.Group,
        footerColor   = NTColors.TextTertiary,
        cardIndex     = 2,
    )

    // ── 4. Customers ─────────────────────────────────────────
    val customersKpi = run {
        val pct = metrics.customerGrowthPercent
        val (footerText, footerIcon, accent) = trendCell(pct, "active this month")
        NTKpiItem(
            title         = "TOTAL CUSTOMERS",
            value         = "${state.customers.size}",
            subtitle      = "${metrics.newCustomersThisMonth} active this month",
            subtitleColor = accent.text,
            icon          = Icons.Rounded.Group,
            iconBg        = NTColors.SuccessLight,
            iconFg        = NTColors.Success,
            accentColor   = NTColors.Success,
            footerText    = "",
            footerIcon    = footerIcon,
            footerColor   = accent.text,
            cardIndex     = 3,
        )
    }

    // ── 5. Monthly profit ────────────────────────────────────
    val profitKpi = run {
        val pct = metrics.profitGrowthPercent
        val (footerText, footerIcon, accent) = trendCell(pct, "vs last month")
        NTKpiItem(
            title         = "MONTHLY PROFIT",
            value         = formatAmount(metrics.thisMonthProfit),
            subtitle      = pctSubtitle(pct, "vs last month"),
            subtitleColor = accent.text,
            icon          = Icons.Rounded.TrendingUp,
            iconBg        = NTColors.SuccessLight,
            iconFg        = NTColors.Success,
            accentColor   = NTColors.Success,
            footerText    = "vs ${formatAmount(metrics.lastMonthProfit)} last month",
            footerIcon    = footerIcon,
            footerColor   = accent.text,
            cardIndex     = 4,
        )
    }

    return listOf(stockKpi, revenueKpi, cansKpi, customersKpi)
}

/** Picks footer text + icon + colour palette for a percent change. */
private fun trendCell(
    pct: Double?,
    suffix: String,
): Triple<String, ImageVector, TrendAccent> = when {
    pct == null  -> Triple("No baseline yet", Icons.Rounded.Remove, TrendAccent.Neutral)
    pct >= 0.5   -> Triple("+${formatPctShort(pct)}% $suffix",   Icons.Rounded.TrendingUp,   TrendAccent.Up)
    pct <= -0.5  -> Triple("-${formatPctShort(-pct)}% $suffix",  Icons.Rounded.TrendingDown, TrendAccent.Down)
    else         -> Triple("Flat $suffix",                       Icons.Rounded.Remove,       TrendAccent.Neutral)
}

private fun pctSubtitle(pct: Double?, suffix: String): String = when {
    pct == null -> "No baseline yet"
    pct >= 0.5  -> "+${formatPctShort(pct)}% $suffix"
    pct <= -0.5 -> "-${formatPctShort(-pct)}% $suffix"
    else        -> "Flat $suffix"
}

/** Coloured accent palette per trend direction. */
private data class TrendAccent(
    val text: Color,
    val iconFg: Color,
    val bgLight: Color,
) {
    companion object {
        val Up      = TrendAccent(NTColors.SuccessText, NTColors.Success,      NTColors.SuccessLight)
        val Down    = TrendAccent(NTColors.ErrorText,   NTColors.Error,        NTColors.ErrorLight)
        val Neutral = TrendAccent(NTColors.TextSecondary, NTColors.TextTertiary, NTColors.SurfaceVar)
    }
}

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

private fun formatCases(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        val rounded = (value * 10).toLong() / 10.0
        if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }
}
