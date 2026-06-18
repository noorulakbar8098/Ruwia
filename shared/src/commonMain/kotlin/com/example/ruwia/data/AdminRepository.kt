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
                .decodeList<ProductCategory>()
                // Deduplicate by name (case-insensitive) — keeps the first occurrence.
                // This guards against accidental duplicate rows that may already exist in the DB.
                .distinctBy { it.name.trim().lowercase() }
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
        // Guard: reject if a product with the same name already exists (active or inactive).
        val existing = try {
            supabase.from("product_categories")
                .select { filter { ilike("name", cat.name.trim()) } }
                .decodeList<ProductCategory>()
        } catch (_: Exception) { emptyList() }

        if (existing.isNotEmpty()) {
            throw IllegalStateException(
                "A product named \"${cat.name.trim()}\" already exists. Please use a different SKU name."
            )
        }

        supabase.from("product_categories").insert(
            buildJsonObject {
                put("name", cat.name.trim())
                put("display_name", cat.displayName.trim())
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

    /**
     * Persist this admin's monthly expense breakdown.
     *
     * The `monthly_expenses` table has a `UNIQUE (month, shop_id)` constraint
     * but `upsert(expense)` defaults to conflict-on-PRIMARY-KEY (`id`). Since
     * the in-memory [MonthlyExpense] always carries `id = null`, the first
     * save inserts cleanly but every subsequent save also tries to INSERT and
     * trips the UNIQUE constraint — which is why edits stopped persisting on
     * the second tap. We work around this by fetching the existing row's id
     * first, then explicitly UPDATEing (or INSERTing if no row exists yet).
     */
    suspend fun saveMonthlyExpense(expense: MonthlyExpense) {
        val month  = expense.month
        val shopId = expense.shopId.ifBlank { "shop1" }

        val existing = try {
            supabase.from("monthly_expenses")
                .select {
                    filter {
                        eq("month", month)
                        eq("shop_id", shopId)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<MonthlyExpense>()
        } catch (_: Exception) { null }

        val payload = buildJsonObject {
            put("month",          month)
            put("shop_id",        shopId)
            put("shop_rent",      expense.shopRent)
            put("admin_salary",   expense.adminSalary)
            put("delivery_staff", expense.deliveryStaff)
            put("miscellaneous",  expense.miscellaneous)
            put("bike_expense",   expense.bikeExpense)
        }

        if (existing?.id != null) {
            supabase.from("monthly_expenses").update(payload) {
                filter { eq("id", existing.id) }
            }
        } else {
            supabase.from("monthly_expenses").insert(payload)
        }
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
        // We deliberately do NOT swallow exceptions here. Earlier we returned
        // a fake local copy with `id = "local_..."` on failure, which made the
        // ViewModel believe the insert succeeded and add the customer to its
        // in-memory list. The customer would appear briefly in the picker,
        // never make it to the DB, and silently vanish on the next refresh —
        // the exact "customers don't show up across apps" bug.
        return supabase.from("customers").insert(
            buildJsonObject {
                put("name", customer.name)
                if (customer.phone != null) put("phone", customer.phone)
                if (customer.address != null) put("address", customer.address)
                if (customer.otherDetails != null) put("other_details", customer.otherDetails)
            }
        ) { select() }.decodeSingle()
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
            supabase.from("shop_stocks").select()
                .decodeList<ShopStockInfo>()
                // Deduplicate by name (case-insensitive) — keeps the first occurrence.
                // Guards against accidental duplicate rows already in the DB.
                .distinctBy { it.name.trim().lowercase() }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getRecentMovements(): List<StockMovement> {
        return try {
            // 1000 rows so we have at least the last ~3 months for dashboard
            // aggregations (inward / outward totals, month-over-month deltas).
            // RLS on `stock_movements` exposes every employee's record to the
            // admin, so this is the cross-team view they expect.
            supabase.from("stock_movements")
                .select { order("created_at", SortOrder.DESCENDING); limit(1000) }
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
        // 1. Persist the movement row itself.
        supabase.from("stock_movements").insert(
            buildJsonObject {
                put("source", source)
                put("qty", qty)
                put("type", type)
                put("shop_name", shopName)
                if (productId != null) put("product_id", productId)
            }
        )

        // 2. Reflect the movement in the product's running stock count so the
        //    Products / Stock dashboards update in real time. Without this, the
        //    `stock_movements` table grows but `product_categories.stock_available`
        //    never changes — and every UI that reads stock_available stays stuck
        //    at the original number.
        if (productId != null && productId.isNotBlank()) {
            runCatching {
                val current = supabase.from("product_categories")
                    .select { filter { eq("id", productId) } }
                    .decodeSingleOrNull<ProductCategory>() ?: return@runCatching
                val delta = when (type) {
                    "inward"     ->  qty
                    "outward"    -> -qty
                    "adjustment" ->  qty
                    else         ->  0
                }
                val newStock = (current.stockAvailable + delta).coerceAtLeast(0)
                supabase.from("product_categories").update(
                    buildJsonObject { put("stock_available", newStock) },
                ) { filter { eq("id", productId) } }
            }
        }
    }

    // ── Suppliers ─────────────────────────────────────────────────────────────

    suspend fun getSuppliers(): List<String> {
        return try {
            supabase.from("suppliers")
                .select { filter { eq("is_active", true) } }
                .decodeList<Supplier>()
                .map { s -> if (s.location != null) "${s.name}  ·  ${s.location}" else s.name }
                // De-duplicate by formatted display string — historical inserts
                // before the unique-constraint was added can leave duplicate
                // rows that would otherwise show up twice in the picker.
                .distinct()
        } catch (e: Exception) {
            listOf(
                "Global Creators  ·  Tiru",
                "Multi Brands  ·  Coimbatore",
                "Aqua Pure Plant  ·  Tiru",
            )
        }
    }

    /** Returns the full supplier rows (including IDs) for management screens. */
    suspend fun getSuppliersFull(): List<Supplier> {
        return try {
            supabase.from("suppliers")
                .select { filter { eq("is_active", true) } }
                .decodeList<Supplier>()
                // De-duplicate by (name, location) so legacy duplicate rows
                // collapse into a single entry in the management UI.
                .distinctBy { "${it.name.trim().lowercase()}|${it.location?.trim()?.lowercase() ?: ""}" }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addSupplier(name: String, location: String?) {
        if (name.isBlank()) return
        // Check whether a row with the same (name, location) already exists.
        // We do this client-side because the suppliers table may not have a
        // unique constraint yet, and silently inserting a duplicate is exactly
        // the bug that caused supplier names to appear twice in pickers.
        val existing = runCatching { getSuppliersFull() }.getOrDefault(emptyList())
        val matches = existing.any {
            it.name.trim().equals(name.trim(), ignoreCase = true) &&
            (it.location?.trim() ?: "").equals((location?.trim() ?: ""), ignoreCase = true)
        }
        if (matches) return

        supabase.from("suppliers").insert(
            buildJsonObject {
                put("name", name.trim())
                if (!location.isNullOrBlank()) put("location", location.trim())
                put("is_active", true)
            }
        )
    }

    suspend fun deleteSupplier(id: String) {
        if (id.isBlank()) {
            throw IllegalArgumentException("Supplier id is blank — cannot delete.")
        }
        // Soft-delete so historical references in stock_movements still resolve.
        // Use `select()` so the response carries back whatever rows were
        // actually updated. If RLS blocks the update or the id doesn't match
        // any row, the response is empty and we surface that as an error
        // instead of silently succeeding (which is what hid the original bug).
        val updated = supabase.from("suppliers").update(
            buildJsonObject { put("is_active", false) }
        ) {
            filter { eq("id", id) }
            select()
        }.decodeList<Supplier>()

        if (updated.isEmpty()) {
            throw IllegalStateException(
                "Supplier could not be deleted. The row may not exist, or your " +
                "account does not have permission to update suppliers."
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
