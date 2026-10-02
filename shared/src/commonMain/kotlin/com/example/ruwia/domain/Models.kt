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
    /**
     * Denormalized product display name stamped at write time so history
     * keeps showing the real name after the product is soft-deleted.
     * Null on rows written before the snapshot column existed.
     */
    @SerialName("product_name") val productName: String? = null,
    @SerialName("employee_id") val employeeId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    /**
     * Idempotency key of the sale that produced this row (see
     * [SaleEntry.clientKey]). Lets the writer verify every line's movement
     * landed and lets retries skip already-written lines.
     */
    @SerialName("client_key") val clientKey: String? = null,
)

@Serializable
data class Supplier(
    val id: String? = null,
    val name: String,
    val location: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
)



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
    @SerialName("low_stock_alert") val lowStockAlert: Int = 5,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)



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
    /**
     * Idempotency key: one UUID per user-confirmed sale, stamped on every
     * line + movement. Retried/double-tapped submissions with the same key
     * are skipped instead of deducting stock twice.
     */
    @SerialName("client_key") val clientKey: String? = null,
)

@Serializable
data class CustomExpense(
    val name: String,
    val amount: Double
)

@Serializable
data class CustomerProductPrice(
    val id: String? = null,
    @SerialName("customer_id") val customerId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("selling_price") val sellingPrice: Double,
    @SerialName("is_active") val isActive: Boolean = true
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

/**
 * Normalises a shop name like "Shop 1 · Main" / "Shop 1" / " shop 1 " into
 * the same lowercase comparison key. Shop names entered in the employee app
 * (`shopInfo.split("·")[0].trim()`) only carry the first segment, so we
 * strip everything after the bullet so they match the seeded names.
 */
fun shopMatchKey(name: String): String =
    name.split("·", limit = 2).firstOrNull()?.trim()?.lowercase() ?: name.trim().lowercase()

/**
 * Canonical shop label for NEW movement rows.
 *
 * Shop display names change over time (Settings renames) while old movement
 * rows keep the label they were written with. Writing a fresh row with a
 * different-but-equivalent label opens a second bucket: per-shop tabs miss
 * the row while All Shops (which sums every bucket) stays correct — the
 * exact "shop tab wrong, All Shops right" symptom.
 *
 * To keep one bucket per shop forever, resolve the target to the most
 * recently used full label for the same shop key. Feeds are newest-first,
 * so the first match is the canonical label. Falls back to [target] when
 * the shop has no history yet. Idempotent: canonical labels resolve to
 * themselves.
 */
fun canonicalShopName(target: String, movements: List<StockMovement>): String {
    val key = shopMatchKey(target)
    if (key.isBlank()) return target
    return movements.firstOrNull { shopMatchKey(it.shopName) == key }?.shopName ?: target
}

/**
 * Display name for a movement row that survives product soft-delete.
 *
 * Resolution order: write-time snapshot → active product lookup → the
 * product's sale-record snapshot (covers rows written before snapshots
 * existed, e.g. Kinly 2L's ₹80k of sales) → null when genuinely unknown.
 * Callers fall back to the movement source text, never a fake product name.
 */
fun resolveMovementProductName(
    movement: StockMovement,
    products: List<ProductCategory>,
    salesByProductId: Map<String, String> = emptyMap(),
): String? {
    movement.productName?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val pid = movement.productId
    if (pid != null) {
        products.find { it.id == pid }
            ?.let { (if (it.displayName.isNotBlank()) it.displayName else it.name).trim().takeIf { n -> n.isNotEmpty() } }
            ?.let { return it }
        salesByProductId[pid]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return null
}

/** A tenant-scoped application setting row (e.g. `empty_cans_baseline`). */
@Serializable
data class AppSetting(
    val id: String? = null,
    @SerialName("settings_key") val key: String = "",
    @SerialName("settings_value") val value: String? = null,
)

/** True if a movement source string represents returned empty cans. */
fun String.isEmptyCansSource(): Boolean {
    val clean = trim().lowercase()
    return clean.startsWith("empty cans") || clean.startsWith("empty cases")
}

/**
 * Net live units per product across ALL shops, derived from the movement log.
 *
 * Inward purchases add stock; outward sales/transfers subtract it. Empty-can
 * returns are recorded as inward movements but are NOT sellable stock, so they
 * are excluded from the net. This is the single source of truth for the global
 * inventory figure, so movements recorded under any shop label (including ones
 * renamed or not yet configured) never get orphaned.
 */
fun netStockPerProduct(movements: List<StockMovement>): Map<String?, Int> =
    movements.groupBy { it.productId }.mapValues { (_, rows) ->
        val inward = rows.filter { it.type == "inward" && !it.source.isEmptyCansSource() }.sumOf { it.qty }
        val outward = rows.filter { it.type == "outward" }.sumOf { it.qty }
        (inward - outward).coerceAtLeast(0)
    }

/** Net live units per product for a single shop, scoped by normalized shop key. */
fun netStockPerProductInShop(movements: List<StockMovement>, shopKey: String): Map<String?, Int> =
    netStockPerProduct(movements.filter { shopMatchKey(it.shopName) == shopKey })

/**
 * Net live units per product across ALL shops, bucketed per shop.
 *
 * Each shop bucket is netted and floored at zero independently, then summed.
 * This is the single source of truth for every "All Shops" total, and it
 * guarantees Σ(shop tabs) == All Shops by construction.
 *
 * Why not [netStockPerProduct] on the whole log? That nets across shops
 * first and floors once, so a shop-level negative (e.g. a transfer or sale
 * recorded at a shop with less recorded inward stock) is silently absorbed
 * by another shop's stock. The result reads *lower* than the sum of the
 * shop tabs (e.g. All = 330 while Shop 1 + Shop 2 = 365) — the exact
 * mismatch this replaces. Bucketing floors per shop, matching what each
 * tab displays, and movements under unknown/blank labels form their own
 * bucket so no stock is ever orphaned.
 */
fun netStockPerProductAllShops(movements: List<StockMovement>): Map<String?, Int> {
    val totals = mutableMapOf<String?, Int>()
    movements.groupBy { shopMatchKey(it.shopName) }.forEach { (_, rows) ->
        netStockPerProduct(rows).forEach { (productId, net) ->
            totals[productId] = (totals[productId] ?: 0) + net
        }
    }
    return totals
}

/** Net live units for one product across all shops (0 when no movement exists). */
fun productNetStock(movements: List<StockMovement>, productId: String?): Int =
    netStockPerProduct(movements)[productId] ?: 0

/**
 * Single source of truth for displayed stock.
 *
 * Shop-wise by design: each product is stocked per shop via movements
 * stamped with that shop's name, so Shop 1 (2 products) and Shop 2
 * (4 products) always show their own counts.
 *
 * - shopKey == null → all shops: bucketed per-shop sum (equals Σ shop tabs).
 *   The DB `stock_available` column is only a fallback for products with no
 *   movement rows anywhere yet (brand-new products before their
 *   opening-stock row lands).
 * - shopKey != null → STRICTLY that shop's movements, missing = 0. The DB
 *   column is global per product, so it must never leak into a shop tab —
 *   otherwise Shop 1 would show Shop 2's stock for products never stocked
 *   in Shop 1.
 */
fun effectiveStockMap(
    products: List<ProductCategory>,
    movements: List<StockMovement>,
    shopKey: String? = null,
): Map<String, Int> {
    if (shopKey != null) {
        val perShop = netStockPerProductInShop(movements, shopKey)
        return products.associate { p -> p.id to (perShop[p.id] ?: 0) }
    }
    val bucketed = netStockPerProductAllShops(movements)
    val movedIds = movements.mapNotNull { it.productId }.toSet()
    return products.associate { p ->
        val units = if (p.id in movedIds) (bucketed[p.id] ?: 0) else p.stockAvailable.coerceAtLeast(0)
        p.id to units
    }
}



/** Total on-hand units across [products] using [effectiveStockMap]. */
fun effectiveTotalStock(
    products: List<ProductCategory>,
    movements: List<StockMovement>,
    shopKey: String? = null,
): Double = effectiveStockMap(products, movements, shopKey).values.sumOf { it.toDouble() }

/**
 * Returns each shop with its [ShopStockInfo] columns recomputed from the
 * movement log. The original metadata fields (`id`, `name`, `location`,
 * `isLive`) are preserved.
 *
 * @param emptyCansBaseline the business-wide admin-set "Empty Cases" reset
 *        baseline. When non-zero it is subtracted from each shop's empty-cans
 *        figure (and therefore its total), so an admin "Reset Empty Cases"
 *        zeroes the figure on the Stock tab too — not only the home KPI.
 */
fun deriveShopStockTotals(
    rawShops: List<ShopStockInfo>,
    movements: List<StockMovement>,
    stockItems: List<StockItem>,
    emptyCansBaseline: Double = 0.0,
): List<ShopStockInfo> {
    if (rawShops.isEmpty()) return emptyList()
    val productMap = stockItems.associateBy { it.id }

    return rawShops.map { shop ->
        val key = shopMatchKey(shop.name)
        val rows = movements.filter { shopMatchKey(it.shopName) == key }
        
        val productNetFull = mutableMapOf<String, Int>()
        var unknownProductFull = 0
        
        rows.forEach { m ->
            // Empty-can returns (both "Empty cans" and "Empty cases" labels)
            // are NOT sellable stock — same exclusion as every other
            // derivation (see isEmptyCansSource). Checking only one label
            // here inflated full-cans whenever the other label was used.
            val isFullIn = m.type == "inward" && !m.source.isEmptyCansSource()
            val isSentOut = m.type == "outward"
            if (isFullIn || isSentOut) {
                val delta = if (isFullIn) m.qty else -m.qty
                if (m.productId != null) {
                    productNetFull[m.productId] = (productNetFull[m.productId] ?: 0) + delta
                } else {
                    unknownProductFull += delta
                }
            }
        }
        
        var totalCases = unknownProductFull.toDouble().coerceAtLeast(0.0)
        productNetFull.forEach { (pid, units) ->
            val p = productMap[pid]
            val actualUnits = units.coerceAtLeast(0)
            if (p != null) {
                totalCases += actualUnits.toDouble()
            } else {
                totalCases += actualUnits.toDouble()
            }
        }
        
        var emptyCases = 0.0
        rows.filter { it.source.isEmptyCansSource() }.forEach { m ->
            val delta = if (m.type == "inward") m.qty else -m.qty
            if (m.productId != null) {
                val p = productMap[m.productId]
                if (p != null) emptyCases += delta.toDouble()
                else emptyCases += delta.toDouble()
            } else {
                emptyCases += delta.toDouble()
            }
        }
        
        var withCust = 0.0
        rows.forEach { m ->
            val isReturn = m.source.isEmptyCansSource() && m.type == "inward"
            val isSale   = !m.source.isEmptyCansSource() && m.type == "outward"
            if (isReturn || isSale) {
                val delta = if (isSale) m.qty else -m.qty
                withCust += delta.toDouble()
            }
        }

        val fullCans = totalCases
        val emptyCansLive = (emptyCases - emptyCansBaseline).coerceAtLeast(0.0)
        val totalCans = fullCans + emptyCansLive + withCust.coerceAtLeast(0.0)

        shop.copy(
            totalCans = totalCans,
            fullCans = fullCans,
            emptyCans = emptyCansLive,
            cansWithCustomers = withCust.coerceAtLeast(0.0),
        )
    }
}


