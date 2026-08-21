package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ProductGroup
import com.fullsail.shoppingmadebetter.feature.pantry.domain.groupInventoryByProduct
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [pantryDashboardCards] — the dashboard card counts, which count the
 * product cards a filter matches rather than the lots behind them.
 */
class PantryDashboardCardsTest {

    private fun lot(
        id: String,
        productId: String = "p-$id",
        expiresInDays: Int? = null,
        location: PantryLocation = PantryLocation.Pantry,
        quantity: Int = 1,
        lowStockThreshold: Int? = null,
    ) = InventoryItem(
        id = id,
        productId = productId,
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

    private fun groupsOf(lots: List<InventoryItem>) = groupInventoryByProduct(lots)

    private val groups = groupsOf(
        listOf(
            lot("expired", expiresInDays = -2),
            lot("today", expiresInDays = 0),
            lot("soon", expiresInDays = EXPIRING_SOON_DAYS - 1),
            lot("boundary", expiresInDays = EXPIRING_SOON_DAYS),
            lot("later", expiresInDays = EXPIRING_SOON_DAYS + 10),
            lot("noDate", expiresInDays = null),
            // Two freezer products, neither expiring soon, so they don't affect other counts.
            lot("freezerA", expiresInDays = EXPIRING_SOON_DAYS + 20, location = PantryLocation.Freezer),
            lot("freezerB", expiresInDays = null, location = PantryLocation.Freezer),
            // Two fridge products, neither expiring soon, so they don't affect other counts.
            lot("fridgeA", expiresInDays = EXPIRING_SOON_DAYS + 20, location = PantryLocation.Fridge),
            lot("fridgeB", expiresInDays = null, location = PantryLocation.Fridge),
        ),
    )

    private fun cardCount(filter: PantryDashboardFilter, groups: List<ProductGroup>) =
        pantryDashboardCards(groups).single { it.filter == filter }.count

    @Test
    fun `produces one card per filter, in declaration order`() {
        val filters = pantryDashboardCards(groups).map { it.filter }
        assertEquals(PantryDashboardFilter.entries, filters)
    }

    @Test
    fun `expiring card shows the real count of expiring products`() {
        // expired, today, soon, and the threshold day itself are all still colored
        // chips; later and the unknown date are not.
        assertEquals(4, cardCount(PantryDashboardFilter.Expiring, groups))
    }

    @Test
    fun `expiring card count matches the expiring filter result size`() {
        val filtered = applyPantryFilters(groups, setOf(PantryDashboardFilter.Expiring))
        assertEquals(filtered.size, cardCount(PantryDashboardFilter.Expiring, groups))
    }

    @Test
    fun `freezer card shows the real count of freezer products`() {
        assertEquals(2, cardCount(PantryDashboardFilter.Freezer, groups))
    }

    @Test
    fun `freezer card count matches the freezer filter result size`() {
        val filtered = applyPantryFilters(groups, setOf(PantryDashboardFilter.Freezer))
        assertEquals(filtered.size, cardCount(PantryDashboardFilter.Freezer, groups))
    }

    @Test
    fun `fridge card shows the real count of fridge products`() {
        assertEquals(2, cardCount(PantryDashboardFilter.Fridge, groups))
    }

    @Test
    fun `fridge card count matches the fridge filter result size`() {
        val filtered = applyPantryFilters(groups, setOf(PantryDashboardFilter.Fridge))
        assertEquals(filtered.size, cardCount(PantryDashboardFilter.Fridge, groups))
    }

    @Test
    fun `pantry card shows the real count of pantry products`() {
        // The six products with no explicit location default to the pantry.
        assertEquals(6, cardCount(PantryDashboardFilter.Pantry, groups))
    }

    @Test
    fun `pantry card count matches the pantry filter result size`() {
        val filtered = applyPantryFilters(groups, setOf(PantryDashboardFilter.Pantry))
        assertEquals(filtered.size, cardCount(PantryDashboardFilter.Pantry, groups))
    }

    // Low stock is opt-in per product, so it needs its own fixtures rather than the
    // shared list (whose products all have a null threshold).
    private val stockGroups = groupsOf(
        listOf(
            lot("low", quantity = 2, lowStockThreshold = 3),
            lot("lowBoundary", quantity = 3, lowStockThreshold = 3),
            lot("out", quantity = 0, lowStockThreshold = 3),
            lot("noThreshold", quantity = 1, lowStockThreshold = null),
            lot("wellStocked", quantity = 5, lowStockThreshold = 3),
        ),
    )

    @Test
    fun `running low card shows the real count of low-stock products`() {
        assertEquals(2, cardCount(PantryDashboardFilter.RunningLow, stockGroups))
    }

    @Test
    fun `running low card count matches the low stock filter result size`() {
        val filtered = applyPantryFilters(stockGroups, setOf(PantryDashboardFilter.RunningLow))
        assertEquals(filtered.size, cardCount(PantryDashboardFilter.RunningLow, stockGroups))
    }

    @Test
    fun `running low card is empty until products get a threshold`() {
        // The shared products have no lowStockThreshold, mirroring today's data. Until
        // a user sets one, the running low filter shows nothing.
        assertEquals(0, cardCount(PantryDashboardFilter.RunningLow, groups))
    }

    @Test
    fun `out card shows the real count of products with nothing on hand`() {
        // Only "out" (quantity 0) qualifies; low, boundary, and well-stocked don't.
        assertEquals(1, cardCount(PantryDashboardFilter.Out, stockGroups))
    }

    @Test
    fun `out card count matches the out filter result size`() {
        val filtered = applyPantryFilters(stockGroups, setOf(PantryDashboardFilter.Out))
        assertEquals(filtered.size, cardCount(PantryDashboardFilter.Out, stockGroups))
    }

    @Test
    fun `out card is empty when everything is in stock`() {
        // The shared products all default to quantity 1, so nothing is out.
        assertEquals(0, cardCount(PantryDashboardFilter.Out, groups))
    }

    // Products bought more than once: a card counts cards, not the lots behind them.

    private val repeatPurchases = groupsOf(
        listOf(
            // Two loaves in separate lots against a threshold of one: two on hand, so
            // the product is not low even though every lot sits at the threshold.
            lot("bread1", productId = "bread", expiresInDays = 2, quantity = 1, lowStockThreshold = 1),
            lot("bread2", productId = "bread", expiresInDays = 5, quantity = 1, lowStockThreshold = 1),
            // Three frozen lots of one product, two of them expiring soon.
            lot("peas1", productId = "peas", expiresInDays = 1, location = PantryLocation.Freezer),
            lot("peas2", productId = "peas", expiresInDays = 3, location = PantryLocation.Freezer),
            lot("peas3", productId = "peas", expiresInDays = 90, location = PantryLocation.Freezer),
        ),
    )

    @Test
    fun `running low card counts products, not lots`() {
        // The reported bug: two lots of one each used to count as two low items while
        // the list showed a single, well-stocked card.
        assertEquals(0, cardCount(PantryDashboardFilter.RunningLow, repeatPurchases))
    }

    @Test
    fun `a card counts a matching product once, however many of its lots match`() {
        // Peas has three frozen lots and contributes exactly one freezer card.
        assertEquals(1, cardCount(PantryDashboardFilter.Freezer, repeatPurchases))

        // Two of the peas lots and one bread lot are expiring — three lots across two
        // products, and the card counts the two products.
        assertEquals(2, cardCount(PantryDashboardFilter.Expiring, repeatPurchases))
    }

    @Test
    fun `every card count matches the size of its own filter's result`() {
        // The invariant the dashboard rests on: the number on a card is exactly how
        // many cards tapping it leaves in the list.
        PantryDashboardFilter.entries.forEach { filter ->
            assertEquals(
                "count for $filter",
                applyPantryFilters(repeatPurchases, setOf(filter)).size,
                cardCount(filter, repeatPurchases),
            )
        }
    }
}
