package com.example.ruwia.data

import com.example.ruwia.domain.AppSetting
import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.CustomerProductPrice
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.domain.Outward
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.netStockPerProductInShop
import com.example.ruwia.domain.shopMatchKey
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
                    shopName = shopName,
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

    /**
     * Running total of empty cans this employee has ever collected (all-time,
     * not limited to today or any date range). Sums every `inward` stock
     * movement whose source is an empty-cans return, filtered by employee.
     */
    suspend fun getEmployeeEmptyCansTotal(employeeId: String): Int {
        if (employeeId.isBlank()) return 0
        return try {
            val adminId = getAdminIdForUser(employeeId)
            supabase.from("stock_movements")
                .select {
                    filter {
                        eq("employee_id", employeeId)
                        if (adminId != null) eq("admin_id", adminId)
                    }
                    // Explicit cap so the all-time total isn't truncated to
                    // PostgREST's default 1000-row page.
                    limit(100000)
                }
                .decodeList<StockMovement>()
                .filter { it.type == "inward" && it.source.isEmptyCansSource() }
                .sumOf { it.qty }
        } catch (e: Exception) { 0 }
    }

    // ── Empty-cases baseline (live "Empty Cases" figure) ──────────────────────

    /** Global baseline persisted by the admin that the live "Empty Cases"
     *  figure is reported relative to.
     *
     *  Returns `-1` when the value cannot be read (e.g. the `app_settings`
     *  tenant migration is not applied) so callers keep the last-known baseline
     *  instead of resetting it to 0. Returns `0` only when readable with no
     *  baseline stored. */
    suspend fun getEmptyCansBaseline(): Int {
        return try {
            val uid = currentUserId() ?: return -1
            val adminId = getAdminIdForUser(uid) ?: return -1
            val row = supabase.from("app_settings")
                .select {
                    filter {
                        eq("admin_id", adminId)
                        eq("settings_key", "empty_cans_baseline")
                    }
                }
                .decodeList<AppSetting>()
                .firstOrNull()
            row?.value?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            // Fall back to the service-role client so an admin reset is still
            // reflected even when the tenant isolation policy blocks the auth
            // session's read of the admin's app_settings row.
            e.printStackTrace()
            try {
                val uid = currentUserId() ?: return -1
                val adminId = getAdminIdForUser(uid) ?: return -1
                initAdminSession()
                val row = supabaseAdmin.from("app_settings")
                    .select {
                        filter {
                            eq("admin_id", adminId)
                            eq("settings_key", "empty_cans_baseline")
                        }
                    }
                    .decodeList<AppSetting>()
                    .firstOrNull()
                row?.value?.toIntOrNull() ?: 0
            } catch (e2: Exception) {
                e2.printStackTrace()
                -1
            }
        }
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

    /**
     * Active customer-specific selling prices for one customer, keyed by
     * product id. Empty when none are set — callers fall back to the
     * product defaults.
     */
    suspend fun getCustomPricesForCustomer(customerId: String): Map<String, Double> {
        if (customerId.isBlank()) return emptyMap()
        return try {
            val uid = currentUserId()
            val adminId = uid?.let { getAdminIdForUser(it) }
            supabase.from("customer_product_prices").select {
                filter { eq("customer_id", customerId) }
                filter { eq("is_active", true) }
                if (adminId != null) filter { eq("admin_id", adminId) }
            }.decodeList<CustomerProductPrice>()
                .associate { it.productId to it.sellingPrice }
        } catch (e: Exception) { emptyMap() }
    }

    suspend fun addCustomer(customer: Customer): Customer {
        // Tenant stamp so the admin's customer list/graph includes
        // employee-created customers (admin reads filter by admin_id).
        val tenantAdminId = currentUserId()?.let { getAdminIdForUser(it) }
        suspend fun insertCustomerInternal(client: io.github.jan.supabase.SupabaseClient): Customer {
            return try {
                client.from("customers").insert(
                    buildJsonObject {
                        put("name", customer.name)
                        if (customer.phone != null) put("phone", customer.phone)
                        if (customer.address != null) put("address", customer.address)
                        if (customer.otherDetails != null) put("other_details", customer.otherDetails)
                        if (!tenantAdminId.isNullOrBlank()) put("admin_id", tenantAdminId)
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
                            if (!tenantAdminId.isNullOrBlank()) put("admin_id", tenantAdminId)
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
                // This business always has exactly two shops. Guard against
                // legacy/duplicate rows so no screen ever shows a third shop.
                .take(2)
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
                    // Without an explicit cap PostgREST silently returns only
                    // 1000 rows, truncating the shop-wide stock / empty-cans
                    // aggregates once the movement log grows past that.
                    limit(100000)
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
        productName: String? = null,
    ) {
        com.example.ruwia.util.requireNotFutureTimestamp("Movement timestamp", createdAt)
        val employeeId = currentUserId()
        val tenantAdminId = employeeId?.let { getAdminIdForUser(it) }
        // Snapshot the product name when the caller didn't pass one so history
        // survives soft-delete (see resolveMovementProductName).
        val snapshotName = productName?.takeIf { it.isNotBlank() } ?: productId?.let { pid ->
            try {
                supabase.from("product_categories")
                    .select { filter { eq("id", pid) } }
                    .decodeSingleOrNull<ProductCategory>()
                    ?.let { (if (it.displayName.isNotBlank()) it.displayName else it.name).trim().takeIf { n -> n.isNotEmpty() } }
            } catch (_: Exception) { null }
        }
        
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

        // 2. Insert stock movement row (legacy retry when the product_name
        // migration has not run yet on this database).
        fun movementPayload(stripSnapshot: Boolean) = buildJsonObject {
            put("source", source)
            put("qty", qty)
            put("type", type)
            put("shop_name", shopName)
            if (productId != null) put("product_id", productId)
            if (!stripSnapshot && snapshotName != null) put("product_name", snapshotName)
            if (employeeId != null) put("employee_id", employeeId)
            if (!tenantAdminId.isNullOrBlank()) put("admin_id", tenantAdminId)
            if (createdAt != null) put("created_at", createdAt)
        }
        try {
            supabase.from("stock_movements").insert(movementPayload(false))
        } catch (e: Exception) {
            if (snapshotName != null && isUnknownColumnError(e, "product_name")) {
                supabase.from("stock_movements").insert(movementPayload(true))
            } else throw e
        }
    }

    // ── Record outward sale (one row per product line item) ──────────────────

    /**
     * Records a manual inward "Empty cans" movement (product-less) so the live
     * Empty Cases figure can be bumped directly from the home screen.
     */
    suspend fun addEmptyCases(qty: Int, shopName: String) {
        if (qty <= 0) return
        addStockMovement(
            source = "Empty cans · Direct Entry",
            qty = qty,
            type = "inward",
            shopName = shopName,
            productId = null,
            createdAt = com.example.ruwia.util.currentDateTimeIso(),
        )
    }

    /**
     * Persist an outward sale for the current employee. Each [SaleLine] becomes
     * a row in `sale_entries` and an `outward` stock movement so the admin's
     * stock dashboard reflects the sale immediately.
     *
     * If [emptyCansCollected] is non-zero, an extra `inward` stock movement is
     * recorded with `source = "Empty cans · {customer}"` so the admin can see
     * which customers returned how many empties.
     */
    /**
     * Persist an outward sale for the current employee. Each [SaleLine] becomes
     * a row in `sale_entries` plus outward stock movement(s) so the admin's
     * stock dashboard reflects the sale immediately. A line is deducted from
     * the sale shop's bucket first, spilling to stock-holding buckets when
     * the sale shop is short (so the business-wide total always drops).
     *
     * Consistency contract (never a recorded sale with silently missing
     * inventory):
     * - Step 1 decrements every line's stock first; any failure throws before
     *   anything is recorded.
     * - Step 2 writes sale + movement rows stamped with [clientSaleKey]. Lines
     *   already present for the key are skipped, so a retried/double-tapped
     *   submission can never deduct stock twice.
     * - Step 3 verifies every line's movement landed; a mismatch throws an
     *   explicit error instead of looking like a success.
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
        clientSaleKey: String? = null,
    ) {
        require(lines.isNotEmpty()) { "Sale has no line items." }
        val employeeId = currentUserId()
        val effectiveDate = saleDate?.takeIf { it.isNotBlank() } ?: today()
        com.example.ruwia.util.requireNotFutureDay("Sale date", effectiveDate)
        // Tenant stamp: admin screens filter every list by admin_id, so rows
        // written without it are invisible to the admin (empty analytics,
        // missing customers, frozen stock). Same pattern as insertCategory.
        val tenantAdminId = employeeId?.let { getAdminIdForUser(it) }
        val saleKey = clientSaleKey?.takeIf { it.isNotBlank() }

        // 0. Idempotency: lines already recorded for this key are done — skip
        // them so retries resume instead of duplicating. On databases where
        // the client_key migration has not run yet, fall back to legacy
        // payloads without keys (verification is skipped there too).
        var useClientKey = saleKey != null
        val keyForLookup = saleKey
        val pendingLines = if (!useClientKey || keyForLookup == null) {
            useClientKey = false
            lines
        } else {
            try {
                val recordedProductIds = supabase.from("sale_entries")
                    .select { filter { eq("client_key", keyForLookup) } }
                    .decodeList<com.example.ruwia.domain.SaleEntry>()
                    .map { it.productId }
                    .toSet()
                lines.filter { it.productId !in recordedProductIds }
            } catch (e: Exception) {
                if (isUnknownColumnError(e, "client_key")) {
                    useClientKey = false
                    lines
                } else throw e
            }
        }
        // Idempotent resume: the sale rows already exist (first attempt wrote
        // them but the process died before all movements landed). Backfill any
        // missing outward movements WITHOUT touching stock again — stock was
        // already decremented on the first attempt, and re-decrementing would
        // double-deduct. Without this, a sale interrupted between the
        // sale_entries insert and the movement insert stays recorded forever
        // with frozen inventory, and every retry no-ops.
        if (pendingLines.isEmpty()) {
            if (useClientKey && keyForLookup != null) {
                backfillMissingMovements(
                    saleKey = keyForLookup,
                    shopName = shopName,
                    lines = lines,
                    emptyCansCollected = emptyCansCollected,
                    employeeId = employeeId,
                    tenantAdminId = tenantAdminId,
                )
            }
            return
        }

        // 1. Perform atomic stock update for all lines first to ensure atomic check & deduction.
        var lastError: Exception? = null
        pendingLines.forEach { line ->
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

        // 2. Persist the sales entries and shop-wise outward movements.
        //
        // Allocation matters: the screen validates against the business-wide
        // total, so each line is deducted first from the sale shop's own
        // bucket with any remainder spilling to the buckets that actually
        // hold this product's stock. Writing the whole line to a single
        // empty bucket would let the per-bucket floor silently absorb it and
        // leave All Shops frozen — the "sale didn't reduce stock" corruption.
        // Split legs share one client_key, so verification still works.
        val ledgerForAllocation = fetchLedgerForAllocation(tenantAdminId)
        pendingLines.forEach { line ->
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
                    put("shop_id", shopName)
                    if (employeeId != null) put("employee_id", employeeId)
                    if (!tenantAdminId.isNullOrBlank()) put("admin_id", tenantAdminId)
                    if (useClientKey && saleKey != null) put("client_key", saleKey)
                }
            )

            allocateOutwardMovements(
                customerName = customerName,
                shopName = shopName,
                line = line,
                ledger = ledgerForAllocation,
                employeeId = employeeId,
                tenantAdminId = tenantAdminId,
                saleKey = if (useClientKey) saleKey else null,
            )
        }

        // 3. Empties picked up at delivery time → inward movement.
        if (emptyCansCollected > 0) {
            fun emptiesPayload(stripKey: Boolean) = buildJsonObject {
                put("source", "Empty cans · $customerName")
                put("qty", emptyCansCollected)
                put("type", "inward")
                put("shop_name", shopName)
                if (employeeId != null) put("employee_id", employeeId)
                if (!tenantAdminId.isNullOrBlank()) put("admin_id", tenantAdminId)
                if (!stripKey && useClientKey && saleKey != null) put("client_key", saleKey)
            }
            try {
                supabase.from("stock_movements").insert(emptiesPayload(false))
            } catch (e: Exception) {
                // Pre-migration database without the client_key column: retry
                // without it rather than failing the whole sale after the
                // sale_entries + outward movements already landed.
                if (isUnknownColumnError(e, "client_key")) {
                    supabase.from("stock_movements").insert(emptiesPayload(true))
                } else throw e
            }
        }

        // 4. Verify every line's outward movement landed. A sale that exists
        // without its inventory movement is the corruption this guards: fail
        // loudly instead of reporting success with frozen stock.
        // (Skipped on pre-migration databases without the client_key column.)
        if (useClientKey && saleKey != null) {
            val landed = try {
                supabase.from("stock_movements")
                    .select { filter { eq("client_key", saleKey) } }
                    .decodeList<StockMovement>()
                    .filter { it.type == "outward" }
                    .map { it.productId }
                    .toSet()
            } catch (e: Exception) {
                null
            }
            val missing = pendingLines.map { it.productId }.filter { it !in (landed ?: emptySet()) }
            if (landed == null || missing.isNotEmpty()) {
                throw IllegalStateException(
                    "Sale was recorded but inventory update could not be verified for ${missing.size} line(s). " +
                        "Please check stock before retrying — retrying is safe and will not deduct twice."
                )
            }
        }
    }

    /**
     * Reads the tenant's movement ledger once so a sale line's outward qty
     * can be allocated across the shop buckets holding that product's stock.
     * Returns empty on failure — allocation then degrades to writing the
     * whole line to the sale shop (previous behaviour, never a crash).
     */
    private suspend fun fetchLedgerForAllocation(tenantAdminId: String?): List<StockMovement> {
        return try {
            supabase.from("stock_movements").select {
                if (tenantAdminId != null) filter { eq("admin_id", tenantAdminId) }
                limit(100000)
            }.decodeList()
        } catch (_: Exception) { emptyList() }
    }

    /**
     * Writes a sale line's outward movement(s), deducting first from the
     * sale shop's own bucket and spilling any remainder to the other buckets
     * holding this product's stock (largest first, orphan labels included).
     * Zero-qty legs are skipped; if the ledger is short of the validated qty
     * (stale read), the remainder is parked on the sale shop so every unit
     * is accounted for in movements.
     */
    private suspend fun allocateOutwardMovements(
        customerName: String,
        shopName: String,
        line: SaleLine,
        ledger: List<StockMovement>,
        employeeId: String?,
        tenantAdminId: String?,
        saleKey: String?,
    ) {
        fun bucketLabel(bucketKey: String): String =
            if (bucketKey == shopMatchKey(shopName)) shopName
            else ledger.firstOrNull { shopMatchKey(it.shopName) == bucketKey }?.shopName ?: shopName

        fun bucketStock(bucketKey: String): Int =
            netStockPerProductInShop(ledger, bucketKey)[line.productId] ?: 0

        val ownKey = shopMatchKey(shopName)
        var remaining = line.qty
        // Own shop first (skipped when empty — the spill below covers it).
        val ownTake = minOf(remaining, bucketStock(ownKey).coerceAtLeast(0))
        if (ownTake > 0) {
            insertSaleOutwardMovement(
                customerName = customerName,
                shopName = bucketLabel(ownKey),
                line = line.copy(qty = ownTake),
                employeeId = employeeId,
                tenantAdminId = tenantAdminId,
                saleKey = saleKey,
            )
            remaining -= ownTake
        }
        if (remaining > 0) {
            val others = ledger.map { shopMatchKey(it.shopName) }.distinct()
                .filter { it != ownKey }
                .sortedByDescending { bucketStock(it) }
            for (bucketKey in others) {
                if (remaining <= 0) break
                val take = minOf(remaining, bucketStock(bucketKey).coerceAtLeast(0))
                if (take <= 0) continue
                insertSaleOutwardMovement(
                    customerName = customerName,
                    shopName = bucketLabel(bucketKey),
                    line = line.copy(qty = take),
                    employeeId = employeeId,
                    tenantAdminId = tenantAdminId,
                    saleKey = saleKey,
                )
                remaining -= take
            }
        }
        if (remaining > 0) {
            insertSaleOutwardMovement(
                customerName = customerName,
                shopName = shopName,
                line = line.copy(qty = remaining),
                employeeId = employeeId,
                tenantAdminId = tenantAdminId,
                saleKey = saleKey,
            )
        }
    }

    /**
     * Inserts one outward `stock_movements` row for a sale line, with legacy
     * retry when the product_name/client_key migrations have not run yet.
     */
    private suspend fun insertSaleOutwardMovement(
        customerName: String,
        shopName: String,
        line: SaleLine,
        employeeId: String?,
        tenantAdminId: String?,
        saleKey: String?,
    ) {
        fun saleMovementPayload(stripNewColumns: Boolean) = buildJsonObject {
            put("source", "Sale · $customerName")
            put("qty", line.qty)
            put("type", "outward")
            put("shop_name", shopName)
            put("product_id", line.productId)
            if (!stripNewColumns) {
                put("product_name", line.productName)
                if (saleKey != null) put("client_key", saleKey)
            }
            if (employeeId != null) put("employee_id", employeeId)
            if (!tenantAdminId.isNullOrBlank()) put("admin_id", tenantAdminId)
        }
        try {
            supabase.from("stock_movements").insert(saleMovementPayload(false))
        } catch (e: Exception) {
            if (isUnknownColumnError(e, "product_name") || isUnknownColumnError(e, "client_key")) {
                supabase.from("stock_movements").insert(saleMovementPayload(true))
            } else throw e
        }
    }

    /**
     * Repairs a sale that was recorded but lost some/all of its inventory
     * movements (crash or network drop between the sale_entries insert and
     * the movement inserts). Re-inserts only the missing outward movements
     * plus the empties movement when absent — stock is NOT decremented here
     * because the first attempt already did that.
     */
    private suspend fun backfillMissingMovements(
        saleKey: String,
        shopName: String,
        lines: List<SaleLine>,
        emptyCansCollected: Int,
        employeeId: String?,
        tenantAdminId: String?,
    ) {
        val existing = try {
            supabase.from("stock_movements")
                .select { filter { eq("client_key", saleKey) } }
                .decodeList<StockMovement>()
        } catch (e: Exception) {
            // Pre-migration DB without client_key: nothing to verify/repair.
            return
        }
        // Customer name is embedded in the movement source; recover it from
        // any already-landed row so the backfilled rows match the originals.
        val customerName = existing.firstOrNull()
            ?.source?.substringAfter("·")?.trim()?.takeIf { it.isNotEmpty() }
            ?: return
        val landedOutward = existing
            .filter { it.type == "outward" }
            .map { it.productId }
            .toSet()
        val missing = lines.filter { it.productId !in landedOutward }
        if (missing.isNotEmpty()) {
            // Same shop-wise allocation as the live path (own shop first,
            // spillover to holding buckets) so the backfilled deduction
            // actually reduces the total instead of flooring away.
            val ledger = fetchLedgerForAllocation(tenantAdminId)
            missing.forEach { line ->
                allocateOutwardMovements(
                    customerName = customerName,
                    shopName = shopName,
                    line = line,
                    ledger = ledger,
                    employeeId = employeeId,
                    tenantAdminId = tenantAdminId,
                    saleKey = saleKey,
                )
            }
        }
        if (emptyCansCollected > 0) {
            val emptiesLanded = existing.any {
                it.type == "inward" && it.source.isEmptyCansSource()
            }
            if (!emptiesLanded) {
                supabase.from("stock_movements").insert(
                    buildJsonObject {
                        put("source", "Empty cans · $customerName")
                        put("qty", emptyCansCollected)
                        put("type", "inward")
                        put("shop_name", shopName)
                        if (employeeId != null) put("employee_id", employeeId)
                        if (!tenantAdminId.isNullOrBlank()) put("admin_id", tenantAdminId)
                        put("client_key", saleKey)
                    }
                )
            }
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
        return try {
            insertCategory(cat, tenantAdminId, includeAlert = true)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (!msg.contains("column \"low_stock_alert\" does not exist", ignoreCase = true)) throw e
            // Backend not migrated yet — save without the threshold (defaults to 5).
            insertCategory(cat, tenantAdminId, includeAlert = false)
        }
    }

    private suspend fun insertCategory(
        cat: ProductCategory,
        tenantAdminId: String,
        includeAlert: Boolean,
    ): ProductCategory {
        return supabase.from("product_categories").insert(
            buildJsonObject {
                put("name", cat.name.trim())
                put("display_name", cat.displayName.trim())
                put("brand_name", cat.brandName.trim())
                put("purchase_price", cat.purchasePrice)
                put("default_sell_price", cat.defaultSellPrice)
                put("stock_available", cat.stockAvailable)
                if (includeAlert) put("low_stock_alert", cat.lowStockAlert)
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
