package com.example.ruwia.data

import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.Order
import com.example.ruwia.domain.Payment
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order as SortOrder

class CustomerRepository(private val auth: AuthRepository) {

    suspend fun myCustomer(): Customer? {
        val uid = auth.currentUserId() ?: return null
        return supabase.from("customers")
            .select { filter { eq("user_id", uid) } }
            .decodeSingleOrNull()
    }

    suspend fun myOrders(customerId: String): List<Order> =
        supabase.from("orders")
            .select {
                filter { eq("customer_id", customerId) }
                order("created_at", SortOrder.DESCENDING)
            }
            .decodeList()

    suspend fun myPayments(customerId: String): List<Payment> =
        supabase.from("payments")
            .select {
                filter { eq("customer_id", customerId) }
                order("created_at", SortOrder.DESCENDING)
            }
            .decodeList()

    suspend fun placeOrder(customerId: String, qty: Int) {
        supabase.from("orders").insert(Order(customerId = customerId, qty = qty))
    }
}
