package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.AdminRepository
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
    val weeklyRevenueLabel: String = "",
    val shopStocks: List<ShopStockInfo> = emptyList(),
    val recentMovements: List<StockMovement> = emptyList(),
    val productCategories: List<ProductCategory> = emptyList(),
    val saleEntries: List<SaleEntry> = emptyList(),
    val currentMonthExpense: MonthlyExpense? = null,
    val lastMonthExpense: MonthlyExpense? = null,
    val suppliers: List<String> = emptyList(),
    /** Typed supplier rows including IDs — for the management screen. */
    val suppliersFull: List<Supplier> = emptyList(),
    val employeeCreation: EmployeeCreationState = EmployeeCreationState.Idle,
    /** "YYYY-MM" — set in [AdminViewModel.loadData]. Empty until first load. */
    val currentMonth: String = "",
    /** "YYYY-MM" of the previous calendar month. */
    val previousMonth: String = "",
)

class AdminViewModel(private val repo: AdminRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state.asStateFlow()

    init {
        loadData()
        startPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(15_000L) // Poll every 15 seconds
                runCatching {
                    // Cross-app freshness: customers / suppliers / products /
                    // stock numbers all change when an employee logs activity,
                    // so we re-pull everything an admin watches at a glance.
                    val shopStocks = repo.getShopStocks()
                    val movements  = repo.getRecentMovements()
                    val customers  = repo.getAllCustomers()
                    val suppliers     = repo.getSuppliers()
                    val suppliersFull = repo.getSuppliersFull()
                    val products      = repo.getProductCategories()
                    val stockItems    = repo.getStockSummaryList()
                    _state.value = _state.value.copy(
                        shopStocks        = shopStocks,
                        recentMovements   = movements,
                        customers         = customers,
                        suppliers         = suppliers,
                        suppliersFull     = suppliersFull,
                        productCategories = products,
                        stockItems        = stockItems,
                    )
                }
            }
        }
    }

    fun loadData() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            val mrr              = repo.getMRR()
            val csat             = repo.getCSAT()
            val fleet            = repo.getActiveFleetCount()
            val stocks           = repo.getStockSummaryList()
            val orders           = repo.getAllOrders()
            val customers        = repo.getAllCustomers()
            val employees        = repo.getEmployees()
            val (weeklyPts, weeklyLabel) = repo.getWeeklyRevenueSummary()
            val shopStocks       = repo.getShopStocks()
            val movements        = repo.getRecentMovements()
            val productCategories = repo.getProductCategories()
            val saleEntries      = repo.getSaleEntries()
            val currentMonth     = currentYearMonth()
            val previousMonth    = previousYearMonth()
            val expense          = repo.getMonthlyExpense(currentMonth)
            val lastExpense      = repo.getMonthlyExpense(previousMonth)
            val suppliers        = repo.getSuppliers()
            val suppliersFull    = repo.getSuppliersFull()
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
                weeklyRevenueLabel   = weeklyLabel,
                shopStocks           = shopStocks,
                recentMovements      = movements,
                productCategories    = productCategories,
                saleEntries          = saleEntries,
                currentMonthExpense  = expense,
                lastMonthExpense     = lastExpense,
                suppliers            = suppliers,
                suppliersFull        = suppliersFull,
                currentMonth         = currentMonth,
                previousMonth        = previousMonth,
            )
        }.onSuccess { newState ->
            _state.value = newState
        }.onFailure {
            _state.value = _state.value.copy(
                loading = false,
                error   = it.message ?: "Failed to load admin telemetry"
            )
        }
    }

    // ── Product categories ────────────────────────────────────────────────────

    fun addProductCategory(cat: ProductCategory) = viewModelScope.launch {
        runCatching { repo.addProductCategory(cat) }
            .onSuccess {
                val updated = repo.getProductCategories()
                _state.value = _state.value.copy(productCategories = updated)
            }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    fun updateProductCategory(cat: ProductCategory) = viewModelScope.launch {
        runCatching { repo.updateProductCategory(cat) }
            .onSuccess {
                _state.value = _state.value.copy(
                    productCategories = _state.value.productCategories.map {
                        if (it.id == cat.id) cat else it
                    }
                )
            }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    fun deleteProductCategory(id: String) = viewModelScope.launch {
        runCatching { repo.deleteProductCategory(id) }
            .onSuccess {
                _state.value = _state.value.copy(
                    productCategories = _state.value.productCategories.filter { it.id != id }
                )
            }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    // ── Sales ─────────────────────────────────────────────────────────────────

    fun addSaleEntry(entry: SaleEntry) = viewModelScope.launch {
        runCatching { repo.addSaleEntry(entry) }
            .onSuccess {
                _state.value = _state.value.copy(
                    saleEntries = listOf(entry) + _state.value.saleEntries
                )
            }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    fun saveMonthlyExpense(expense: MonthlyExpense) = viewModelScope.launch {
        runCatching { repo.saveMonthlyExpense(expense) }
            .onSuccess { _state.value = _state.value.copy(currentMonthExpense = expense) }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    // ── Stock movements ───────────────────────────────────────────────────────

    fun addStockMovement(
        source: String,
        qty: Int,
        type: String,
        shopName: String,
        productId: String? = null,
    ) = viewModelScope.launch {
        runCatching { repo.addStockMovement(source, qty, type, shopName, productId) }
            .onSuccess {
                val updated = repo.getRecentMovements()
                _state.value = _state.value.copy(recentMovements = updated)
            }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    // ── Customers ─────────────────────────────────────────────────────────────

    fun addCustomer(customer: Customer) = viewModelScope.launch {
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
                _state.value = _state.value.copy(customers = merged)
            }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
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
        _state.value = _state.value.copy(employeeCreation = EmployeeCreationState.Loading)
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
                    )
                )
            }
            .onFailure { err ->
                _state.value = _state.value.copy(
                    employeeCreation = EmployeeCreationState.Error(
                        sanitizeError(err.message)
                    )
                )
            }
    }

    fun clearEmployeeCreation() {
        _state.value = _state.value.copy(employeeCreation = EmployeeCreationState.Idle)
    }

    fun assignOrderToEmployee(orderId: String, employeeId: String) = viewModelScope.launch {
        repo.assignEmployee(orderId, employeeId)
        loadData()
    }

    // ── Suppliers ─────────────────────────────────────────────────────────────

    fun addSupplier(name: String, location: String?) = viewModelScope.launch {
        runCatching { repo.addSupplier(name, location) }
            .onSuccess {
                // Refresh both list views — the picker (string) and the
                // management screen (typed) are kept in sync.
                val full     = repo.getSuppliersFull()
                val display  = repo.getSuppliers()
                _state.value = _state.value.copy(
                    suppliersFull = full,
                    suppliers     = display,
                )
            }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    fun deleteSupplier(id: String) = viewModelScope.launch {
        runCatching { repo.deleteSupplier(id) }
            .onSuccess {
                val full     = repo.getSuppliersFull()
                val display  = repo.getSuppliers()
                _state.value = _state.value.copy(
                    suppliersFull = full,
                    suppliers     = display,
                )
            }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
