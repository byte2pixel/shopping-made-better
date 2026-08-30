package com.fullsail.shoppingmadebetter.feature.history.domain

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the spend arithmetic: month bucketing, the store breakdown's
 * shares, and the two comparisons that must refuse to mislead when a store cannot
 * price the whole basket.
 */
class SpendInsightsTest {

    @Test
    fun `a month starts on the first`() {
        assertEquals(AUGUST, LocalDate(2026, 8, 28).startOfMonth())
        assertEquals(AUGUST, AUGUST.startOfMonth())
    }

    @Test
    fun `the previous month crosses a year boundary`() {
        assertEquals(LocalDate(2026, 7, 1), LocalDate(2026, 8, 28).previousMonthStart())
        assertEquals(LocalDate(2025, 12, 1), LocalDate(2026, 1, 15).previousMonthStart())
    }

    @Test
    fun `a month total sums every store in it`() {
        val rows = listOf(
            monthRow(AUGUST, storeId = "s-1", total = 71.20, tripCount = 2),
            monthRow(AUGUST, storeId = "s-2", total = 48.10, tripCount = 1),
            monthRow(JULY, storeId = "s-1", total = 999.0, tripCount = 9),
        )

        val total = rows.monthTotal(AUGUST)

        assertEquals(119.30, total.total, EPSILON)
        assertEquals(3, total.tripCount)
        assertEquals(AUGUST, total.monthStart)
    }

    @Test
    fun `a month with no rows totals zero trips`() {
        // What a first-of-the-month visit sees; the card shows a total, not a delta.
        val total = emptyList<com.fullsail.shoppingmadebetter.feature.history.data.SpendByMonthStoreDto>()
            .monthTotal(AUGUST)

        assertEquals(0.0, total.total, EPSILON)
        assertEquals(0, total.tripCount)
    }

    @Test
    fun `the breakdown is biggest first and its shares sum to one`() {
        val rows = listOf(
            monthRow(AUGUST, storeId = "s-1", storeName = "ALDI", total = 25.0),
            monthRow(AUGUST, storeId = "s-2", storeName = "Publix", total = 75.0),
        )

        val breakdown = rows.storeBreakdown(AUGUST)

        assertEquals(listOf("Publix", "ALDI"), breakdown.map { it.storeName })
        assertEquals(0.75, breakdown[0].share, EPSILON)
        assertEquals(0.25, breakdown[1].share, EPSILON)
        assertEquals(1.0, breakdown.sumOf { it.share }, EPSILON)
    }

    @Test
    fun `the breakdown ignores other months`() {
        val rows = listOf(
            monthRow(AUGUST, storeId = "s-1", total = 10.0),
            monthRow(JULY, storeId = "s-2", total = 90.0),
        )

        assertEquals(1, rows.storeBreakdown(AUGUST).size)
    }

    @Test
    fun `a month that spent nothing has no breakdown`() {
        // Guards the share division; a zero month must not divide by zero.
        val rows = listOf(monthRow(AUGUST, total = 0.0))

        assertTrue(rows.storeBreakdown(AUGUST).isEmpty())
    }

    @Test
    fun `the cheapest store is the one with the lowest total`() {
        val costs = listOf(
            costRow(storeId = "s-1", storeName = "ALDI", costHere = 40.0, paidForSameItems = 50.0),
            costRow(storeId = "s-2", storeName = "Publix", costHere = 48.0, paidForSameItems = 50.0),
        )

        val cheapest = cheapestStore(costs)

        assertEquals("ALDI", cheapest?.storeName)
        assertEquals(40.0, cheapest?.cost ?: 0.0, EPSILON)
        assertEquals(10.0, cheapest?.saving ?: 0.0, EPSILON)
    }

    @Test
    fun `a store missing prices is not in the running`() {
        // Its total covers a smaller basket, so it would win by not stocking things.
        val costs = listOf(
            costRow(storeId = "s-1", storeName = "ALDI", costHere = 5.0, itemsPriced = 1),
            costRow(storeId = "s-2", storeName = "Publix", costHere = 48.0, paidForSameItems = 50.0),
            costRow(storeId = "s-3", storeName = "Whole Foods", costHere = 49.0, paidForSameItems = 50.0),
        )

        assertEquals("Publix", cheapestStore(costs)?.storeName)
    }

    @Test
    fun `a store absent from one trip is not in the running`() {
        // Publix is only on one of the two trips, so its total covers less shopping
        // than the stores it would beat. Two full-coverage stores remain, so the
        // comparison still happens -- just without it.
        val costs = listOf(
            costRow(purchaseId = "t-1", storeId = "s-1", costHere = 10.0, paidForSameItems = 12.0),
            costRow(purchaseId = "t-2", storeId = "s-1", costHere = 10.0, paidForSameItems = 12.0),
            costRow(purchaseId = "t-1", storeId = "s-3", storeName = "Whole Foods", costHere = 11.0),
            costRow(purchaseId = "t-2", storeId = "s-3", storeName = "Whole Foods", costHere = 11.0),
            costRow(purchaseId = "t-1", storeId = "s-2", storeName = "Publix", costHere = 1.0),
        )

        assertEquals("ALDI", cheapestStore(costs)?.storeName)
    }

    @Test
    fun `one store alone is not a comparison`() {
        assertNull(cheapestStore(listOf(costRow(storeId = "s-1"))))
    }

    @Test
    fun `dropping stores below two candidates cancels the comparison`() {
        // Only ALDI prices the whole basket, so there is nothing to call it cheaper
        // than -- naming it would be a claim the data cannot support.
        val costs = listOf(
            costRow(storeId = "s-1", storeName = "ALDI", costHere = 10.0, paidForSameItems = 12.0),
            costRow(storeId = "s-2", storeName = "Publix", costHere = 1.0, itemsPriced = 1),
        )

        assertNull(cheapestStore(costs))
    }

    @Test
    fun `no saving means no card`() {
        // Already shopping at the cheapest store; claiming a saving would be a lie.
        val costs = listOf(
            costRow(storeId = "s-1", costHere = 50.0, paidForSameItems = 50.0),
            costRow(storeId = "s-2", storeName = "Publix", costHere = 55.0, paidForSameItems = 50.0),
        )

        assertNull(cheapestStore(costs))
    }

    @Test
    fun `no trips means no cheapest store`() {
        assertNull(cheapestStore(emptyList()))
    }

    @Test
    fun `a trip comparison is cheapest first with signed differences`() {
        val costs = listOf(
            costRow(storeId = "s-1", storeName = "ALDI", costHere = 44.0, paidForSameItems = 42.0),
            costRow(storeId = "s-2", storeName = "Publix", costHere = 40.0, paidForSameItems = 42.0),
        )

        val comparison = tripComparison(costs)

        assertEquals(listOf("Publix", "ALDI"), comparison.map { it.storeName })
        // Publix would have been cheaper; ALDI dearer, so its difference is negative.
        assertEquals(2.0, comparison[0].difference, EPSILON)
        assertEquals(-2.0, comparison[1].difference, EPSILON)
    }

    @Test
    fun `a trip comparison drops stores missing prices`() {
        val costs = listOf(
            costRow(storeId = "s-1", storeName = "ALDI"),
            costRow(storeId = "s-2", storeName = "Publix"),
            costRow(storeId = "s-3", storeName = "Whole Foods", itemsPriced = 2, itemsTotal = 4),
        )

        assertEquals(listOf("ALDI", "Publix"), tripComparison(costs).map { it.storeName }.sorted())
    }

    @Test
    fun `a trip only one store can price has nothing to compare`() {
        val costs = listOf(
            costRow(storeId = "s-1", storeName = "ALDI"),
            costRow(storeId = "s-2", storeName = "Publix", itemsPriced = 0, itemsTotal = 4),
        )

        assertTrue(tripComparison(costs).isEmpty())
    }

    @Test
    fun `a trip with no items is not comparable`() {
        val costs = listOf(
            costRow(storeId = "s-1", itemsPriced = 0, itemsTotal = 0),
            costRow(storeId = "s-2", itemsPriced = 0, itemsTotal = 0),
        )

        assertTrue(tripComparison(costs).isEmpty())
    }

    private companion object {
        val AUGUST = LocalDate(2026, 8, 1)
        val JULY = LocalDate(2026, 7, 1)
        const val EPSILON = 0.001
    }
}
