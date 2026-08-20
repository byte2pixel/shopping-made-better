package com.fullsail.shoppingmadebetter.feature.pantry.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [groupInventoryByProduct] and the totals [ProductGroup] derives. */
class ProductGroupTest {

    private fun lot(
        id: String,
        productId: String = "p1",
        expiresInDays: Int? = null,
        quantity: Int = 1,
        location: PantryLocation = PantryLocation.Pantry,
        lowStockThreshold: Int? = null,
    ) = InventoryItem(
        id = id,
        productId = productId,
        name = "name-$productId",
        brand = "brand-$productId",
        description = "desc-$productId",
        size = "size-$productId",
        imageUrl = "img-$productId",
        quantity = quantity,
        expiresInDays = expiresInDays,
        location = location,
        lowStockThreshold = lowStockThreshold,
    )

    @Test
    fun `lots of the same product collapse into one group`() {
        val groups = groupInventoryByProduct(
            listOf(
                lot("1", productId = "milk"),
                lot("2", productId = "milk"),
                lot("3", productId = "bread"),
            ),
        )

        assertEquals(2, groups.size)
        val milk = groups.single { it.productId == "milk" }
        assertEquals(listOf("1", "2"), milk.lots.map { it.id })
        assertEquals(listOf("3"), groups.single { it.productId == "bread" }.lots.map { it.id })
    }

    @Test
    fun `display fields come from the product's lots`() {
        val group = groupInventoryByProduct(listOf(lot("1", productId = "milk"))).single()

        assertEquals("milk", group.productId)
        assertEquals("name-milk", group.name)
        assertEquals("brand-milk", group.brand)
        assertEquals("desc-milk", group.description)
        assertEquals("size-milk", group.size)
        assertEquals("img-milk", group.imageUrl)
    }

    @Test
    fun `totalQuantity sums every lot`() {
        val group = groupInventoryByProduct(
            listOf(lot("1", quantity = 2), lot("2", quantity = 3), lot("3", quantity = 0)),
        ).single()

        assertEquals(5, group.totalQuantity)
    }

    @Test
    fun `earliestExpiresInDays takes the soonest dated lot and ignores undated ones`() {
        val group = groupInventoryByProduct(
            listOf(
                lot("1", expiresInDays = 9),
                lot("2", expiresInDays = null),
                lot("3", expiresInDays = 2),
            ),
        ).single()

        assertEquals(2, group.earliestExpiresInDays)
    }

    @Test
    fun `earliestExpiresInDays keeps an already-expired lot as the soonest`() {
        val group = groupInventoryByProduct(
            listOf(lot("1", expiresInDays = 4), lot("2", expiresInDays = -3)),
        ).single()

        assertEquals(-3, group.earliestExpiresInDays)
    }

    @Test
    fun `earliestExpiresInDays is null when no lot is dated`() {
        val group = groupInventoryByProduct(
            listOf(lot("1", expiresInDays = null), lot("2", expiresInDays = null)),
        ).single()

        assertNull(group.earliestExpiresInDays)
    }

    @Test
    fun `lots are ordered by soonest expiry with undated ones last`() {
        val group = groupInventoryByProduct(
            listOf(
                lot("undated", expiresInDays = null),
                lot("later", expiresInDays = 30),
                lot("expired", expiresInDays = -1),
                lot("soon", expiresInDays = 3),
            ),
        ).single()

        assertEquals(listOf("expired", "soon", "later", "undated"), group.lots.map { it.id })
    }

    @Test
    fun `groups are ordered by earliest expiry with undated-only products last`() {
        val groups = groupInventoryByProduct(
            listOf(
                lot("1", productId = "undated", expiresInDays = null),
                lot("2", productId = "later", expiresInDays = 20),
                lot("3", productId = "soon", expiresInDays = 6),
                // A second lot pulls "later" ahead of "soon".
                lot("4", productId = "later", expiresInDays = 1),
            ),
        )

        assertEquals(listOf("later", "soon", "undated"), groups.map { it.productId })
    }

    @Test
    fun `locations collects the distinct places a product is stored`() {
        val group = groupInventoryByProduct(
            listOf(
                lot("1", location = PantryLocation.Fridge),
                lot("2", location = PantryLocation.Freezer),
                lot("3", location = PantryLocation.Fridge),
            ),
        ).single()

        assertEquals(setOf(PantryLocation.Fridge, PantryLocation.Freezer), group.locations)
    }

    @Test
    fun `singleLocation is the shared location, or null when the lots are mixed`() {
        val uniform = groupInventoryByProduct(
            listOf(
                lot("1", location = PantryLocation.Fridge),
                lot("2", location = PantryLocation.Fridge),
            ),
        ).single()
        val mixed = groupInventoryByProduct(
            listOf(
                lot("1", location = PantryLocation.Fridge),
                lot("2", location = PantryLocation.Pantry),
            ),
        ).single()

        assertEquals(PantryLocation.Fridge, uniform.singleLocation)
        assertNull(mixed.singleLocation)
    }

    @Test
    fun `lowStockThreshold carries over from the product's lots`() {
        val withThreshold = groupInventoryByProduct(
            listOf(lot("1", lowStockThreshold = 2), lot("2", lowStockThreshold = 2)),
        ).single()
        val without = groupInventoryByProduct(listOf(lot("1"))).single()

        assertEquals(2, withThreshold.lowStockThreshold)
        assertNull(without.lowStockThreshold)
    }

    @Test
    fun `an empty inventory groups to an empty list`() {
        assertTrue(groupInventoryByProduct(emptyList()).isEmpty())
    }
}
