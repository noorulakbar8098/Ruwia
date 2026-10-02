package com.example.ruwia.data

import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.MonthlyExpense
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.SaleEntry
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.netStockPerProductAllShops
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Deterministic 2-year test-data generator for the admin app's seeding tool.
 *
 * Pure Kotlin: given shop labels + a seed it builds every row (products,
 * customers, employees, sale entries, stock movements, monthly expenses) with
 * timestamps spread over the past [daysBack] days, ending yesterday so live
 * "today" figures stay clean for real testing.
 *
 * Consistency is structural, not hoped for:
 * - Every outward sale movement mirrors a sale entry (same product/qty/day).
 * - Monthly inward restocks cover each product's monthly sales plus a buffer,
 *   so no product ever goes negative.
 * - Movement rows carry the product_name snapshot, so history survives
 *   soft-delete from day one.
 * - [expectedStock] is computed with the production [netStockPerProductAllShops]
 *   over the generated movements — the executor writes exactly that into
 *   `product_categories.stock_available`, so derived and stored stock agree.
 *
 * Product references use temporary ids ("seed-p0" …); the executor inserts
 * products first and rewrites them to real ids via [resolveIds].
 */
object TestDataSeeder {

    data class EmployeeSeed(
        val name: String,
        val phone: String,
        val role: String,
        val shopName: String,
        val salary: Double,
        val email: String,
        val password: String,
    )

    data class SeedPlan(
        val products: List<ProductCategory>,
        val customers: List<Customer>,
        val employees: List<EmployeeSeed>,
        /** Movements with temporary product ids — call [resolveIds] first. */
        val movements: List<StockMovement>,
        /** Sales with temporary product ids — call [resolveIds] first. */
        val sales: List<SaleEntry>,
        val expenses: List<MonthlyExpense>,
        /** tempProductId -> closing stock the executor must persist. */
        val expectedStock: Map<String, Int>,
        val clientKeyPrefix: String,
    )

    private data class ProductSpec(
        val displayName: String,
        val sizeName: String,
        val brand: String,
        val purchase: Double,
        val sell: Double,
    )

    private val PRODUCT_SPECS = listOf(
        ProductSpec("Neer Thuli 20L", "20L", "Neer Thuli", 70.0, 90.0),
        ProductSpec("Neer Thuli 2L", "2L", "Neer Thuli", 18.0, 25.0),
        ProductSpec("Neer Thuli 1L", "1L", "Neer Thuli", 10.0, 15.0),
        ProductSpec("Kinly 2L", "2L", "Kinly", 20.0, 28.0),
        ProductSpec("Kinly 1L", "1L", "Kinly", 11.0, 16.0),
        ProductSpec("Kinly 500ml", "500ml", "Kinly", 7.0, 10.0),
        ProductSpec("AquaFresh 20L", "20L", "AquaFresh", 65.0, 85.0),
        ProductSpec("AquaFresh 1L", "1L", "AquaFresh", 9.0, 14.0),
        ProductSpec("PureDrop 20L", "20L", "PureDrop", 68.0, 88.0),
        ProductSpec("PureDrop 250ml", "250ml", "PureDrop", 5.0, 8.0),
    )

    private val FIRST_NAMES = listOf(
        "Ravi", "Priya", "Kumar", "Divya", "Suresh", "Anitha", "Karthik", "Meena",
        "Rajesh", "Lakshmi", "Arun", "Deepa", "Vikram", "Sneha", "Manoj", "Kavitha",
        "Sathish", "Ramya", "Dinesh", "Geetha", "Prakash", "Saranya", "Balaji", "Nithya",
        "Gopal", "Revathi", "Harish", "Poonam", "Naresh", "Asha", "Kiran", "Shalini",
    )
    private val LAST_NAMES = listOf(
        "Velu", "Nair", "Iyer", "Sharma", "Reddy", "Gupta", "Menon", "Pillai",
        "Rajan", "Khan", "Das", "Murthy", "Chandran", "Bose", "Patel", "Singh",
    )
    private val STREETS = listOf(
        "Gandhi Nagar", "Anna Salai", "Kamaraj Street", "Nehru Colony", "MGR Nagar",
        "Vivekananda Street", "Bharathi Nagar", "Tagore Street", "Netaji Road", "Kamarajar Salai",
    )

    fun buildPlan(
        shopA: String,
        shopB: String,
        seed: Long = 20261001L,
        daysBack: Int = 730,
    ): SeedPlan {
        val random = Random(seed)
        val shops = listOf(shopA.trim().ifBlank { "Shop 1" }, shopB.trim().ifBlank { "Shop 2" })
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val firstDay = today.minus(DatePeriod(days = daysBack))
        val lastDay = today.minus(DatePeriod(days = 1))

        // ── Products (temporary ids; executor rewrites to real ones) ──
        val products = PRODUCT_SPECS.mapIndexed { index, spec ->
            ProductCategory(
                id = "seed-p$index",
                name = spec.sizeName,
                displayName = spec.displayName,
                brandName = spec.brand,
                supplierGroup = shops[index % shops.size],
                purchasePrice = spec.purchase,
                defaultSellPrice = spec.sell,
                stockAvailable = 0,
                lowStockAlert = 20,
                isActive = true,
            )
        }
        // Heavy 20L sellers first in the draw order.
        val weightedProducts = buildList {
            products.forEach { p ->
                val weight = when {
                    p.name == "20L" -> 5
                    p.name == "2L" -> 3
                    p.name == "1L" -> 2
                    else -> 1
                }
                repeat(weight) { add(p) }
            }
        }

        // ── Customers ──
        val customers = (1..80).map { i ->
            val name = "${FIRST_NAMES[random.nextInt(FIRST_NAMES.size)]} ${LAST_NAMES[random.nextInt(LAST_NAMES.size)]}"
            Customer(
                name = name,
                phone = "+91 98${(1000000 + random.nextInt(8999999))}",
                address = "${1 + random.nextInt(120)}, ${STREETS[random.nextInt(STREETS.size)]}",
            )
        }

        // ── Employees (auth creation is best-effort in the executor) ──
        val employeeNames = listOf("Seed Tester One", "Seed Tester Two", "Seed Tester Three", "Seed Tester Four")
        val employees = employeeNames.mapIndexed { i, name ->
            EmployeeSeed(
                name = name,
                phone = "+91 97${(1000000 + random.nextInt(8999999))}",
                role = if (i % 2 == 0) "driver" else "stock",
                shopName = shops[i % shops.size],
                salary = 12000.0 + i * 1000,
                email = "seed.emp${i + 1}@neerthuli.in",
                password = "SeedTest@${123 + i}",
            )
        }

        // ── Day-by-day sales (+ mirrored outward movements) ──
        val movements = mutableListOf<StockMovement>()
        val sales = mutableListOf<SaleEntry>()
        // Outward totals per month/shop/product to size restocks afterwards.
        val monthlyOutward = mutableMapOf<Triple<String, String, String>, Int>() // (ym, shop, tempPid) -> qty
        var lineCounter = 0

        var day = firstDay
        while (day <= lastDay) {
            val dayStr = day.toDbString()
            for (shop in shops) {
                val linesToday = 3 + random.nextInt(7) // 3..9 sales per shop per day
                repeat(linesToday) {
                    val product = weightedProducts[random.nextInt(weightedProducts.size)]
                    val qty = 1 + random.nextInt(6)
                    val customer = customers[random.nextInt(customers.size)]
                    val hour = 9 + random.nextInt(12) // 09:00..20:xx
                    val minute = random.nextInt(60)
                    val createdAt = LocalDateTime(day.year, day.monthNumber, day.dayOfMonth, hour, minute)
                        .toInstant(TimeZone.UTC).toString()
                    val key = "seed-$seed-$lineCounter"
                    lineCounter++
                    val margin = product.defaultSellPrice - product.purchasePrice
                    sales += SaleEntry(
                        date = dayStr,
                        customerName = customer.name,
                        productId = product.id,
                        productName = product.displayName,
                        qty = qty,
                        purchasePricePerUnit = product.purchasePrice,
                        sellingPricePerUnit = product.defaultSellPrice,
                        salesMarginPerUnit = margin,
                        totalSelling = product.defaultSellPrice * qty,
                        totalMargin = margin * qty,
                        shopId = shop,
                        employeeId = null, // executor maps to seeded employees round-robin
                        createdAt = createdAt,
                        clientKey = key,
                    )
                    movements += StockMovement(
                        source = "Sale · ${customer.name}",
                        qty = qty,
                        type = "outward",
                        shopName = shop,
                        productId = product.id,
                        productName = product.displayName,
                        createdAt = createdAt,
                        clientKey = key,
                    )
                    val ym = "${day.year}-${day.monthNumber.toString().padStart(2, '0')}"
                    val bucket = Triple(ym, shop, product.id)
                    monthlyOutward[bucket] = (monthlyOutward[bucket] ?: 0) + qty
                }
                // Empty-can returns most days.
                if (random.nextDouble() < 0.6) {
                    val customer = customers[random.nextInt(customers.size)]
                    val minute = random.nextInt(60)
                    val createdAt = LocalDateTime(day.year, day.monthNumber, day.dayOfMonth, 18, minute)
                        .toInstant(TimeZone.UTC).toString()
                    movements += StockMovement(
                        source = "Empty cans · ${customer.name}",
                        qty = 2 + random.nextInt(9),
                        type = "inward",
                        shopName = shop,
                        createdAt = createdAt,
                    )
                }
            }
            day = day.plus(DatePeriod(days = 1))
        }

        // ── Monthly restocks: cover the month's sales + buffer (never negative) ──
        val months = monthlyOutward.keys.map { it.first }.distinct().sorted()
        for (ym in months) {
            for (shop in shops) {
                for (product in products) {
                    val sold = monthlyOutward[Triple(ym, shop, product.id)] ?: 0
                    if (sold <= 0) continue
                    val buffer = 20 + random.nextInt(41)
                    val y = ym.substring(0, 4).toInt()
                    val m = ym.substring(5, 7).toInt()
                    val createdAt = LocalDateTime(y, m, 1, 8, random.nextInt(60))
                        .toInstant(TimeZone.UTC).toString()
                    movements += StockMovement(
                        source = "Inward Purchase",
                        qty = sold + buffer,
                        type = "inward",
                        shopName = shop,
                        productId = product.id,
                        productName = product.displayName,
                        createdAt = createdAt,
                    )
                }
            }
        }

        // ── Monthly expenses per shop ──
        val expenses = months.flatMap { ym ->
            shops.map { shop ->
                MonthlyExpense(
                    month = ym,
                    shopId = shop,
                    shopRent = 8000.0 + random.nextInt(4001),
                    adminSalary = 25000.0,
                    deliveryStaff = 12000.0 + random.nextInt(4001),
                    miscellaneous = 1500.0 + random.nextInt(3501),
                    bikeExpense = 2000.0 + random.nextInt(2001),
                )
            }
        }

        // Closing stock per product with the production derivation — the
        // executor persists exactly this, so stored and derived stock agree.
        val expectedStock = netStockPerProductAllShops(movements)
            .mapNotNull { (pid, net) -> pid?.let { it to net } }
            .toMap()

        return SeedPlan(
            products = products,
            customers = customers,
            employees = employees,
            movements = movements.sortedByDescending { it.createdAt ?: "" },
            sales = sales,
            expenses = expenses,
            expectedStock = expectedStock,
            clientKeyPrefix = "seed-$seed-",
        )
    }

    private fun LocalDate.toDbString(): String =
        "$year-${monthNumber.toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}"
}
