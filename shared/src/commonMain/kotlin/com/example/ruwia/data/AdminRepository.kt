package com.example.ruwia.data

import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.EmployeeInfo
import com.example.ruwia.domain.MonthlyExpense
import com.example.ruwia.domain.Order
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.Profile
import com.example.ruwia.domain.SaleEntry
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockItem
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.Supplier
import com.example.ruwia.domain.UserRole
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order as SortOrder
import io.github.jan.supabase.auth.auth
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AdminRepository {

    // ── Date helpers ──────────────────────────────────────────────────────────

    private fun currentYearMonth(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
    }

    // ── Metrics ───────────────────────────────────────────────────────────────

    suspend fun getMRR(): Double {
        return try {
            val prefix = currentYearMonth()
            supabase.from("sale_entries")
                .select { order("created_at", SortOrder.DESCENDING); limit(500) }
                .decodeList<SaleEntry>()
                .filter { it.createdAt?.startsWith(prefix) == true }
                .sumOf { it.totalSelling }
        } catch (e: Exception) { 0.0 }
    }

    suspend fun getCSAT(): Double = 4.7  // placeholder until ratings table is added

    suspend fun getActiveFleetCount(): Int {
        return try {
            supabase.from("employees")
                .select { filter { eq("status", "active") } }
                .decodeList<EmployeeInfo>()
                .size
        } catch (e: Exception) { 0 }
    }

    // ── Products ──────────────────────────────────────────────────────────────

    suspend fun getProductCategories(): List<ProductCategory> {
        return try {
            supabase.from("product_categories")
                .select { filter { eq("is_active", true) } }
                .decodeList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getStockSummaryList(): List<StockItem> {
        return try {
            getProductCategories().map { cat ->
                StockItem(
                    id             = cat.id,
                    name           = cat.displayName,
                    capacityLiters = parseLiters(cat.name),
                    stockAvailable = cat.stockAvailable,
                    pricePerCan    = cat.defaultSellPrice,
                    costPrice      = cat.purchasePriceGC,
                    tags           = cat.supplierGroup,
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addProductCategory(cat: ProductCategory) {
        supabase.from("product_categories").insert(
            buildJsonObject {
                put("name", cat.name)
                put("display_name", cat.displayName)
                put("supplier_group", cat.supplierGroup)
                put("purchase_price_gc", cat.purchasePriceGC)
                put("purchase_price_mb", cat.purchasePriceMB)
                put("default_sell_price", cat.defaultSellPrice)
                put("stock_available", cat.stockAvailable)
                put("is_active", cat.isActive)
            }
        )
    }

    suspend fun updateProductCategory(cat: ProductCategory) {
        if (cat.id.isBlank()) return
        supabase.from("product_categories").update(
            buildJsonObject {
                put("name", cat.name)
                put("display_name", cat.displayName)
                put("supplier_group", cat.supplierGroup)
                put("purchase_price_gc", cat.purchasePriceGC)
                put("purchase_price_mb", cat.purchasePriceMB)
                put("default_sell_price", cat.defaultSellPrice)
                put("stock_available", cat.stockAvailable)
                put("is_active", cat.isActive)
            }
        ) { filter { eq("id", cat.id) } }
    }

    suspend fun deleteProductCategory(id: String) {
        if (id.isBlank()) return
        supabase.from("product_categories").update(buildJsonObject { put("is_active", false) }) {
            filter { eq("id", id) }
        }
    }

    // ── Sales ─────────────────────────────────────────────────────────────────

    suspend fun getSaleEntries(productName: String? = null): List<SaleEntry> {
        return try {
            supabase.from("sale_entries")
                .select {
                    order("created_at", SortOrder.DESCENDING)
                    limit(500)
                }
                .decodeList<SaleEntry>()
                .let { entries ->
                    if (productName != null)
                        entries.filter { it.productName.contains(productName, ignoreCase = true) }
                    else entries
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addSaleEntry(entry: SaleEntry) {
        supabase.from("sale_entries").insert(
            buildJsonObject {
                put("date", entry.date)
                put("customer_name", entry.customerName)
                put("product_id", entry.productId)
                put("product_name", entry.productName)
                put("qty", entry.qty)
                put("purchase_price_per_unit", entry.purchasePricePerUnit)
                put("selling_price_per_unit", entry.sellingPricePerUnit)
                put("sales_margin_per_unit", entry.salesMarginPerUnit)
                put("total_selling", entry.totalSelling)
                put("total_margin", entry.totalMargin)
                put("shop_id", entry.shopId)
                if (entry.employeeId != null) put("employee_id", entry.employeeId)
            }
        )
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    suspend fun getMonthlyExpense(month: String): MonthlyExpense? {
        return try {
            supabase.from("monthly_expenses")
                .select { filter { eq("month", month) } }
                .decodeSingleOrNull()
        } catch (e: Exception) { null }
    }

    suspend fun saveMonthlyExpense(expense: MonthlyExpense) {
        supabase.from("monthly_expenses").upsert(expense)
    }

    // ── Revenue chart ─────────────────────────────────────────────────────────

    suspend fun getWeeklyRevenueSummary(): Pair<List<Float>, String> {
        return try {
            val entries = supabase.from("sale_entries")
                .select { order("created_at", SortOrder.DESCENDING); limit(200) }
                .decodeList<SaleEntry>()

            if (entries.isEmpty()) return defaultRevenuePlaceholder()

            val byDay = entries
                .groupBy { it.createdAt?.take(10) ?: "" }
                .filterKeys { it.isNotEmpty() }
                .entries
                .sortedByDescending { it.key }
                .take(7)
                .reversed()
                .map { (_, es) -> es.sumOf { it.totalSelling }.toFloat() }

            if (byDay.isEmpty()) return defaultRevenuePlaceholder()

            val maxVal = byDay.max().takeIf { it > 0f } ?: 1f
            val points = byDay.map { it / maxVal }
            val total  = byDay.sum()
            val label  = if (total >= 100000f) "₹${(total / 100000f * 100).toInt() / 100.0} L"
                         else "₹${total.toInt()}"
            points to label
        } catch (e: Exception) {
            defaultRevenuePlaceholder()
        }
    }

    private fun defaultRevenuePlaceholder() =
        listOf(0.4f, 0.5f, 0.45f, 0.6f, 0.55f, 0.7f, 0.5f) to "₹0"

    // ── Customers ─────────────────────────────────────────────────────────────

    suspend fun getAllCustomers(): List<Customer> {
        return try {
            supabase.from("customers").select().decodeList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addCustomer(customer: Customer): Customer {
        return try {
            supabase.from("customers").insert(
                buildJsonObject {
                    put("name", customer.name)
                    if (customer.phone != null) put("phone", customer.phone)
                    if (customer.address != null) put("address", customer.address)
                    if (customer.otherDetails != null) put("other_details", customer.otherDetails)
                }
            ) { select() }.decodeSingle()
        } catch (e: Exception) {
            customer.copy(id = "local_${customer.name.hashCode()}")
        }
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    suspend fun getAllOrders(): List<Order> {
        return try {
            supabase.from("orders")
                .select { order("created_at", SortOrder.DESCENDING); limit(50) }
                .decodeList()
        } catch (e: Exception) { emptyList() }
    }

    // ── Employees ─────────────────────────────────────────────────────────────

    suspend fun getEmployees(): List<EmployeeInfo> {
        return try {
            supabase.from("employees").select().decodeList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addEmployee(
        name: String,
        phone: String,
        role: String,
        shopName: String,
        salary: Double,
        email: String,
        password: String,
    ): EmployeeInfo {
        initAdminSession()

        val newUser = supabaseAdmin.auth.admin.createUserWithEmail {
            this.email       = email
            this.password    = password
            this.autoConfirm = true
        }
        val userId = newUser.id

        supabaseAdmin.from("profiles").upsert(
            Profile(id = userId, role = UserRole.employee, fullName = name, phone = phone)
        )

        supabaseAdmin.from("employees").insert(
            buildJsonObject {
                put("id",             userId)
                put("name",           name)
                put("phone",          phone)
                put("role",           role.lowercase())
                put("shop_name",      shopName)
                put("monthly_salary", salary)
                put("status",         "active")
            }
        )

        return EmployeeInfo(
            id            = userId,
            name          = name,
            phone         = phone,
            role          = role.lowercase(),
            shopName      = shopName,
            monthlySalary = salary,
            status        = "active",
        )
    }

    // ── Stock ─────────────────────────────────────────────────────────────────

    suspend fun getShopStocks(): List<ShopStockInfo> {
        return try {
            supabase.from("shop_stocks").select().decodeList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getRecentMovements(): List<StockMovement> {
        return try {
            supabase.from("stock_movements")
                .select { order("created_at", SortOrder.DESCENDING); limit(20) }
                .decodeList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addStockMovement(
        source: String,
        qty: Int,
        type: String,
        shopName: String,
        productId: String? = null,
    ) {
        supabase.from("stock_movements").insert(
            buildJsonObject {
                put("source", source)
                put("qty", qty)
                put("type", type)
                put("shop_name", shopName)
                if (productId != null) put("product_id", productId)
            }
        )
    }

    // ── Suppliers ─────────────────────────────────────────────────────────────

    suspend fun getSuppliers(): List<String> {
        return try {
            supabase.from("suppliers")
                .select { filter { eq("is_active", true) } }
                .decodeList<Supplier>()
                .map { s -> if (s.location != null) "${s.name}  ·  ${s.location}" else s.name }
        } catch (e: Exception) {
            listOf(
                "Global Creators  ·  Tiru",
                "Multi Brands  ·  Coimbatore",
                "Aqua Pure Plant  ·  Tiru",
            )
        }
    }

    suspend fun assignEmployee(orderId: String, employeeId: String) {
        try {
            supabase.from("orders").update(buildJsonObject { put("employee_id", employeeId) }) {
                filter { eq("id", orderId) }
            }
        } catch (_: Exception) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseLiters(name: String): Int {
        val n = name.lowercase()
        return when {
            n.contains("20") -> 20
            n.contains("10") -> 10
            n.contains("5")  -> 5
            n.contains("2")  -> 2
            n.contains("1")  -> 1
            n.contains("500") -> 0
            n.contains("300") -> 0
            else -> 0
        }
    }
}
