package com.example.ruwia.data

import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.Order
import com.example.ruwia.domain.Payment
import com.example.ruwia.domain.CustomerProductPrice
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

    suspend fun getCustomerProductPrice(customerId: String, productId: String): CustomerProductPrice? {
        return supabase.from("customer_product_prices")
            .select {
                filter { eq("customer_id", customerId) }
                filter { eq("product_id", productId) }
                filter { eq("is_active", true) }
            }
            .decodeSingleOrNull()
    }

    suspend fun getCustomerProductPricesForCustomer(customerId: String): List<CustomerProductPrice> {
        return supabase.from("customer_product_prices")
            .select {
                filter { eq("customer_id", customerId) }
                filter { eq("is_active", true) }
            }
            .decodeList()
    }

    suspend fun getCustomerProductPricesForProduct(productId: String): List<CustomerProductPrice> {
        return supabase.from("customer_product_prices")
            .select {
                filter { eq("product_id", productId) }
                filter { eq("is_active", true) }
            }
            .decodeList()
    }

    suspend fun saveCustomerProductPrice(price: CustomerProductPrice): String {
        return supabase.from("customer_product_prices").insert(price) { select() }
            .decodeSingleOrNull<CustomerProductPrice>()
            ?.id
            ?: throw Exception("Failed to save customer product price")
    }

    suspend fun updateCustomerProductPrice(price: CustomerProductPrice) {
        val priceId = price.id
        if (!priceId.isNullOrBlank()) {
            supabase.from("customer_product_prices").update(price) { filter { eq("id", priceId) } }
        }
    }

    suspend fun deleteCustomerProductPrice(id: String) {
        if (id.isNotBlank()) {
            supabase.from("customer_product_prices").delete { filter { eq("id", id) } }
        }
    }
}