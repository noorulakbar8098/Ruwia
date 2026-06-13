package com.example.ruwia.data

import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.DeliveryTask
import com.example.ruwia.domain.Outward
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.Supplier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order as SortOrder
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class EmployeeRepository {

    // ── Identity ──────────────────────────────────────────────────────────────

    /** The currently logged-in user's UUID (== profiles.id == employees.id). */
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // ── Date helpers ──────────────────────────────────────────────────────────

    private fun today(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.year}-${now.monthNumber.toString().padStart(2,'0')}-${now.dayOfMonth.toString().padStart(2,'0')}"
    }

    // ── Route tasks ───────────────────────────────────────────────────────────

    suspend fun getTodayRouteTasks(employeeId: String): List<DeliveryTask> {
        return try {
            supabase.from("route_tasks")
                .select {
                    filter {
                        eq("employee_id", employeeId)
                        eq("scheduled_date", today())
                    }
                    order("created_at", SortOrder.ASCENDING)
                }
                .decodeList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun recordDeliveryCompletion(
        taskId: String,
        customerId: String,
        deliveredQty: Int,
        returnedEmptyQty: Int,
        collectedAmount: Double,
        paymentMode: String,
        employeeId: String,
    ) {
        try {
            supabase.from("outward").insert(
                Outward(
                    customerId         = customerId,
                    qtyDelivered       = deliveredQty,
                    qtyEmptyReturned   = returnedEmptyQty,
                    rate               = collectedAmount,
                )
            )
            supabase.from("route_tasks").update(
                buildJsonObject { put("status", "done") }
            ) { filter { eq("id", taskId) } }
        } catch (_: Exception) {}
    }

    // ── Summary queries ───────────────────────────────────────────────────────

    suspend fun getDailyEarningsSummary(employeeId: String): Double {
        return try {
            val prefix = today()
            supabase.from("sale_entries")
                .select {
                    filter { eq("employee_id", employeeId) }
                    order("created_at", SortOrder.DESCENDING)
                    limit(200)
                }
                .decodeList<com.example.ruwia.domain.SaleEntry>()
                .filter { it.createdAt?.startsWith(prefix) == true }
                .sumOf { it.totalSelling }
        } catch (e: Exception) { 0.0 }
    }

    suspend fun getDailyCansSummary(employeeId: String): Pair<Int, Int> {
        return try {
            val prefix = today()
            val outward = supabase.from("outward")
                .select {
                    filter { eq("employee_id", employeeId) }
                    limit(200)
                }
                .decodeList<Outward>()
            val delivered = outward.sumOf { it.qtyDelivered }
            val returned  = outward.sumOf { it.qtyEmptyReturned }
            delivered to returned
        } catch (e: Exception) { 0 to 0 }
    }

    // ── Customers ─────────────────────────────────────────────────────────────

    suspend fun getCustomers(): List<Customer> {
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

    // ── Products ──────────────────────────────────────────────────────────────

    suspend fun getProductCategories(): List<ProductCategory> {
        return try {
            supabase.from("product_categories")
                .select { filter { eq("is_active", true) } }
                .decodeList()
        } catch (e: Exception) { emptyList() }
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

    // ── Recent stock entries ───────────────────────────────────────────────────

    suspend fun getRecentEntries(employeeId: String): List<StockMovement> {
        return try {
            supabase.from("stock_movements")
                .select {
                    filter { eq("employee_id", employeeId) }
                    order("created_at", SortOrder.DESCENDING)
                    limit(20)
                }
                .decodeList()
        } catch (e: Exception) { emptyList() }
    }

    // ── Record inward stock purchase ──────────────────────────────────────────

    suspend fun addStockMovement(
        source: String,
        qty: Int,
        type: String,
        shopName: String,
        productId: String? = null,
    ) {
        val employeeId = currentUserId()
        supabase.from("stock_movements").insert(
            buildJsonObject {
                put("source", source)
                put("qty", qty)
                put("type", type)
                put("shop_name", shopName)
                if (productId != null) put("product_id", productId)
                if (employeeId != null) put("employee_id", employeeId)
            }
        )
    }
}
