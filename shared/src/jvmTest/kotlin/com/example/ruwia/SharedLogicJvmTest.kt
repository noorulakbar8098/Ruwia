package com.example.ruwia

import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.domain.deriveShopStockTotals
import com.example.ruwia.domain.netStockPerProduct
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
}