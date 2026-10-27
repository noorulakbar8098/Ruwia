package com.example.ruwia

import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.SizeUnit
import com.example.ruwia.domain.canBalancePreview
import com.example.ruwia.domain.canonicalProductSize
import com.example.ruwia.domain.is20LitreCan
import com.example.ruwia.domain.parseCanCount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanTrackingTest {

    @Test
    fun detects20LitreSkuVariants() {
        listOf("20L", "20 L", "20ltr", "20 litre", "20 Liters").forEach { raw ->
            assertEquals(20.0, canonicalProductSize(raw)?.value, raw)
            assertEquals(SizeUnit.LITRE, canonicalProductSize(raw)?.unit, raw)
        }
    }

    @Test
    fun doesNotConfuseOtherSizesOrLitres() {
        listOf("200ml", "2L", "10L", "201L", "20 ml", "5 litre").forEach { raw ->
            val size = canonicalProductSize(raw)
            assertFalse(size?.unit == SizeUnit.LITRE && size.value == 20.0, raw)
        }
    }

    @Test
    fun identifiesOnly20LitreProducts() {
        assertTrue(ProductCategory(id = "a", name = "20L", displayName = "Clear - 20L").is20LitreCan())
        assertTrue(ProductCategory(id = "b", name = "20 L", displayName = "Brand").is20LitreCan())
        assertFalse(ProductCategory(id = "c", name = "10L", displayName = "Clear - 10L").is20LitreCan())
        assertFalse(ProductCategory(id = "d", name = "500ml", displayName = "Clear - 500ml").is20LitreCan())
    }

    @Test
    fun fallsBackToDisplayNameWhenSkuHasNoSize() {
        assertTrue(ProductCategory(id = "e", name = "CLEAR", displayName = "Neer Thuli 20L").is20LitreCan())
        assertFalse(ProductCategory(id = "f", name = "CLEAR", displayName = "Neer Thuli 10L").is20LitreCan())
    }

    @Test
    fun balanceMatchesTheBusinessFormula() {
        assertEquals(5, canBalancePreview(10, 0, 5).outstandingAfter)
        assertEquals(8, canBalancePreview(10, 3, 5).outstandingAfter)
        assertEquals(0, canBalancePreview(0, 0, 0).outstandingAfter)
    }

    @Test
    fun returnLimitUsesOnlyThePreSaleOutstandingBalance() {
        val preview = canBalancePreview(10, 3, 10)
        assertTrue(preview.isWithinLimit)
        assertEquals(10, preview.maxReturnable)
        assertFalse(canBalancePreview(10, 3, 13).isWithinLimit)
        assertFalse(canBalancePreview(10, 0, 11).isWithinLimit)
    }

    @Test
    fun currentSaleCansDoNotIncreaseTheReturnLimit() {
        val preview = canBalancePreview(5, 5, 6)
        assertEquals(5, preview.maxReturnable)
        assertFalse(preview.isWithinLimit)
        assertEquals(5, canBalancePreview(5, 5, 5).outstandingAfter)
    }

    @Test
    fun parsesDigitsOnlyCounts() {
        assertEquals(0, parseCanCount("0"))
        assertEquals(5, parseCanCount(" 5 "))
        assertNull(parseCanCount(""))
        assertNull(parseCanCount("-1"))
        assertNull(parseCanCount("5 cans"))
    }
}
