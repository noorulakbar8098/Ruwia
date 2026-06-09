package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.AdminRepository
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.Order
import com.example.ruwia.domain.StockItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminState(
    val loading: Boolean = false,
    val error: String? = null,
    val mrr: Double = 0.0,
    val csat: Double = 0.0,
    val fleetActiveCount: Int = 0,
    val stockItems: List<StockItem> = emptyList(),
    val orders: List<Order> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val employees: List<EmployeeInfo> = emptyList()
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
            AdminState(
                loading = false,
                mrr = mrr,
                csat = csat,
                fleetActiveCount = fleet,
                stockItems = stocks,
                orders = orders,
                customers = customers,
                employees = employees
            )
        }.onSuccess { newState ->
            _state.value = newState
        }.onFailure {
            _state.value = _state.value.copy(loading = false, error = it.message ?: "Failed to load admin telemetry")
        }
    }

    fun assignOrderToEmployee(orderId: String, employeeId: String) = viewModelScope.launch {
        repo.assignEmployee(orderId, employeeId)
        loadData()
    }
}
