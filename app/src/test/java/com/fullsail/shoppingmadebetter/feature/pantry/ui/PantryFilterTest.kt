package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [applyPantryFilters] — the pantry dashboard filtering logic. */
class PantryFilterTest {

    private fun item(id: String, expiresInDays: Int?) = InventoryItem(
        id = id,
        productId = "p-$id",
        name = "Item $id",
        brand = "Brand",
        description = "",
        size = "1 ea",
        imageUrl = "",
        quantity = 1,
        expiresInDays = expiresInDays,
    )

    private val expired = item("expired", -2)
    private val today = item("today", 0)
    private val soon = item("soon", EXPIRING_SOON_DAYS - 1)
    private val boundary = item("boundary", EXPIRING_SOON_DAYS)
    private val pastBoundary = item("pastBoundary", EXPIRING_SOON_DAYS + 1)
    private val later = item("later", EXPIRING_SOON_DAYS + 10)
    private val noDate = item("noDate", null)

    private val allItems =
        listOf(expired, today, soon, boundary, pastBoundary, later, noDate)

    @Test
    fun `no filters returns the full list unchanged`() {
        assertEquals(allItems, applyPantryFilters(allItems, emptySet()))
    }

    @Test
    fun `expiring filter keeps only items due within the threshold, including overdue ones`() {
        val result = applyPantryFilters(allItems, setOf(PantryDashboardFilter.Expiring))

        // Overdue, due today, and due on or before the threshold day survive;
        // anything past it and unknown dates are filtered out.
        assertEquals(listOf(expired, today, soon, boundary), result)
    }

    @Test
    fun `expiring filter keeps exactly the items whose chip is not grey`() {
        // The dashboard card and the card chips must agree on what "expiring"
        // means: the threshold day is still yellow, so it must survive the filter.
        assertEquals(ExpiryBucket.Soon, expiryBucket(EXPIRING_SOON_DAYS))
        assertEquals(ExpiryBucket.Later, expiryBucket(EXPIRING_SOON_DAYS + 1))

        val result = applyPantryFilters(allItems, setOf(PantryDashboardFilter.Expiring))

        assertTrue(boundary in result)
        assertFalse(pastBoundary in result)
    }

    @Test
    fun `an unwired filter alone leaves the list unchanged`() {
        assertEquals(allItems, applyPantryFilters(allItems, setOf(PantryDashboardFilter.RunningLow)))
    }

    @Test
    fun `expiring filter still applies when combined with an unwired filter`() {
        val result = applyPantryFilters(
            allItems,
            setOf(PantryDashboardFilter.Expiring, PantryDashboardFilter.Fridge),
        )

        assertEquals(listOf(expired, today, soon, boundary), result)
    }
}