package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.EmployeeRepository
import com.example.ruwia.data.awaitAuthentication
import kotlinx.coroutines.Job
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.netStockPerProduct
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class EmployeeState(
    val loading: Boolean = false,
    val error: String? = null,
    val tasks: List<DeliveryTask> = emptyList(),
    val dailyEarnings: Double = 0.0,
    /** Total inward cans recorded by this employee today (stock_movements). */
    val todayInward: Int = 0,
    /** Total outward cans (stock movements + delivery completions) today. */
    val todayOutward: Int = 0,
    /** Empty cans returned by customers and recorded by this employee today. */
    val todayEmptyCans: Int = 0,
    val selectedTask: DeliveryTask? = null,
    val recentEntries: List<StockMovement> = emptyList(),
    /** All movements (any employee) at the current employee's shop — used by
     *  the Stock tab to compute live can balances. */
    val shopMovements: List<StockMovement> = emptyList(),
    val productCategories: List<ProductCategory> = emptyList(),
    val customers: List<Customer> = emptyList(),

    /** Shop-level can balances mirrored from shop_stocks. */
    val shopStocks: List<ShopStockInfo> = emptyList(),
    val currentDate: String = "",
    /** Running total of empty cans this employee has collected (all-time). */
    val emptyCansTotal: Int = 0,
    /** Custom shop name from AdminSettings, used as fallback when employee's
     *  database shop name is blank. */
    val assignedShop: String = "",
    /** Global baseline subtracted from the live "Empty Cases" figure. */
    val emptyCansBaseline: Int = 0,
)

class EmployeeViewModel(private val repo: EmployeeRepository) : ViewModel() {

    private val _state = MutableStateFlow(EmployeeState())
    val state: StateFlow<EmployeeState> = _state.asStateFlow()

    private var currentEmployeeId: String = ""
    private var currentShopName: String = ""

    private var refreshJob: Job? = null

    init {
        startPeriodicRefresh()
    }

    /**
     * Background poll that refreshes the high-volatility slices of the
     * employee dashboard every 15 s — recent entries, today's totals, shop
     * stocks, customer list and product catalogue. Mirrors
     * [com.example.ruwia.presentation.AdminViewModel]'s approach so the
     * employee's UI updates in near-real time without forcing them to
     * pull-to-refresh, and so a customer added by the admin appears in the
     * employee's picker within a few seconds.
     */
    fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(15_000L)
                if (!awaitAuthentication()) continue
                runCatching {
                    val products   = repo.getProductCategories()
                    val shopStocks = repo.getShopStocks()
                    val customers  = repo.getCustomers()
                    
                    val shopMovements = repo.getShopMovements("All Shops")
                    
                    var recentEntries = _state.value.recentEntries
                    var todayInward = _state.value.todayInward
                    var todayOutward = _state.value.todayOutward
                    var todayEmptyCans = _state.value.todayEmptyCans
                    var dailyEarnings = _state.value.dailyEarnings
                    var emptyCansTotal = _state.value.emptyCansTotal
                    val emptyCansBaseline = repo.getEmptyCansBaseline()
                    
                    if (currentEmployeeId.isNotBlank()) {
                        recentEntries = repo.getRecentEntries(currentEmployeeId)
                        val cans = repo.getDailyCansSummary(currentEmployeeId)
                        todayInward = cans.inward
                        todayOutward = cans.outward
                        todayEmptyCans = cans.emptyReturned
                        dailyEarnings = repo.getDailyEarningsSummary(currentEmployeeId)
                        emptyCansTotal = repo.getEmployeeEmptyCansTotal(currentEmployeeId)
                    }
                    
                    // Prevent replacing valid cached data with empty lists if RLS returned empty lists due to a race
                    if (products.isNotEmpty() || shopMovements.isNotEmpty() || _state.value.productCategories.isEmpty()) {
                        _state.value = _state.value.copy(
                            productCategories = products,
                            shopStocks        = shopStocks,
                            customers         = customers,
                            shopMovements     = if (shopMovements.isNotEmpty() || currentEmployeeId.isBlank()) shopMovements else recentEntries,
                            recentEntries     = recentEntries,
                            todayInward       = todayInward,
                            todayOutward      = todayOutward,
                            todayEmptyCans    = todayEmptyCans,
                            dailyEarnings     = dailyEarnings,
                            emptyCansTotal    = emptyCansTotal,
                            emptyCansBaseline = if (emptyCansBaseline >= 0) emptyCansBaseline else _state.value.emptyCansBaseline,
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
        _state.value = EmployeeState()
    }

    /**
     * Set the shop the current employee is assigned to. Called by the UI
     * layer once it parses the auth-layer `shopInfo` string. We need this
     * stored on the VM so the periodic refresh can keep pulling shop-scoped
     * stock movements without re-asking the UI.
     */
    fun setAssignedShop(shopName: String) {
        val cleaned = shopName.trim()
        if (cleaned == currentShopName) return
        currentShopName = cleaned
        _state.value = _state.value.copy(assignedShop = cleaned)
        // Kick off an immediate fetch so the Stock tab populates without
        // waiting for the next 15 s tick.
        if (currentEmployeeId.isNotBlank() && cleaned.isNotBlank()) {
            viewModelScope.launch {
                runCatching { repo.getShopMovements("All Shops") }
                    .onSuccess { movs ->
                        _state.value = _state.value.copy(shopMovements = movs).deriveStockFromMovements()
                    }
            }
        }
    }

    fun loadDashboard(employeeId: String? = null) = viewModelScope.launch {
        // Use the real logged-in user's UUID. Ignore placeholder ids like "e1";
        // the employee_id columns are real UUIDs, so a fake id matches nothing.
        val empId = employeeId
            ?.takeIf { it.isNotBlank() && it != "e1" }
            ?: repo.currentUserId()
            ?: ""
        currentEmployeeId = empId
        _state.value = _state.value.copy(loading = true, error = null)
        if (!awaitAuthentication()) {
            _state.value = _state.value.copy(loading = false, error = "Not authenticated")
            return@launch
        }
        runCatching {
            val dbShop = repo.getEmployeeShopName(empId)
            val shopStocks = repo.getShopStocks()
            // Prefer the saved shop assignment on the employee record; fall back
            // to the auth-layer shop info, then to the first configured shop so
            // an unassigned employee lands on a real shop instead of a made-up
            // label.
            val assignedShopName = if (dbShop.isNotBlank()) {
                dbShop
            } else {
                currentShopName
                    .ifBlank { shopStocks.firstOrNull()?.name.orEmpty() }
                    .ifBlank { "" }
            }
            currentShopName = assignedShopName

            val tasks            = if (empId.isNotBlank()) repo.getTodayRouteTasks(empId) else emptyList()
            val earnings         = if (empId.isNotBlank()) repo.getDailyEarningsSummary(empId) else 0.0
            val cans             = if (empId.isNotBlank()) repo.getDailyCansSummary(empId) else com.example.ruwia.data.DailyCansSummary(0, 0, 0)
            val recentEntries    = if (empId.isNotBlank()) repo.getRecentEntries(empId) else emptyList()
            val emptyCansTotal   = if (empId.isNotBlank()) repo.getEmployeeEmptyCansTotal(empId) else 0
            val emptyCansBaseline = repo.getEmptyCansBaseline()
            val productCategories = repo.getProductCategories()
            val customers        = repo.getCustomers()
            val shopMovements    = repo.getShopMovements("All Shops")
            val currentDate      = formattedToday()
            EmployeeState(
                loading           = false,
                tasks             = tasks,
                dailyEarnings     = earnings,
                todayInward       = cans.inward,
                todayOutward      = cans.outward,
                todayEmptyCans    = cans.emptyReturned,
                recentEntries     = recentEntries,
                emptyCansTotal    = emptyCansTotal,
                shopMovements     = shopMovements,
                productCategories = productCategories,
                customers         = customers,
                shopStocks        = shopStocks,
                currentDate       = currentDate,
                assignedShop      = assignedShopName,
                emptyCansBaseline = if (emptyCansBaseline >= 0) emptyCansBaseline else 0,
            )
        }.onSuccess { newState ->
            _state.value = newState.deriveStockFromMovements()
        }.onFailure {
            _state.value = _state.value.copy(
                loading = false,
                error   = it.message ?: "Failed to load shift routes"
            )
        }
    }

    fun selectTask(task: DeliveryTask?) {
        _state.value = _state.value.copy(selectedTask = task)
    }

    fun completeDelivery(
        taskId: String,
        customerId: String,
        employeeId: String,
        delivered: Int,
        returned: Int,
        amount: Double,
        mode: String
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)
        runCatching {
            repo.recordDeliveryCompletion(
                taskId           = taskId,
                customerId       = customerId,
                deliveredQty     = delivered,
                returnedEmptyQty = returned,
                collectedAmount  = amount,
                paymentMode      = mode,
                employeeId       = employeeId,
                shopName         = currentShopName,
            )
        }.onSuccess {
            loadDashboard(employeeId)
            _state.value = _state.value.copy(selectedTask = null)
        }.onFailure {
            _state.value = _state.value.copy(loading = false, error = it.message ?: "Submission failed")
        }
    }

    fun addCustomer(customer: Customer) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.addCustomer(customer) }
            .onSuccess { saved ->
                // Replace any optimistic placeholder of this customer (id = null
                // or "local_...") created by [CustomerPickerOverlay] with the
                // real DB row so the picker can later resolve a stable id.
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

    fun clearError() {
        _state.value = _state.value.copy(error = null)
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
            val productId = if (product != null) {
                if (product.purchasePrice != purchasePrice || product.defaultSellPrice != sellingPrice) {
                    val updatedProduct = product.copy(
                        purchasePrice = purchasePrice,
                        defaultSellPrice = sellingPrice
                    )
                    repo.updateProductCategory(updatedProduct)
                }
                product.id
            } else {
                val displayName = if (brandClean.isNotBlank()) "$brandClean - $skuClean" else skuClean
                val newProduct = ProductCategory(
                    id = "",
                    name = skuClean,
                    displayName = displayName,
                    brandName = brandClean,
                    purchasePrice = purchasePrice,
                    defaultSellPrice = sellingPrice,
                    stockAvailable = 0,
                    isActive = true
                )
                repo.addProductCategory(newProduct).id
            }

            val totalUnits = qty
            repo.addStockMovement(
                source = "Inward Purchase",
                qty = totalUnits,
                type = "inward",
                shopName = shopName,
                productId = productId,
                createdAt = createdAt
            )

            if (emptyCans > 0) {
                // Returned empties come INTO the shop, so this is an inward
                // movement — recording it as outward would subtract from the
                // live Empty Cases figure whenever an inward purchase happened.
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
            loadDashboard(currentEmployeeId)
        }.onFailure {
            _state.value = _state.value.copy(error = it.message ?: "Failed to save stock purchase", loading = false)
        }
    }

    // ── Empty-cases direct entry ──────────────────────────────────────────────

    /** Records a manual inward "Empty cans" movement so the live Empty Cases
     *  figure can be bumped straight from the home screen. */
    fun addEmptyCases(qty: Int) = viewModelScope.launch {
        if (qty <= 0) return@launch
        _state.value = _state.value.copy(loading = false, error = null)
        val shop = currentShopName.takeIf { it.isNotBlank() } ?: _state.value.assignedShop
        runCatching { repo.addEmptyCases(qty, shop.ifBlank { "Shop 1" }) }
            .onSuccess { loadDashboard(currentEmployeeId) }
            .onFailure {
                it.printStackTrace()
                _state.value = _state.value.copy(error = it.message ?: "Failed to add empty cases", loading = false)
            }
    }

    /**
     * Persist an outward sale for the current employee.
     *
     * Each line item creates one `sale_entries` row + one `outward`
     * `stock_movements` row, and any [emptyCansCollected] become a single
     * `inward` movement so admins can see returned empties on the dashboard.
     *
     * After the sale is persisted we re-load the dashboard so the today's-stats
     * tiles, recent entries and earnings reflect the new transaction
     * immediately.
     */
    fun addOutwardSale(
        customerName: String,
        shopName: String,
        lines: List<EmployeeRepository.SaleLine>,
        emptyCansCollected: Int,
        saleDate: String? = null,
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            repo.addOutwardSale(
                customerName       = customerName,
                shopName           = shopName,
                lines              = lines,
                emptyCansCollected = emptyCansCollected,
                saleDate           = saleDate,
            )
        }.onSuccess {
            // Refresh today's totals, sales and recent entries.
            loadDashboard(currentEmployeeId)
        }.onFailure {
            _state.value = _state.value.copy(loading = false, error = it.message ?: "Could not save sale")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun EmployeeState.deriveStockFromMovements(): EmployeeState {
        if (shopMovements.isEmpty()) return this

        val net = netStockPerProduct(shopMovements)
        val updatedProducts = productCategories.map { p ->
            p.copy(stockAvailable = net[p.id] ?: 0)
        }

        return this.copy(productCategories = updatedProducts)
    }

    private fun formattedToday(): String {
        return try {
            com.example.ruwia.util.toDisplayDate(Clock.System.now())
        } catch (_: Exception) { "" }
    }
}
