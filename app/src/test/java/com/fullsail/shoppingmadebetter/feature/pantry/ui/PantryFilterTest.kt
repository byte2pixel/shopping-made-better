package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import org.junit.Assert.assertEquals
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
    private val later = item("later", EXPIRING_SOON_DAYS + 10)
    private val noDate = item("noDate", null)

    private val allItems = listOf(expired, today, soon, boundary, later, noDate)

    @Test
    fun `no filters returns the full list unchanged`() {
        assertEquals(allItems, applyPantryFilters(allItems, emptySet()))
    }

    @Test
    fun `expiring filter keeps only items due within the threshold, including overdue ones`() {
        val result = applyPantryFilters(allItems, setOf(PantryDashboardFilter.Expiring))

        // Overdue, due today, and due within the threshold survive; the boundary
        // day, later dates, and unknown dates are filtered out.
        assertEquals(listOf(expired, today, soon), result)
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

        assertEquals(listOf(expired, today, soon), result)
    }
}