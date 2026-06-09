package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.AdminRepository
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.Order
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockItem
import com.example.ruwia.domain.StockMovement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Tracks the lifecycle of a single employee-creation attempt. */
sealed class EmployeeCreationState {
    object Idle    : EmployeeCreationState()
    object Loading : EmployeeCreationState()
    /** Supabase confirmed the user was created; carries the credentials to display. */
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
    // Dedicated state for the employee-creation flow
    val employeeCreation: EmployeeCreationState = EmployeeCreationState.Idle
)

class AdminViewModel(private val repo: AdminRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            val mrr = repo.getMRR()
            val csat = repo.getCSAT()
            val fleet = repo.getActiveFleetCount()
            val stocks = repo.getStockSummaryList()
            val orders = repo.getAllOrders()
            val customers = repo.getAllCustomers()
            val employees = repo.getEmployees()
            val (weeklyPts, weeklyLabel) = repo.getWeeklyRevenueSummary()
            val shopStocks  = repo.getShopStocks()
            val movements   = repo.getRecentMovements()
            AdminState(
                loading = false,
                mrr = mrr,
                csat = csat,
                fleetActiveCount = fleet,
                stockItems = stocks,
                orders = orders,
                customers = customers,
                employees = employees,
                weeklyRevenuePoints = weeklyPts,
                weeklyRevenueLabel = weeklyLabel,
                shopStocks = shopStocks,
                recentMovements = movements
            )
        }.onSuccess { newState ->
            _state.value = newState
        }.onFailure {
            _state.value = _state.value.copy(loading = false, error = it.message ?: "Failed to load admin telemetry")
        }
    }

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
            employeeCreation = EmployeeCreationState.Loading
        )
        runCatching { repo.addEmployee(name, phone, role, shop, salary, email, password) }
            .onSuccess { newEmp ->
                _state.value = _state.value.copy(
                    employees        = _state.value.employees + newEmp,
                    employeeCreation = EmployeeCreationState.Success(
                        userId   = newEmp.id,
                        name     = newEmp.name,
                        email    = email,
                        password = password,   // shown once to admin, not persisted
                        role     = newEmp.role,
                        shop     = newEmp.shopName
                    )
                )
            }
            .onFailure { err ->
                _state.value = _state.value.copy(
                    employeeCreation = EmployeeCreationState.Error(
                        err.message ?: "Failed to create employee account"
                    )
                )
            }
    }

    /** Call after the admin has seen (and dismissed) the credentials dialog. */
    fun clearEmployeeCreation() {
        _state.value = _state.value.copy(employeeCreation = EmployeeCreationState.Idle)
    }

    fun assignOrderToEmployee(orderId: String, employeeId: String) = viewModelScope.launch {
        repo.assignEmployee(orderId, employeeId)
        loadData()
    }
}
