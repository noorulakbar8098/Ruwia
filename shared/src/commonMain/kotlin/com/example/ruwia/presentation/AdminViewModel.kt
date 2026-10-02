package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.AdminRepository
import com.example.ruwia.data.TestDataSeeder
import com.example.ruwia.data.awaitAuthentication
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Job
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.CustomerProductPrice
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.MonthlyExpense
import com.example.ruwia.domain.Order
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
    /** Global baseline subtracted from the live "Empty Cases" figure. */
    val emptyCansBaseline: Int = 0,
    /** Active customer-specific prices per product, keyed by product id. */
    val customerPrices: Map<String, List<CustomerProductPrice>> = emptyMap(),
)

class AdminViewModel(private val repo: AdminRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    init {
        loadData()
        startPeriodicRefresh()
    }

    fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(15_000L) // Poll every 15 seconds
                if (!awaitAuthentication()) continue
                runCatching {
                    // Cross-app freshness: customers / suppliers / products /
                    // stock numbers all change when an employee logs activity,
                    // so we re-pull everything an admin watches at a glance.
                    val shopStocks = repo.getShopStocks()
                    val movements  = repo.getRecentMovements()
                    val customers  = repo.getAllCustomers()
                    val products      = repo.getProductCategories()
                    val stockItems    = repo.getStockSummaryList()
                    val saleEntries   = repo.getSaleEntries()
                    val baseline      = repo.getEmptyCansBaseline()
                    
                    // Prevent replacing valid cached data with empty lists if RLS returned empty lists due to a race
                    if (products.isNotEmpty() || movements.isNotEmpty() || _state.value.productCategories.isEmpty()) {
                        _state.value = _state.value.copy(
                            shopStocks        = shopStocks,
                            recentMovements   = movements,
                            customers         = customers,
                            productCategories = products,
                            stockItems        = stockItems,
                            saleEntries       = saleEntries,
                            emptyCansBaseline = if (baseline >= 0) baseline else _state.value.emptyCansBaseline,
                        ).deriveStockFromMovements()
                    }
                }
            }
        }
    }

    fun stopPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun clearState() {
        stopPeriodicRefresh()
        _state.value = AdminState()
    }

    fun loadData() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        if (!awaitAuthentication()) {
            _state.value = _state.value.copy(loading = false, error = "Not authenticated")
            return@launch
        }
        runCatching {
            val mrr              = repo.getMRR()
            val csat             = repo.getCSAT()
            val fleet            = repo.getActiveFleetCount()
            val stocks           = repo.getStockSummaryList()
            val orders           = repo.getAllOrders()
            val customers        = repo.getAllCustomers()
            val employees        = repo.getEmployees()
            val (weeklyPts, weeklyRaw, weeklyLabel) = repo.getWeeklyRevenueSummary()
            val shopStocks       = repo.getShopStocks()
            val movements        = repo.getRecentMovements()
            val productCategories = repo.getProductCategories()
            val saleEntries      = repo.getSaleEntries()
            val currentMonth     = currentYearMonth()
            val previousMonth    = previousYearMonth()
            val expense          = repo.getMonthlyExpense(currentMonth)
            val lastExpense      = repo.getMonthlyExpense(previousMonth)
            val shopNames        = repo.getShopNames()
            val shopName         = shopNames.firstOrNull().orEmpty()
            val baseline         = repo.getEmptyCansBaseline()
            AdminState(
                loading              = false,
                mrr                  = mrr,
                csat                 = csat,
                fleetActiveCount     = fleet,
                stockItems           = stocks,
                orders               = orders,
                customers            = customers,
                employees            = employees,
                weeklyRevenuePoints  = weeklyPts,
                weeklyRevenueRaw     = weeklyRaw,
                weeklyRevenueLabel   = weeklyLabel,
                shopStocks           = shopStocks,
                recentMovements      = movements,
                productCategories    = productCategories,
                saleEntries          = saleEntries,
                currentMonthExpense  = expense,
                lastMonthExpense     = lastExpense,
                monthlyExpenses      = listOfNotNull(expense, lastExpense).associateBy { it.month },
                currentMonth         = currentMonth,
                previousMonth        = previousMonth,
                shopNames            = shopNames,
                shopName             = shopName,
                emptyCansBaseline    = if (baseline >= 0) baseline else 0,
            )
        }.onSuccess { newState ->
            _state.value = newState.deriveStockFromMovements()
        }.onFailure {
            _state.value = _state.value.copy(
                loading = false,
                error   = it.message ?: "Failed to load admin telemetry"
            )
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

    // ── Empty-cases direct entry ──────────────────────────────────────────────

    /** Records a manual inward "Empty cans" movement so the live Empty Cases
     *  figure can be bumped from the home screen without a full screen form. */
    fun addEmptyCases(qty: Int) = viewModelScope.launch {
        if (qty <= 0) return@launch
        _state.value = _state.value.copy(loading = false, error = null)
        val shop = _state.value.shopNames.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Shop 1"
        runCatching { repo.addEmptyCases(qty, shop) }
            .onSuccess { loadData() }
            .onFailure {
                it.printStackTrace()
                _state.value = _state.value.copy(error = it.message ?: "Failed to add empty cases", loading = false)
            }
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
            }
            .onFailure { _state.value = _state.value.copy(error = it.message, loading = false) }
    }

    fun deleteProductCategory(id: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.deleteProductCategory(id) }
            .onSuccess {
                _state.value = _state.value.copy(
                    productCategories = _state.value.productCategories.filter { it.id != id },
                    loading = false
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
            }
            .onFailure { _state.value = _state.value.copy(error = it.message, loading = false) }
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
            }
            .onFailure {
                it.printStackTrace()
                _state.value = _state.value.copy(error = it.message, loading = false)
            }
    }

    fun deleteCustomer(id: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.deleteCustomer(id) }
            .onSuccess {
                val updated = repo.getAllCustomers()
                _state.value = _state.value.copy(customers = updated, loading = false)
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

    fun clearError() {
        _state.value = _state.value.copy(error = null)
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
        runCatching {
            val skuClean = sku.trim()
            val brandClean = brandName.trim()
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
            val displayName = if (brandClean.isNotBlank()) "$brandClean - $skuClean" else skuClean
            // Canonical bucket label so later adjustments land in this shop's
            // history (see canonicalShopName).
            val canonicalShop = canonicalShopName(shopName.trim(), _state.value.recentMovements)
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
}