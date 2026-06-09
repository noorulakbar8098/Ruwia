package com.example.ruwia.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UserRole { admin, user, employee }

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

@Serializable
data class StockItem(
    val id: String? = null,
    val name: String,
    val capacityLiters: Int,
    val stockAvailable: Int,
    val pricePerCan: Double,
    val costPrice: Double = 0.0,  // purchase cost per can
    val tags: String = ""          // e.g. "DAILY · STAPLE"
)

@Serializable
data class EmployeeInfo(
    val id: String,
    val name: String,
    val phone: String,
    val status: String = "active",   // "active" | "inactive" | "on_delivery"
    val role: String = "staff",      // "manager" | "stock" | "cashier" | "driver"
    val shopName: String = "Shop 1",
    val completedDeliveries: Int = 0,
    val totalDeliveries: Int = 0,
    val monthlySalary: Double = 0.0,
    val rating: Double = 5.0,
    val todayStat: Int = 0,          // key daily metric value
    val statLabel: String = "TODAY"  // label for todayStat
)

@Serializable
data class DeliveryTask(
    val id: String,
    val customerName: String,
    val phone: String,
    val address: String,
    val canQty: Int,
    val etaText: String,
    val status: String = "queued" // "queued" | "en_route" | "delivered" | "cancelled"
)

@Serializable
data class ShopStockInfo(
    val id: String,
    val name: String,       // e.g. "Shop 1 · Main"
    val location: String,   // e.g. "SAIBABA COLONY"
    val totalCans: Int,
    val fullCans: Int,
    val emptyCans: Int,
    val cansWithCustomers: Int,
    val isLive: Boolean = true
)

@Serializable
data class StockMovement(
    val id: String,
    val source: String,     // supplier name or customer name
    val qty: Int,
    val type: String,       // "inward" | "outward" | "adjustment"
    val shopName: String,
    @SerialName("created_at") val createdAt: String? = null
)

