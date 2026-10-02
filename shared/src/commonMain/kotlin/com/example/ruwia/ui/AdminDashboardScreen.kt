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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.Order
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.presentation.AdminViewModel
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
    var showStockHistory by remember { mutableStateOf(false) }

    // Pull-to-refresh indicator: set on swipe, cleared when the reload
    // finishes (or fails) so the spinner never sticks.
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(state.loading) {
        if (!state.loading) isRefreshing = false
    }

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

    if (showStockHistory) {
        SystemBackHandler { showStockHistory = false }
        com.example.ruwia.ui.StockHistoryScreen(
            state  = state,
            onBack = { showStockHistory = false },
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
            onUpdateCustomer = vm::updateCustomer,
            onDeleteCustomer = vm::deleteCustomer,
            onClearError     = vm::clearError,
            onBack           = { showCustomers = false },
            saleEntries      = state.saleEntries,
            movements        = state.recentMovements,
            customerPrices   = state.customerPrices,
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
            shops      = state.shopNames,
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
            employees      = state.employees,
            shops          = state.shopNames,
            onBack         = { showEmployees = false },
            onAddEmployee  = { showAddEmployee = true },
            onUpdateEmployee = vm::updateEmployee,
            onDeleteEmployee = vm::deleteEmployee,
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
            shops      = state.shopNames.take(2).mapIndexed { index, name ->
                val location = state.shopStocks.getOrNull(index)?.location
                    ?: if (index == 0) "Primary Shop" else "Secondary Shop"
                name to location
            },
            onBack     = { productToRestock = null; editModeStock = 0; showAddStock = false },
            onClose    = { productToRestock = null; editModeStock = 0; showAddStock = false },
            onSave     = { productId, sku, brandName, purchasePrice, sellingPrice, qty, shopName, dateTimeIso, emptyCans, lowStockAlert, notes ->
                if (productToRestock != null) {
                    // Edit mode: update product details + stock delta
                    vm.updateProductWithStockDelta(
                        productId = productToRestock!!.id,
                        brandName = brandName,
                        purchasePrice = purchasePrice,
                        sellingPrice = sellingPrice,
                        newStock = qty,
                        previousStock = editModeStock,
                        shopName = shopName,
                        lowStockAlert = lowStockAlert,
                        createdAt = dateTimeIso
                    )
                } else {
                    vm.addInwardStockEntry(
                        sku = sku,
                        brandName = brandName,
                        purchasePrice = purchasePrice,
                        sellingPrice = sellingPrice,
                        qty = qty,
                        shopName = shopName,
                        createdAt = dateTimeIso,
                        emptyCans = emptyCans,
                        lowStockAlert = lowStockAlert,
                        notes = notes
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
        // Pull-to-refresh on every tab: swiping down re-pulls the whole
        // admin dataset (shops, movements, products, sales, customers) from
        // the backend. Inner tabs keep consuming `contentPadding` exactly as
        // before, so insets are unchanged.
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                vm.loadData()
            },
            modifier = Modifier.fillMaxSize(),
        ) {
        when (selectedTab) {
            0 -> AdminHomeTab(
                     state          = state,
                     vm             = vm,
                     contentPadding = contentPadding,
                     adminName      = adminName,
                     onLogout       = onLogout,
                     onOpenSettings = { selectedTab = 4 },
                     onOpenAnalytics = { selectedTab = 3 },
                     onOpenCustomers = { showCustomers = true },
                     onOpenInventory = { selectedTab = 1 },
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
                     onOpenStockHistory = { showStockHistory = true },
                     onSaveCustomerPrice = vm::saveCustomerProductPrice,
                     onLoadCustomerPrices = vm::loadCustomerPrices,
                     onDeleteCustomerPrice = vm::deleteCustomerProductPrice,
                     contentPadding = contentPadding,
                 )
            2 -> TransactionsScreen(
                     state          = state,
                     contentPadding = contentPadding,
                     onBackHome     = { selectedTab = 0 },
                 )
            3 -> ProfitDashboardScreen(
                     state           = state,
                     onBack          = {},
                     onExpenseMonthSelected = vm::loadMonthlyExpense,
                     onExpenseSave     = vm::saveMonthlyExpense,
                     onProductClick    = { pName -> productDetailName = pName },
                     onAddStock        = { showAddStock = true },
                     onAddProduct      = { selectedTab = 1 },
                     onOpenTransactions = { selectedTab = 2 },
                     onClearData       = vm::clearStockAndRevenue,
                     contentPadding    = contentPadding,
                 )
            4 -> SettingsScreen(
                     state                  = state,
                     adminName              = adminName,
                     adminEmail             = adminEmail,
                     onBack                 = { selectedTab = 0 },
                     onNavigateToEmployees  = { showEmployees = true },
                     onNavigateToPricing    = { showPricing = true },
                     onNavigateToCustomers  = { showCustomers = true },
                     onLogout               = onLogout,
                     onResetEmptyCases      = { vm.resetEmptyCases() },
                     onAddEmptyCases        = vm::addEmptyCases,
                     onShopNameChange       = { index, name -> vm.updateShopName(index, name) },
                     contentPadding         = contentPadding
                 )
        }
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
    onOpenAnalytics: () -> Unit = {},
    onOpenCustomers: () -> Unit = {},
    onOpenInventory: () -> Unit = {},
) {
    when {
        state.loading  -> NTDashboardSkeleton(contentPadding)
        state.error != null -> NTErrorState(
            message = state.error,
            onRetry  = vm::loadData,
            contentPadding = contentPadding
        )
        else -> AdminDashboardContent(
            state,
            contentPadding,
            adminName,
            onLogout,
            onOpenSettings,
            onOpenAnalytics = onOpenAnalytics,
            onOpenCustomers = onOpenCustomers,
            onOpenInventory = onOpenInventory,
        )
    }
}

@Composable
private fun AdminDashboardContent(
    state: AdminState,
    contentPadding: PaddingValues,
    adminName: String,
    @Suppress("UNUSED_PARAMETER") onLogout: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenCustomers: () -> Unit = {},
    onOpenInventory: () -> Unit = {},
) {
    // Revenue + customer pickers drive their own charts.
    // Overview period dropdown defaults to Month.
    var revenueRange by remember { mutableStateOf(NTDateRange.MONTHLY) }
    var customerRange by remember { mutableStateOf(NTDateRange.MONTHLY) }
    var overviewPeriod by remember { mutableStateOf(OverviewPeriod.MONTH) }

    val monthMetrics = remember(state) {
        state.toDashboardMetrics(DashboardRange.MONTH)
    }
    val quarterMetrics = remember(state) {
        state.toDashboardMetrics(DashboardRange.QUARTER)
    }
    val yearMetrics = remember(state) {
        state.toDashboardMetrics(DashboardRange.YEAR)
    }
    val revenueMetrics = remember(state, revenueRange) {
        state.toDashboardMetrics(revenueRange.toDashboardRange())
    }
    val customerMetrics = remember(state, customerRange) {
        state.toDashboardMetrics(customerRange.toDashboardRange())
    }

    val pendingOrders = state.orders.count { it.status == "pending" }
    val deliveredToday = state.orders.count { it.status == "delivered" }

    val (greeting, subtext) = buildGreeting(
        profit    = monthMetrics.thisMonthProfit,
        pending   = pendingOrders,
        delivered = deliveredToday,
        revGrowth = monthMetrics.revenueGrowthPercent,
    )

    val displayShopName = state.shopNames.firstOrNull()?.takeIf { it.isNotBlank() } ?: "ALHUDHA"

    // ── Overview numbers for the selected dropdown period ──────
    // Month / Last Month show NET PROFIT (expense data exists only for
    // these two months). Quarter / Yearly fall back to TOTAL REVENUE.
    val totalCustomers = state.customers.size
    val lastMonthCustomers = (totalCustomers - monthMetrics.newCustomersThisMonth).coerceAtLeast(0)

    // ── Stock alert — active products at or below the low-stock threshold.
    // ── Stock alert — each product carries its own low-stock threshold,
    // set while adding the product (presets 1/3/5/8/10 or custom).
    val lowStockProducts = remember(state.productCategories) {
        state.productCategories.filter {
            it.isActive && !it.isDeleted && it.stockAvailable <= it.lowStockAlert
        }
    }

    val overviewChip: String = when (overviewPeriod) {
        OverviewPeriod.MONTH        -> monthMetrics.rangeLabel
        OverviewPeriod.LAST_MONTH   -> formatYearMonth(state.previousMonth)
        OverviewPeriod.LAST_QUARTER -> "Last Quarter"
        OverviewPeriod.YEARLY       -> state.currentMonth.take(4).takeIf { it.length == 4 } ?: "Yearly"
    }
    val overviewLeftLabel: String = when (overviewPeriod) {
        OverviewPeriod.LAST_QUARTER, OverviewPeriod.YEARLY -> "TOTAL REVENUE"
        else -> "NET PROFIT"
    }
    val overviewLeftValue: Double = when (overviewPeriod) {
        OverviewPeriod.LAST_MONTH   -> monthMetrics.lastMonthProfit
        OverviewPeriod.LAST_QUARTER -> quarterMetrics.rangeRevenue
        OverviewPeriod.YEARLY       -> yearMetrics.rangeRevenue
        else                        -> monthMetrics.thisMonthProfit
    }
    val overviewLeftGrowth: Double? = when (overviewPeriod) {
        OverviewPeriod.MONTH        -> monthMetrics.profitGrowthPercent
        OverviewPeriod.LAST_QUARTER -> quarterMetrics.revenueRangeGrowthPercent
        OverviewPeriod.YEARLY       -> yearMetrics.revenueRangeGrowthPercent
        else                        -> null
    }
    val overviewRightValue: Int = when (overviewPeriod) {
        OverviewPeriod.LAST_MONTH   -> lastMonthCustomers
        OverviewPeriod.LAST_QUARTER -> quarterMetrics.rangeCustomers
        OverviewPeriod.YEARLY       -> yearMetrics.rangeCustomers
        else                        -> totalCustomers
    }
    val overviewRightGrowth: Double? = when (overviewPeriod) {
        OverviewPeriod.MONTH        -> monthMetrics.customerGrowthPercent
        OverviewPeriod.LAST_QUARTER -> quarterMetrics.customerRangeGrowthPercent
        OverviewPeriod.YEARLY       -> yearMetrics.customerRangeGrowthPercent
        else                        -> null
    }

    // ── Analytics numbers (range-scoped) ───────────────────────
    val revenueGrowth: Double? = when (revenueRange) {
        NTDateRange.WEEKLY -> revenueMetrics.weeklyGrowthPercent
        NTDateRange.MONTHLY -> revenueMetrics.revenueGrowthPercent
        NTDateRange.QUARTERLY -> revenueMetrics.revenueGrowthPercent
        NTDateRange.YEARLY -> revenueMetrics.revenueGrowthPercent
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
                shopName            = displayShopName,
                adminName           = adminName.ifBlank { "Admin" },
                notificationCount   = pendingOrders,
                onAvatarClick       = onOpenSettings,
                onSearchClick       = {},
                onNotificationClick = {}
            )
        }

        // Greeting
        item {
            NTGreetingSection(greeting = greeting, subtext = subtext)
        }

        // Business overview — dark teal KPI card, NO graph.
        item {
            NTBusinessOverviewCard(
                period = overviewPeriod,
                onPeriodChange = { overviewPeriod = it },
                chipLabel = overviewChip,
                leftLabel = overviewLeftLabel,
                leftValue = overviewLeftValue,
                leftGrowth = overviewLeftGrowth,
                rightValue = overviewRightValue,
                rightGrowth = overviewRightGrowth,
            )
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // Analytics — revenue line chart reacts to its own picker.
        item {
            Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
                NTSectionHeader(label = "ANALYTICS", title = "Revenue trend")
                Spacer(modifier = Modifier.height(NTDp.md))
                NTDateRangePicker(
                    selected = revenueRange,
                    onSelect = { revenueRange = it },
                )
                Spacer(modifier = Modifier.height(NTDp.md))
                NTLineChartCard(
                    title = rangeAnalyticsTitle(revenueRange),
                    valueLabel = formatAmount(revenueMetrics.rangeRevenue),
                    growthPercent = revenueGrowth ?: 0.0,
                    points = revenueMetrics.chartPoints,
                    labels = revenueMetrics.xAxisLabels,
                    rawValues = revenueMetrics.chartRaw,
                    xRawLabels = revenueMetrics.xAxisLabels,
                    onViewAllClick = onOpenAnalytics,
                    viewAllLabel = "Open analytics",
                )
            }
        }

        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // Customer graph — existing line chart, own month/quarter/year picker.
        item {
            Column(modifier = Modifier.padding(horizontal = NTDp.screenPad)) {
                NTSectionHeader(label = "CUSTOMERS", title = "Customer trend")
                Spacer(modifier = Modifier.height(NTDp.md))
                NTDateRangePicker(
                    selected = customerRange,
                    onSelect = { customerRange = it },
                    options = listOf(
                        NTDateRange.MONTHLY,
                        NTDateRange.QUARTERLY,
                        NTDateRange.YEARLY,
                    ),
                )
                Spacer(modifier = Modifier.height(NTDp.md))
                NTLineChartCard(
                    title = rangeCustomerTitle(customerRange),
                    valueLabel = "${customerMetrics.rangeCustomers} customers",
                    growthPercent = customerMetrics.customerRangeGrowthPercent ?: 0.0,
                    points = customerMetrics.customerChartPoints,
                    labels = customerMetrics.customerXLabels,
                    rawValues = customerMetrics.customerChartRaw,
                    xRawLabels = customerMetrics.customerXLabels,
                    valueFormatter = { "${it.toInt()} customers" },
                    onViewAllClick = onOpenCustomers,
                    viewAllLabel = "Open customers",
                )
            }
        }
        item { Spacer(modifier = Modifier.height(NTDp.lg)) }

        // Stock alert — warning tab at the bottom, opens Inventory.
        item {
            StockAlertCard(
                lowCount = lowStockProducts.size,
                onOpen = onOpenInventory,
            )
        }
        item { Spacer(modifier = Modifier.height(NTDp.lg)) }
    }
}

// ── Stock alert card ────────────────────────────────────────────
//  Warning tab at the bottom of home; tap opens the Inventory screen.

@Composable
private fun StockAlertCard(
    lowCount: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = when {
        lowCount <= 0 -> "All stocks healthy"
        lowCount == 1 -> "1 product needs restock"
        else          -> "$lowCount products need restock"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NTDp.screenPad)
            .shadow(3.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(NTColors.Surface)
            .border(1.dp, NTColors.Border, RoundedCornerShape(24.dp))
            .clickable(onClickLabel = "Open inventory", onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(NTColors.WarningLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = NTColors.Warning,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "STOCK ALERT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = NTColors.TextTertiary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.2).sp,
                color = NTColors.TextPrimary,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(NTColors.SurfaceVar)
                .border(1.dp, NTColors.Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = NTColors.Warning,
                modifier = Modifier.size(20.dp),
            )
        }
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

private fun rangeCustomerTitle(range: NTDateRange): String = when (range) {
    NTDateRange.WEEKLY    -> "WEEKLY CUSTOMERS"
    NTDateRange.MONTHLY   -> "MONTHLY CUSTOMERS"
    NTDateRange.QUARTERLY -> "QUARTERLY CUSTOMERS"
    NTDateRange.YEARLY    -> "YEARLY CUSTOMERS"
}

/** "2026-08" → "Aug 2026" for the overview period chip. */
private fun formatYearMonth(yearMonth: String): String {
    if (yearMonth.length < 7) return "Last Month"
    val year = yearMonth.substring(0, 4)
    val month = yearMonth.substring(5, 7).toIntOrNull() ?: return "Last Month"
    val abbr = when (month) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
        5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
        9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
        else -> return "Last Month"
    }
    return "$abbr $year"
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
                Text("Qty: ${order.qty} cases · ${order.createdAt?.let { com.example.ruwia.util.isoToDisplayDate(it) } ?: ""}",
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
