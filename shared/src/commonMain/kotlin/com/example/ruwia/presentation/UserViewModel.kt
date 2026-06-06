package com.example.ruwia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruwia.data.AuthRepository
import com.example.ruwia.data.CustomerRepository
import com.example.ruwia.data.supabase
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.Order
import com.example.ruwia.domain.Payment
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserState(
    val loading: Boolean = false,
    val error: String? = null,
    val customer: Customer? = null,
    val orders: List<Order> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val orderPlaced: Boolean = false,
)

class UserViewModel(
    private val repo: CustomerRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UserState())
    val state: StateFlow<UserState> = _state.asStateFlow()

    private var ordersChannel: RealtimeChannel? = null

    init { loadData() }

    fun loadData() = viewModelScope.launch {
        _state.value = UserState(loading = true)
        runCatching {
            val customer = repo.myCustomer()
            val cid = customer?.id
            Triple(
                customer,
                if (cid != null) repo.myOrders(cid) else emptyList(),
                if (cid != null) repo.myPayments(cid) else emptyList(),
            )
        }.onSuccess { (customer, orders, payments) ->
            _state.value = UserState(customer = customer, orders = orders, payments = payments)
            customer?.id?.let { subscribeOrders(it) }
        }.onFailure {
            _state.value = UserState(error = it.message ?: "Failed to load")
        }
    }

    private fun subscribeOrders(customerId: String) = viewModelScope.launch {
        val channel = supabase.channel("user-orders-$customerId")
        ordersChannel = channel

        launch {
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "orders"
            }.collect {
                // RLS ensures only our rows come through; re-fetch to get latest state
                runCatching { repo.myOrders(customerId) }.onSuccess { updated ->
                    _state.value = _state.value.copy(orders = updated)
                }
            }
        }

        channel.subscribe()
    }

    fun placeOrder(qty: Int) = viewModelScope.launch {
        val cid = _state.value.customer?.id ?: return@launch
        _state.value = _state.value.copy(loading = true, error = null, orderPlaced = false)
        runCatching { repo.placeOrder(cid, qty) }
            .onSuccess {
                val orders = repo.myOrders(cid)
                _state.value = _state.value.copy(loading = false, orders = orders, orderPlaced = true)
            }
            .onFailure {
                _state.value = _state.value.copy(
                    loading = false,
                    error = it.message ?: "Order failed",
                )
            }
    }

    fun clearOrderPlaced() {
        _state.value = _state.value.copy(orderPlaced = false)
    }

    fun logout() = viewModelScope.launch {
        runCatching { auth.logout() }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { ordersChannel?.unsubscribe() }
    }
}
