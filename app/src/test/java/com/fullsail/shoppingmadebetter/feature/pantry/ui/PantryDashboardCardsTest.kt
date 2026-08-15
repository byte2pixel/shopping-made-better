package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for [pantryDashboardCards] — the dashboard card count logic. */
class PantryDashboardCardsTest {

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

    private val items = listOf(
        item("expired", -2),
        item("today", 0),
        item("soon", EXPIRING_SOON_DAYS - 1),
        item("boundary", EXPIRING_SOON_DAYS),
        item("later", EXPIRING_SOON_DAYS + 10),
        item("noDate", null),
        // Two freezer items, neither expiring soon, so they don't affect other counts.
        item("freezerA", EXPIRING_SOON_DAYS + 20, location = PantryLocation.Freezer),
        item("freezerB", null, location = PantryLocation.Freezer),
        // Two fridge items, neither expiring soon, so they don't affect other counts.
        item("fridgeA", EXPIRING_SOON_DAYS + 20, location = PantryLocation.Fridge),
        item("fridgeB", null, location = PantryLocation.Fridge),
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
        // expired, today, soon, and the threshold day itself are all still colored
        // chips; later and the unknown date are not.
        assertEquals(4, cardCount(PantryDashboardFilter.Expiring, items))
    }

    @Test
    fun `expiring card count matches the expiring filter result size`() {
        val filtered = applyPantryFilters(items, setOf(PantryDashboardFilter.Expiring))
        assertEquals(filtered.size, cardCount(PantryDashboardFilter.Expiring, items))
    }

    @Test
    fun `freezer card shows the real count of freezer items`() {
        assertEquals(2, cardCount(PantryDashboardFilter.Freezer, items))
    }

    @Test
    fun `freezer card count matches the freezer filter result size`() {
        val filtered = applyPantryFilters(items, setOf(PantryDashboardFilter.Freezer))
        assertEquals(filtered.size, cardCount(PantryDashboardFilter.Freezer, items))
    }

    @Test
    fun `fridge card shows the real count of fridge items`() {
        assertEquals(2, cardCount(PantryDashboardFilter.Fridge, items))
    }

    @Test
    fun `fridge card count matches the fridge filter result size`() {
        val filtered = applyPantryFilters(items, setOf(PantryDashboardFilter.Fridge))
        assertEquals(filtered.size, cardCount(PantryDashboardFilter.Fridge, items))
    }

    @Test
    fun `pantry card shows the real count of pantry items`() {
        // The six items with no explicit location default to the pantry.
        assertEquals(6, cardCount(PantryDashboardFilter.Pantry, items))
    }

    @Test
    fun `pantry card count matches the pantry filter result size`() {
        val filtered = applyPantryFilters(items, setOf(PantryDashboardFilter.Pantry))
        assertEquals(filtered.size, cardCount(PantryDashboardFilter.Pantry, items))
    }

    // Low stock is opt-in per item, so it needs its own fixtures rather than the
    // shared list (whose items all have a null threshold).
    private val stockItems = listOf(
        item("low", null, quantity = 2, lowStockThreshold = 3),
        item("lowBoundary", null, quantity = 3, lowStockThreshold = 3),
        item("out", null, quantity = 0, lowStockThreshold = 3),
        item("noThreshold", null, quantity = 1, lowStockThreshold = null),
        item("wellStocked", null, quantity = 5, lowStockThreshold = 3),
    )

    @Test
    fun `running low card shows the real count of low-stock items`() {
        assertEquals(2, cardCount(PantryDashboardFilter.RunningLow, stockItems))
    }

    @Test
    fun `running low card count matches the low stock filter result size`() {
        val filtered = applyPantryFilters(stockItems, setOf(PantryDashboardFilter.RunningLow))
        assertEquals(filtered.size, cardCount(PantryDashboardFilter.RunningLow, stockItems))
    }

    @Test
    fun `running low card is empty until items get a threshold`() {
        // The shared items have no lowStockThreshold, mirroring today's data. Until
        // SCRUM-158 lets users set one, the running low filter shows nothing.
        assertEquals(0, cardCount(PantryDashboardFilter.RunningLow, items))
    }

    @Test
    fun `unwired card counts are placeholders, independent of the inventory`() {
        // A filter without a predicate keeps its stand-in count regardless of what
        // is in the pantry.
        assertEquals(
            cardCount(PantryDashboardFilter.Out, emptyList()),
            cardCount(PantryDashboardFilter.Out, items),
        )
    }
}