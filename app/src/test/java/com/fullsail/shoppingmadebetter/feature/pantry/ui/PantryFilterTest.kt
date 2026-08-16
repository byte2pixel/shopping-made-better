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
        quantity: Int = 1,
        lowStockThreshold: Int? = null,
    ) = InventoryItem(
        id = id,
        productId = "p-$id",
        name = "Item $id",
        brand = "Brand",
        description = "",
        size = "1 ea",
        imageUrl = "",
        quantity = quantity,
        expiresInDays = expiresInDays,
        location = location,
        lowStockThreshold = lowStockThreshold,
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
    private val pantryB = item("pantryB", expiresInDays = 2, location = PantryLocation.Pantry)

    private val locatedItems = listOf(freezerA, fridge, freezerB, pantry, fridgeB, pantryB)

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
    fun `fridge and expiring compose as an intersection`() {
        val result = applyPantryFilters(
            locatedItems,
            setOf(PantryDashboardFilter.Fridge, PantryDashboardFilter.Expiring),
        )

        // fridge is due in 3 days so it clears the threshold; fridgeB is 30 days out
        // and drops. Only the fridge item matching both predicates survives.
        assertEquals(listOf(fridge), result)
    }

    @Test
    fun `pantry filter keeps only items stored in the pantry`() {
        val result = applyPantryFilters(locatedItems, setOf(PantryDashboardFilter.Pantry))

        // Order is preserved and fridge/freezer locations are dropped.
        assertEquals(listOf(pantry, pantryB), result)
    }

    @Test
    fun `pantry and expiring compose as an intersection`() {
        val result = applyPantryFilters(
            locatedItems,
            setOf(PantryDashboardFilter.Pantry, PantryDashboardFilter.Expiring),
        )

        // pantry is 100 days out and drops; pantryB is due in 2 days so it clears
        // the threshold. Only the pantry item matching both predicates survives.
        assertEquals(listOf(pantryB), result)
    }

    // Low stock is opt-in per item: an item counts as low only when it has a
    // lowStockThreshold set and its quantity is between 1 and that threshold.
    private val low = item("low", expiresInDays = null, quantity = 2, lowStockThreshold = 3)
    private val lowBoundary = item("lowBoundary", expiresInDays = null, quantity = 3, lowStockThreshold = 3)
    private val outWithThreshold = item("out", expiresInDays = null, quantity = 0, lowStockThreshold = 3)
    private val outNoThreshold = item("outNoThreshold", expiresInDays = null, quantity = 0, lowStockThreshold = null)
    private val noThreshold = item("noThreshold", expiresInDays = null, quantity = 1, lowStockThreshold = null)
    private val wellStocked = item("wellStocked", expiresInDays = null, quantity = 5, lowStockThreshold = 3)

    private val stockItems =
        listOf(low, lowBoundary, outWithThreshold, outNoThreshold, noThreshold, wellStocked)

    @Test
    fun `low stock filter keeps items at or below their own threshold`() {
        val result = applyPantryFilters(stockItems, setOf(PantryDashboardFilter.RunningLow))

        // Order is preserved; only items within 1..threshold survive.
        assertEquals(listOf(low, lowBoundary), result)
    }

    @Test
    fun `low stock filter excludes out-of-stock and items with no threshold`() {
        val result = applyPantryFilters(stockItems, setOf(PantryDashboardFilter.RunningLow))

        // Quantity 0 is "out" not "low"; a null threshold is never low; quantity
        // above the threshold is well stocked.
        assertFalse(outWithThreshold in result)
        assertFalse(noThreshold in result)
        assertFalse(wellStocked in result)
    }

    @Test
    fun `out filter keeps only zero-quantity items, regardless of threshold`() {
        val result = applyPantryFilters(stockItems, setOf(PantryDashboardFilter.Out))

        // Order is preserved; both out items survive whether or not they have a
        // threshold, and nothing with stock on hand does.
        assertEquals(listOf(outWithThreshold, outNoThreshold), result)
    }

    @Test
    fun `out and low stock are disjoint, so their intersection is empty`() {
        // Quantity 0 is "out"; "low" is 1..threshold. No item can be both, so
        // composing the two filters yields nothing.
        val result = applyPantryFilters(
            stockItems,
            setOf(PantryDashboardFilter.RunningLow, PantryDashboardFilter.Out),
        )

        assertTrue(result.isEmpty())
    }
}