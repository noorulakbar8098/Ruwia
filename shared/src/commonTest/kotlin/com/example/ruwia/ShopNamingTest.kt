package com.example.ruwia

import com.example.ruwia.domain.DEFAULT_SHOP_ONE
import com.example.ruwia.domain.DEFAULT_SHOP_TWO
import com.example.ruwia.domain.ShopStockInfo
import com.example.ruwia.domain.orderedShopSlots
import com.example.ruwia.domain.resolveShopNames
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopNamingTest {

    private fun shop(id: String, name: String, location: String = "") =
        ShopStockInfo(id = id, name = name, location = location)

    @Test
    fun emptyConfigurationFallsBackToDefaultNames() {
        assertEquals(listOf(DEFAULT_SHOP_ONE, DEFAULT_SHOP_TWO), resolveShopNames(emptyList()))
    }

    @Test
    fun customNamesAreReturnedExactlyOnce() {
        val rows = listOf(
            shop("1", "Thondamuthur", "Primary Shop"),
            shop("2", "RS Puram", "Secondary Shop"),
        )
        assertEquals(listOf("Thondamuthur", "RS Puram"), resolveShopNames(rows))
    }

    @Test
    fun strayDefaultRowNeverBecomesAThirdShop() {
        val rows = listOf(
            shop("1", "Thondamuthur", "Primary Shop"),
            shop("2", "RS Puram", "Secondary Shop"),
            shop("3", DEFAULT_SHOP_ONE, "Primary Shop"),
        )
        assertEquals(listOf("Thondamuthur", "RS Puram"), resolveShopNames(rows))
        assertEquals(2, orderedShopSlots(rows).size)
    }

    @Test
    fun oneCustomShopPairsWithTheMissingDefault() {
        val rows = listOf(shop("1", "Thondamuthur", "Primary Shop"))
        assertEquals(listOf("Thondamuthur", DEFAULT_SHOP_TWO), resolveShopNames(rows))
    }

    @Test
    fun locationsKeepTheOriginalShopMapping() {
        val rows = listOf(
            shop("1", "RS Puram", "Secondary Shop"),
            shop("2", "Thondamuthur", "Primary Shop"),
        )
        assertEquals(listOf("Thondamuthur", "RS Puram"), resolveShopNames(rows))
    }
}
