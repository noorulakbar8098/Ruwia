package com.example.ruwia

import com.example.ruwia.domain.ProductCategory
import com.example.ruwia.domain.StockMovement
import com.example.ruwia.ui.DispatchRange
import com.example.ruwia.ui.aggregateDispatch
import com.example.ruwia.ui.calculateYAxisRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

private val utc = TimeZone.UTC

private fun mov(
    id: String,
    qty: Int,
    productId: String?,
    createdAt: String,
    type: String = "outward",
    source: String = "Sale · Test",
) = StockMovement(
    id = id, source = source, qty = qty, type = type,
    productId = productId, createdAt = createdAt,
)

private val products = listOf(
    ProductCategory(id = "p1", name = "1L", displayName = "Kite - 1L"),
    ProductCategory(id = "p2", name = "2L", displayName = "Kite - 2L"),
    ProductCategory(id = "p3", name = "20L", displayName = "Clear - 20L"),
)

class DispatchAnalyticsTest {

    // 2026-09-19 is a Saturday.
    private val today = LocalDate(2026, 9, 19)

    @Test
    fun todayBucketsHourlyAndVariantsSplit() {
        val data = aggregateDispatch(
            listOf(
                mov("a", 2, "p1", "2026-09-19T08:10:00Z"),
                mov("b", 1, "p2", "2026-09-19T08:45:00Z"),
                mov("c", 3, "p1", "2026-09-19T15:05:00Z"),
                // Yesterday counts toward the previous period, not the buckets.
                mov("d", 9, "p1", "2026-09-18T10:00:00Z"),
            ),
            products, DispatchRange.TODAY, today, utc,
        )
        assertEquals(12, data.buckets.size)
        assertEquals(3, data.buckets[4].total) // 8–10 AM bin
        assertEquals(3, data.buckets[7].total) // 2–4 PM bin
        assertEquals(6, data.total)
        val byKey = data.variants.associateBy({ it.key }, { it.total })
        assertEquals(5, byKey["p1"])
        assertEquals(1, byKey["p2"])
        // Series carry the real product names with litre quantity.
        assertEquals("Kite - 1L", data.variants.find { it.key == "p1" }?.label)
        assertEquals("Kite - 2L", data.variants.find { it.key == "p2" }?.label)
        // Total always equals the sum of the variant series.
        assertEquals(6, data.variants.sumOf { it.total })
        assertEquals(9, data.previousTotal)
    }

    @Test
    fun excludesNonDispatchMovements() {
        val data = aggregateDispatch(
            listOf(
                mov("a", 5, "p1", "2026-09-19T09:00:00Z"),
                mov("b", 4, "p1", "2026-09-19T09:00:00Z", type = "inward", source = "Purchase"),
                mov("c", 3, "p1", "2026-09-19T09:00:00Z", source = "Empty cans · Test"),
                mov("d", 0, "p1", "2026-09-19T09:00:00Z"),
            ),
            products, DispatchRange.TODAY, today, utc,
        )
        assertEquals(5, data.total)
    }

    @Test
    fun unknownProductBecomesOther() {
        val data = aggregateDispatch(
            listOf(mov("a", 2, null, "2026-09-19T09:00:00Z")),
            products, DispatchRange.TODAY, today, utc,
        )
        assertEquals(1, data.variants.size)
        assertEquals("OTHER", data.variants[0].key)
        assertEquals("Other", data.variants[0].label)
    }

    @Test
    fun last7DaysDailyBucketsAndPreviousWindow() {
        val data = aggregateDispatch(
            listOf(
                mov("a", 2, "p1", "2026-09-19T08:00:00Z"), // today (Sat)
                mov("b", 3, "p1", "2026-09-13T08:00:00Z"), // 6 days ago — inside
                mov("c", 4, "p1", "2026-09-12T08:00:00Z"), // 7 days ago — previous
                mov("d", 5, "p1", "2026-09-06T08:00:00Z"), // 13 days ago — previous edge
                mov("e", 6, "p1", "2026-09-05T08:00:00Z"), // outside both windows
            ),
            products, DispatchRange.LAST_7_DAYS, today, utc,
        )
        assertEquals(7, data.buckets.size)
        assertEquals(5, data.total)
        assertEquals(9, data.previousTotal)
        assertEquals("Sat", data.buckets[6].axisLabel)
    }

    @Test
    fun thisMonthDailyBuckets() {
        val data = aggregateDispatch(
            listOf(
                mov("a", 2, "p3", "2026-09-01T08:00:00Z"),
                mov("b", 3, "p3", "2026-09-19T08:00:00Z"),
            ),
            products, DispatchRange.THIS_MONTH, today, utc,
        )
        assertEquals(19, data.buckets.size)
        assertEquals("1", data.buckets[0].axisLabel)
        assertEquals(5, data.total)
        assertEquals(0, data.previousTotal)
    }

    @Test
    fun axisIgnoresTotalUsesSeriesPeak() {
        // One bucket holds 20+18+15+12+12 = 77 total, but the highest
        // plotted series point is 20 -> the axis must ignore 77.
        val five = (1..5).map { ProductCategory(id = "w$it", name = "S$it", displayName = "W - S$it") }
        val qtys = listOf(20, 18, 15, 12, 12)
        val data = aggregateDispatch(
            qtys.mapIndexed { i, q -> mov("t$i", q, "w${i + 1}", "2026-09-19T16:10:00Z") },
            five, DispatchRange.TODAY, today, utc,
        )
        assertEquals(77, data.total)
        val plotted = data.buckets.flatMap { b -> data.variants.map { b.counts[it.key] ?: 0 } }
        assertEquals(20, plotted.maxOrNull())
        val axis = calculateYAxisRange(plotted)
        assertEquals(25, axis.max)
        assertEquals(listOf(0, 5, 10, 15, 20, 25), axis.ticks)
        assertTrue(axis.max < data.total)
    }

    @Test
    fun axisSmallPeak() {
        // Case B: series 5, 3, 2 -> axis from 5.
        val axis = calculateYAxisRange(listOf(5, 3, 2, 0))
        assertEquals(6, axis.max)
        assertEquals(listOf(0, 2, 4, 6), axis.ticks)
    }

    @Test
    fun axisLargePeak() {
        // Case C: series 100, 80, 65 -> axis from 100, not 245.
        val axis = calculateYAxisRange(listOf(100, 80, 65))
        assertEquals(120, axis.max)
        assertEquals(listOf(0, 40, 80, 120), axis.ticks)
    }

    @Test
    fun axisSingleValueAndEmpty() {
        // Case D: one product at 20 still renders with headroom.
        val single = calculateYAxisRange(listOf(20))
        assertEquals(25, single.max)
        // Case E: no data never crashes, yields a minimal axis.
        val empty = calculateYAxisRange(emptyList())
        assertEquals(1, empty.max)
        assertEquals(listOf(0, 1), empty.ticks)
    }

    @Test
    fun axisVeryLargeValues() {
        assertEquals(80, calculateYAxisRange(listOf(73)).max)
        assertEquals(150, calculateYAxisRange(listOf(125)).max)
        assertEquals(500, calculateYAxisRange(listOf(420)).max)
        assertEquals(1500, calculateYAxisRange(listOf(1280)).max)
        assertEquals(10000, calculateYAxisRange(listOf(9500)).max)
    }

    @Test
    fun axisRangeExamples() {
        // Spec §5: per-period peaks map to sensible maxima.
        assertEquals(25, calculateYAxisRange(listOf(20)).max) // Today
        assertEquals(50, calculateYAxisRange(listOf(46)).max) // Last 7 Days
        assertEquals(150, calculateYAxisRange(listOf(137)).max) // This Month
        assertEquals(100, calculateYAxisRange(listOf(82)).max) // Last Month
    }

    @Test
    fun everyVariantKeepsItsOwnNamedSeries() {
        // Five distinct products -> five series, no "Other" merging.
        val many = (1..5).map { ProductCategory(id = "q$it", name = "S$it", displayName = "B - S$it") }
        val data = aggregateDispatch(
            (1..5).map { mov("m$it", it, "q$it", "2026-09-19T09:00:00Z") },
            many, DispatchRange.TODAY, today, utc,
        )
        assertEquals(5, data.variants.size)
        assertTrue(data.variants.none { it.key == "OTHER" })
        // Ranked by volume, real names shown.
        assertEquals("q5", data.variants[0].key)
        assertEquals("B - S5", data.variants[0].label)
        assertEquals(15, data.total)
        assertEquals(15, data.variants.sumOf { it.total })
    }
}
