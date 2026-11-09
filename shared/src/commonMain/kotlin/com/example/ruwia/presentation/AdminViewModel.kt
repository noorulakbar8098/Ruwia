package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.AdminRepository
import com.example.ruwia.data.NotificationRepository
import com.example.ruwia.data.TestDataSeeder
import com.example.ruwia.data.awaitAuthentication
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.CustomerLedger
import com.example.ruwia.domain.CustomerProductPrice
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.MonthlyExpense
import com.example.ruwia.domain.NotificationItem
import com.example.ruwia.domain.Order
import com.example.ruwia.domain.Payment
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.SaleEntry
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockItem
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.Supplier
import com.example.ruwia.domain.canonicalShopName
import com.example.ruwia.domain.effectiveStockMap
import com.example.ruwia.util.sanitizeError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Tracks the lifecycle of a single employee-creation attempt. */
sealed class EmployeeCreationState {
    object Idle    : EmployeeCreationState()
    object Loading : EmployeeCreationState()
    data class Success(
        val userId:   String,
        val name:     String,
        val email:    String,
        val password: String,
        val role:     String,
        val shop:     String
    ) : EmployeeCreationState()
    data class Error(val message: String) : EmployeeCreationState()
}

/** Progress of the 2-year test-data seeding run (null = idle). */
data class SeedProgress(
    val stage: String,
    val done: Int,
    val total: Int,
    val finished: Boolean = false,
    val error: String? = null,
) {
    val fraction: Float get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
}

data class AdminState(
    val loading: Boolean = false,
    val error: String? = null,
    val mrr: Double = 0.0,
    val csat: Double = 0.0,
    val fleetActiveCount: Int = 0,
    val stockItems: List<StockItem> = emptyList(),
    val orders: List<Order> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val employees: List<EmployeeInfo> = emptyList(),
    val weeklyRevenuePoints: List<Float> = emptyList(),
    val weeklyRevenueRaw: List<Float> = emptyList(),
    val weeklyRevenueLabel: String = "",
    val shopStocks: List<ShopStockInfo> = emptyList(),
    val recentMovements: List<StockMovement> = emptyList(),
    val productCategories: List<ProductCategory> = emptyList(),
    val saleEntries: List<SaleEntry> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val ledger: CustomerLedger? = null,
    val ledgerCustomerId: String? = null,
    val ledgerLoading: Boolean = false,
    val paymentSaving: Boolean = false,
    val paymentMessage: String? = null,
    val currentMonthExpense: MonthlyExpense? = null,
    val lastMonthExpense: MonthlyExpense? = null,
    val monthlyExpenses: Map<String, MonthlyExpense> = emptyMap(),

    val employeeCreation: EmployeeCreationState = EmployeeCreationState.Idle,
    /** "YYYY-MM" — set in [AdminViewModel.loadData]. Empty until first load. */
    val currentMonth: String = "",
    /** "YYYY-MM" of the previous calendar month. */
    val previousMonth: String = "",
    /** Display names of the business's two shops (index 0 = Shop 1, 1 = Shop 2). */
    val shopNames: List<String> = listOf("Shop 1", "Shop 2"),
    /** Custom shop name set by the admin (kept as the primary display fallback). */
    val shopName: String = "",
    /** Custom label shown above the admin's name on the dashboard. */
    val adminConsoleName: String = "ADMIN CONSOLE",
    /** Global baseline subtracted from the live "Empty Cases" figure. */
    val emptyCansBaseline: Int = 0,
    /** Active customer-specific prices per product, keyed by product id. */
    val customerPrices: Map<String, List<CustomerProductPrice>> = emptyMap(),
    /** In-app notification feed (broadcasts + directs), newest first. */
    val notifications: List<NotificationItem> = emptyList(),
    /** Unread badge count for the header bell. */
    val unreadNotifications: Int = 0,
    val tasks: List<DeliveryTask> = emptyList(),
)

private data class HomeSnapshot(
    val products: List<ProductCategory>,
    val movements: List<StockMovement>,
    val saleEntries: List<SaleEntry>,
    val customers: List<Customer>,
    val orders: List<Order>,
    val tasks: List<DeliveryTask>,
    val feed: List<NotificationItem>,
    val shopNames: List<String>,
    val adminConsoleName: String,
    val baseline: Int,
    val expense: MonthlyExpense?,
    val lastExpense: MonthlyExpense?,
    val csat: Double,
)

class AdminViewModel(
    private val repo: AdminRepository,
    private val notifications: NotificationRepository = NotificationRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var initialLoadJob: Job? = null
    private var tabLoadJob: Job? = null
    private var saleHistoryJob: Job? = null
    private var overlayLoadJob: Job? = null
    private var customersLoadJob: Job? = null
    private var activeTab = 0
    private var initialLoaded = false
    private var inventoryLoaded = false
    private var transactionsLoaded = false
    private var analyticsLoaded = false
    private var settingsLoaded = false
    /** Earliest YYYY-MM-DD currently held in [AdminState.saleEntries]. */
    private var loadedSaleStart = ""

    init {
        startPeriodicRefresh()
    }

    fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(15_000L)
                // Only the visible tab is polled. Hidden tabs load on demand,
                // so we no longer re-download the whole admin dataset here.
                if (activeTab != 0 || !initialLoaded) continue
                if (!awaitAuthentication()) continue
                runCatching { refreshHomeNow() }
            }
        }
    }

    fun stopPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun clearState() {
        stopPeriodicRefresh()
        initialLoadJob?.cancel()
        tabLoadJob?.cancel()
        saleHistoryJob?.cancel()
        overlayLoadJob?.cancel()
        customersLoadJob?.cancel()
        activeTab = 0
        initialLoaded = false
        inventoryLoaded = false
        transactionsLoaded = false
        analyticsLoaded = false
        settingsLoaded = false
        loadedSaleStart = ""
        _state.value = AdminState()
    }

    // ── Initial load ──────────────────────────────────────────────────────────

    /**
     * Loads only what the Home tab renders on the first frame. Inventory,
     * transactions, analytics and settings are loaded when their tab opens.
     */
    fun loadInitialData() {
        if (initialLoadJob?.isActive == true || initialLoaded) return
        if (refreshJob?.isActive != true) startPeriodicRefresh()
        initialLoadJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            if (!awaitAuthentication()) {
                _state.value = _state.value.copy(loading = false, error = "Not authenticated")
                return@launch
            }
            val currentMonth = currentYearMonth()
            val previousMonth = previousYearMonth()
            val saleStart = monthStartKey(1)
            runCatching {
                coroutineScope {
                    val products   = async { repo.getProductCategories() }
                    val movements  = async { repo.getRecentMovements() }
                    val sales      = async { repo.getSaleEntriesSince(saleStart) }
                    val customers  = async { repo.getAllCustomers() }
                    val orders     = async { repo.getAllOrders() }
                    val tasks      = async { repo.getDeliveryTasks() }
                    val shopNames  = async { repo.getShopNames() }
                    val adminConsoleName = async { repo.getAdminConsoleName() }
                    val baseline   = async { repo.getEmptyCansBaseline() }
                    val expense    = async { repo.getMonthlyExpense(currentMonth) }
                    val lastExpense = async { repo.getMonthlyExpense(previousMonth) }
                    val feed       = async { notifications.getFeed() }
                    val csat       = async { repo.getCSAT() }
                    HomeSnapshot(
                        products     = products.await(),
                        movements    = movements.await(),
                        saleEntries  = sales.await(),
                        customers    = customers.await(),
                        orders       = orders.await(),
                        tasks        = tasks.await(),
                        feed         = feed.await(),
                        shopNames    = shopNames.await(),
                        adminConsoleName = adminConsoleName.await(),
                        baseline     = baseline.await(),
                        expense      = expense.await(),
                        lastExpense  = lastExpense.await(),
                        csat         = csat.await(),
                    )
                }
            }.onSuccess { snapshot ->
                val previous = _state.value
                val products = snapshot.products
                val sales = mergeSaleEntries(snapshot.saleEntries, previous.saleEntries, saleStart)
                val weekly = weeklyRevenue(sales)
                _state.value = previous.copy(
                    loading             = false,
                    error               = null,
                    mrr                 = sales.filter { it.createdAt?.startsWith(currentMonth) == true }
                        .sumOf { it.totalSelling },
                    csat                = snapshot.csat,
                    fleetActiveCount    = previous.employees.count { it.status == "active" },
                    stockItems          = repo.stockSummaryList(products),
                    orders              = snapshot.orders,
                    customers           = snapshot.customers,
                    recentMovements     = snapshot.movements,
                    weeklyRevenuePoints = weekly.first,
                    weeklyRevenueRaw    = weekly.second,
                    weeklyRevenueLabel  = weekly.third,
                    productCategories   = products,
                    saleEntries         = sales,
                    currentMonthExpense = snapshot.expense,
                    lastMonthExpense    = snapshot.lastExpense,
                    monthlyExpenses     = listOfNotNull(snapshot.expense, snapshot.lastExpense)
                        .associateBy { it.month },
                    currentMonth        = currentMonth,
                    previousMonth       = previousMonth,
                    shopNames           = snapshot.shopNames,
                    shopName            = snapshot.shopNames.firstOrNull().orEmpty(),
                    adminConsoleName    = snapshot.adminConsoleName,
                    emptyCansBaseline   = if (snapshot.baseline >= 0) snapshot.baseline else previous.emptyCansBaseline,
                    notifications       = snapshot.feed,
                    unreadNotifications = snapshot.feed.count { !it.isRead },
                    tasks               = snapshot.tasks,
                ).deriveStockFromMovements()
                markSaleHistoryLoaded(saleStart)
                initialLoaded = true
            }.onFailure {
                _state.value = _state.value.copy(
                    loading = false,
                    error   = it.message ?: "Failed to load dashboard",
                )
            }
        }
    }

    /** Pull-to-refresh / retry for the currently visible tab. */
    fun refreshCurrentTab() = viewModelScope.launch {
        if (!awaitAuthentication()) {
            _state.value = _state.value.copy(loading = false, error = "Not authenticated")
            return@launch
        }
        if (activeTab == 0 && !initialLoaded) {
            loadInitialData()
            return@launch
        }
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            when (activeTab) {
                0 -> refreshHomeNow(includeMovements = true)
                1 -> loadInventoryData()
                2 -> loadTransactionsData()
                3 -> loadAnalyticsData()
                4 -> loadSettingsData()
            }
        }.onSuccess {
            _state.value = _state.value.copy(loading = false)
        }.onFailure {
            _state.value = _state.value.copy(
                loading = false,
                error   = it.message ?: "Could not refresh this screen",
            )
        }
    }

    /** Called whenever the bottom navigation changes. */
    fun onTabSelected(tab: Int) {
        activeTab = tab
        when (tab) {
            0 -> if (!initialLoaded) loadInitialData()
            1 -> if (!inventoryLoaded) runTabLoad { loadInventoryData() }
            2 -> if (!transactionsLoaded) runTabLoad { loadTransactionsData() }
            3 -> if (!analyticsLoaded) runTabLoad { loadAnalyticsData() }
            4 -> if (!settingsLoaded) runTabLoad { loadSettingsData() }
        }
    }

    private fun runTabLoad(block: suspend () -> Unit) {
        tabLoadJob?.cancel()
        tabLoadJob = viewModelScope.launch {
            if (!awaitAuthentication()) return@launch
            runCatching { block() }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Could not load this screen") }
        }
    }

    private suspend fun loadInventoryData() {
        coroutineScope {
            val products  = async { repo.getProductCategories() }
            val movements = async { repo.getRecentMovements() }
            val shops     = async { repo.getShopStocks() }
            val employees = async { repo.getEmployees() }
            val productList = products.await()
            _state.value = _state.value.copy(
                productCategories = productList,
                recentMovements   = movements.await(),
                shopStocks        = shops.await(),
                employees         = employees.await(),
                stockItems        = repo.stockSummaryList(productList),
            ).deriveStockFromMovements()
        }
        inventoryLoaded = true
        settingsLoaded = true
    }

    private suspend fun loadTransactionsData() {
        coroutineScope {
            val employees = async { repo.getEmployees() }
            val shops     = async { repo.getShopStocks() }
            val customers = async { repo.getAllCustomers() }
            fetchSaleHistory(23)
            _state.value = _state.value.copy(
                employees  = employees.await(),
                shopStocks = shops.await(),
                customers  = customers.await(),
            )
        }
        transactionsLoaded = true
    }

    private suspend fun loadAnalyticsData() {
        coroutineScope {
            val products  = async { repo.getProductCategories() }
            val movements = async { repo.getRecentMovements() }
            fetchSaleHistory(23)
            val productList = products.await()
            _state.value = _state.value.copy(
                productCategories = productList,
                recentMovements   = movements.await(),
                stockItems        = repo.stockSummaryList(productList),
            ).deriveStockFromMovements()
        }
        analyticsLoaded = true
        inventoryLoaded = true
    }

    private suspend fun loadSettingsData() {
        coroutineScope {
            val employees = async { repo.getEmployees() }
            val customers = async { repo.getAllCustomers() }
            val shops     = async { repo.getShopStocks() }
            _state.value = _state.value.copy(
                employees  = employees.await(),
                customers  = customers.await(),
                shopStocks = shops.await(),
            )
        }
        settingsLoaded = true
    }

    /** Expands the sale window only when a wider dashboard range is chosen. */
    fun ensureSaleHistory(monthsBack: Int) {
        if (!initialLoaded) return
        val target = monthStartKey(monthsBack)
        if (loadedSaleStart.isNotBlank() && loadedSaleStart <= target) return
        saleHistoryJob?.cancel()
        saleHistoryJob = viewModelScope.launch {
            if (!awaitAuthentication()) return@launch
            runCatching { fetchSaleHistory(monthsBack) }
        }
    }

    /** Loads the full rows the notifications overlay needs. */
    fun loadNotifications() {
        if (overlayLoadJob?.isActive == true) return
        overlayLoadJob = viewModelScope.launch {
            if (!awaitAuthentication()) return@launch
            runCatching { notifications.getFeed() }
                .onSuccess { feed ->
                    _state.value = _state.value.copy(
                        notifications = feed,
                        unreadNotifications = feed.count { !it.isRead },
                    )
                }
        }
    }

    fun loadCustomersForScreen() {
        if (customersLoadJob?.isActive == true) return
        customersLoadJob = viewModelScope.launch {
            if (!awaitAuthentication()) return@launch
            coroutineScope {
                val customers = async { repo.getAllCustomers() }
                val movements = async { repo.getRecentMovements() }
                fetchSaleHistory(23)
                _state.value = _state.value.copy(
                    customers       = customers.await(),
                    recentMovements = movements.await(),
                )
            }
        }
    }

    fun loadEmployees() {
        viewModelScope.launch {
            if (!awaitAuthentication()) return@launch
            runCatching { repo.getEmployees() }
                .onSuccess { list ->
                    _state.value = _state.value.copy(
                        employees = list,
                        fleetActiveCount = list.count { it.status == "active" },
                    )
                }
        }
    }

    fun loadPricing() {
        viewModelScope.launch {
            if (!awaitAuthentication()) return@launch
            runCatching { repo.getProductCategories() }
                .onSuccess { products ->
                    _state.value = _state.value.copy(
                        productCategories = products,
                        stockItems = repo.stockSummaryList(products),
                    )
                }
        }
    }

    fun loadStockItems() {
        viewModelScope.launch {
            if (!awaitAuthentication()) return@launch
            coroutineScope {
                val products = async { repo.getProductCategories() }
                val shops    = async { repo.getShopStocks() }
                val productList = products.await()
                _state.value = _state.value.copy(
                    productCategories = productList,
                    stockItems = repo.stockSummaryList(productList),
                    shopStocks = shops.await(),
                )
            }
        }
    }

    private suspend fun fetchSaleHistory(monthsBack: Int) {
        val start = monthStartKey(monthsBack)
        if (loadedSaleStart.isNotBlank() && loadedSaleStart <= start) return
        val fresh = repo.getSaleEntriesSince(start)
        val previous = _state.value.saleEntries
        // A transient read failure returns an empty list. Keep the cache rather
        // than blanking charts that were already showing valid data.
        if (fresh.isEmpty() && previous.isNotEmpty()) return
        _state.value = _state.value.copy(
            saleEntries = mergeSaleEntries(fresh, previous, start),
        )
        markSaleHistoryLoaded(start)
    }

    private suspend fun refreshHomeNow(includeMovements: Boolean = false) {
        val currentMonth = currentYearMonth()
        val previousMonth = previousYearMonth()
        val saleStart = monthStartKey(1)
        val snapshot = coroutineScope {
            val products    = async { repo.getProductCategories() }
            val movements   = if (includeMovements) async { repo.getRecentMovements() } else null
            val sales       = async { repo.getSaleEntriesSince(saleStart) }
            val customers   = async { repo.getAllCustomers() }
            val orders      = async { repo.getAllOrders() }
            val tasks       = async { repo.getDeliveryTasks() }
            val expense     = async { repo.getMonthlyExpense(currentMonth) }
            val lastExpense = async { repo.getMonthlyExpense(previousMonth) }
            val feed        = async { notifications.getFeed() }
            HomeSnapshot(
                products     = products.await(),
                movements    = movements?.await() ?: _state.value.recentMovements,
                saleEntries  = sales.await(),
                customers    = customers.await(),
                orders       = orders.await(),
                tasks        = tasks.await(),
                feed         = feed.await(),
                shopNames    = emptyList(),
                baseline     = -1,
                expense      = expense.await(),
                lastExpense  = lastExpense.await(),
                csat         = _state.value.csat,
            )
        }
        val previous = _state.value
        val products = snapshot.products
        val sales = mergeSaleEntries(snapshot.saleEntries, previous.saleEntries, saleStart)
        val weekly = weeklyRevenue(sales)
        _state.value = previous.copy(
            productCategories   = products,
            saleEntries         = sales,
            stockItems          = repo.stockSummaryList(products),
            customers           = snapshot.customers,
            orders              = snapshot.orders,
            tasks               = snapshot.tasks,
            recentMovements     = snapshot.movements,
            notifications       = snapshot.feed,
            unreadNotifications = snapshot.feed.count { !it.isRead },
            currentMonthExpense = snapshot.expense,
            lastMonthExpense    = snapshot.lastExpense,
            monthlyExpenses     = previous.monthlyExpenses +
                listOfNotNull(snapshot.expense, snapshot.lastExpense).associateBy { it.month },
            weeklyRevenuePoints = weekly.first,
            weeklyRevenueRaw    = weekly.second,
            weeklyRevenueLabel  = weekly.third,
            mrr                 = sales.filter { it.createdAt?.startsWith(currentMonth) == true }
                .sumOf { it.totalSelling },
        ).deriveStockFromMovements()
        markSaleHistoryLoaded(saleStart)
    }

    /**
     * Full reload used after writes and destructive operations. It reuses the
     * sale window already loaded instead of fetching the entire table again.
     */
    fun loadData() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        if (!awaitAuthentication()) {
            _state.value = _state.value.copy(loading = false, error = "Not authenticated")
            return@launch
        }
        val currentMonth = currentYearMonth()
        val previousMonth = previousYearMonth()
        val saleStart = loadedSaleStart.ifBlank { monthStartKey(1) }
        runCatching {
            coroutineScope {
                val csatDeferred      = async { repo.getCSAT() }
                val stocks           = async { repo.getProductCategories() }
                val orders           = async { repo.getAllOrders() }
                val customers        = async { repo.getAllCustomers() }
                val employees        = async { repo.getEmployees() }
                val shopStocks       = async { repo.getShopStocks() }
                val movements        = async { repo.getRecentMovements() }
                val saleEntries      = async { repo.getSaleEntriesSince(saleStart) }
                val expense          = async { repo.getMonthlyExpense(currentMonth) }
                val lastExpense      = async { repo.getMonthlyExpense(previousMonth) }
                val shopNames        = async { repo.getShopNames() }
                val baseline         = async { repo.getEmptyCansBaseline() }
                val tasks            = async { repo.getDeliveryTasks() }
                val feed             = async { notifications.getFeed() }
                val productCategories = stocks.await()
                val sales            = saleEntries.await()
                val staff            = employees.await()
                val weekly           = weeklyRevenue(sales)
                val monthExpense     = expense.await()
                val previousExpense  = lastExpense.await()
                val names            = shopNames.await()
                val notificationFeed = feed.await()
                AdminState(
                    loading              = false,
                    mrr                  = sales.filter { it.createdAt?.startsWith(currentMonth) == true }
                        .sumOf { it.totalSelling },
                    csat                 = csatDeferred.await(),
                    fleetActiveCount     = staff.count { it.status == "active" },
                    stockItems           = repo.stockSummaryList(productCategories),
                    orders               = orders.await(),
                    customers            = customers.await(),
                    employees            = staff,
                    weeklyRevenuePoints  = weekly.first,
                    weeklyRevenueRaw     = weekly.second,
                    weeklyRevenueLabel   = weekly.third,
                    shopStocks           = shopStocks.await(),
                    recentMovements      = movements.await(),
                    productCategories    = productCategories,
                    saleEntries          = sales,
                    currentMonthExpense  = monthExpense,
                    lastMonthExpense     = previousExpense,
                    monthlyExpenses      = listOfNotNull(monthExpense, previousExpense)
                        .associateBy { it.month },
                    currentMonth         = currentMonth,
                    previousMonth        = previousMonth,
                    shopNames            = names,
                    shopName             = names.firstOrNull().orEmpty(),
                    emptyCansBaseline    = baseline.await().let { if (it >= 0) it else 0 },
                    notifications        = notificationFeed,
                    unreadNotifications  = notificationFeed.count { !it.isRead },
                    tasks                = tasks.await(),
                )
            }
        }.onSuccess { newState ->
            val previous = _state.value
            _state.value = newState.copy(
                payments = previous.payments,
                ledger = previous.ledger,
                ledgerCustomerId = previous.ledgerCustomerId,
                ledgerLoading = previous.ledgerLoading,
                paymentSaving = previous.paymentSaving,
                paymentMessage = previous.paymentMessage,
            ).deriveStockFromMovements()
            markSaleHistoryLoaded(saleStart)
            initialLoaded = true
            inventoryLoaded = true
            settingsLoaded = true
        }.onFailure {
            _state.value = _state.value.copy(
                loading = false,
                error   = it.message ?: "Failed to load admin telemetry"
            )
        }
    }

    private fun markSaleHistoryLoaded(start: String) {
        if (start.isBlank()) return
        loadedSaleStart = if (loadedSaleStart.isBlank() || start < loadedSaleStart) start else loadedSaleStart
    }

    private fun mergeSaleEntries(
        fresh: List<SaleEntry>,
        existing: List<SaleEntry>,
        start: String,
    ): List<SaleEntry> = (fresh + existing.filter { it.date.take(10) < start })
        .distinctBy { entry ->
            entry.id?.takeIf { it.isNotBlank() }
                ?: entry.clientKey?.takeIf { it.isNotBlank() }
                ?: "${entry.date}|${entry.customerName}|${entry.productId}|${entry.qty}|${entry.totalSelling}"
        }
        .sortedByDescending { it.date }

    private fun weeklyRevenue(entries: List<SaleEntry>): Triple<List<Float>, List<Float>, String> {
        val byDay = entries
            .groupBy { it.date.take(10) }
            .filterKeys { it.isNotEmpty() }
            .entries
            .sortedByDescending { it.key }
            .take(7)
            .reversed()
            .map { (_, sales) -> sales.sumOf { it.totalSelling }.toFloat() }
        if (byDay.isEmpty()) {
            return Triple(emptyList(), emptyList(), "₹0")
        }
        val maxValue = byDay.max().takeIf { it > 0f } ?: 1f
        val total = byDay.sum()
        val label = if (total >= 100_000f) {
            "₹${(total / 100_000f * 100).toInt() / 100.0} L"
        } else {
            "₹${total.toInt()}"
        }
        return Triple(byDay.map { it / maxValue }, byDay, label)
    }

    private fun monthStartKey(monthsBack: Int): String {
        return try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            var year = now.year
            var month = now.monthNumber - monthsBack.coerceAtLeast(0)
            while (month <= 0) {
                month += 12
                year -= 1
            }
            "$year-${month.toString().padStart(2, '0')}-01"
        } catch (_: Exception) {
            ""
        }
    }

    // ── Empty-cases reset ─────────────────────────────────────────────────────

    /** Zeroes the live "Empty Cases" figure business-wide. Sets the persisted
     *  baseline to the current total; the reported figure becomes 0 and any
     *  new returns accumulate on top. */
    fun resetEmptyCases() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.resetEmptyCases() }
            .onSuccess { newBaseline ->
                if (newBaseline >= 0) {
                    _state.value = _state.value.copy(
                        loading           = false,
                        emptyCansBaseline = newBaseline,
                    )
                } else {
                    // The baseline could not be persisted (app_settings table not
                    // migrated on the live database / RLS blocked the write).
                    // Keep the last-known baseline so the count does not come
                    // back on the next refresh, and tell the user why.
                    _state.value = _state.value.copy(
                        loading = false,
                        error   = "Empty Cases reset could not be saved. The " +
                            "app_settings table may not be created yet — run " +
                            "supabase_schema.sql in the Supabase SQL Editor.",
                    )
                }
            }
            .onFailure {
                it.printStackTrace()
                _state.value = _state.value.copy(error = it.message, loading = false)
            }
    }

    // ── Empty-cans returns ────────────────────────────────────────────────────

    /** Records a standalone 20L empty-can return against the customer ledger. */
    fun recordEmptyCanReturn(
        customerId: String,
        qty: Int,
        referenceKey: String,
        shopName: String,
    ) = viewModelScope.launch {
        if (qty <= 0) return@launch
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            repo.recordEmptyCanReturn(
                customerId   = customerId,
                qty          = qty,
                referenceKey = referenceKey,
                shopName     = shopName.ifBlank {
                    _state.value.shopNames.firstOrNull().orEmpty().ifBlank { "Shop 1" }
                },
            )
        }.onSuccess { newBalance ->
            _state.value = _state.value.copy(
                customers = _state.value.customers.map { customer ->
                    if (customer.id == customerId) {
                        customer.copy(cansHeld = newBalance)
                    } else {
                        customer
                    }
                },
            )
            loadData()
            val customerName = _state.value.customers.find { it.id == customerId }?.name ?: "Customer"
            notifyEmployees(
                "Stock updated",
                "$customerName · returned $qty empty can(s)",
                kind = "stock",
                relatedId = customerId,
            )
        }.onFailure {
            it.printStackTrace()
            _state.value = _state.value.copy(
                loading = false,
                error   = it.message ?: "Could not record empty cans",
            )
        }
    }

    // ── Customer payments ─────────────────────────────────────────────────────

    /** Loads the payment feed shown on the receivables screen. */
    fun loadPayments() = viewModelScope.launch {
        if (!awaitAuthentication()) return@launch
        runCatching { repo.getAllPayments() }
            .onSuccess { _state.value = _state.value.copy(payments = it) }
            .onFailure {
                _state.value = _state.value.copy(
                    error = it.message ?: "Could not load payments",
                )
            }
    }

    /** Opens one customer's running money statement. */
    fun openCustomerLedger(customerId: String) = viewModelScope.launch {
        if (customerId.isBlank()) return@launch
        _state.value = _state.value.copy(
            ledgerCustomerId = customerId,
            ledgerLoading = true,
            error = null,
        )
        runCatching { repo.getCustomerLedger(customerId) }
            .onSuccess {
                _state.value = _state.value.copy(ledger = it, ledgerLoading = false)
            }
            .onFailure {
                _state.value = _state.value.copy(
                    ledgerLoading = false,
                    error = it.message ?: "Could not load the customer statement",
                )
            }
    }

    fun closeCustomerLedger() {
        _state.value = _state.value.copy(
            ledger = null,
            ledgerCustomerId = null,
            ledgerLoading = false,
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    fun recordCustomerPayment(
        customerId: String,
        amountText: String,
        mode: String,
        note: String = "",
    ) = viewModelScope.launch {
        if (_state.value.paymentSaving) return@launch
        val amount = amountText.trim().toDoubleOrNull() ?: 0.0
        if (amount <= 0.0) {
            _state.value = _state.value.copy(error = "Enter a payment amount greater than zero.")
            return@launch
        }
        _state.value = _state.value.copy(paymentSaving = true, error = null, paymentMessage = null)
        runCatching {
            repo.recordCustomerPayment(
                customerId = customerId,
                amount = amount,
                mode = mode,
                referenceKey = Uuid.random().toString(),
                note = note.trim().takeIf { it.isNotBlank() },
            )
        }.onSuccess { newBalance ->
            _state.value = _state.value.copy(
                paymentSaving = false,
                customers = _state.value.customers.map { customer ->
                    if (customer.id == customerId) customer.copy(balance = newBalance) else customer
                },
                paymentMessage = "Payment of ₹${amount.toInt()} recorded.",
            )
            loadPayments()
            if (_state.value.ledgerCustomerId == customerId) {
                openCustomerLedger(customerId)
            }
        }.onFailure {
            _state.value = _state.value.copy(
                paymentSaving = false,
                error = it.message ?: "Could not record the payment",
            )
        }
    }

    fun clearPaymentMessage() {
        _state.value = _state.value.copy(paymentMessage = null)
    }

    // ── Product categories ────────────────────────────────────────────────────
    fun addProductCategory(cat: ProductCategory, openingStock: Int = 0, shopName: String = "") = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        val resolvedShop = shopName.ifBlank { _state.value.shopNames.firstOrNull().orEmpty() }
        runCatching {
            // We store the assigned shop in supplierGroup so All Shops view can correctly
            // identify which shop this product belongs to for stock adjustments.
            val productWithShop = cat.copy(supplierGroup = resolvedShop)
            val savedId = repo.addProductCategory(productWithShop)
            if (openingStock > 0 && savedId.isNotBlank()) {
                repo.addRawStockMovement(
                    source = "Opening Stock",
                    qty = openingStock,
                    type = "inward",
                    shopName = resolvedShop,
                    productId = savedId
                )
            }
        }
            .onSuccess {
                loadData()
                val label = cat.displayName.ifBlank { cat.name }
                notifyEmployees(
                    "New product",
                    if (openingStock > 0) "$label · +$openingStock units @ $resolvedShop" else label,
                )
            }
            .onFailure {
                it.printStackTrace()
                _state.value = _state.value.copy(error = "Database Error: ${it.message}", loading = false)
            }
    }

    fun updateProductCategory(cat: ProductCategory) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        val old = _state.value.productCategories.find { it.id == cat.id }
        // Attribute the adjustment to the product's assigned shop (supplierGroup)
        // so the per-shop ledger stays consistent; fall back to the first shop
        // name when the product was never assigned a real shop. Canonicalized
        // to the existing history bucket (see canonicalShopName).
        val adjustmentShop = canonicalShopName(
            cat.supplierGroup.trim().let {
                if (it.isNotBlank() && it != "GC" && it != "MB") it
                else _state.value.shopNames.firstOrNull().orEmpty()
            },
            _state.value.recentMovements
        )
        if (old != null && old.stockAvailable != cat.stockAvailable) {
            val diff = cat.stockAvailable - old.stockAvailable
            val type = if (diff > 0) "inward" else "outward"
            runCatching {
                repo.addRawStockMovement(
                    source = "Admin Adjustment",
                    qty = kotlin.math.abs(diff),
                    type = type,
                    shopName = adjustmentShop,
                    productId = cat.id
                )
            }
        }
        runCatching { repo.updateProductCategory(cat) }
            .onSuccess {
                _state.value = _state.value.copy(
                    productCategories = _state.value.productCategories.map {
                        if (it.id == cat.id) cat else it
                    }
                )
                loadData()
                notifyEmployees(
                    "Product updated",
                    cat.displayName.ifBlank { cat.name },
                    kind = "stock",
                    relatedId = cat.id,
                )
            }
            .onFailure { _state.value = _state.value.copy(error = it.message, loading = false) }
    }

    fun deleteProductCategory(id: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        val productName = _state.value.productCategories
            .find { it.id == id }
            ?.let { it.displayName.ifBlank { it.name } }
            .orEmpty()
        runCatching { repo.deleteProductCategory(id) }
            .onSuccess {
                _state.value = _state.value.copy(
                    productCategories = _state.value.productCategories.filter { it.id != id },
                    loading = false
                )
                notifyEmployees(
                    "Product removed",
                    productName.ifBlank { "A product was removed" },
                    kind = "stock",
                    relatedId = id,
                )
            }
            .onFailure { _state.value = _state.value.copy(error = it.message, loading = false) }
    }

    // ── Sales ─────────────────────────────────────────────────────────────────

    // ── Customer-specific pricing ─────────────────────────────────────────────

    fun loadCustomerPrices(productId: String) = viewModelScope.launch {
        runCatching { repo.getCustomerProductPricesForProduct(productId) }
            .onSuccess { list ->
                _state.value = _state.value.copy(
                    customerPrices = _state.value.customerPrices + (productId to list)
                )
            }
            .onFailure { _state.value = _state.value.copy(error = pricingErrorMessage(it.message)) }
    }

    fun saveCustomerProductPrice(price: CustomerProductPrice) = viewModelScope.launch {
        runCatching { repo.upsertCustomerProductPrice(price) }
            .onSuccess { loadCustomerPrices(price.productId) }
            .onFailure { _state.value = _state.value.copy(error = pricingErrorMessage(it.message)) }
    }

    fun deleteCustomerProductPrice(customerId: String, productId: String) = viewModelScope.launch {
        runCatching { repo.deleteCustomerProductPrice(customerId, productId) }
            .onSuccess { loadCustomerPrices(productId) }
            .onFailure { _state.value = _state.value.copy(error = pricingErrorMessage(it.message)) }
    }

    /** Translates raw database errors into actionable admin-facing messages. */
    private fun pricingErrorMessage(raw: String?): String {
        val message = raw.orEmpty()
        return if (message.contains("Could not find the table", ignoreCase = true)) {
            "Customer pricing table is missing in the database. " +
                "Run the setup SQL in the Supabase dashboard, then try again."
        } else {
            message.ifBlank { "Could not save the customer price" }
        }
    }

    // ── Sales ─────────────────────────────────────────────────────────────────

    fun addSaleEntry(entry: SaleEntry) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.addSaleEntry(entry) }
            .onSuccess {
                // The sale also wrote an outward movement + stock decrement,
                // so refresh those feeds — otherwise the inventory screens
                // keep showing the pre-sale count.
                runCatching {
                    val movements = repo.getRecentMovements()
                    val products = repo.getProductCategories()
                    val sales = repo.getSaleEntries()
                    _state.value = _state.value.copy(
                        saleEntries = sales.ifEmpty { listOf(entry) + _state.value.saleEntries },
                        recentMovements = movements.ifEmpty { _state.value.recentMovements },
                        productCategories = products.ifEmpty { _state.value.productCategories },
                        loading = false
                    ).deriveStockFromMovements()
                }.onFailure {
                    _state.value = _state.value.copy(
                        saleEntries = listOf(entry) + _state.value.saleEntries,
                        loading = false
                    )
                }
                notifyEmployees(
                    "Sale recorded",
                    "${entry.customerName} · ${entry.qty} unit(s)",
                    kind = "sale",
                    relatedId = entry.id,
                )
            }
            .onFailure { _state.value = _state.value.copy(error = it.message, loading = false) }
    }

    fun updateSaleCustomer(
        sales: List<SaleEntry>,
        customer: Customer,
        onComplete: (Boolean) -> Unit = {},
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.updateSaleCustomer(sales, customer) }
            .onSuccess {
                onComplete(true)
                loadData()
            }
            .onFailure {
                _state.value = _state.value.copy(error = it.message, loading = false)
                onComplete(false)
            }
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    fun saveMonthlyExpense(expense: MonthlyExpense) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.saveMonthlyExpense(expense) }
            .onSuccess {
                val state = _state.value
                _state.value = state.copy(
                    currentMonthExpense = if (expense.month == state.currentMonth) expense else state.currentMonthExpense,
                    lastMonthExpense    = if (expense.month == state.previousMonth) expense else state.lastMonthExpense,
                    monthlyExpenses     = state.monthlyExpenses + (expense.month to expense),
                    loading             = false
                )
            }
            .onFailure { _state.value = _state.value.copy(error = it.message, loading = false) }
    }

    fun loadMonthlyExpense(month: String) = viewModelScope.launch {
        if (month.isBlank()) return@launch
        // The dashboard comparison header also needs the month *before* the one
        // being viewed (prev-month revenue/profit vs expenses), which may never
        // have been cached when the admin jumps back beyond the last two months.
        loadMonthlyExpenseIntoCache(month)
        loadMonthlyExpenseIntoCache(previousMonthKey(month))
    }

    private suspend fun loadMonthlyExpenseIntoCache(month: String) {
        if (month.isBlank() || _state.value.monthlyExpenses.containsKey(month)) return
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.getMonthlyExpense(month) }
            .onSuccess { expense ->
                if (expense != null) {
                    _state.value = _state.value.copy(
                        monthlyExpenses = _state.value.monthlyExpenses + (month to expense),
                        loading = false
                    )
                } else {
                    _state.value = _state.value.copy(loading = false)
                }
            }
            .onFailure { _state.value = _state.value.copy(error = it.message, loading = false) }
    }

    /** "YYYY-MM" of the calendar month before [month], e.g. "2026-03" -> "2026-02". */
    private fun previousMonthKey(month: String): String {
        val parts = month.split("-")
        if (parts.size != 2) return ""
        val y = parts[0].toIntOrNull() ?: return ""
        val m = parts[1].toIntOrNull() ?: return ""
        return if (m == 1) "${y - 1}-12" else "$y-${(m - 1).toString().padStart(2, '0')}"
    }

    // ── Stock movements ───────────────────────────────────────────────────────

    fun addStockMovement(
        source: String,
        qty: Int,
        type: String,
        shopName: String,
        productId: String? = null,
        createdAt: String? = null,
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        // Canonicalize to the shop's existing history bucket so per-shop tabs
        // keep seeing the row (see canonicalShopName).
        val canonicalShop = canonicalShopName(shopName, _state.value.recentMovements)
        runCatching { repo.addStockMovement(source, qty, type, canonicalShop, productId) }
            .onSuccess {
                // Refresh all data that feeds the stock dashboard so the totals
                // and per-shop breakdowns are immediately correct after saving.
                val updated       = repo.getRecentMovements()
                val stockItems    = repo.getStockSummaryList()
                val shopStocks    = repo.getShopStocks()
                val productCats   = repo.getProductCategories()
                _state.value = _state.value.copy(
                    // Never blank the feed on a transient read hiccup: if the
                    // refresh returns nothing, keep whatever we already had.
                    recentMovements   = updated.ifEmpty { _state.value.recentMovements },
                    stockItems        = stockItems,
                    shopStocks        = shopStocks,
                    productCategories = productCats,
                    loading           = false
                ).deriveStockFromMovements()
                val sign = if (type == "inward") "+" else "−"
                notifyEmployees("Stock updated", "$source · $sign$qty units @ $canonicalShop")
            }
            .onFailure { _state.value = _state.value.copy(error = it.message, loading = false) }
    }

    /**
     * Saves multiple product quantities as separate inward stock movements in
     * a single coroutine (sequential inserts), then performs one full data
     * refresh after ALL inserts have committed to the DB.
     *
     * This replaces the old forEach { vm.addStockMovement(...) } + vm.loadData()
     * pattern which had a race condition: loadData() fired before the inserts
     * were complete, so the dashboard always showed stale counts.
     */
    fun addBulkStockMovements(
        source: String,
        shopName: String,
        quantities: Map<String, Int>,   // productId -> units
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        // Insert every product line one by one. If one fails we still attempt
        // the rest so partial stock entries are written rather than silently dropped.
        var anyError: String? = null
        val canonicalShop = canonicalShopName(shopName, _state.value.recentMovements)
        quantities.filter { it.value > 0 }.forEach { (productId, qty) ->
            runCatching {
                repo.addStockMovement(
                    source    = source,
                    qty       = qty,
                    type      = "inward",
                    shopName  = canonicalShop,
                    productId = productId,
                )
            }.onFailure { anyError = it.message }
        }
        // Single full refresh after ALL inserts — guarantees the UI reads the
        // committed state for every product and both shops.
        val linesWritten = quantities.filter { it.value > 0 }.size
        runCatching {
            val movements   = repo.getRecentMovements()
            val stockItems  = repo.getStockSummaryList()
            val shopStocks  = repo.getShopStocks()
            val productCats = repo.getProductCategories()
            _state.value = _state.value.copy(
                recentMovements   = movements,
                stockItems        = stockItems,
                shopStocks        = shopStocks,
                productCategories = productCats,
                error             = anyError,
                loading           = false
            ).deriveStockFromMovements()
            if (anyError == null && linesWritten > 0) {
                notifyEmployees(
                    "Stock added",
                    "$linesWritten product(s) restocked @ $canonicalShop",
                )
            }
        }.onFailure {
            _state.value = _state.value.copy(
                error   = it.message ?: anyError,
                loading = false
            )
        }
    }

    // ── Customers ─────────────────────────────────────────────────────────────

    fun addCustomer(customer: Customer) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.addCustomer(customer) }
            .onSuccess { saved ->
                // Drop any optimistic placeholder for the same customer name
                // (drafted by the picker with id = null or "local_...") and
                // append the saved DB row so the picker ends up with a single
                // entry carrying the real UUID.
                val merged = _state.value.customers
                    .filterNot { existing ->
                        ((existing.id == null || existing.id.startsWith("local_")) &&
                         existing.name.trim().equals(saved.name.trim(), ignoreCase = true)) ||
                        existing.id == saved.id
                    } + saved
                _state.value = _state.value.copy(customers = merged, loading = false)
                    notifyEmployees("New customer", saved.name, kind = "customer", relatedId = saved.id)
            }
            .onFailure {
                it.printStackTrace()
                _state.value = _state.value.copy(error = it.message, loading = false)
            }
    }

    fun deleteCustomer(id: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        val customerName = _state.value.customers.find { it.id == id }?.name.orEmpty()
        runCatching { repo.deleteCustomer(id) }
            .onSuccess {
                val updated = repo.getAllCustomers()
                _state.value = _state.value.copy(customers = updated, loading = false)
                notifyEmployees(
                    "Customer removed",
                    customerName.ifBlank { "A customer was removed" },
                    kind = "customer",
                    relatedId = id,
                )
            }
            .onFailure {
                it.printStackTrace()
                _state.value = _state.value.copy(error = it.message, loading = false)
            }
    }

    fun updateCustomer(customer: Customer) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.updateCustomer(customer) }
            .onSuccess { saved ->
                val merged = _state.value.customers
                    .map { if (it.id == saved.id) saved else it }
                _state.value = _state.value.copy(customers = merged, loading = false)
                // A rename also rewrites denormalized copies in sale_entries and
                // stock_movements sources, so refresh so every screen (report
                // summary, stock history) shows the new name immediately.
                loadData()
                notifyEmployees("Customer updated", saved.name, kind = "customer", relatedId = saved.id)
            }
            .onFailure {
                it.printStackTrace()
                _state.value = _state.value.copy(error = it.message, loading = false)
            }
    }

    fun updateShopName(index: Int, newName: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.updateShopName(index, newName) }
            .onSuccess {
                val updated = _state.value.shopNames.toMutableList()
                if (index in updated.indices) updated[index] = newName
                _state.value = _state.value.copy(
                    shopNames = updated,
                    shopName  = if (index == 0) newName else _state.value.shopName,
                    loading   = false
                )
                // Reload everything: stock_movements.shop_name, product supplier
                // groups, employee assignments and sale entries were migrated to
                // the new name in the DB, so the whole UI must reflect it now
                // instead of waiting for the next periodic refresh.
                loadData()
            }
            .onFailure {
                _state.value = _state.value.copy(error = it.message, loading = false)
            }
    }

    fun updateAdminConsoleName(newName: String) = viewModelScope.launch {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) {
            _state.value = _state.value.copy(error = "Admin console name cannot be blank.")
            return@launch
        }
        runCatching { repo.updateAdminConsoleName(cleanName) }
            .onSuccess {
                _state.value = _state.value.copy(adminConsoleName = cleanName)
            }
            .onFailure {
                _state.value = _state.value.copy(error = it.message ?: "Could not update admin console name.")
            }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    // ── In-app notifications ────────────────────────────────────────────────

    /**
     * Broadcasts a stock event to every employee of the tenant. Fire-and-
     * forget: a notification failure must never fail the stock write itself.
     * Refreshes the admin's own feed copy so the bell clears correctly.
     */
    fun notifyEmployees(
        title: String,
        body: String,
        kind: String = "stock",
        relatedId: String? = null,
    ) {
        viewModelScope.launch {
            runCatching { notifications.broadcastToEmployees(title, body, kind, relatedId) }
            refreshNotifications()
        }
    }

    private fun notifyEmployee(
        employeeId: String,
        title: String,
        body: String,
        kind: String,
        relatedId: String? = null,
    ) {
        if (employeeId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                notifications.sendToEmployee(employeeId, title, body, kind, relatedId)
            }
            refreshNotifications()
        }
    }

    fun refreshNotifications() = viewModelScope.launch {
        runCatching {
            val feed = notifications.getFeed()
            _state.value = _state.value.copy(
                notifications = feed,
                unreadNotifications = feed.count { !it.isRead },
            )
        }
    }

    fun markNotificationRead(id: String) = viewModelScope.launch {
        runCatching { notifications.markRead(id) }
        _state.value = _state.value.let { s ->
            s.copy(
                notifications = s.notifications.map { if (it.id == id) it.copy(isRead = true) else it },
                unreadNotifications = s.notifications.count { it.id != id && !it.isRead },
            )
        }
    }

    fun markAllNotificationsRead() = viewModelScope.launch {
        val ids = _state.value.notifications.filter { !it.isRead }.map { it.id }
        runCatching { notifications.markAllRead(ids) }
        _state.value = _state.value.let { s ->
            s.copy(
                notifications = s.notifications.map { it.copy(isRead = true) },
                unreadNotifications = 0,
            )
        }
    }

    // ── Employees ─────────────────────────────────────────────────────────────

    fun addEmployee(
        name: String,
        phone: String,
        role: String,
        shop: String,
        salary: Double,
        email: String,
        password: String
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(
            employeeCreation = EmployeeCreationState.Loading,
            loading = true,
            error = null
        )
        runCatching { repo.addEmployee(name, phone, role, shop, salary, email, password) }
            .onSuccess { newEmp ->
                _state.value = _state.value.copy(
                    employees        = _state.value.employees + newEmp,
                    employeeCreation = EmployeeCreationState.Success(
                        userId   = newEmp.id,
                        name     = newEmp.name,
                        email    = email,
                        password = password,
                        role     = newEmp.role,
                        shop     = newEmp.shopName
                    ),
                    loading          = false
                )
            }
            .onFailure { err ->
                _state.value = _state.value.copy(
                    employeeCreation = EmployeeCreationState.Error(
                        sanitizeError(err.message)
                    ),
                    loading          = false
                )
            }
    }

    fun clearEmployeeCreation() {
        _state.value = _state.value.copy(employeeCreation = EmployeeCreationState.Idle)
    }

    fun updateEmployee(employee: EmployeeInfo) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.updateEmployee(employee) }
            .onSuccess { saved ->
                val merged = _state.value.employees.map { if (it.id == saved.id) saved else it }
                _state.value = _state.value.copy(employees = merged, loading = false)
            }
            .onFailure {
                it.printStackTrace()
                _state.value = _state.value.copy(error = it.message, loading = false)
            }
    }

    fun deleteEmployee(id: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.deleteEmployee(id) }
            .onSuccess {
                // Soft-delete: keep the row (as inactive) so historical reports
                // still resolve the employee's name.
                val updated = _state.value.employees.map {
                    if (it.id == id) it.copy(status = "inactive") else it
                }
                _state.value = _state.value.copy(employees = updated, loading = false)
            }
            .onFailure {
                it.printStackTrace()
                _state.value = _state.value.copy(error = it.message, loading = false)
            }
    }

    fun addStockForProduct(
        productId: String,
        purchasePrice: Double,
        sellingPrice: Double,
        qty: Int,
        shopName: String,
        createdAt: String,
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            // 1. Add stock movement
            repo.addStockMovement(
                source = "Restock",
                qty = qty,
                type = "inward",
                shopName = shopName,
                productId = productId
            )

            // 2. Check if prices need to be updated
            val product = _state.value.productCategories.find { it.id == productId }
            if (product != null && (product.purchasePrice != purchasePrice || product.defaultSellPrice != sellingPrice)) {
                val updatedProduct = product.copy(
                    purchasePrice = purchasePrice,
                    defaultSellPrice = sellingPrice
                )
                repo.updateProductCategory(updatedProduct)
            }
        }.onSuccess {
            loadData() // Refresh all data
            val productName = _state.value.productCategories
                .find { it.id == productId }
                ?.let { it.displayName.ifBlank { it.name } }
                ?: productId
            notifyEmployees(
                "Stock restocked",
                "$productName · +$qty units @ $shopName",
                kind = "stock",
                relatedId = productId,
            )
        }.onFailure {
            _state.value = _state.value.copy(error = it.message, loading = false)
        }
    }

    fun assignOrderToEmployee(orderId: String, employeeId: String) = viewModelScope.launch {
        repo.assignEmployee(orderId, employeeId)
        loadData()
    }

    // ── Inward Stock Entry ────────────────────────────────────────────────────

    fun addInwardStockEntry(
        sku: String,
        brandName: String,
        purchasePrice: Double,
        sellingPrice: Double,
        qty: Int,
        shopName: String,
        createdAt: String,
        emptyCans: Int = 0,
        lowStockAlert: Int = 5,
        notes: String = "",
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        val skuClean = sku.trim()
        val brandClean = brandName.trim()
        // Hoisted: also feeds the employee broadcast on success.
        val displayName = if (brandClean.isNotBlank()) "$brandClean - $skuClean" else skuClean
        // Canonical bucket label so later adjustments land in this shop's
        // history (see canonicalShopName).
        val canonicalShop = canonicalShopName(shopName.trim(), _state.value.recentMovements)
        runCatching {
            var product = _state.value.productCategories.find {
                it.name.equals(skuClean, ignoreCase = true) &&
                it.brandName.equals(brandClean, ignoreCase = true)
            }

            if (product != null) {
                throw IllegalStateException(
                    "Product with size '$skuClean' already exists under brand '$brandClean'. Cannot add duplicate product."
                )
            }

            // Product does not exist, create it
            val newProduct = ProductCategory(
                id = "",
                name = skuClean,
                displayName = displayName,
                brandName = brandClean,
                supplierGroup = canonicalShop, // Store assigned shop name
                purchasePrice = purchasePrice,
                defaultSellPrice = sellingPrice,
                stockAvailable = 0, // will be updated by the movement
                lowStockAlert = lowStockAlert.coerceIn(0, 999),
                isActive = true
            )
            val productId = repo.addProductCategory(newProduct)

            // Add stock movement (optional note appended so it surfaces in history).
            val noteSuffix = notes.trim().take(120).takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
            val totalUnits = qty
            repo.addStockMovement(
                source = "Inward Purchase$noteSuffix",
                qty = totalUnits,
                type = "inward",
                shopName = canonicalShop,
                productId = productId,
                createdAt = createdAt,
                productName = displayName
            )

            // Returned empties come INTO the shop as an extra inward movement.
            if (emptyCans > 0) {
                repo.addStockMovement(
                    source = "Empty cans · Inward Purchase",
                    qty = emptyCans,
                    type = "inward",
                    shopName = canonicalShop,
                    productId = null,
                    createdAt = createdAt
                )
            }
        }.onSuccess {
            loadData()
            notifyEmployees("New stock added", "$displayName · +$qty units @ $canonicalShop")
        }.onFailure {
            _state.value = _state.value.copy(error = it.message ?: "Failed to save stock purchase", loading = false)
        }
    }

    /** Edit mode: update product details and only adjust stock if qty changed. */
    fun updateProductWithStockDelta(
        productId: String,
        brandName: String,
        purchasePrice: Double,
        sellingPrice: Double,
        newStock: Int,
        previousStock: Int,
        shopName: String,
        lowStockAlert: Int,
        createdAt: String? = null,
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            val product = _state.value.productCategories.find { it.id == productId }
                ?: throw IllegalStateException("Product not found")

            // Update product details (brand, prices, low-stock threshold)
            val displayName = if (brandName.isNotBlank()) "$brandName - ${product.name}" else product.name
            val updatedProduct = product.copy(
                brandName = brandName.trim(),
                displayName = displayName,
                supplierGroup = shopName.trim(),
                purchasePrice = purchasePrice,
                defaultSellPrice = sellingPrice,
                lowStockAlert = lowStockAlert.coerceIn(0, 999)
            )
            repo.updateProductCategory(updatedProduct)

            // Only adjust stock if quantity changed
            val delta = newStock - previousStock
            if (delta != 0) {
                val type = if (delta > 0) "inward" else "outward"
                val absQty = kotlin.math.abs(delta)
                repo.addStockMovement(
                    source = "Stock Adjustment (Edit)",
                    qty = absQty,
                    type = type,
                    shopName = canonicalShopName(shopName, _state.value.recentMovements),
                    productId = productId,
                    createdAt = createdAt,
                    productName = displayName
                )
            }
        }.onSuccess {
            loadData()
            if (newStock != previousStock) {
                val productName = _state.value.productCategories
                    .find { it.id == productId }
                    ?.let { it.displayName.ifBlank { it.name } }
                    ?: productId
                val sign = if (newStock > previousStock) "+" else "−"
                notifyEmployees(
                    "Stock adjusted",
                    "$productName · $sign${kotlin.math.abs(newStock - previousStock)} units @ $shopName",
                    kind = "stock",
                    relatedId = productId,
                )
            }
        }.onFailure {
            _state.value = _state.value.copy(error = it.message ?: "Failed to update product", loading = false)
        }
    }

    fun clearStockAndRevenue() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.clearStockAndRevenue() }
            .onSuccess {
                loadData()
            }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
    }

    fun deleteAllData() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.deleteAllData() }
            .onSuccess {
                loadData()
            }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
    }

    /** Progress of the 2-year test-data seeding run (null = idle). */
    private val _seedProgress = MutableStateFlow<SeedProgress?>(null)
    val seedProgress: StateFlow<SeedProgress?> = _seedProgress.asStateFlow()

    fun clearSeedProgress() {
        _seedProgress.value = null
    }

    /**
     * Seeds ~2 years of realistic test data (products, customers, employees,
     * sales, movements, expenses) so charts, history and reports can be
     * exercised. Refuses to run on top of existing sales/movements — wipe
     * first from Settings → Danger Zone. Employee auth creation is
     * best-effort: failures are reported but never abort the run.
     */
    fun seedTwoYearTestData() = viewModelScope.launch {
        if (_seedProgress.value?.finished == false) return@launch
        try {
            if (_state.value.saleEntries.isNotEmpty() || _state.value.recentMovements.isNotEmpty()) {
                throw IllegalStateException(
                    "Existing sales/movements found. Delete All Data first (Settings → Danger Zone), then seed."
                )
            }
            val adminId = repo.getTenantAdminId().takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Not logged in.")
            val configured = _state.value.shopNames.take(2).map { it.trim() }.filter { it.isNotBlank() }
            val shopA = configured.getOrNull(0) ?: "Shop 1"
            val shopB = configured.getOrNull(1) ?: "Shop 2"

            _seedProgress.value = SeedProgress("Generating 2-year plan…", 0, 100)
            val plan = TestDataSeeder.buildPlan(shopA, shopB)

            val saleChunks = plan.sales.chunked(500)
            val movementChunks = plan.movements.chunked(500)
            val customerChunks = plan.customers.chunked(500)
            val totalUnits = plan.products.size + plan.employees.size +
                customerChunks.size + saleChunks.size + movementChunks.size +
                1 + plan.expectedStock.size + 1
            var done = 0
            fun progress(stage: String) {
                done++
                _seedProgress.value = SeedProgress(stage, done, totalUnits)
            }

            // 1. Products first (need real ids for every other row).
            val idMap = mutableMapOf<String, String>()
            for (p in plan.products) {
                val realId = repo.addProductCategory(p)
                idMap[p.id] = realId
                progress("Products ${idMap.size}/${plan.products.size}")
            }

            // 2. Employees, best-effort (auth creation may fail; never aborts).
            val employeeIds = mutableListOf<String>()
            val skippedEmployees = mutableListOf<String>()
            for (e in plan.employees) {
                try {
                    val created = repo.addEmployee(
                        name = e.name,
                        phone = e.phone,
                        role = e.role,
                        shopName = e.shopName,
                        salary = e.salary,
                        email = e.email,
                        password = e.password,
                    )
                    employeeIds += created.id
                } catch (ex: Exception) {
                    skippedEmployees += "${e.name} (${ex.message})"
                }
                progress("Employees ${employeeIds.size + skippedEmployees.size}/${plan.employees.size}")
            }

            // Deterministic employee attribution: sale i -> employee i % n.
            val saleEmployeeByKey = mutableMapOf<String, String?>()
            plan.sales.forEachIndexed { index, sale ->
                val emp = employeeIds.getOrNull(if (employeeIds.isEmpty()) -1 else index % employeeIds.size)
                sale.clientKey?.let { saleEmployeeByKey[it] = emp }
            }

            // 3. Customers.
            for ((i, chunk) in customerChunks.withIndex()) {
                repo.seedInsertRows("customers", chunk.map { c ->
                    buildJsonObject {
                        put("name", c.name)
                        if (c.phone != null) put("phone", c.phone)
                        if (c.address != null) put("address", c.address)
                        put("admin_id", adminId)
                    }
                })
                progress("Customers chunk ${i + 1}/${customerChunks.size}")
            }

            // 4. Sales.
            for ((i, chunk) in saleChunks.withIndex()) {
                repo.seedInsertRows("sale_entries", chunk.mapIndexed { j, s ->
                    val globalIdx = i * 500 + j
                    buildJsonObject {
                        put("date", s.date)
                        put("customer_name", s.customerName)
                        put("product_id", idMap[s.productId] ?: s.productId)
                        put("product_name", s.productName)
                        put("qty", s.qty)
                        put("purchase_price_per_unit", s.purchasePricePerUnit)
                        put("selling_price_per_unit", s.sellingPricePerUnit)
                        put("sales_margin_per_unit", s.salesMarginPerUnit)
                        put("total_selling", s.totalSelling)
                        put("total_margin", s.totalMargin)
                        put("shop_id", s.shopId)
                        val emp = employeeIds.getOrNull(if (employeeIds.isEmpty()) -1 else globalIdx % employeeIds.size)
                        if (emp != null) put("employee_id", emp)
                        put("admin_id", adminId)
                        if (s.clientKey != null) put("client_key", s.clientKey)
                        if (s.createdAt != null) put("created_at", s.createdAt)
                    }
                })
                progress("Sales ${i + 1}/${saleChunks.size}")
            }

            // 5. Movements.
            for ((i, chunk) in movementChunks.withIndex()) {
                repo.seedInsertRows("stock_movements", chunk.map { m ->
                    buildJsonObject {
                        put("source", m.source)
                        put("qty", m.qty)
                        put("type", m.type)
                        put("shop_name", m.shopName)
                        if (m.productId != null) put("product_id", idMap[m.productId] ?: m.productId)
                        if (m.productName != null) put("product_name", m.productName)
                        val emp = m.clientKey?.let { saleEmployeeByKey[it] }
                        if (emp != null) put("employee_id", emp)
                        put("admin_id", adminId)
                        if (m.clientKey != null) put("client_key", m.clientKey)
                        if (m.createdAt != null) put("created_at", m.createdAt)
                    }
                })
                progress("Stock movements ${i + 1}/${movementChunks.size}")
            }

            // 6. Expenses.
            repo.seedInsertRows("monthly_expenses", plan.expenses.map { e ->
                buildJsonObject {
                    put("month", e.month)
                    put("shop_id", e.shopId)
                    put("shop_rent", e.shopRent)
                    put("admin_salary", e.adminSalary)
                    put("delivery_staff", e.deliveryStaff)
                    put("miscellaneous", e.miscellaneous)
                    put("bike_expense", e.bikeExpense)
                    put("admin_id", adminId)
                }
            })
            progress("Monthly expenses")

            // 7. Closing stock = production derivation over the seeded log.
            for ((tempId, stock) in plan.expectedStock) {
                val realId = idMap[tempId] ?: continue
                repo.seedUpdateProductStock(realId, stock)
            }
            progress("Syncing stock levels")

            loadData()
            val empNote = if (skippedEmployees.isEmpty()) "" else " Skipped employees: ${skippedEmployees.joinToString("; ")}"
            _seedProgress.value = SeedProgress(
                "Seeded ${plan.sales.size} sales, ${plan.movements.size} movements, " +
                    "${plan.customers.size} customers, ${plan.products.size} products.$empNote",
                totalUnits, totalUnits, finished = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _seedProgress.value = SeedProgress("Seeding failed", 0, 1, finished = true, error = e.message)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun AdminState.deriveStockFromMovements(): AdminState {
        if (recentMovements.isEmpty()) return this

        // Single source of truth: movement log wins whenever a product has
        // rows; DB column is only the fallback for products with no history
        // yet. This keeps every stockAvailable-based display (home KPI,
        // dashboard, sale validation) identical to the movement-derived
        // inventory screens. See effectiveStockMap.
        val effective = effectiveStockMap(productCategories, recentMovements)
        val updatedProducts = productCategories.map { p ->
            p.copy(stockAvailable = effective[p.id] ?: p.stockAvailable)
        }

        return this.copy(productCategories = updatedProducts)
    }

    private fun currentYearMonth(): String {
        return try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
        } catch (_: Exception) { "" }
    }

    /** Returns the calendar month immediately before the current one, e.g. "2026-05". */
    private fun previousYearMonth(): String {
        return try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val (py, pm) = if (now.monthNumber == 1) {
                (now.year - 1) to 12
            } else {
                now.year to (now.monthNumber - 1)
            }
            "$py-${pm.toString().padStart(2, '0')}"
        } catch (_: Exception) { "" }
    }

    fun loadTasks() = viewModelScope.launch {
        if (!awaitAuthentication()) return@launch
        coroutineScope {
            val tasks     = async { repo.getDeliveryTasks() }
            val employees = async { repo.getEmployees() }
            val customers = async { repo.getAllCustomers() }
            val products  = async { repo.getProductCategories() }
            val shops     = async { repo.getShopStocks() }
            val productList = products.await()
            _state.value = _state.value.copy(
                tasks             = tasks.await(),
                employees         = employees.await(),
                customers         = customers.await(),
                productCategories = productList,
                stockItems        = repo.stockSummaryList(productList),
                shopStocks        = shops.await(),
            )
        }
    }

    fun createDeliveryTask(
        customerId: String,
        customerName: String,
        customerAddress: String,
        customerPhone: String,
        employeeId: String,
        productId: String,
        productName: String,
        quantity: Int,
        deliveryDate: String,
        deliveryTime: String,
        employeeName: String = "",
        destLatitude: Double? = null,
        destLongitude: Double? = null,
        destName: String? = null,
        mapsLink: String? = null,
        onFinished: (created: Boolean) -> Unit = {},
    ) = viewModelScope.launch {
        if (
            customerId.isBlank() || employeeId.isBlank() || productId.isBlank() ||
            quantity <= 0 || deliveryDate.isBlank() || deliveryTime.isBlank()
        ) {
            _state.value = _state.value.copy(error = "Complete all task details before saving.")
            onFinished(false)
            return@launch
        }
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            repo.createDeliveryTask(
                customerId = customerId,
                customerName = customerName,
                customerAddress = customerAddress,
                customerPhone = customerPhone,
                employeeId = employeeId,
                employeeName = employeeName,
                productId = productId,
                productName = productName,
                quantity = quantity,
                deliveryDate = deliveryDate,
                deliveryTime = deliveryTime,
                destLatitude = destLatitude,
                destLongitude = destLongitude,
                destName = destName,
                mapsLink = mapsLink,
            )
        }.onSuccess { task ->
            _state.value = _state.value.copy(
                tasks = listOf(task) + _state.value.tasks,
                loading = false,
            )
            notifyEmployee(
                employeeId = employeeId,
                title = "New delivery assigned",
                body = "$customerName · $productName × $quantity",
                kind = "delivery",
                relatedId = task.id,
            )
            onFinished(true)
        }.onFailure {
            _state.value = _state.value.copy(
                error = deliveryTaskErrorMessage(it, "Could not create delivery task"),
                loading = false,
            )
            onFinished(false)
        }
    }

    fun updateDeliveryTaskStatus(taskId: String, status: String) = viewModelScope.launch {
        if (taskId.isBlank()) return@launch
        _state.value = _state.value.copy(error = null)
        runCatching { repo.updateDeliveryTaskStatus(taskId, status) }
            .onSuccess { saved ->
                if (saved != null) {
                    _state.value = _state.value.copy(
                        tasks = _state.value.tasks.map { if (it.id == taskId) saved else it }
                    )
                    notifyEmployee(
                        employeeId = saved.employeeId.orEmpty(),
                        title = "Delivery updated",
                        body = "${saved.customerName} · ${saved.status.replaceFirstChar { it.uppercase() }}",
                        kind = "delivery",
                        relatedId = saved.id,
                    )
                } else {
                    _state.value = _state.value.copy(
                        error = "This task could not be updated. Refresh the board and try again."
                    )
                }
            }
            .onFailure {
                _state.value = _state.value.copy(
                    error = deliveryTaskErrorMessage(it, "Unable to update this delivery task.")
                )
            }
    }
}

/**
 * Supabase reports a missing route_tasks column as
 * "Could not find the 'x' column ... in the schema cache". Point the admin at
 * the migration instead of surfacing that raw PostgREST message.
 */
private fun deliveryTaskErrorMessage(error: Throwable, fallback: String): String {
    val raw = error.message.orEmpty()
    return if (raw.contains("route_tasks", ignoreCase = true) &&
        raw.contains("schema cache", ignoreCase = true)
    ) {
        "The delivery tasks table is missing required columns. Run " +
            "supabase_migration_delivery_tasks.sql in the Supabase SQL Editor, then try again."
    } else {
        raw.ifBlank { fallback }
    }
}
