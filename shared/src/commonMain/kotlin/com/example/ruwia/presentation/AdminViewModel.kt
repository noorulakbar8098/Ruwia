package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.AdminRepository
import com.example.ruwia.data.awaitAuthentication
import kotlinx.coroutines.Job
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.MonthlyExpense
import com.example.ruwia.domain.Order
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.SaleEntry
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockItem
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.Supplier
import com.example.ruwia.domain.netStockPerProduct
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
        // name when the product was never assigned a real shop.
        val adjustmentShop = cat.supplierGroup.trim().let {
            if (it.isNotBlank() && it != "GC" && it != "MB") it
            else _state.value.shopNames.firstOrNull().orEmpty()
        }
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

    fun addSaleEntry(entry: SaleEntry) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.addSaleEntry(entry) }
            .onSuccess {
                _state.value = _state.value.copy(
                    saleEntries = listOf(entry) + _state.value.saleEntries,
                    loading = false
                )
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
        runCatching { repo.addStockMovement(source, qty, type, shopName, productId) }
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
        quantities.filter { it.value > 0 }.forEach { (productId, qty) ->
            runCatching {
                repo.addStockMovement(
                    source    = source,
                    qty       = qty,
                    type      = "inward",
                    shopName  = shopName,
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
            val newProduct = ProductCategory(
                id = "",
                name = skuClean,
                displayName = displayName,
                brandName = brandClean,
                supplierGroup = shopName.trim(), // Store assigned shop name
                purchasePrice = purchasePrice,
                defaultSellPrice = sellingPrice,
                stockAvailable = 0, // will be updated by the movement
                isActive = true
            )
            val productId = repo.addProductCategory(newProduct)

            // Add stock movement
            val totalUnits = qty
            repo.addStockMovement(
                source = "Inward Purchase",
                qty = totalUnits,
                type = "inward",
                shopName = shopName,
                productId = productId,
                createdAt = createdAt
            )

            // Returned empties come INTO the shop as an extra inward movement.
            if (emptyCans > 0) {
                repo.addStockMovement(
                    source = "Empty cans · Inward Purchase",
                    qty = emptyCans,
                    type = "inward",
                    shopName = shopName,
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
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            val product = _state.value.productCategories.find { it.id == productId }
                ?: throw IllegalStateException("Product not found")

            // Update product details (brand, prices)
            val displayName = if (brandName.isNotBlank()) "$brandName - ${product.name}" else product.name
            val updatedProduct = product.copy(
                brandName = brandName.trim(),
                displayName = displayName,
                supplierGroup = shopName.trim(),
                purchasePrice = purchasePrice,
                defaultSellPrice = sellingPrice
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
                    shopName = shopName,
                    productId = productId
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun AdminState.deriveStockFromMovements(): AdminState {
        if (recentMovements.isEmpty()) return this

        val net = netStockPerProduct(recentMovements)
        val updatedProducts = productCategories.map { p ->
            p.copy(stockAvailable = net[p.id] ?: 0)
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