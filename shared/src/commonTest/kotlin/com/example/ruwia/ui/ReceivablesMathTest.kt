package com.example.ruwia.ui

import com.example.ruwia.domain.Customer
import com.example.ruwia.domain.Payment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReceivablesMathTest {

    @Test
    fun formatsWholeRupeesWithIndianGrouping() {
        assertEquals("₹0", formatReceivableAmount(0.0))
        assertEquals("₹750", formatReceivableAmount(750.0))
        assertEquals("₹1,23,456", formatReceivableAmount(123456.0))
    }

    @Test
    fun formatsPaiseOnlyWhenPresent() {
        assertEquals("₹1,234.50", formatReceivableAmount(1234.5))
        assertEquals("₹1,23,456.78", formatReceivableAmount(123456.78))
        assertEquals("₹1,000", formatReceivableAmount(999.999))
    }

    @Test
    fun formatsCreditsWithLeadingMinus() {
        assertEquals("-₹1,234.50", formatReceivableAmount(-1234.5))
    }

    @Test
    fun totalsExcludeNegativeBalancesAndLegacyPayments() {
        val customers = listOf(
            Customer(name = "Asha", balance = 100.0),
            Customer(name = "Bala", balance = 50.5),
            Customer(name = "Credit", balance = -20.0),
            Customer(name = "Clear", balance = 0.0),
        )
        val payments = listOf(
            Payment(amount = 20.0, referenceKey = "sale-payment:1"),
            Payment(amount = 10.0, referenceKey = "sale-payment:2"),
            Payment(amount = 999.0, referenceKey = null),
        )

        val totals = computeReceivableTotals(customers, payments)

        assertEquals(150.5, totals.outstanding)
        assertEquals(30.0, totals.collected)
        assertEquals(180.5, totals.billed)
    }

    @Test
    fun totalsAreZeroWithoutData() {
        val totals = computeReceivableTotals(emptyList(), emptyList())

        assertEquals(0.0, totals.outstanding)
        assertEquals(0.0, totals.collected)
        assertEquals(0.0, totals.billed)
    }

    @Test
    fun sanitizesTypedAmountsToTwoPaiseDigits() {
        assertEquals("", sanitizePaymentAmountInput(""))
        assertEquals("0", sanitizePaymentAmountInput("000"))
        assertEquals("12.34", sanitizePaymentAmountInput("12abc.345"))
        assertEquals("0.50", sanitizePaymentAmountInput(".50"))
        assertEquals("12.", sanitizePaymentAmountInput("12."))
    }

    @Test
    fun parsesAndValidatesPaymentAmounts() {
        assertEquals(42.5, parsePaymentAmount("42.5"))
        assertEquals(0.0, parsePaymentAmount("not a number"))

        assertTrue(isValidPaymentAmount(100.0, 100.0))
        assertTrue(isValidPaymentAmount(0.5, 100.0))
        assertFalse(isValidPaymentAmount(0.0, 100.0))
        assertFalse(isValidPaymentAmount(-5.0, 100.0))
        assertFalse(isValidPaymentAmount(100.05, 100.0))
    }

    @Test
    fun paymentInputPrefillsTheBalanceWithoutCurrencyNoise() {
        assertEquals("1000", paymentAmountInput(1000.0))
        assertEquals("1234.50", paymentAmountInput(1234.5))
    }

    @Test
    fun collectionProgressIsClampedToZeroToOne() {
        assertEquals(0f, collectionProgress(0.0, 0.0))
        assertEquals(0f, collectionProgress(10.0, 0.0))
        assertEquals(0.25f, collectionProgress(50.0, 200.0))
        assertEquals(1f, collectionProgress(250.0, 200.0))
        assertEquals(0f, collectionProgress(-10.0, 200.0))
    }
}
