package com.example.ruwia.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UserRole { admin, user }

@Serializable
data class Profile(
    val id: String,
    val role: UserRole = UserRole.user,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
)

@Serializable
data class Customer(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("cans_held") val cansHeld: Int = 0,
    val balance: Double = 0.0,
)

@Serializable
data class Order(
    val id: String? = null,
    @SerialName("customer_id") val customerId: String,
    val qty: Int,
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class Inward(
    val id: String? = null,
    val qty: Int,
    val type: String, // "purchase" | "refill"
    val note: String? = null,
)

@Serializable
data class Outward(
    val id: String? = null,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("customer_id") val customerId: String,
    @SerialName("qty_delivered") val qtyDelivered: Int,
    @SerialName("qty_empty_returned") val qtyEmptyReturned: Int = 0,
    val rate: Double = 0.0,
)

@Serializable
data class Payment(
    val id: String? = null,
    @SerialName("customer_id") val customerId: String,
    val amount: Double,
    val mode: String = "cash",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class StockSummary(
    @SerialName("total_filled") val totalFilled: Int,
    @SerialName("total_empty") val totalEmpty: Int,
    @SerialName("cans_with_customers") val cansWithCustomers: Int,
)
