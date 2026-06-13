package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.EmployeeRepository
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.StockMovement
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
    val filledDelivered: Int = 0,
    val emptyReturned: Int = 0,
    val selectedTask: DeliveryTask? = null,
    val recentEntries: List<StockMovement> = emptyList(),
    val productCategories: List<ProductCategory> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val suppliers: List<String> = emptyList(),
    val currentDate: String = "",
)

class EmployeeViewModel(private val repo: EmployeeRepository) : ViewModel() {

    private val _state = MutableStateFlow(EmployeeState())
    val state: StateFlow<EmployeeState> = _state.asStateFlow()

    private var currentEmployeeId: String = ""

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
            val currentDate      = formattedToday()
            EmployeeState(
                loading           = false,
                tasks             = tasks,
                dailyEarnings     = earnings,
                filledDelivered   = cans.first,
                emptyReturned     = cans.second,
                recentEntries     = recentEntries,
                productCategories = productCategories,
                customers         = customers,
                suppliers         = suppliers,
                currentDate       = currentDate,
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
                _state.value = _state.value.copy(customers = _state.value.customers + saved)
            }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    fun addInwardStock(
        supplier: String,
        quantities: Map<String, Int>,
        shopName: String,
    ) = viewModelScope.launch {
        quantities.filter { it.value > 0 }.forEach { (_, qty) ->
            runCatching { repo.addStockMovement(supplier, qty, "inward", shopName) }
        }
        val updated = repo.getRecentEntries(currentEmployeeId)
        _state.value = _state.value.copy(recentEntries = updated)
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
