package com.example.ruwia

import com.example.ruwia.domain.getUnitsPerCase
import com.example.ruwia.presentation.toDashboardMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import io.github.jan.supabase.postgrest.from

class SharedLogicJvmTest {

    @Test
    fun testGetUnitsPerCase() {
        // All should return 1 as they are considered single items
        // 300ml -> 1
        assertEquals(1, getUnitsPerCase("300ml"))
        assertEquals(1, getUnitsPerCase("300ML Water Can"))

        // 500ml -> 1
        assertEquals(1, getUnitsPerCase("500ml"))
        assertEquals(1, getUnitsPerCase("500ML Water Can"))

        // 1 Litre / 1L / 1lt -> 1
        assertEquals(1, getUnitsPerCase("1 Litre"))
        assertEquals(1, getUnitsPerCase("1 - Litre"))
        assertEquals(1, getUnitsPerCase("1 Litre Water Can"))
        assertEquals(1, getUnitsPerCase("1 Litres"))
        assertEquals(1, getUnitsPerCase("1L"))
        assertEquals(1, getUnitsPerCase("1 l"))
        assertEquals(1, getUnitsPerCase("1lt"))
        assertEquals(1, getUnitsPerCase("1 lt"))
        assertEquals(1, getUnitsPerCase("Kinley 1 Litre"))
        assertEquals(1, getUnitsPerCase("Kinley 1L"))

        // 2 Litre / 2L / 2lt -> 1
        assertEquals(1, getUnitsPerCase("2 Litre"))
        assertEquals(1, getUnitsPerCase("2 - Litre"))
        assertEquals(1, getUnitsPerCase("2 Litre Water Can"))
        assertEquals(1, getUnitsPerCase("2 Litres"))
        assertEquals(1, getUnitsPerCase("2L"))
        assertEquals(1, getUnitsPerCase("2 l"))
        assertEquals(1, getUnitsPerCase("2lt"))
        assertEquals(1, getUnitsPerCase("2 lt"))
        assertEquals(1, getUnitsPerCase("Kinley 2 Litre"))
        assertEquals(1, getUnitsPerCase("Kinley 2L"))

        // 5 Litre -> 1
        assertEquals(1, getUnitsPerCase("5 Litre"))
        assertEquals(1, getUnitsPerCase("5 - litre"))
        assertEquals(1, getUnitsPerCase("5 Litre Water Can"))
        assertEquals(1, getUnitsPerCase("5L"))

        // 20 Litre -> 1
        assertEquals(1, getUnitsPerCase("20 Litre"))
        assertEquals(1, getUnitsPerCase("20 - litre"))
        assertEquals(1, getUnitsPerCase("20L Water Can"))
        assertEquals(1, getUnitsPerCase("20L"))
        assertEquals(1, getUnitsPerCase("20 l"))
    }

    @Test
    fun testDeriveShopStockTotals() {
        val rawShops = listOf(
            com.example.ruwia.domain.ShopStockInfo(
                id = "s1",
                name = "Shop 1 · Main",
                location = "SAIBABA COLONY"
            )
        )
        val movements = listOf(
            com.example.ruwia.domain.StockMovement(
                id = "m1",
                source = "Empty cans · Customer A",
                qty = 30,
                type = "inward",
                shopName = "Shop 1"
            ),
            com.example.ruwia.domain.StockMovement(
                id = "m2",
                source = "Empty cans · Customer B",
                qty = 40,
                type = "inward",
                shopName = "Shop 1"
            ),
            com.example.ruwia.domain.StockMovement(
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
        val derived = com.example.ruwia.ui.deriveShopStockTotals(rawShops, movements, stockItems)
        assertEquals(1, derived.size)
        assertEquals(50.0, derived[0].emptyCans)
    }

    @Test
    fun testDashboardMetricsTotalStockCases() {
        val state = com.example.ruwia.presentation.AdminState(
            productCategories = listOf(
                com.example.ruwia.domain.ProductCategory(id = "p1", name = "500ML", displayName = "Neerthuli - 500ML", stockAvailable = 20),
                com.example.ruwia.domain.ProductCategory(id = "p2", name = "1L", displayName = "Droply - 1L", stockAvailable = 10),
                com.example.ruwia.domain.ProductCategory(id = "p3", name = "20L", displayName = "Bisleri - 20L", stockAvailable = 20)
            )
        )
        val metrics = state.toDashboardMetrics()
        // Verify that the total stock is the direct sum of the available stocks (20 + 10 + 20 = 50.0)
        // and is not divided by any case size logic (which would make it ~37.5 cases in the old code)
        assertEquals(50.0, metrics.totalStockUnits)
    }
}








