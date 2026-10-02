package com.example.ruwia

import com.example.ruwia.data.isServiceKeyConfigured
import com.example.ruwia.data.isServiceKeyRejection
import com.example.ruwia.data.serviceKeySetupMessage
import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.deriveShopStockTotals
import com.example.ruwia.domain.netStockPerProduct
import com.example.ruwia.domain.netStockPerProductAllShops
import com.example.ruwia.domain.netStockPerProductInShop
import com.example.ruwia.domain.productNetStock
import com.example.ruwia.domain.shopMatchKey
import com.example.ruwia.presentation.AdminState
import com.example.ruwia.presentation.toDashboardMetrics
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedLogicJvmTest {

    @Test
    fun testDeriveShopStockTotals() {
        val rawShops = listOf(
            ShopStockInfo(
                id = "s1",
                name = "Shop 1 · Main",
                location = "SAIBABA COLONY"
            )
        )
        val movements = listOf(
            StockMovement(
                id = "m1",
                source = "Empty cans · Customer A",
                qty = 30,
                type = "inward",
                shopName = "Shop 1"
            ),
            StockMovement(
                id = "m2",
                source = "Empty cans · Customer B",
                qty = 40,
                type = "inward",
                shopName = "Shop 1"
            ),
            StockMovement(
                id = "m3",
                source = "Empty cans · Supplier X",
                qty = 20,
                type = "outward",
                shopName = "Shop 1"
            )
        )
        val stockItems = listOf(
            com.example.ruwia.domain.StockItem(
                id = "p1",
                name = "20L Water Can",
                stockAvailable = 10
            )
        )
        val derived = deriveShopStockTotals(rawShops, movements, stockItems)
        assertEquals(1, derived.size)
        assertEquals(50.0, derived[0].emptyCans)
    }

    // ── Regression: "Total Inventory shows 0 while per-shop shows 25/30" ─────

    @Test
    fun testGlobalNetIncludesMovementsUnderAnyShopLabel() {
        // Movements recorded under a shop label that is NOT one of the two
        // configured display shops must still count toward the global net.
        val movements = listOf(
            StockMovement(id = "m1", source = "Opening Stock", qty = 25, type = "inward", shopName = "Alhudha 2", productId = "p1"),
            StockMovement(id = "m2", source = "Inward Purchase", qty = 30, type = "inward", shopName = "XYZ Store", productId = "p1"),
            StockMovement(id = "m3", source = "Sale · Rama", qty = 5, type = "outward", shopName = "Alhudha 2", productId = "p1"),
        )
        val net = netStockPerProduct(movements)
        // 25 + 30 − 5 = 50, regardless of shop label
        assertEquals(50, net["p1"] ?: 0)
        assertEquals(50, productNetStock(movements, "p1"))
    }

    @Test
    fun testEmptyCansReturnsDoNotInflateNetStock() {
        val movements = listOf(
            StockMovement(id = "m1", source = "Inward Purchase", qty = 30, type = "inward", shopName = "Shop 1", productId = "p1"),
            StockMovement(id = "m2", source = "Empty cans · Customer A", qty = 12, type = "inward", shopName = "Shop 1", productId = "p1"),
        )
        // Empty-can returns are not sellable stock.
        assertEquals(30, productNetStock(movements, "p1"))
    }

    @Test
    fun testPerShopScopeMatchesGlobalOnlyWhenLabelAligns() {
        val movements = listOf(
            StockMovement(id = "m1", source = "Inward Purchase", qty = 25, type = "inward", shopName = "Shop 1 · Main", productId = "p1"),
            StockMovement(id = "m2", source = "Inward Purchase", qty = 30, type = "inward", shopName = "Shop 2", productId = "p1"),
            StockMovement(id = "m3", source = "Sale · Rama", qty = 5, type = "outward", shopName = "Shop 1", productId = "p1"),
        )
        val shop1 = netStockPerProductInShop(movements, shopMatchKey("Shop 1"))
        val shop2 = netStockPerProductInShop(movements, shopMatchKey("Shop 2"))
        assertEquals(20, shop1["p1"] ?: 0)
        assertEquals(30, shop2["p1"] ?: 0)
        // Global is the sum of both shops (empty-cans excluded).
        assertEquals(50, productNetStock(movements, "p1"))
    }

    // ── Regression: "All Shops = 330 but Shop 1 + Shop 2 = 365" ──────────────
    // A shop-level negative net (outward recorded without matching inward at
    // the same shop — e.g. transfers, large-quantity misattribution) used to
    // be absorbed by the other shop's stock in the global figure, while each
    // tab floored it to zero. All Shops must equal the tabs added up.

    @Test
    fun testAllShopsTotalEqualsSumOfShopTabs() {
        val movements = listOf(
            StockMovement(id = "m1", source = "Inward Purchase", qty = 100, type = "inward", shopName = "Shop 1", productId = "p1"),
            StockMovement(id = "m2", source = "Sale · Rama", qty = 120, type = "outward", shopName = "Shop 1", productId = "p1"),
            StockMovement(id = "m3", source = "Inward Purchase", qty = 200, type = "inward", shopName = "Shop 2", productId = "p1"),
            StockMovement(id = "m4", source = "Sale · Sita", qty = 150, type = "outward", shopName = "Shop 2", productId = "p1"),
        )
        val shop1 = netStockPerProductInShop(movements, shopMatchKey("Shop 1"))
        val shop2 = netStockPerProductInShop(movements, shopMatchKey("Shop 2"))
        // Shop 1 net is −20 → floored to 0 on its tab; Shop 2 nets 50.
        assertEquals(0, shop1["p1"] ?: -1)
        assertEquals(50, shop2["p1"] ?: 0)
        // Old cross-shop netting would report 300 − 270 = 30 here.
        assertEquals(30, productNetStock(movements, "p1"))
        // Bucketed All-Shops total matches the tabs added up — never negative.
        val all = netStockPerProductAllShops(movements)
        assertEquals(50, all["p1"] ?: 0)
        assertEquals((shop1["p1"] ?: 0) + (shop2["p1"] ?: 0), all["p1"] ?: -1)
        assertTrue((all["p1"] ?: 0) >= 0)
    }

    @Test
    fun testAllShopsTotalMatchesTabsAfterTransfer() {
        val movements = listOf(
            StockMovement(id = "m1", source = "Inward Purchase", qty = 100, type = "inward", shopName = "Shop 1", productId = "p1"),
            StockMovement(id = "m2", source = "Transfer to Shop 2", qty = 30, type = "outward", shopName = "Shop 1", productId = "p1"),
            StockMovement(id = "m3", source = "Transfer from Shop 1", qty = 30, type = "inward", shopName = "Shop 2", productId = "p1"),
        )
        val shop1 = netStockPerProductInShop(movements, shopMatchKey("Shop 1"))
        val shop2 = netStockPerProductInShop(movements, shopMatchKey("Shop 2"))
        assertEquals(70, shop1["p1"] ?: 0)
        assertEquals(30, shop2["p1"] ?: 0)
        val all = netStockPerProductAllShops(movements)
        assertEquals(100, all["p1"] ?: 0)
        assertEquals((shop1["p1"] ?: 0) + (shop2["p1"] ?: 0), all["p1"] ?: -1)
    }

    @Test
    fun testShopMatchKeyNormalisesBulletSuffixAndCase() {
        assertEquals(shopMatchKey("Shop 1 · Main"), shopMatchKey("Shop 1"))
        assertEquals(shopMatchKey("Shop 1"), shopMatchKey("shop 1 "))
        assertEquals(shopMatchKey("Alhudha"), shopMatchKey("Alhudha · RS Puram 2"))
    }

    @Test
    fun testDashboardMetricsTotalStockCases() {
        val state = AdminState(
            productCategories = listOf(
                ProductCategory(id = "p1", name = "500ML", displayName = "Neerthuli - 500ML", stockAvailable = 20),
                ProductCategory(id = "p2", name = "1L", displayName = "Droply - 1L", stockAvailable = 10),
                ProductCategory(id = "p3", name = "20L", displayName = "Bisleri - 20L", stockAvailable = 20)
            )
        )
        val metrics = state.toDashboardMetrics()
        assertEquals(50.0, metrics.totalStockUnits)
    }

    // ── Date format: DD/MM/YYYY everywhere ────────────────────────────────────

    @Test
    fun testDisplayDatesAreNumericDdMmYyyy() {
        // ISO timestamps render as DD/MM/YYYY (day only asserted by shape since
        // the local day can shift across a timezone boundary).
        val iso = com.example.ruwia.util.isoToDisplayDate("2026-09-29T12:00:00Z")
        assertTrue(Regex("""^\d{2}/\d{2}/\d{4}$""").containsMatchIn(iso), "expected DD/MM/YYYY but was '$iso'")
        assertEquals("29/09/2026", com.example.ruwia.util.dbToDisplayDate("2026-09-29"))
        assertEquals("05/06/2026", com.example.ruwia.util.dbToDisplayDate("2026-06-05"))
    }

    @Test
    fun testDisplayDateRoundTripsToDbFormat() {
        // "29/09/2026" (display) persists as "2026-09-29" (DB) — and the parse
        // accepts single-digit day/month too ("5/9/2026").
        assertEquals("2026-09-29", com.example.ruwia.util.displayDateToDb("29/09/2026"))
        assertEquals("2026-09-05", com.example.ruwia.util.displayDateToDb("5/9/2026"))
        assertEquals(null, com.example.ruwia.util.displayDateToDb("29/13/2026"))
        assertEquals(null, com.example.ruwia.util.displayDateToDb("not-a-date"))
    }

    // ── Time format: legacy (offset-less) and TZ-aware timestamps both render ─

    @Test
    fun testLegacyOffsetlessTimestampRendersDateAndTime() {
        // Older writes stored local wall-clock time without an offset. These must
        // still display (previously Instant.parse threw and the time was dropped).
        assertEquals("29/09/2026", com.example.ruwia.util.isoToDisplayDate("2026-09-29T14:30:00"))
        assertEquals("2:30 PM", com.example.ruwia.util.isoToDisplayTime("2026-09-29T14:30:00"))
        assertEquals("29/09/2026 · 2:30 PM", com.example.ruwia.util.isoToDisplayDateTime("2026-09-29T14:30:00"))
    }

    @Test
    fun testTimezoneAwareTimestampRendersLocalClockTime() {
        // Absolute instants are converted to the device's local timezone, so the
        // expected value is derived from the same zone the formatter uses.
        val inst = kotlinx.datetime.Instant.parse("2026-09-29T02:30:00Z")
        val local = inst.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val h12 = when { local.hour == 0 -> 12; local.hour > 12 -> local.hour - 12; else -> local.hour }
        val ampm = if (local.hour >= 12) "PM" else "AM"
        val expectedTime = "${h12}:${local.minute.toString().padStart(2, '0')} $ampm"
        assertEquals(expectedTime, com.example.ruwia.util.isoToDisplayTime("2026-09-29T02:30:00Z"))
        // Date renders from the same converted instant.
        assertEquals(local.dayOfMonth.toString().padStart(2, '0') + "/" +
                local.monthNumber.toString().padStart(2, '0') + "/${local.year}",
            com.example.ruwia.util.isoToDisplayDate("2026-09-29T02:30:00Z"))
    }

    @Test
    fun testCurrentDateTimeIsoIsTimezoneAware() {
        // The writer must emit a TZ-aware ISO string so supabase stores an
        // absolute instant (not local-no-offset which shifts/fails on read-back).
        val iso = com.example.ruwia.util.currentDateTimeIso()
        assertTrue(iso.contains('T'), "expected TZ-aware ISO, got '$iso'")
        assertTrue(iso.isNotBlank())
        kotlinx.datetime.Instant.parse(iso) // must not throw
    }

    // ── Service-key diagnostics (employee creation) ──────────────────────────

    @Test
    fun testInstalledServiceKeyIsConfigured() {
        // A real service-role secret is installed, so admin calls (employee
        // creation) can authenticate. The setup message stays available
        // for builds where the key is still a placeholder.
        assertTrue(isServiceKeyConfigured())
        assertTrue(serviceKeySetupMessage().contains("SUPABASE_SERVICE_KEY"))
    }

    @Test
    fun testServiceKeyRejectionMatcher() {
        assertTrue(isServiceKeyRejection("Invalid API key"))
        assertTrue(isServiceKeyRejection("invalid JWT: unable to parse"))
        assertTrue(isServiceKeyRejection("401 Unauthorized"))
        assertTrue(isServiceKeyRejection("User not allowed (service_role required)"))
        // Business errors must keep their original message.
        assertTrue(!isServiceKeyRejection("User already registered"))
        assertTrue(!isServiceKeyRejection("duplicate key value violates unique constraint"))
        assertTrue(!isServiceKeyRejection(null))
    }

    // ── Inventory consistency: canonical shop buckets ─────────────────────

    @Test
    fun testCanonicalShopNameReusesExistingBucket() {
        val movements = listOf(
            StockMovement(id = "m1", source = "Inward Purchase", qty = 100, type = "inward", shopName = "Shop 1"),
            StockMovement(id = "m2", source = "Sale · X", qty = 10, type = "outward", shopName = "Shop 2"),
        )
        // Renamed/configured labels resolve to the stored history label…
        assertEquals("Shop 1", com.example.ruwia.domain.canonicalShopName("shop 1 · Main", movements))
        assertEquals("Shop 1", com.example.ruwia.domain.canonicalShopName("SHOP 1", movements))
        assertEquals("Shop 2", com.example.ruwia.domain.canonicalShopName("Shop 2", movements))
        // …unknown shops pass through so new shops can open a bucket…
        assertEquals("Shop 3", com.example.ruwia.domain.canonicalShopName("Shop 3", movements))
        assertEquals("Shop 3", com.example.ruwia.domain.canonicalShopName("Shop 3", emptyList()))
        // …and canonical labels are idempotent.
        assertEquals("Shop 1", com.example.ruwia.domain.canonicalShopName("Shop 1", movements))
    }

    // ── Inventory consistency: deleted-product names in history ───────────

    @Test
    fun testResolveMovementProductNamePrefersSnapshot() {
        val products = listOf(
            ProductCategory(id = "p1", name = "2L", displayName = "Kinly 2L", brandName = "Kinly"),
        )
        // Snapshot wins even when the product still exists.
        assertEquals(
            "Kinly 2L",
            com.example.ruwia.domain.resolveMovementProductName(
                StockMovement(id = "m", source = "Sale · X", qty = 1, type = "outward", productId = "p1", productName = "Kinly 2L"),
                products
            )
        )
        // Active lookup when no snapshot.
        assertEquals(
            "Kinly 2L",
            com.example.ruwia.domain.resolveMovementProductName(
                StockMovement(id = "m", source = "Sale · X", qty = 1, type = "outward", productId = "p1"),
                products
            )
        )
        // Sale-record snapshot recovers soft-deleted products (no catalog row).
        assertEquals(
            "Kinly 2L",
            com.example.ruwia.domain.resolveMovementProductName(
                StockMovement(id = "m", source = "Sale · X", qty = 1, type = "outward", productId = "p1"),
                emptyList(),
                mapOf("p1" to "Kinly 2L")
            )
        )
        // Genuinely unknown → null (callers fall back to the source text).
        assertEquals(
            null,
            com.example.ruwia.domain.resolveMovementProductName(
                StockMovement(id = "m", source = "Manual adjustment", qty = 1, type = "inward", productId = "p9"),
                emptyList()
            )
        )
    }

    // ── Transaction timestamp guards ──────────────────────────────────────

    @Test
    fun testFutureTimestampGuards() {
        assertTrue(!com.example.ruwia.util.isFutureTimestamp("2000-01-01T00:00:00Z"))
        assertTrue(com.example.ruwia.util.isFutureTimestamp("2999-01-01T00:00:00Z"))
        assertTrue(!com.example.ruwia.util.isFutureTimestamp(null))
        assertTrue(!com.example.ruwia.util.isFutureDay("2000-01-01"))
        assertTrue(!com.example.ruwia.util.isFutureDay("2000-01-01 2:30 PM"))
        assertTrue(com.example.ruwia.util.isFutureDay("2999-12-31"))
        // Past values pass validation; future values throw.
        com.example.ruwia.util.requireNotFutureTimestamp("Test", "2000-01-01T00:00:00Z")
        com.example.ruwia.util.requireNotFutureDay("Test", "2000-01-01")
        var thrown = false
        try {
            com.example.ruwia.util.requireNotFutureTimestamp("Test", "2999-01-01T00:00:00Z")
        } catch (_: IllegalStateException) {
            thrown = true
        }
        assertTrue(thrown, "future timestamp must be rejected")
        thrown = false
        try {
            com.example.ruwia.util.requireNotFutureDay("Test", "2999-12-31")
        } catch (_: IllegalStateException) {
            thrown = true
        }
        assertTrue(thrown, "future day must be rejected")
    }

    // ── 2-year test-data seeder ─────────────────────────────────────────

    @Test
    fun testTwoYearSeedPlanIsConsistent() {
        val plan = com.example.ruwia.data.TestDataSeeder.buildPlan("Shop 1", "Shop 2", seed = 42L, daysBack = 60)
        // Catalog + customers + employees present.
        assertTrue(plan.products.size == 10, "expected 10 products, got ${plan.products.size}")
        assertTrue(plan.customers.size == 80, "expected 80 customers, got ${plan.customers.size}")
        assertTrue(plan.employees.size == 4, "expected 4 employees, got ${plan.employees.size}")
        assertTrue(plan.sales.isNotEmpty() && plan.movements.isNotEmpty())
        // Every sale mirrors an outward movement with the same key/qty/product.
        val outwardByKey = plan.movements.filter { it.type == "outward" }.associateBy { it.clientKey }
        for (sale in plan.sales) {
            val movement = outwardByKey[sale.clientKey]
            assertTrue(movement != null, "sale ${sale.clientKey} has no mirrored movement")
            assertEquals(sale.qty, movement!!.qty)
            assertEquals(sale.productId, movement.productId)
            assertEquals(sale.date, movement.createdAt?.take(10))
            assertTrue(!movement.productName.isNullOrBlank(), "movement snapshot missing")
        }
        // Closing stock equals the production derivation over the log.
        val derived = com.example.ruwia.domain.netStockPerProductAllShops(plan.movements)
        for ((tempId, expected) in plan.expectedStock) {
            assertEquals(expected, derived[tempId], "stock mismatch for $tempId")
            assertTrue(expected >= 0, "negative closing stock for $tempId")
        }
        // Deterministic: same seed, same output.
        val again = com.example.ruwia.data.TestDataSeeder.buildPlan("Shop 1", "Shop 2", seed = 42L, daysBack = 60)
        assertEquals(plan.sales.size, again.sales.size)
        assertEquals(plan.sales.firstOrNull()?.totalSelling, again.sales.firstOrNull()?.totalSelling)
    }
}