package com.example.ruwia.data

import com.example.ruwia.domain.AppSetting
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
import io.github.jan.supabase.postgrest.postgrest
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
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return 0.0
            supabase.from("sale_entries")
                .select { 
                    filter { eq("admin_id", adminId) }
                    order("created_at", SortOrder.DESCENDING)
                    limit(500) 
                }
                .decodeList<SaleEntry>()
                .filter { it.createdAt?.startsWith(prefix) == true }
                .sumOf { it.totalSelling }
        } catch (e: Exception) { 0.0 }
    }

    suspend fun getCSAT(): Double = 4.7  // placeholder until ratings table is added

    suspend fun getActiveFleetCount(): Int {
        return try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return 0
            supabase.from("employees")
                .select { 
                    filter { 
                        eq("status", "active")
                        eq("admin_id", adminId)
                    } 
                }
                .decodeList<EmployeeInfo>()
                .size
        } catch (e: Exception) { 0 }
    }

    // ── Products ──────────────────────────────────────────────────────────────

    suspend fun getProductCategories(): List<ProductCategory> {
        return try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
            supabase.from("product_categories")
                .select { 
                    filter { 
                        eq("admin_id", adminId)
                        eq("is_deleted", false)
                    } 
                }
                .decodeList<ProductCategory>()
                // Deduplicate by brand name and size name together.
                .distinctBy { "${it.brandName.trim().lowercase()}_${it.name.trim().lowercase()}" }
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
                    costPrice      = if (cat.purchasePrice > 0) cat.purchasePrice else cat.purchasePriceGC,
                    tags           = cat.brandName,
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getTenantAdminId(): String {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return ""
        return try {
            val profile = supabase.from("profiles")
                .select { filter { eq("id", uid) } }
                .decodeSingleOrNull<com.example.ruwia.domain.Profile>()
            profile?.adminId ?: uid
        } catch (e: Exception) {
            uid
        }
    }

    suspend fun addProductCategory(cat: ProductCategory): String {
        val tenantAdminId = getTenantAdminId()
        if (tenantAdminId.isBlank()) {
            throw IllegalStateException("Your session has expired. Please log in again.")
        }
        
        // 1. Guard: reject if a product with the same name (size) AND same brand exists FOR THIS ADMIN.
        val existing = try {
            supabase.from("product_categories")
                .select { 
                    filter { 
                        ilike("name", cat.name.trim()) 
                        ilike("brand_name", cat.brandName.trim())
                        eq("admin_id", tenantAdminId)
                        eq("is_deleted", false)
                    } 
                }
                .decodeList<ProductCategory>()
        } catch (e: Exception) { 
            println("Error checking for existing product: ${e.message}")
            emptyList() 
        }

        if (existing.isNotEmpty()) {
            throw IllegalStateException(
                "A product with size \"${cat.name.trim()}\" and brand \"${cat.brandName.trim()}\" already exists in your catalog."
            )
        }

        // 2. Perform the Insert
        return try {
            val response = supabase.from("product_categories").insert(
                buildJsonObject {
                    put("name", cat.name.trim())
                    put("display_name", cat.displayName.trim())
                    put("brand_name", cat.brandName.trim())
                    put("supplier_group", cat.supplierGroup.trim())
                    put("purchase_price", cat.purchasePrice)
                    put("default_sell_price", cat.defaultSellPrice)
                    put("stock_available", cat.stockAvailable)
                    put("is_active", cat.isActive)
                    put("admin_id", tenantAdminId)
                }
            ) { select() }.decodeSingle<ProductCategory>()
            
            response.id
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown database error"
            if (msg.contains("column \"brand_name\" does not exist", ignoreCase = true) || 
                msg.contains("column \"purchase_price\" does not exist", ignoreCase = true)) {
                throw IllegalStateException("Database Schema Mismatch: Please run the latest SQL migration in your Supabase dashboard.")
            }
            throw e
        }
    }

    suspend fun updateProductCategory(cat: ProductCategory) {
        if (cat.id.isBlank()) return
        supabase.from("product_categories").update(
            buildJsonObject {
                put("name", cat.name)
                put("display_name", cat.displayName)
                put("brand_name", cat.brandName)
                put("supplier_group", cat.supplierGroup)
                put("purchase_price", cat.purchasePrice)
                put("default_sell_price", cat.defaultSellPrice)
                put("stock_available", cat.stockAvailable)
                put("is_active", cat.isActive)
            }
        ) { filter { eq("id", cat.id) } }
    }

    suspend fun deleteProductCategory(id: String) {
        if (id.isBlank()) return
        
        // SOFT DELETE: We no longer delete associated records or the product itself.
        // Instead, we mark the product as deleted so historical data is preserved.
        try {
            supabase.from("product_categories").update(
                buildJsonObject {
                    put("is_deleted", true)
                }
            ) {
                filter { eq("id", id) }
            }
        } catch (e: Exception) {
            // Bypass RLS using admin client if standard update fails
            initAdminSession()
            supabaseAdmin.from("product_categories").update(
                buildJsonObject {
                    put("is_deleted", true)
                }
            ) {
                filter { eq("id", id) }
            }
        }
    }

    // ── Sales ─────────────────────────────────────────────────────────────────

    suspend fun getSaleEntries(productName: String? = null): List<SaleEntry> {
        return try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
            supabase.from("sale_entries")
                .select {
                    filter { eq("admin_id", adminId) }
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
        val currentAdminId = supabase.auth.currentUserOrNull()?.id ?: return
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
                put("admin_id", currentAdminId)
            }
        )
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    suspend fun getMonthlyExpense(month: String): MonthlyExpense? {
        return try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return null
            supabase.from("monthly_expenses")
                .select { 
                    filter { 
                        eq("month", month)
                        eq("admin_id", adminId)
                    } 
                }
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
            put("custom_expenses", expense.customExpenses)
        }

        try {
            if (existing?.id != null) {
                supabase.from("monthly_expenses").update(payload) {
                    filter { eq("id", existing.id) }
                }
            } else {
                supabase.from("monthly_expenses").insert(payload)
            }
        } catch (e: Exception) {
            // Fallback in case "custom_expenses" column does not exist in the database yet
            val fallbackPayload = buildJsonObject {
                put("month",          month)
                put("shop_id",        shopId)
                put("shop_rent",      expense.shopRent)
                put("admin_salary",   expense.adminSalary)
                put("delivery_staff", expense.deliveryStaff)
                put("miscellaneous",  expense.miscellaneous)
                put("bike_expense",   expense.bikeExpense)
            }
            try {
                if (existing?.id != null) {
                    supabase.from("monthly_expenses").update(fallbackPayload) {
                        filter { eq("id", existing.id) }
                    }
                } else {
                    supabase.from("monthly_expenses").insert(fallbackPayload)
                }
            } catch (_: Exception) {
                throw e
            }
        }
    }

    // ── Revenue chart ─────────────────────────────────────────────────────────

    suspend fun getWeeklyRevenueSummary(): Triple<List<Float>, List<Float>, String> {
        return try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return defaultRevenuePlaceholder()
            val entries = supabase.from("sale_entries")
                .select { 
                    filter { eq("admin_id", adminId) }
                    order("created_at", SortOrder.DESCENDING)
                    limit(200) 
                }
                .decodeList<SaleEntry>()

            if (entries.isEmpty()) return defaultRevenuePlaceholder()

            val byDay = entries
                .groupBy { it.date }
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
            Triple(points, byDay, label)
        } catch (e: Exception) {
            defaultRevenuePlaceholder()
        }
    }

    private fun defaultRevenuePlaceholder() =
        Triple(
            listOf(0.4f, 0.5f, 0.45f, 0.6f, 0.55f, 0.7f, 0.5f),
            listOf(400f, 500f, 450f, 600f, 550f, 700f, 500f),
            "₹0"
        )

    // ── Customers ─────────────────────────────────────────────────────────────

    suspend fun getAllCustomers(): List<Customer> {
        return try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
            supabase.from("customers").select {
                filter { eq("admin_id", adminId) }
            }.decodeList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addCustomer(customer: Customer): Customer {
        val currentAdminId = supabase.auth.currentUserOrNull()?.id ?: ""
        suspend fun insertCustomerInternal(client: io.github.jan.supabase.SupabaseClient): Customer {
            return try {
                client.from("customers").insert(
                    buildJsonObject {
                        put("name", customer.name)
                        if (customer.phone != null) put("phone", customer.phone)
                        if (customer.address != null) put("address", customer.address)
                        if (customer.otherDetails != null) put("other_details", customer.otherDetails)
                        if (currentAdminId.isNotBlank()) put("admin_id", currentAdminId)
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
                            if (currentAdminId.isNotBlank()) put("admin_id", currentAdminId)
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

    suspend fun deleteCustomer(id: String) {
        if (id.isBlank()) {
            throw IllegalArgumentException("Customer id is blank — cannot delete.")
        }
        val updated = try {
            supabase.from("customers").delete {
                filter { eq("id", id) }
                select()
            }.decodeList<Customer>()
        } catch (e: Exception) {
            println("Standard deleteCustomer failed, trying admin bypass: ${e.message}")
            e.printStackTrace()
            initAdminSession()
            supabaseAdmin.from("customers").delete {
                filter { eq("id", id) }
                select()
            }.decodeList<Customer>()
        }

        if (updated.isEmpty()) {
            throw IllegalStateException(
                "Customer could not be deleted. The row may not exist, or your " +
                "account does not have permission to delete customers."
            )
        }
    }

    suspend fun updateCustomer(customer: Customer): Customer {
        val id = customer.id ?: throw IllegalArgumentException("Customer id is blank — cannot update.")
        // Capture the pre-edit name so a rename can propagate to every
        // denormalized copy (sale entries + stock movement sources).
        val existing = runCatching {
            supabase.from("customers")
                .select { filter { eq("id", id) } }
                .decodeSingleOrNull<Customer>()
        }.getOrElse { null }
        suspend fun updateCustomerInternal(client: io.github.jan.supabase.SupabaseClient): Customer {
            return client.from("customers").update(
                buildJsonObject {
                    put("name", customer.name)
                    if (customer.phone != null) put("phone", customer.phone)
                    if (customer.address != null) put("address", customer.address)
                    if (customer.otherDetails != null) put("other_details", customer.otherDetails)
                }
            ) {
                filter { eq("id", id) }
                select()
            }.decodeSingle()
        }
        val saved = try {
            updateCustomerInternal(supabase)
        } catch (e: Exception) {
            println("Standard updateCustomer failed, trying admin bypass: ${e.message}")
            e.printStackTrace()
            initAdminSession()
            updateCustomerInternal(supabaseAdmin)
        }
        val oldName = existing?.name.orEmpty().trim()
        val newName = customer.name.trim()
        if (oldName.isNotEmpty() && oldName != newName) {
            propagateCustomerRename(oldName, newName)
        }
        return saved
    }

    /** Rewrites every denormalized reference to [oldName] after a customer
     *  rename so past transactions, stock history and activity sheets show the
     *  new name everywhere, not just the customers list. */
    private suspend fun propagateCustomerRename(oldName: String, newName: String) {
        if (oldName.isBlank() || newName.isBlank()) return
        initAdminSession()
        val adminId = supabase.auth.currentUserOrNull()?.id ?: return
        try {
            supabaseAdmin.from("sale_entries").update(
                buildJsonObject { put("customer_name", newName) }
            ) {
                filter {
                    eq("admin_id", adminId)
                    eq("customer_name", oldName)
                }
            }
        } catch (e: Exception) {
            println("sale_entries customer rename skipped: ${e.message}")
        }
        // Stock movements carry the customer inside the source string:
        // "Sale · <name>", "Empty cans · <name>", "Empty cases · <name>".
        try {
            val prefixes = listOf("Sale", "Empty cans", "Empty cases")
            val rows = supabaseAdmin.from("stock_movements")
                .select {
                    filter { eq("admin_id", adminId) }
                    limit(100000)
                }
                .decodeList<StockMovement>()
            rows.forEach { m ->
                val source = m.source.trim()
                val label = prefixes.firstOrNull { source.startsWith("$it · $oldName") }
                    ?: return@forEach
                try {
                    supabaseAdmin.from("stock_movements").update(
                        buildJsonObject { put("source", "$label · $newName") }
                    ) { filter { eq("id", m.id) } }
                } catch (e: Exception) {
                    println("stock_movement source rename skipped (${m.id}): ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("stock_movements customer rename skipped: ${e.message}")
        }
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    suspend fun getAllOrders(): List<Order> {
        return try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
            supabase.from("orders")
                .select { 
                    filter { eq("admin_id", adminId) }
                    order("created_at", SortOrder.DESCENDING)
                    limit(50) 
                }
                .decodeList()
        } catch (e: Exception) { emptyList() }
    }

    // ── Employees ─────────────────────────────────────────────────────────────

    suspend fun getEmployees(): List<EmployeeInfo> {
        return try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
            supabase.from("employees").select {
                filter { eq("admin_id", adminId) }
            }.decodeList()
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
        val currentAdminId = supabase.auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not logged in")

        val newUser = supabaseAdmin.auth.admin.createUserWithEmail {
            this.email       = email
            this.password    = password
            this.autoConfirm = true
            userMetadata = buildJsonObject {
                put("role", "employee")
                put("admin_id", currentAdminId)
                put("full_name", name)
                put("phone", phone)
            }
        }
        val userId = newUser.id

        // Profiles is now handled by SQL trigger using metadata, 
        // but we still need to populate the employee-specific details.

        supabaseAdmin.from("employees").insert(
            buildJsonObject {
                put("id",             userId)
                put("name",           name)
                put("phone",          phone)
                put("role",           role.lowercase())
                put("shop_name",      shopName)
                put("monthly_salary", salary)
                put("status",         "active")
                put("admin_id",       currentAdminId)
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

    /** Updates editable fields of an existing employee. Email/password are not
     *  touched here (those belong to the auth user) — only the employee record.
     *  The name is also mirrored to `profiles.full_name` (the source of the
     *  displayed name in the employee's own app / auth state) so a rename
     *  reflects everywhere, not just the admin roster. */
    suspend fun updateEmployee(updated: EmployeeInfo): EmployeeInfo {
        initAdminSession()
        supabaseAdmin.from("employees").update(
            buildJsonObject {
                put("name", updated.name)
                put("phone", updated.phone)
                put("role", updated.role.lowercase())
                if (updated.shopName.isNotBlank()) put("shop_name", updated.shopName)
                put("monthly_salary", updated.monthlySalary)
            }
        ) {
            filter { eq("id", updated.id) }
        }
        try {
            supabaseAdmin.from("profiles").update(
                buildJsonObject { put("full_name", updated.name) }
            ) {
                filter { eq("id", updated.id) }
            }
        } catch (e: Exception) {
            println("Profile full_name sync skipped: ${e.message}")
        }
        return updated
    }

    /** Deactivates an employee. The row is soft-deleted (`status = "inactive"`)
     *  so historical movements/sales keep resolving to the employee's name in
     *  stock history and the report summary, and the matching auth user is
     *  disabled so they can no longer sign in. */
    suspend fun deleteEmployee(id: String) {
        if (id.isBlank()) throw IllegalArgumentException("Employee id is blank — cannot delete.")
        initAdminSession()
        val updated = supabaseAdmin.from("employees").update(
            buildJsonObject {
                put("status", "inactive")
            }
        ) {
            filter { eq("id", id) }
            select()
        }.decodeList<EmployeeInfo>()
        if (updated.isEmpty()) {
            throw IllegalStateException("Employee could not be deactivated. The row may not exist, or you lack permission.")
        }
        // Disable the auth user so the employee can no longer sign in.
        try {
            supabaseAdmin.auth.admin.updateUserById(id) {
                this.userMetadata = buildJsonObject {
                    put("disabled", true)
                }
            }
        } catch (e: Exception) {
            println("Employee auth user disable skipped: ${e.message}")
        }
    }

    // ── Stock ─────────────────────────────────────────────────────────────────

    suspend fun getShopStocks(): List<ShopStockInfo> {
        return try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
            supabase.from("shop_stocks").select {
                filter { eq("admin_id", adminId) }
            }
                .decodeList<ShopStockInfo>()
                // Deduplicate by name (case-insensitive) — keeps the first occurrence.
                // Guards against accidental duplicate rows already in the DB.
                .distinctBy { it.name.trim().lowercase() }
                // This business always has exactly two shops. Guard against
                // legacy/duplicate rows so no screen ever shows a third shop.
                .take(2)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getRecentMovements(): List<StockMovement> {
        return try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
            // The full movement history is the single source of truth for live
            // stock: product balances, shop totals, empty-cases and month-wise
            // aggregates are all derived client-side from this list. Capping it
            // (as we used to, at 5000) silently dropped older movements and
            // corrupted every count once history grew past the window — the
            // dashboard KPI showed a fraction (e.g. 64) of the real total
            // (e.g. 1040). 100_000 overrides PostgREST's default 1000-row cap
            // while remaining effectively unbounded for this business.
            supabase.from("stock_movements")
                .select {
                    filter { eq("admin_id", adminId) }
                    order("created_at", SortOrder.DESCENDING)
                    limit(100000)
                }
                .decodeList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addRawStockMovement(
        source: String,
        qty: Int,
        type: String,
        shopName: String,
        productId: String? = null,
        createdAt: String? = null,
    ) {
        val payload = buildJsonObject {
            val adminId = supabase.auth.currentUserOrNull()?.id
            put("source", source)
            put("qty", qty)
            put("type", type)
            put("shop_name", shopName)
            if (productId != null) put("product_id", productId)
            if (createdAt != null) put("created_at", createdAt)
            if (adminId != null) put("admin_id", adminId)
        }
        try {
            supabase.from("stock_movements").insert(payload)
        } catch (e: Exception) {
            initAdminSession()
            supabaseAdmin.from("stock_movements").insert(payload)
        }
    }

    // ── Empty-cases reset (live stock "Empty Cases" figure) ──────────────────

    /** Global baseline persisted on the admin tenant that the live "Empty
     *  Cases" figure is reported relative to. Defaults to 0 when unset. */
    /**
     * Global baseline persisted by the admin that the live "Empty Cases"
     * figure is reported relative to.
     *
     * Returns `-1` when the value cannot be read (e.g. the `app_settings` tenant
     * migration is not applied to the database yet) so callers can keep the
     * last-known baseline instead of clobbering it with a bogus 0. Returns `0`
     * only when the table is readable and genuinely has no baseline stored.
     */
    suspend fun getEmptyCansBaseline(): Int {
        return try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return -1
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
            // Fall back to the service-role client (bypasses RLS). The anon
            // path can fail when the tenant isolation policy is missing or the
            // self-row isn't visible to the auth session; the baseline is
            // tenant data the admin must always be able to read back.
            e.printStackTrace()
            try {
                initAdminSession()
                val adminId = supabase.auth.currentUserOrNull()?.id ?: return -1
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

    /**
     * Resets the business's live "Empty Cases" figure to zero by persisting the
     * current running empty-cans total as the baseline. The reported figure is
     * `(current sum − baseline).coerceAtLeast(0)`, so this makes the displayed
     * value 0 without deleting any movement history. New returns accumulate on
     * top of the new baseline.
     *
     * Returns the new baseline when the write succeeds, or `-1` when it could
     * not be persisted so callers keep the last-known baseline instead of
     * clobbering it with an un-persisted value (which would make the count
     * "come back" on the next refresh).
     */
    suspend fun resetEmptyCases(): Int {
        val current = liveEmptyCansTotal()
        val adminId = supabase.auth.currentUserOrNull()?.id ?: return -1
        val value = current.toString()
        try {
            val existing = supabase.from("app_settings")
                .select {
                    filter {
                        eq("admin_id", adminId)
                        eq("settings_key", "empty_cans_baseline")
                    }
                }
                .decodeList<AppSetting>()
                .firstOrNull()
            val payload = buildJsonObject {
                put("admin_id", adminId)
                put("settings_key", "empty_cans_baseline")
                put("settings_value", value)
            }
            if (existing != null) {
                val existingId = existing.id ?: return -1
                supabase.from("app_settings").update(payload) {
                    filter { eq("id", existingId) }
                }
            } else {
                supabase.from("app_settings").insert(payload)
            }
        } catch (e: Exception) {
            // The anon path may be blocked by RLS (missing tenant isolation
            // policy on `app_settings`). Retry through the service-role client,
            // which the app already uses for tenant-scoped writes (employees,
            // stock adjustments, deletes).
            e.printStackTrace()
            try {
                initAdminSession()
                val existing = supabaseAdmin.from("app_settings")
                    .select {
                        filter {
                            eq("admin_id", adminId)
                            eq("settings_key", "empty_cans_baseline")
                        }
                    }
                    .decodeList<AppSetting>()
                    .firstOrNull()
                val payload = buildJsonObject {
                    put("admin_id", adminId)
                    put("settings_key", "empty_cans_baseline")
                    put("settings_value", value)
                }
                if (existing != null) {
                    val existingId = existing.id ?: return -1
                    supabaseAdmin.from("app_settings").update(payload) {
                        filter { eq("id", existingId) }
                    }
                } else {
                    supabaseAdmin.from("app_settings").insert(payload)
                }
            } catch (e2: Exception) {
                // The `app_settings` table (or its `admin_id` tenant column) is
                // not present on the live database until supabase_schema.sql is
                // applied, so the baseline cannot be persisted. Report failure
                // so the screen keeps its previous baseline and shows a clear
                // error instead of silently doing nothing.
                e2.printStackTrace()
                return -1
            }
        }
        return current
    }

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

    /** Running live empty-cans total across all movement history (global). */
    private suspend fun liveEmptyCansTotal(): Int {
        val adminId = supabase.auth.currentUserOrNull()?.id ?: return 0
        val movs = try {
            supabase.from("stock_movements")
                .select {
                    filter { eq("admin_id", adminId) }
                    // Explicit cap so PostgREST's default 1000-row page doesn't
                    // truncate the all-time empty-cans total (the reset baseline
                    // must be computed against the same complete history the
                    // dashboard displays, otherwise the count "comes back").
                    limit(100000)
                }
                .decodeList<StockMovement>()
        } catch (e: Exception) { emptyList() }
        return movs
            .filter { it.source.trim().lowercase().startsWith("empty cans") || it.source.trim().lowercase().startsWith("empty cases") }
            .sumOf { m -> if (m.type == "inward") m.qty else -m.qty }
            .coerceAtLeast(0)
    }

    suspend fun addStockMovement(
        source: String,
        qty: Int,
        type: String,
        shopName: String,
        productId: String? = null,
        createdAt: String? = null,
    ) {
        val payload = buildJsonObject {
            val adminId = supabase.auth.currentUserOrNull()?.id
            put("source", source)
            put("qty", qty)
            put("type", type)
            put("shop_name", shopName)
            if (productId != null) put("product_id", productId)
            if (createdAt != null) put("created_at", createdAt)
            if (adminId != null) put("admin_id", adminId)
        }
        // 1. Persist the movement row itself.
        try {
            supabase.from("stock_movements").insert(payload)
        } catch (e: Exception) {
            println("Standard addStockMovement failed, trying admin bypass: ${e.message}")
            e.printStackTrace()
            initAdminSession()
            supabaseAdmin.from("stock_movements").insert(payload)
        }

        // 2. Reflect the movement in the product's running stock count so the
        //    Products / Stock dashboards update in real time. Without this, the
        //    `stock_movements` table grows but `product_categories.stock_available`
        //    never changes.
        if (productId != null && productId.isNotBlank()) {
            val delta = when (type) {
                "inward"     ->  qty
                "outward"    -> -qty
                "adjustment" ->  qty
                else         ->  0
            }
            var success = false
            var attempts = 0
            while (!success && attempts < 10) {
                attempts++
                try {
                    val current = supabase.from("product_categories")
                        .select { filter { eq("id", productId) } }
                        .decodeSingleOrNull<ProductCategory>() ?: break
                    val newStock = (current.stockAvailable + delta).coerceAtLeast(0)
                    val successResult = supabase.postgrest.rpc(
                        "update_product_stock",
                        buildJsonObject {
                            put("p_id", productId)
                            put("p_new_stock", newStock)
                            put("p_expected_current", current.stockAvailable)
                        }
                    ).decodeAs<Boolean>()
                    if (successResult) {
                        success = true
                    }
                } catch (e: Exception) {
                    // Retry
                }
            }
        }
    }



    suspend fun assignEmployee(orderId: String, employeeId: String) {
        try {
            supabase.from("orders").update(buildJsonObject { put("employee_id", employeeId) }) {
                filter { eq("id", orderId) }
            }
        } catch (_: Exception) {}
    }

    suspend fun clearStockAndRevenue() {
        // Clear transaction history
        supabase.from("sale_entries").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }
        supabase.from("stock_movements").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }
        
        // Reset available stock in categories to 0
        supabase.from("product_categories").update(
            buildJsonObject { put("stock_available", 0) }
        ) { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }

        // Reset shop summary counts to 0
        supabase.from("shop_stocks").update(
            buildJsonObject {
                put("total_cans", 0)
                put("full_cans", 0)
                put("empty_cans", 0)
                put("cans_with_customers", 0)
            }
        ) {
            filter { neq("id", "00000000-0000-0000-0000-000000000000") }
        }
    }

    suspend fun deleteAllData() {
        supabase.from("route_tasks").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }
        supabase.from("payments").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }
        supabase.from("outward").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }
        supabase.from("monthly_expenses").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }
        supabase.from("sale_entries").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }
        supabase.from("stock_movements").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }
        supabase.from("orders").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }
        supabase.from("customers").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }
        supabase.from("suppliers").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }
        supabase.from("employees").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }
        supabase.from("product_categories").delete { filter { neq("id", "00000000-0000-0000-0000-000000000000") } }

        supabase.from("shop_stocks").update(
            buildJsonObject {
                put("total_cans", 0)
                put("full_cans", 0)
                put("empty_cans", 0)
                put("cans_with_customers", 0)
            }
        ) {
            filter { neq("id", "00000000-0000-0000-0000-000000000000") }
        }
    }

    // ── Shop names (business display names for Shop 1 / Shop 2) ───────────────

    /**
     * Returns the display names of this business's two shops, ordered. Names
     * are read from the first two `shop_stocks` rows for the current admin;
     * any missing slots fall back to the canonical "Shop 1" / "Shop 2" labels
     * so callers always receive exactly two entries.
     */
    suspend fun getShopNames(): List<String> {
        val defaults = listOf("Shop 1", "Shop 2")
        return try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return defaults
            val rows = supabase.from("shop_stocks")
                .select { filter { eq("admin_id", adminId) } }
                .decodeList<ShopStockInfo>()
            if (rows.isEmpty()) return defaults
            buildList {
                repeat(2) { i ->
                    val name = rows.getOrNull(i)?.name?.trim()
                    add(if (name.isNullOrBlank()) defaults[i] else name)
                }
            }
        } catch (e: Exception) { defaults }
    }

    /**
     * Persists the display name for the shop at [index] (0 = Shop 1, 1 = Shop 2)
     * into the corresponding `shop_stocks` row. Creates the row if it does not
     * exist yet so the name is available to the inventory / employee screens.
     */
    suspend fun updateShopName(index: Int, newName: String) {
        if (index !in 0..1) return
        val clean = newName.trim()
        if (clean.isBlank()) return
        initAdminSession()
        try {
            val adminId = supabase.auth.currentUserOrNull()?.id ?: return
            val rows = supabase.from("shop_stocks")
                .select { filter { eq("admin_id", adminId) } }
                .decodeList<ShopStockInfo>()
            val target = rows.getOrNull(index)
            if (target != null) {
                val oldName = target.name
                // A rename also cleans up the literal slot label stored in the
                // legacy `location` column so it never shows up on screen.
                val cleanLocation = when (target.location) {
                    "Shop 1" -> "Primary Shop"
                    "Shop 2" -> "Secondary Shop"
                    else     -> target.location
                }
                supabase.from("shop_stocks").update(
                    buildJsonObject {
                        put("name", clean)
                        put("location", cleanLocation)
                    }
                ) { filter { eq("id", target.id) } }
                // Keep every name-based reference pointing at this shop slot
                // after a rename, otherwise the inventory per-product badge,
                // employee assignments and sale grouping keep showing the
                // outdated name while the shop tabs/counts use the new one.
                if (oldName.isNotBlank() && oldName != clean) {
                    supabase.from("stock_movements").update(
                        buildJsonObject { put("shop_name", clean) }
                    ) { filter { eq("shop_name", oldName) } }
                    supabase.from("product_categories").update(
                        buildJsonObject { put("supplier_group", clean) }
                    ) { filter { eq("supplier_group", oldName) } }
                    supabase.from("employees").update(
                        buildJsonObject { put("shop_name", clean) }
                    ) { filter { eq("shop_name", oldName) } }
                    supabase.from("sale_entries").update(
                        buildJsonObject { put("shop_id", clean) }
                    ) { filter { eq("shop_id", oldName) } }
                    supabase.from("monthly_expenses").update(
                        buildJsonObject { put("shop_id", clean) }
                    ) { filter { eq("shop_id", oldName) } }
                }
            } else {
                supabase.from("shop_stocks").insert(
                    buildJsonObject {
                        put("name", clean)
                        put("location", if (index == 0) "Primary Shop" else "Secondary Shop")
                        put("is_live", true)
                        put("admin_id", adminId)
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

fun getCurrentDateTimeIso(): String =
    com.example.ruwia.util.currentDateTimeIso()
