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
import com.example.ruwia.domain.isEmptyCansSource
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
                    
                    // Prevent replacing valid cached data with empty lists if RLS returned empty lists due to a race
                    if (products.isNotEmpty() || movements.isNotEmpty() || _state.value.productCategories.isEmpty()) {
                        _state.value = _state.value.copy(
                            shopStocks        = shopStocks,
                            recentMovements   = movements,
                            customers         = customers,
                            productCategories = products,
                            stockItems        = stockItems,
                            saleEntries       = saleEntries,
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

    // ── Product categories ────────────────────────────────────────────────────

    fun addProductCategory(cat: ProductCategory, openingStock: Int = 0, shopName: String = "Shop 1") = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { 
            // We store the assigned shop in supplierGroup so All Shops view can correctly 
            // identify which shop this product belongs to for stock adjustments.
            val productWithShop = cat.copy(supplierGroup = shopName)
            val savedId = repo.addProductCategory(productWithShop)
            if (openingStock > 0 && savedId.isNotBlank()) {
                repo.addRawStockMovement(
                    source = "Opening Stock",
                    qty = openingStock,
                    type = "inward",
                    shopName = shopName,
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
        if (old != null && old.stockAvailable != cat.stockAvailable) {
            val diff = cat.stockAvailable - old.stockAvailable
            val type = if (diff > 0) "inward" else "outward"
            runCatching {
                repo.addRawStockMovement(
                    source = "Admin Adjustment",
                    qty = kotlin.math.abs(diff),
                    type = type,
                    shopName = "Shop 1",
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
        if (month.isBlank() || _state.value.monthlyExpenses.containsKey(month)) return@launch
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
                    recentMovements   = updated,
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
        
        val updatedProducts = productCategories.map { p ->
            val rows = recentMovements.filter { it.productId == p.id }
            val inward = rows.filter { it.type == "inward" }.sumOf { it.qty }
            val outward = rows.filter { it.type == "outward" }.sumOf { it.qty }
            p.copy(stockAvailable = (inward - outward).coerceAtLeast(0))
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