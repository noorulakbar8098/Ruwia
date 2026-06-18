package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.EmployeeRepository
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
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
    val selectedTask: DeliveryTask? = null,
    val recentEntries: List<StockMovement> = emptyList(),
    /** All movements (any employee) at the current employee's shop — used by
     *  the Stock tab to compute live can balances. */
    val shopMovements: List<StockMovement> = emptyList(),
    val productCategories: List<ProductCategory> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val suppliers: List<String> = emptyList(),
    /** Shop-level can balances mirrored from shop_stocks. */
    val shopStocks: List<ShopStockInfo> = emptyList(),
    val currentDate: String = "",
    /** "Shop 1" / "Shop 2" — extracted from the auth-layer shopInfo so we can
     *  filter shop-scoped data in repo calls. */
    val assignedShop: String = "",
)

class EmployeeViewModel(private val repo: EmployeeRepository) : ViewModel() {

    private val _state = MutableStateFlow(EmployeeState())
    val state: StateFlow<EmployeeState> = _state.asStateFlow()

    private var currentEmployeeId: String = ""
    private var currentShopName: String = ""

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
    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(15_000L)
                if (currentEmployeeId.isBlank()) continue
                runCatching {
                    val recentEntries = repo.getRecentEntries(currentEmployeeId)
                    val cans          = repo.getDailyCansSummary(currentEmployeeId)
                    val earnings      = repo.getDailyEarningsSummary(currentEmployeeId)
                    val shopStocks    = repo.getShopStocks()
                    val customers     = repo.getCustomers()
                    val products      = repo.getProductCategories()
                    val suppliers     = repo.getSuppliers()
                    // Shop-wide movements drive the Stock tab's live totals
                    // (not just this employee's). Falls back to recentEntries
                    // when no shop is configured yet.
                    val shopMovements = if (currentShopName.isNotBlank())
                        repo.getShopMovements(currentShopName)
                    else recentEntries
                    _state.value = _state.value.copy(
                        recentEntries     = recentEntries,
                        shopMovements     = shopMovements,
                        todayInward       = cans.first,
                        todayOutward      = cans.second,
                        dailyEarnings     = earnings,
                        shopStocks        = shopStocks,
                        customers         = customers,
                        productCategories = products,
                        suppliers         = suppliers,
                    )
                }
            }
        }
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
                runCatching { repo.getShopMovements(cleaned) }
                    .onSuccess { movs ->
                        _state.value = _state.value.copy(shopMovements = movs)
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
        runCatching {
            val tasks            = repo.getTodayRouteTasks(empId)
            val earnings         = repo.getDailyEarningsSummary(empId)
            val cans             = repo.getDailyCansSummary(empId)
            val recentEntries    = repo.getRecentEntries(empId)
            val productCategories = repo.getProductCategories()
            val customers        = repo.getCustomers()
            val suppliers        = repo.getSuppliers()
            val shopStocks       = repo.getShopStocks()
            val shopMovements    = if (currentShopName.isNotBlank())
                repo.getShopMovements(currentShopName)
            else recentEntries
            val currentDate      = formattedToday()
            EmployeeState(
                loading           = false,
                tasks             = tasks,
                dailyEarnings     = earnings,
                todayInward       = cans.first,
                todayOutward      = cans.second,
                recentEntries     = recentEntries,
                shopMovements     = shopMovements,
                productCategories = productCategories,
                customers         = customers,
                suppliers         = suppliers,
                shopStocks        = shopStocks,
                currentDate       = currentDate,
                assignedShop      = currentShopName,
            )
        }.onSuccess { newState ->
            _state.value = newState
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
            )
        }.onSuccess {
            loadDashboard(employeeId)
            _state.value = _state.value.copy(selectedTask = null)
        }.onFailure {
            _state.value = _state.value.copy(loading = false, error = it.message ?: "Submission failed")
        }
    }

    fun addCustomer(customer: Customer) = viewModelScope.launch {
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
                _state.value = _state.value.copy(customers = merged)
            }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    /**
     * Persist a new supplier from the employee app. The added row is visible
     * to the admin and every other employee on next refresh thanks to RLS.
     * After the insert succeeds, refresh the supplier picker so the new value
     * shows up immediately for the current user too.
     */
    fun addSupplier(name: String, location: String?) = viewModelScope.launch {
        runCatching { repo.addSupplier(name, location) }
            .onSuccess {
                val updated = repo.getSuppliers()
                _state.value = _state.value.copy(suppliers = updated)
            }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    fun addInwardStock(
        supplier: String,
        quantities: Map<String, Int>,
        shopName: String,
    ) = viewModelScope.launch {
        // The map's KEY is the productId — without passing it through to
        // [EmployeeRepository.addStockMovement] the repo can't bump the
        // matching `product_categories.stock_available` row, which is why the
        // admin's "STOCK ON HAND" KPI used to stay at zero even after an
        // employee logged inward stock.
        quantities.filter { it.value > 0 }.forEach { (productId, qty) ->
            runCatching {
                repo.addStockMovement(
                    source    = supplier,
                    qty       = qty,
                    type      = "inward",
                    shopName  = shopName,
                    productId = productId,
                )
            }
        }
        // Refresh the slices that change as a result of the inward batch so
        // the dashboard reflects the new totals immediately.
        runCatching {
            val updatedEntries  = repo.getRecentEntries(currentEmployeeId)
            val updatedProducts = repo.getProductCategories()
            val updatedShops    = repo.getShopStocks()
            val cans            = repo.getDailyCansSummary(currentEmployeeId)
            _state.value = _state.value.copy(
                recentEntries     = updatedEntries,
                productCategories = updatedProducts,
                shopStocks        = updatedShops,
                todayInward       = cans.first,
                todayOutward      = cans.second,
            )
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
    ) = viewModelScope.launch {
        runCatching {
            repo.addOutwardSale(
                customerName       = customerName,
                shopName           = shopName,
                lines              = lines,
                emptyCansCollected = emptyCansCollected,
            )
        }.onSuccess {
            // Refresh today's totals, sales and recent entries.
            loadDashboard(currentEmployeeId)
        }.onFailure {
            _state.value = _state.value.copy(error = it.message ?: "Could not save sale")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun formattedToday(): String {
        return try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val day = now.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            val m   = now.monthNumber
            val monthName = when (m) {
                1  -> "Jan"; 2  -> "Feb"; 3  -> "Mar"; 4  -> "Apr"
                5  -> "May"; 6  -> "Jun"; 7  -> "Jul"; 8  -> "Aug"
                9  -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dec"
            }
            "$day, ${now.dayOfMonth} $monthName ${now.year}"
        } catch (_: Exception) { "" }
    }
}
