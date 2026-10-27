package com.example.ruwia

import com.example.ruwia.domain.CustomerLedgerEntry
import com.example.ruwia.domain.summarizeCustomerLedger
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomerLedgerTest {

    @Test
    fun emptyLedgerSummarizesToZero() {
        val ledger = summarizeCustomerLedger(emptyList())

        assertEquals(0.0, ledger.billed)
        assertEquals(0.0, ledger.collected)
        assertEquals(0.0, ledger.outstanding)
    }

    @Test
    fun saleAndPaymentSummarizeToRemainingBalance() {
        val ledger = summarizeCustomerLedger(
            listOf(
                CustomerLedgerEntry(
                    customerId = "customer-1",
                    entryType = "sale",
                    saleClientKey = "sale-1",
                    debit = 20000.0,
                    balanceAfter = 20000.0,
                    referenceKey = "sale:sale-1",
                    createdAt = "2026-10-26T10:00:00Z",
                ),
                CustomerLedgerEntry(
                    customerId = "customer-1",
                    entryType = "payment",
                    saleClientKey = "sale-1",
                    paymentId = "payment-1",
                    credit = 10000.0,
                    balanceAfter = 10000.0,
                    referenceKey = "sale-payment:sale-1",
                    mode = "cash",
                    createdAt = "2026-10-26T10:05:00Z",
                ),
            ),
        )

        assertEquals(20000.0, ledger.billed, "The sale should appear as billed once.")
        assertEquals(10000.0, ledger.collected, "The received payment should appear as collected.")
        assertEquals(10000.0, ledger.outstanding, "Only the remaining balance should be outstanding.")
    }

    @Test
    fun fullyPaidSaleSummarizesToNoOutstandingBalance() {
        val ledger = summarizeCustomerLedger(
            listOf(
                CustomerLedgerEntry(
                    customerId = "customer-1",
                    entryType = "sale",
                    saleClientKey = "sale-2",
                    debit = 20000.0,
                    balanceAfter = 20000.0,
                    referenceKey = "sale:sale-2",
                    createdAt = "2026-10-27T10:00:00Z",
                ),
                CustomerLedgerEntry(
                    customerId = "customer-1",
                    entryType = "payment",
                    saleClientKey = "sale-2",
                    paymentId = "payment-2",
                    credit = 20000.0,
                    balanceAfter = 0.0,
                    referenceKey = "sale-payment:sale-2",
                    mode = "upi",
                    createdAt = "2026-10-27T10:05:00Z",
                ),
            ),
        )

        assertEquals(0.0, ledger.outstanding)
        assertEquals(20000.0, ledger.collected)
    }
}
