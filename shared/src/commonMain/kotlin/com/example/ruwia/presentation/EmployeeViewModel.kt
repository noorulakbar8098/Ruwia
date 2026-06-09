package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.EmployeeRepository
import com.example.ruwia.domain.DeliveryTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EmployeeState(
    val loading: Boolean = false,
    val error: String? = null,
    val tasks: List<DeliveryTask> = emptyList(),
    val dailyEarnings: Double = 0.0,
    val filledDelivered: Int = 0,
    val emptyReturned: Int = 0,
    val selectedTask: DeliveryTask? = null
)

class EmployeeViewModel(private val repo: EmployeeRepository) : ViewModel() {

    private val _state = MutableStateFlow(EmployeeState())
    val state: StateFlow<EmployeeState> = _state.asStateFlow()

    fun loadDashboard(employeeId: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            val tasks = repo.getTodayRouteTasks(employeeId)
            val earnings = repo.getDailyEarningsSummary(employeeId)
            val cans = repo.getDailyCansSummary(employeeId)
            EmployeeState(
                loading = false,
                tasks = tasks,
                dailyEarnings = earnings,
                filledDelivered = cans.first,
                emptyReturned = cans.second
            )
        }.onSuccess { newState ->
            _state.value = newState
        }.onFailure {
            _state.value = _state.value.copy(loading = false, error = it.message ?: "Failed to load shift routes")
        }
    }

    fun selectTask(task: DeliveryTask?) {
        _state.value = _state.value.copy(selectedTask = task)
    }

    fun completeDelivery(
        taskId: String,
        employeeId: String,
        delivered: Int,
        returned: Int,
        amount: Double,
        mode: String
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)
        runCatching {
            repo.recordDeliveryCompletion(taskId, delivered, returned, amount, mode)
        }.onSuccess {
            loadDashboard(employeeId)
            _state.value = _state.value.copy(selectedTask = null)
        }.onFailure {
            _state.value = _state.value.copy(loading = false, error = it.message ?: "Submission failed")
        }
    }
}
