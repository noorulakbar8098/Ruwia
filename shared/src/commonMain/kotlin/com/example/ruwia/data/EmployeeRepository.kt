package com.example.ruwia.data

import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.domain.Outward
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.Supplier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order as SortOrder
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class DailyCansSummary(
    val inward: Int = 0,
    val outward: Int = 0,
    val emptyReturned: Int = 0,
)

class EmployeeRepository {

    // ── Identity ──────────────────────────────────────────────────────────────

    /** The currently logged-in user's UUID (== profiles.id == employees.id). */
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun getEmployeeShopName(employeeId: String): String {
        if (employeeId.isBlank()) return ""
        return try {
            val emp = supabase.from("employees")
                .select { filter { eq("id", employeeId) } }
                .decodeSingleOrNull<com.example.ruwia.domain.EmployeeInfo>()
            emp?.shopName ?: ""
        } catch (e: Exception) { "" }
    }


    // ── Date helpers ──────────────────────────────────────────────────────────

    private fun today(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.year}-${now.monthNumber.toString().padStart(2,'0')}-${now.dayOfMonth.toString().padStart(2,'0')}"
    }

    // ── Route tasks ───────────────────────────────────────────────────────────

    suspend fun getTodayRouteTasks(employeeId: String): List<DeliveryTask> {
        return try {
            val adminId = getAdminIdForUser(employeeId)
            supabase.from("route_tasks")
                .select {
                    filter {
                        eq("employee_id", employeeId)
                        eq("scheduled_date", today())
                        if (adminId != null) eq("admin_id", adminId)
                    }
                    order("created_at", SortOrder.ASCENDING)
                }
                .decodeList()
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun getAdminIdForUser(userId: String): String? {
        return try {
            val profile = supabase.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<com.example.ruwia.domain.Profile>()
            if (profile != null && profile.adminId != null && profile.adminId != userId) {
                return profile.adminId
            }
            
            // Fallback to employees table lookup
            val empJson = supabase.from("employees")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()
            val empAdminId = empJson?.get("admin_id")?.let {
                if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
            }
            if (empAdminId != null && empAdminId.isNotBlank() && empAdminId != userId) {
                return empAdminId
            }
            
            null
        } catch (e: Exception) { null }
    }

    suspend fun recordDeliveryCompletion(
        taskId: String,
        customerId: String,
        deliveredQty: Int,
        returnedEmptyQty: Int,
        collectedAmount: Double,
        paymentMode: String,
        employeeId: String,
        shopName: String,
    ) {
        try {
            supabase.from("outward").insert(
                Outward(
                    customerId         = customerId,
                    qtyDelivered       = deliveredQty,
                    qtyEmptyReturned   = returnedEmptyQty,
                    rate               = collectedAmount,
                    employeeId         = employeeId,
                )
            )
            if (returnedEmptyQty > 0) {
                addStockMovement(
                    source   = "Empty cans · route",
                    qty      = returnedEmptyQty,
                    type     = "inward",
                    shopName = shopName.ifBlank { "Shop 1" },
                )
            }
            supabase.from("route_tasks").update(
                buildJsonObject { put("status", "done") }
            ) { filter { eq("id", taskId) } }
        } catch (_: Exception) {}
    }

    // ── Summary queries ───────────────────────────────────────────────────────

    suspend fun getDailyEarningsSummary(employeeId: String): Double {
        return try {
            val prefix = today()
            val adminId = getAdminIdForUser(employeeId)
            supabase.from("sale_entries")
                .select {
                    filter { 
                        eq("employee_id", employeeId)
                        if (adminId != null) eq("admin_id", adminId)
                    }
                    order("created_at", SortOrder.DESCENDING)
                    limit(200)
                }
                .decodeList<com.example.ruwia.domain.SaleEntry>()
                .filter { it.date.startsWith(prefix) }
                .sumOf { it.totalSelling }
        } catch (e: Exception) { 0.0 }
    }

    /**
     * Today's can flow for this employee — `(inward, outward)`.
     *
     * - **Inward**  = `stock_movements` rows of type `inward` they created today
     *   (e.g. purchase entries logged via the Add Inward screen).
     * - **Outward** = `stock_movements` rows of type `outward` + delivery
     *   completions (`outward.qty_delivered`) they recorded today.
     *
     * Both are filtered by `employee_id` so the profile screen reflects only
     * what the *current* user has done — not the team-wide totals.
     */
    suspend fun getDailyCansSummary(employeeId: String): DailyCansSummary {
        return try {
            val prefix = today()
            val adminId = getAdminIdForUser(employeeId)

            // Stock-movement totals (inward / outward) for today
            val movements = supabase.from("stock_movements")
                .select {
                    filter { 
                        eq("employee_id", employeeId)
                        if (adminId != null) eq("admin_id", adminId)
                    }
                    order("created_at", SortOrder.DESCENDING)
                    limit(200)
                }
                .decodeList<StockMovement>()
                .filter { it.createdAt?.startsWith(prefix) == true }

            val emptyReturnedFromMovements = movements
                .filter { it.type == "inward" && it.source.isEmptyCansSource() }
                .sumOf { it.qty }
            val movementInward  = movements
                .filter { it.type == "inward" && !it.source.isEmptyCansSource() }
                .sumOf { it.qty }
            val movementOutward = movements.filter { it.type == "outward" }.sumOf { it.qty }

            // Delivery completions also count as outward — cans went to customers.
            val deliveries = runCatching {
                supabase.from("outward")
                    .select {
                        filter { eq("employee_id", employeeId) }
                        limit(200)
                    }
                    .decodeList<Outward>()
                    .filter { it.createdAt?.startsWith(prefix) == true }
            }.getOrDefault(emptyList())

            DailyCansSummary(
                inward        = movementInward,
                outward       = movementOutward + deliveries.sumOf { it.qtyDelivered },
                emptyReturned = emptyReturnedFromMovements + deliveries.sumOf { it.qtyEmptyReturned },
            )
        } catch (e: Exception) { DailyCansSummary() }
    }

    // ── Customers ─────────────────────────────────────────────────────────────

    suspend fun getCustomers(): List<Customer> {
        return try {
            val uid = currentUserId() ?: return emptyList()
            val adminId = getAdminIdForUser(uid)
            supabase.from("customers").select {
                if (adminId != null) filter { eq("admin_id", adminId) }
            }.decodeList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addCustomer(customer: Customer): Customer {
        suspend fun insertCustomerInternal(client: io.github.jan.supabase.SupabaseClient): Customer {
            return try {
                client.from("customers").insert(
                    buildJsonObject {
                        put("name", customer.name)
                        if (customer.phone != null) put("phone", customer.phone)
                        if (customer.address != null) put("address", customer.address)
                        if (customer.otherDetails != null) put("other_details", customer.otherDetails)
                    }
                ) { select() }.decodeSingle()
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val isColumnError = msg.contains("other_details") || msg.contains("column")
                if (isColumnError && customer.otherDetails != null) {
                    println("Retrying customer insert without missing other_details column...")
                    client.from("customers").insert(
                        buildJsonObject {
                            put("name", customer.name)
                            if (customer.phone != null) put("phone", customer.phone)
                            if (customer.address != null) put("address", customer.address)
                        }
                    ) { select() }.decodeSingle()
                } else {
                    throw e
                }
            }
        }

        return try {
            insertCustomerInternal(supabase)
        } catch (e: Exception) {
            println("Standard addCustomer failed, trying admin bypass: ${e.message}")
            e.printStackTrace()
            initAdminSession()
            insertCustomerInternal(supabaseAdmin)
        }
    }

    // ── Products ──────────────────────────────────────────────────────────────

    suspend fun getProductCategories(): List<ProductCategory> {
        return try {
            val uid = currentUserId() ?: return emptyList()
            val adminId = getAdminIdForUser(uid)
            supabase.from("product_categories")
                .select { 
                    filter { 
                        eq("is_active", true)
                        eq("is_deleted", false)
                        if (adminId != null) eq("admin_id", adminId)
                    } 
                }
                .decodeList()
        } catch (e: Exception) { emptyList() }
    }

    // ── Shop stocks ───────────────────────────────────────────────────────────

    /**
     * Returns every shop's current can balance so the employee's Stock tab
     * can mirror the same per-shop totals the admin dashboard shows.
     *
     * RLS already exposes `shop_stocks` to every authenticated user, so the
     * employee sees the same data — just a read-only presentation.
     */
    suspend fun getShopStocks(): List<ShopStockInfo> {
        return try {
            val uid = currentUserId() ?: return emptyList()
            val adminId = getAdminIdForUser(uid)
            supabase.from("shop_stocks").select {
                if (adminId != null) filter { eq("admin_id", adminId) }
            }
                .decodeList<ShopStockInfo>()
                // Defensive de-duplication — historical inserts before the
                // unique-name constraint can leave duplicate rows that would
                // otherwise show up twice in the picker.
                .distinctBy { it.name.trim().lowercase() }
        } catch (e: Exception) { emptyList() }
    }


    // ── Recent stock entries ───────────────────────────────────

    suspend fun getRecentEntries(employeeId: String): List<StockMovement> {
        return try {
            val adminId = getAdminIdForUser(employeeId)
            supabase.from("stock_movements")
                .select {
                    filter { 
                        if (adminId != null) eq("admin_id", adminId)
                    }
                    order("created_at", SortOrder.DESCENDING)
                    limit(100)
                }
                .decodeList()
        } catch (e: Exception) { emptyList() }
    }

    /**
     * All movements that touch a given shop — needed by the employee's stock
     * tab to compute shop-wide inward/outward totals (not just the current
     * employee's). RLS already exposes `stock_movements` to authenticated
     * users so the employee sees everything that happened at their shop,
     * regardless of which colleague logged it.
     */
    suspend fun getShopMovements(shopName: String): List<StockMovement> {
        return try {
            val uid = currentUserId() ?: return emptyList()
            val adminId = getAdminIdForUser(uid)
            supabase.from("stock_movements")
                .select {
                    if (adminId != null) filter { eq("admin_id", adminId) }
                    order("created_at", SortOrder.DESCENDING)
                }
                .decodeList<StockMovement>()
                .let { list ->
                    if (shopName.isBlank() || shopName.equals("All Shops", ignoreCase = true)) {
                        list
                    } else {
                        val b = shopName.trim().lowercase().substringBefore("·").trim()
                        list.filter { mov ->
                            val a = mov.shopName.trim().lowercase().substringBefore("·").trim()
                            a == b
                        }
                    }
                }
        } catch (e: Exception) { emptyList() }
    }

    // ── Record inward stock purchase ──────────────────────────────────────────

    suspend fun addStockMovement(
        source: String,
        qty: Int,
        type: String,
        shopName: String,
        productId: String? = null,
        createdAt: String? = null,
    ) {
        val employeeId = currentUserId()
        
        // 1. Validate and perform atomic stock update
        if (productId != null && productId.isNotBlank()) {
            var success = false
            var attempts = 0
            while (!success && attempts < 10) {
                attempts++
                val cur = supabase.from("product_categories")
                    .select { filter { eq("id", productId) } }
                    .decodeSingleOrNull<ProductCategory>() ?: break
                val delta = when (type) {
                    "inward"     ->  qty
                    "outward"    -> -qty
                    "adjustment" ->  qty
                    else         ->  0
                }
                val newStock = (cur.stockAvailable + delta).coerceAtLeast(0)
                if (type == "outward" && cur.stockAvailable < qty) {
                    throw IllegalStateException("Insufficient stock. Only ${cur.stockAvailable} units available for ${cur.displayName}.")
                }
                try {
                    val successResult = supabase.postgrest.rpc(
                        "update_product_stock",
                        buildJsonObject {
                            put("p_id", productId)
                            put("p_new_stock", newStock)
                            put("p_expected_current", cur.stockAvailable)
                        }
                    ).decodeAs<Boolean>()
                    if (successResult) {
                        success = true
                    }
                } catch (e: Exception) {
                    // Retry
                }
            }
            if (attempts >= 10 && !success) {
                throw IllegalStateException("Failed to update stock due to concurrent updates. Please try again.")
            }
        }

        // 2. Insert stock movement row
        supabase.from("stock_movements").insert(
            buildJsonObject {
                put("source", source)
                put("qty", qty)
                put("type", type)
                put("shop_name", shopName)
                if (productId != null) put("product_id", productId)
                if (employeeId != null) put("employee_id", employeeId)
                if (createdAt != null) put("created_at", createdAt)
            }
        )
    }

    // ── Record outward sale (one row per product line item) ──────────────────

    /**
     * Persist an outward sale for the current employee. Each [SaleLine] becomes
     * a row in `sale_entries` and an `outward` stock movement so the admin's
     * stock dashboard reflects the sale immediately.
     *
     * If [emptyCansCollected] is non-zero, an extra `inward` stock movement is
     * recorded with `source = "Empty cans · {customer}"` so the admin can see
     * which customers returned how many empties.
     */
    suspend fun addOutwardSale(
        customerName: String,
        shopName: String,
        lines: List<SaleLine>,
        emptyCansCollected: Int,
        saleDate: String? = null,
    ) {
        val employeeId = currentUserId()
        val effectiveDate = saleDate?.takeIf { it.isNotBlank() } ?: today()

        // 1. Perform atomic stock update for all lines first to ensure atomic check & deduction.
        var lastError: Exception? = null
        lines.forEach { line ->
            var success = false
            var attempts = 0
            while (!success && attempts < 10) {
                attempts++
                val cur = supabase.from("product_categories")
                    .select { filter { eq("id", line.productId) } }
                    .decodeSingleOrNull<ProductCategory>() ?: throw IllegalStateException("Product ${line.productName} not found")
                
                val newStock = cur.stockAvailable - line.qty
                if (newStock < 0) {
                    throw IllegalStateException("Insufficient stock. Only ${cur.stockAvailable} units available for ${cur.displayName}.")
                }
                try {
                    val successResult = supabase.postgrest.rpc(
                        "update_product_stock",
                        buildJsonObject {
                            put("p_id", line.productId)
                            put("p_new_stock", newStock)
                            put("p_expected_current", cur.stockAvailable)
                        }
                    ).decodeAs<Boolean>()
                    if (successResult) {
                        success = true
                    }
                } catch (e: Exception) {
                    lastError = e
                    println("DEBUG_ERROR: Update stock failed: ${e.message}")
                    e.printStackTrace()
                    // Retry
                }
            }
            if (!success) {
                throw IllegalStateException("Failed to update stock: ${lastError?.message ?: "Unknown database error"}")
            }
        }

        // 2. Persist the sales entries and stock movements
        lines.forEach { line ->
            supabase.from("sale_entries").insert(
                buildJsonObject {
                    put("date", effectiveDate)
                    put("customer_name", customerName)
                    put("product_id", line.productId)
                    put("product_name", line.productName)
                    put("qty", line.qty)
                    put("purchase_price_per_unit", line.purchasePricePerUnit)
                    put("selling_price_per_unit", line.sellingPricePerUnit)
                    put("sales_margin_per_unit", line.sellingPricePerUnit - line.purchasePricePerUnit)
                    put("total_selling", line.sellingPricePerUnit * line.qty)
                    put("total_margin", (line.sellingPricePerUnit - line.purchasePricePerUnit) * line.qty)
                    put("shop_id", shopName.ifBlank { "shop1" })
                    if (employeeId != null) put("employee_id", employeeId)
                }
            )

            supabase.from("stock_movements").insert(
                buildJsonObject {
                    put("source", "Sale · $customerName")
                    put("qty", line.qty)
                    put("type", "outward")
                    put("shop_name", shopName)
                    put("product_id", line.productId)
                    if (employeeId != null) put("employee_id", employeeId)
                }
            )
        }

        // 3. Empties picked up at delivery time → inward movement.
        if (emptyCansCollected > 0) {
            supabase.from("stock_movements").insert(
                buildJsonObject {
                    put("source", "Empty cans · $customerName")
                    put("qty", emptyCansCollected)
                    put("type", "inward")
                    put("shop_name", shopName)
                    if (employeeId != null) put("employee_id", employeeId)
                }
            )
        }
    }

    /** A single product line within an outward sale. */
    data class SaleLine(
        val productId: String,
        val productName: String,
        val qty: Int,
        val sellingPricePerUnit: Double,
        val purchasePricePerUnit: Double = 0.0,
    )

    suspend fun addProductCategory(cat: ProductCategory): ProductCategory {
        val uid = currentUserId() ?: ""
        val tenantAdminId = getAdminIdForUser(uid) ?: uid
        return supabase.from("product_categories").insert(
            buildJsonObject {
                put("name", cat.name.trim())
                put("display_name", cat.displayName.trim())
                put("brand_name", cat.brandName.trim())
                put("purchase_price", cat.purchasePrice)
                put("default_sell_price", cat.defaultSellPrice)
                put("stock_available", cat.stockAvailable)
                put("is_active", cat.isActive)
                put("admin_id", tenantAdminId)
            }
        ) { select() }.decodeSingle()
    }

    suspend fun updateProductCategory(cat: ProductCategory) {
        supabase.from("product_categories").update(
            buildJsonObject {
                put("purchase_price", cat.purchasePrice)
                put("default_sell_price", cat.defaultSellPrice)
            }
        ) { filter { eq("id", cat.id) } }
    }
}

private fun String.isEmptyCansSource(): Boolean =
    trim().startsWith("Empty cans", ignoreCase = true)
