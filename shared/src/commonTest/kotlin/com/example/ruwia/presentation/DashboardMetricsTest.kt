package com.example.ruwia.presentation

import com.example.ruwia.domain.SaleEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashboardMetricsTest {

    private fun sale(date: String, customer: String) = SaleEntry(
        date = date,
        customerName = customer,
        productId = "p1",
        productName = "20L Can",
        qty = 1,
        totalSelling = 100.0,
    )

    @Test
    fun `monthly customer trend adds up cumulatively`() {
        val state = AdminState(
            currentMonth = "2026-11",
            previousMonth = "2026-10",
            saleEntries = listOf(
                sale("2026-11-02", "Asha"),
                sale("2026-11-03", "Asha"), // repeat customer: must not add twice
                sale("2026-11-03", "Bala"),
                sale("2026-11-05", "Chitra"),
            ),
        )

        val metrics = state.toDashboardMetrics(DashboardRange.MONTH)
        val series = metrics.customerChartRaw

        assertEquals(3, metrics.rangeCustomers)
        assertEquals(3.0, series.last())
        assertTrue(series.zipWithNext().all { (a, b) -> b >= a }, "customer trend must accumulate")
        assertEquals(1.0, series[1]) // Nov 2
        assertEquals(2.0, series[2]) // Nov 3 — Bala is the only new customer
        assertEquals(3.0, series[4]) // Nov 5 — Chitra
    }

    @Test
    fun `quarterly customer trend ends at distinct customers in the quarter`() {
        val state = AdminState(
            currentMonth = "2026-11",
            previousMonth = "2026-10",
            saleEntries = listOf(
                sale("2026-09-10", "Asha"),
                sale("2026-10-11", "Bala"),
                sale("2026-11-12", "Asha"),
            ),
        )

        val metrics = state.toDashboardMetrics(DashboardRange.QUARTER)
        val series = metrics.customerChartRaw

        assertEquals(2, metrics.rangeCustomers)
        assertEquals(2.0, series.last())
        assertTrue(series.zipWithNext().all { (a, b) -> b >= a })
    }
}
