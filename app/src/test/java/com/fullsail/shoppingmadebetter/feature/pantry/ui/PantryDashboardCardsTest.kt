package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for [pantryDashboardCards] — the dashboard card count logic. */
class PantryDashboardCardsTest {

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

    private val items = listOf(
        item("expired", -2),
        item("today", 0),
        item("soon", EXPIRING_SOON_DAYS - 1),
        item("boundary", EXPIRING_SOON_DAYS),
        item("later", EXPIRING_SOON_DAYS + 10),
        item("noDate", null),
    )

    private fun cardCount(filter: PantryDashboardFilter, items: List<InventoryItem>) =
        pantryDashboardCards(items).single { it.filter == filter }.count

    @Test
    fun `produces one card per filter, in declaration order`() {
        val filters = pantryDashboardCards(items).map { it.filter }
        assertEquals(PantryDashboardFilter.entries, filters)
    }

    @Test
    fun `expiring card shows the real count of expiring items`() {
        // expired, today, and soon are within the threshold; boundary, later, and
        // the unknown date are not.
        assertEquals(3, cardCount(PantryDashboardFilter.Expiring, items))
    }

    @Test
    fun `expiring card count matches the expiring filter result size`() {
        val filtered = applyPantryFilters(items, setOf(PantryDashboardFilter.Expiring))
        assertEquals(filtered.size, cardCount(PantryDashboardFilter.Expiring, items))
    }

    @Test
    fun `unwired card counts are placeholders, independent of the inventory`() {
        // A filter without a predicate keeps its stand-in count regardless of what
        // is in the pantry.
        assertEquals(
            cardCount(PantryDashboardFilter.RunningLow, emptyList()),
            cardCount(PantryDashboardFilter.RunningLow, items),
        )
    }
}