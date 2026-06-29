package com.example.ruwia.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class UserRole { admin, user, employee }

@Serializable
data class Profile(
    val id: String,
    val role: UserRole = UserRole.user,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    @SerialName("admin_id") val adminId: String? = null,
)

@Serializable
data class Customer(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("other_details") val otherDetails: String? = null,
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
    val type: String,
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
    @SerialName("employee_id") val employeeId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
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
    @SerialName("capacity_liters") val capacityLiters: Int = 0,
    @SerialName("stock_available") val stockAvailable: Int = 0,
    @SerialName("price_per_can") val pricePerCan: Double = 0.0,
    @SerialName("cost_price") val costPrice: Double = 0.0,
    val tags: String = ""
)

@Serializable
data class EmployeeInfo(
    val id: String,
    val name: String,
    val phone: String = "",
    val status: String = "active",
    val role: String = "staff",
    @SerialName("shop_name") val shopName: String = "Shop 1",
    @SerialName("completed_deliveries") val completedDeliveries: Int = 0,
    @SerialName("total_deliveries") val totalDeliveries: Int = 0,
    @SerialName("monthly_salary") val monthlySalary: Double = 0.0,
    val rating: Double = 5.0,
    @SerialName("today_stat") val todayStat: Int = 0,
    @SerialName("stat_label") val statLabel: String = "TODAY",
)

@Serializable
data class DeliveryTask(
    val id: String,
    @SerialName("customer_name") val customerName: String,
    val phone: String = "",
    val address: String = "",
    @SerialName("can_qty") val canQty: Int,
    @SerialName("eta_text") val etaText: String = "",
    val status: String = "queued",
)

@Serializable
data class ShopStockInfo(
    val id: String,
    val name: String,
    val location: String = "",
    @SerialName("total_cans") val totalCans: Double = 0.0,
    @SerialName("full_cans") val fullCans: Double = 0.0,
    @SerialName("empty_cans") val emptyCans: Double = 0.0,
    @SerialName("cans_with_customers") val cansWithCustomers: Double = 0.0,
    @SerialName("is_live") val isLive: Boolean = true,
)

@Serializable
data class StockMovement(
    val id: String = "",
    val source: String,
    val qty: Int,
    val type: String,
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("product_id") val productId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class Supplier(
    val id: String? = null,
    val name: String,
    val location: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
)

fun getUnitsPerCase(productName: String): Int {
    val name = productName.lowercase()
    return when {
        name.contains("300") -> 30
        name.contains("500") -> 24
        name.contains("20")  -> 1
        name.contains("10")  -> 1
        name.contains("5")   -> 1
        name.contains("2")   -> 9
        name.contains("1")   -> 12
        else                 -> 1
    }
}


val StockItem.unitsPerCase: Int get() = getUnitsPerCase(name)

@Serializable
data class ProductCategory(
    val id: String = "",
    val name: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("brand_name") val brandName: String = "",
    @SerialName("supplier_group") val supplierGroup: String = "GC",
    @SerialName("purchase_price") val purchasePrice: Double = 0.0,
    @SerialName("purchase_price_gc") val purchasePriceGC: Double = 0.0,
    @SerialName("purchase_price_mb") val purchasePriceMB: Double = 0.0,
    @SerialName("default_sell_price") val defaultSellPrice: Double = 0.0,
    @SerialName("stock_available") val stockAvailable: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
)

val ProductCategory.unitsPerCase: Int get() = getUnitsPerCase(displayName)

@Serializable
data class SaleEntry(
    val id: String? = null,
    val date: String,
    @SerialName("customer_name") val customerName: String,
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    val qty: Int,
    @SerialName("purchase_price_per_unit") val purchasePricePerUnit: Double = 0.0,
    @SerialName("selling_price_per_unit") val sellingPricePerUnit: Double = 0.0,
    @SerialName("sales_margin_per_unit") val salesMarginPerUnit: Double = 0.0,
    @SerialName("total_selling") val totalSelling: Double = 0.0,
    @SerialName("total_margin") val totalMargin: Double = 0.0,
    @SerialName("shop_id") val shopId: String = "shop1",
    @SerialName("employee_id") val employeeId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class CustomExpense(
    val name: String,
    val amount: Double
)

@Serializable
data class MonthlyExpense(
    val id: String? = null,
    val month: String,
    @SerialName("shop_id") val shopId: String = "shop1",
    @SerialName("shop_rent") val shopRent: Double = 0.0,
    @SerialName("admin_salary") val adminSalary: Double = 0.0,
    @SerialName("delivery_staff") val deliveryStaff: Double = 0.0,
    val miscellaneous: Double = 0.0,
    @SerialName("bike_expense") val bikeExpense: Double = 0.0,
    @SerialName("custom_expenses") val customExpenses: String? = null,
) {
    val customExpensesList: List<CustomExpense> get() = try {
        if (!customExpenses.isNullOrBlank()) {
            Json.decodeFromString<List<CustomExpense>>(customExpenses)
        } else {
            emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }

    val total: Double get() = shopRent + adminSalary + deliveryStaff + miscellaneous + bikeExpense + customExpensesList.sumOf { it.amount }
}

