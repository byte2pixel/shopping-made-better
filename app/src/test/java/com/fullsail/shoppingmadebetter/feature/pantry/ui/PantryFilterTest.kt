package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [applyPantryFilters] — the pantry dashboard filtering logic. */
class PantryFilterTest {

    private fun item(
        id: String,
        expiresInDays: Int?,
        location: PantryLocation = PantryLocation.Pantry,
    ) = InventoryItem(
        id = id,
        productId = "p-$id",
        name = "Item $id",
        brand = "Brand",
        description = "",
        size = "1 ea",
        imageUrl = "",
        quantity = 1,
        expiresInDays = expiresInDays,
        location = location,
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

    private val freezerA = item("freezerA", expiresInDays = 30, location = PantryLocation.Freezer)
    private val freezerB = item("freezerB", expiresInDays = null, location = PantryLocation.Freezer)
    private val fridge = item("fridge", expiresInDays = 3, location = PantryLocation.Fridge)
    private val fridgeB = item("fridgeB", expiresInDays = 30, location = PantryLocation.Fridge)
    private val pantry = item("pantry", expiresInDays = 100, location = PantryLocation.Pantry)

    private val locatedItems = listOf(freezerA, fridge, freezerB, pantry, fridgeB)

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
            setOf(PantryDashboardFilter.Expiring, PantryDashboardFilter.RunningLow),
        )

        assertEquals(listOf(expired, today, soon, boundary), result)
    }

    @Test
    fun `freezer filter keeps only items stored in the freezer`() {
        val result = applyPantryFilters(locatedItems, setOf(PantryDashboardFilter.Freezer))

        // Order is preserved and non-freezer locations are dropped.
        assertEquals(listOf(freezerA, freezerB), result)
    }

    @Test
    fun `clearing the freezer filter shows every item again`() {
        // Tapping the card again removes it from the selected set; with no active
        // predicate the full list comes back unchanged.
        assertEquals(locatedItems, applyPantryFilters(locatedItems, emptySet()))
    }

    @Test
    fun `freezer filter still applies when combined with an unwired filter`() {
        val result = applyPantryFilters(
            locatedItems,
            setOf(PantryDashboardFilter.Freezer, PantryDashboardFilter.RunningLow),
        )

        assertEquals(listOf(freezerA, freezerB), result)
    }

    @Test
    fun `freezer and expiring compose as an intersection`() {
        val result = applyPantryFilters(
            locatedItems,
            setOf(PantryDashboardFilter.Freezer, PantryDashboardFilter.Expiring),
        )

        // freezerA is in the freezer but not expiring soon (30 days); freezerB has no
        // date; only items matching both survive — here, none do.
        assertTrue(result.isEmpty())

        // A freezer item that is also expiring soon survives both predicates.
        val expiringFreezerItem =
            item("freezerExpiring", expiresInDays = 1, location = PantryLocation.Freezer)
        val withExpiring = applyPantryFilters(
            locatedItems + expiringFreezerItem,
            setOf(PantryDashboardFilter.Freezer, PantryDashboardFilter.Expiring),
        )
        assertEquals(listOf(expiringFreezerItem), withExpiring)
    }

    @Test
    fun `fridge filter keeps only items stored in the fridge`() {
        val result = applyPantryFilters(locatedItems, setOf(PantryDashboardFilter.Fridge))

        // Order is preserved and non-fridge locations are dropped.
        assertEquals(listOf(fridge, fridgeB), result)
    }

    @Test
    fun `fridge filter still applies when combined with an unwired filter`() {
        val result = applyPantryFilters(
            locatedItems,
            setOf(PantryDashboardFilter.Fridge, PantryDashboardFilter.RunningLow),
        )

        assertEquals(listOf(fridge, fridgeB), result)
    }

    @Test
    fun `fridge and expiring compose as an intersection`() {
        val result = applyPantryFilters(
            locatedItems,
            setOf(PantryDashboardFilter.Fridge, PantryDashboardFilter.Expiring),
        )

        // fridge is due in 3 days so it clears the threshold; fridgeB is 30 days out
        // and drops. Only the fridge item matching both predicates survives.
        assertEquals(listOf(fridge), result)
    }
}