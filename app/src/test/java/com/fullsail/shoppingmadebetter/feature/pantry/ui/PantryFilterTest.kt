package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ProductGroup
import com.fullsail.shoppingmadebetter.feature.pantry.domain.groupInventoryByProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [applyPantryFilters] — the pantry dashboard filtering logic, which
 * runs over the product cards the list actually shows.
 */
class PantryFilterTest {

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

    /** Groups [lots] the way the pantry list receives them, soonest expiry first. */
    private fun groupsOf(lots: List<InventoryItem>) = groupInventoryByProduct(lots)

    /** The product ids left after filtering — the cards the list would show. */
    private fun List<ProductGroup>.ids() = map { it.productId }

    // One single-lot product each, so these cover the lot-level filters on their own.
    private val expired = lot("expired", expiresInDays = -2)
    private val today = lot("today", expiresInDays = 0)
    private val soon = lot("soon", expiresInDays = EXPIRING_SOON_DAYS - 1)
    private val boundary = lot("boundary", expiresInDays = EXPIRING_SOON_DAYS)
    private val pastBoundary = lot("pastBoundary", expiresInDays = EXPIRING_SOON_DAYS + 1)
    private val later = lot("later", expiresInDays = EXPIRING_SOON_DAYS + 10)
    private val noDate = lot("noDate", expiresInDays = null)

    private val allLots = listOf(expired, today, soon, boundary, pastBoundary, later, noDate)
    private val allGroups = groupsOf(allLots)

    // Declared in the order grouping produces — soonest expiry first, undated last.
    private val pantryB = lot("pantryB", expiresInDays = 2, location = PantryLocation.Pantry)
    private val fridge = lot("fridge", expiresInDays = 3, location = PantryLocation.Fridge)
    private val freezerA = lot("freezerA", expiresInDays = 30, location = PantryLocation.Freezer)
    private val fridgeB = lot("fridgeB", expiresInDays = 40, location = PantryLocation.Fridge)
    private val pantry = lot("pantry", expiresInDays = 100, location = PantryLocation.Pantry)
    private val freezerB = lot("freezerB", expiresInDays = null, location = PantryLocation.Freezer)

    private val locatedLots = listOf(pantryB, fridge, freezerA, fridgeB, pantry, freezerB)
    private val locatedGroups = groupsOf(locatedLots)

    @Test
    fun `no filters returns every card unchanged`() {
        assertEquals(allGroups, applyPantryFilters(allGroups, emptySet()))
    }

    @Test
    fun `expiring filter keeps only products due within the threshold, including overdue ones`() {
        val result = applyPantryFilters(allGroups, setOf(PantryDashboardFilter.Expiring))

        // Overdue, due today, and due on or before the threshold day survive;
        // anything past it and unknown dates are filtered out.
        assertEquals(listOf("p-expired", "p-today", "p-soon", "p-boundary"), result.ids())
    }

    @Test
    fun `expiring filter keeps exactly the products whose chip is not grey`() {
        // The dashboard card and the card chips must agree on what "expiring"
        // means: the threshold day is still yellow, so it must survive the filter.
        assertEquals(ExpiryBucket.Soon, expiryBucket(EXPIRING_SOON_DAYS))
        assertEquals(ExpiryBucket.Later, expiryBucket(EXPIRING_SOON_DAYS + 1))

        val result = applyPantryFilters(allGroups, setOf(PantryDashboardFilter.Expiring)).ids()

        assertTrue("p-boundary" in result)
        assertFalse("p-pastBoundary" in result)
    }

    @Test
    fun `freezer filter keeps only products stored in the freezer`() {
        val result = applyPantryFilters(locatedGroups, setOf(PantryDashboardFilter.Freezer))

        // Card order is preserved and non-freezer locations are dropped.
        assertEquals(listOf("p-freezerA", "p-freezerB"), result.ids())
    }

    @Test
    fun `clearing the freezer filter shows every product again`() {
        // Tapping the card again removes it from the selected set; with no active
        // predicate the full list comes back unchanged.
        assertEquals(locatedGroups, applyPantryFilters(locatedGroups, emptySet()))
    }

    @Test
    fun `freezer and expiring compose as an intersection`() {
        val result = applyPantryFilters(
            locatedGroups,
            setOf(PantryDashboardFilter.Freezer, PantryDashboardFilter.Expiring),
        )

        // freezerA is in the freezer but not expiring soon (30 days); freezerB has no
        // date; only products matching both survive — here, none do.
        assertTrue(result.isEmpty())

        // A freezer product that is also expiring soon survives both predicates.
        val expiringFreezer =
            lot("freezerExpiring", expiresInDays = 1, location = PantryLocation.Freezer)
        val withExpiring = applyPantryFilters(
            groupsOf(locatedLots + expiringFreezer),
            setOf(PantryDashboardFilter.Freezer, PantryDashboardFilter.Expiring),
        )
        assertEquals(listOf("p-freezerExpiring"), withExpiring.ids())
    }

    @Test
    fun `fridge filter keeps only products stored in the fridge`() {
        val result = applyPantryFilters(locatedGroups, setOf(PantryDashboardFilter.Fridge))

        // Card order is preserved and non-fridge locations are dropped.
        assertEquals(listOf("p-fridge", "p-fridgeB"), result.ids())
    }

    @Test
    fun `fridge and expiring compose as an intersection`() {
        val result = applyPantryFilters(
            locatedGroups,
            setOf(PantryDashboardFilter.Fridge, PantryDashboardFilter.Expiring),
        )

        // fridge is due in 3 days so it clears the threshold; fridgeB is 40 days out
        // and drops. Only the fridge product matching both predicates survives.
        assertEquals(listOf("p-fridge"), result.ids())
    }

    @Test
    fun `pantry filter keeps only products stored in the pantry`() {
        val result = applyPantryFilters(locatedGroups, setOf(PantryDashboardFilter.Pantry))

        // Card order is preserved and fridge/freezer locations are dropped.
        assertEquals(listOf("p-pantryB", "p-pantry"), result.ids())
    }

    @Test
    fun `pantry and expiring compose as an intersection`() {
        val result = applyPantryFilters(
            locatedGroups,
            setOf(PantryDashboardFilter.Pantry, PantryDashboardFilter.Expiring),
        )

        // pantry is 100 days out and drops; pantryB is due in 2 days so it clears
        // the threshold. Only the pantry product matching both predicates survives.
        assertEquals(listOf("p-pantryB"), result.ids())
    }

    @Test
    fun `freezer and fridge OR-join to the union of both locations`() {
        val result = applyPantryFilters(
            locatedGroups,
            setOf(PantryDashboardFilter.Freezer, PantryDashboardFilter.Fridge),
        )

        // Locations are mutually exclusive per lot, so combining them widens rather
        // than empties the result. Card order is kept, pantry products dropped.
        assertEquals(listOf("p-fridge", "p-freezerA", "p-fridgeB", "p-freezerB"), result.ids())
    }

    @Test
    fun `selecting all three locations returns every located product`() {
        val result = applyPantryFilters(
            locatedGroups,
            setOf(
                PantryDashboardFilter.Freezer,
                PantryDashboardFilter.Fridge,
                PantryDashboardFilter.Pantry,
            ),
        )

        // The union of every location covers the whole list, card order preserved.
        assertEquals(locatedGroups, result)
    }

    @Test
    fun `expiring ANDs against the OR-joined cold-storage locations`() {
        // Cross-category composition: the location group (Fridge OR Freezer) is
        // intersected with Expiring, yielding cold products that are also expiring soon.
        val coldAndExpiring = setOf(
            PantryDashboardFilter.Expiring,
            PantryDashboardFilter.Fridge,
            PantryDashboardFilter.Freezer,
        )
        val result = applyPantryFilters(locatedGroups, coldAndExpiring)

        // fridge (3 days) is cold and expiring soon; freezerA/freezerB/fridgeB are
        // cold but not expiring soon; pantryB is expiring but not cold.
        assertEquals(listOf("p-fridge"), result.ids())

        // A freezer product that is also expiring soon joins the fridge one in the union.
        val expiringFreezer =
            lot("freezerExpiring", expiresInDays = 1, location = PantryLocation.Freezer)
        val withExpiring =
            applyPantryFilters(groupsOf(locatedLots + expiringFreezer), coldAndExpiring)
        assertEquals(listOf("p-freezerExpiring", "p-fridge"), withExpiring.ids())
    }

    // Low stock is opt-in per product: it counts as low only when it has a
    // lowStockThreshold set and its total is between 1 and that threshold.
    private val low = lot("low", quantity = 2, lowStockThreshold = 3)
    private val lowBoundary = lot("lowBoundary", quantity = 3, lowStockThreshold = 3)
    private val outWithThreshold = lot("out", quantity = 0, lowStockThreshold = 3)
    private val outNoThreshold = lot("outNoThreshold", quantity = 0, lowStockThreshold = null)
    private val noThreshold = lot("noThreshold", quantity = 1, lowStockThreshold = null)
    private val wellStocked = lot("wellStocked", quantity = 5, lowStockThreshold = 3)

    private val stockGroups = groupsOf(
        listOf(low, lowBoundary, outWithThreshold, outNoThreshold, noThreshold, wellStocked),
    )

    @Test
    fun `low stock filter keeps products at or below their own threshold`() {
        val result = applyPantryFilters(stockGroups, setOf(PantryDashboardFilter.RunningLow))

        // Card order is preserved; only products within 1..threshold survive.
        assertEquals(listOf("p-low", "p-lowBoundary"), result.ids())
    }

    @Test
    fun `low stock filter excludes out-of-stock products and ones with no threshold`() {
        val result = applyPantryFilters(stockGroups, setOf(PantryDashboardFilter.RunningLow)).ids()

        // A total of 0 is "out" not "low"; a null threshold is never low; a total
        // above the threshold is well stocked.
        assertFalse("p-out" in result)
        assertFalse("p-noThreshold" in result)
        assertFalse("p-wellStocked" in result)
    }

    @Test
    fun `out filter keeps only products with nothing on hand, regardless of threshold`() {
        val result = applyPantryFilters(stockGroups, setOf(PantryDashboardFilter.Out))

        // Card order is preserved; both empty products survive whether or not they
        // have a threshold, and nothing with stock on hand does.
        assertEquals(listOf("p-out", "p-outNoThreshold"), result.ids())
    }

    @Test
    fun `running low and out OR-join to the union of both`() {
        // Stock statuses are mutually exclusive — a total of 0 is "out", "low" is
        // 1..threshold — so combining them widens rather than empties the result.
        val result = applyPantryFilters(
            stockGroups,
            setOf(PantryDashboardFilter.RunningLow, PantryDashboardFilter.Out),
        )

        // Low OR out, card order preserved; well-stocked and no-threshold products drop.
        assertEquals(listOf("p-low", "p-lowBoundary", "p-out", "p-outNoThreshold"), result.ids())
    }

    // Products varying in both stock status and location, for cross-category tests.
    private val outFreezer = lot("outFreezer", location = PantryLocation.Freezer, quantity = 0)
    private val lowFreezer = lot(
        "lowFreezer",
        location = PantryLocation.Freezer,
        quantity = 2,
        lowStockThreshold = 3,
    )
    private val stockedFreezer = lot(
        "stockedFreezer",
        location = PantryLocation.Freezer,
        quantity = 5,
        lowStockThreshold = 3,
    )
    private val outFridge = lot("outFridge", location = PantryLocation.Fridge, quantity = 0)

    private val mixedGroups = groupsOf(listOf(outFreezer, lowFreezer, stockedFreezer, outFridge))

    @Test
    fun `stock filter ANDs against a location`() {
        val result = applyPantryFilters(
            mixedGroups,
            setOf(PantryDashboardFilter.Out, PantryDashboardFilter.Freezer),
        )

        // Out AND Freezer: only the empty freezer product; outFridge is out but in the
        // wrong location, lowFreezer/stockedFreezer are freezer but not out.
        assertEquals(listOf("p-outFreezer"), result.ids())
    }

    @Test
    fun `OR-joined stock group ANDs against a location`() {
        val result = applyPantryFilters(
            mixedGroups,
            setOf(
                PantryDashboardFilter.RunningLow,
                PantryDashboardFilter.Out,
                PantryDashboardFilter.Freezer,
            ),
        )

        // (Low OR Out) AND Freezer: outFreezer and lowFreezer qualify; stockedFreezer
        // is well stocked and outFridge is in the wrong location.
        assertEquals(listOf("p-outFreezer", "p-lowFreezer"), result.ids())
    }

    // Products bought more than once, where lot-level and product-level facts diverge.

    @Test
    fun `running low is judged on the product total, not on a single lot`() {
        // A loaf for now and a loaf for later: two on hand against a threshold of one,
        // so the product is not low — even though every lot sits at the threshold.
        val groups = groupsOf(
            listOf(
                lot("bread1", productId = "bread", expiresInDays = 2, quantity = 1, lowStockThreshold = 1),
                lot("bread2", productId = "bread", expiresInDays = 5, quantity = 1, lowStockThreshold = 1),
            ),
        )

        assertEquals(2, groups.single().totalQuantity)
        assertTrue(applyPantryFilters(groups, setOf(PantryDashboardFilter.RunningLow)).isEmpty())
        assertTrue(applyPantryFilters(groups, setOf(PantryDashboardFilter.Out)).isEmpty())
    }

    @Test
    fun `a product is low when its lots together fall to the threshold`() {
        val groups = groupsOf(
            listOf(
                lot("milk1", productId = "milk", expiresInDays = 2, quantity = 1, lowStockThreshold = 3),
                lot("milk2", productId = "milk", expiresInDays = 5, quantity = 1, lowStockThreshold = 3),
            ),
        )

        // Two on hand against a threshold of three: low, and listed as one card.
        assertEquals(
            listOf("milk"),
            applyPantryFilters(groups, setOf(PantryDashboardFilter.RunningLow)).ids(),
        )
    }

    @Test
    fun `a product is out only when every lot is empty`() {
        val partlyEmpty = groupsOf(
            listOf(
                lot("eggs1", productId = "eggs", expiresInDays = 2, quantity = 0),
                lot("eggs2", productId = "eggs", expiresInDays = 5, quantity = 2),
            ),
        )
        assertTrue(applyPantryFilters(partlyEmpty, setOf(PantryDashboardFilter.Out)).isEmpty())

        val allEmpty = groupsOf(
            listOf(
                lot("rice1", productId = "rice", expiresInDays = 2, quantity = 0),
                lot("rice2", productId = "rice", expiresInDays = 5, quantity = 0),
            ),
        )
        assertEquals(
            listOf("rice"),
            applyPantryFilters(allEmpty, setOf(PantryDashboardFilter.Out)).ids(),
        )
    }

    @Test
    fun `a location filter matches when any lot is stored there, and the card keeps every lot`() {
        val groups = groupsOf(
            listOf(
                lot("a1", productId = "pA", location = PantryLocation.Pantry),
                lot("a2", productId = "pA", location = PantryLocation.Freezer),
                lot("b1", productId = "pB", location = PantryLocation.Pantry),
            ),
        )

        val result = applyPantryFilters(groups, setOf(PantryDashboardFilter.Freezer))

        // pA shows because one lot is frozen; the card keeps both lots. pB drops.
        assertEquals(listOf("pA"), result.ids())
        assertEquals(listOf("a1", "a2"), result.single().lots.map { it.id })
    }

    @Test
    fun `expiring matches when any lot is expiring, and the card keeps its true aggregates`() {
        val groups = groupsOf(
            listOf(
                lot("a1", productId = "pA", expiresInDays = 1, quantity = 2),
                lot("a2", productId = "pA", expiresInDays = 60, quantity = 3),
                lot("b1", productId = "pB", expiresInDays = 60, quantity = 4),
            ),
        )

        val result = applyPantryFilters(groups, setOf(PantryDashboardFilter.Expiring))

        // Filtering decides which cards are listed, never what a listed card holds:
        // the far-out lot stays and the header totals still cover both.
        assertEquals(listOf("pA"), result.ids())
        assertEquals(2, result.single().lots.size)
        assertEquals(5, result.single().totalQuantity)
        assertEquals(1, result.single().earliestExpiresInDays)
    }

    @Test
    fun `lot-level filters have to be satisfied by the same lot`() {
        val coldAndExpiring = setOf(PantryDashboardFilter.Freezer, PantryDashboardFilter.Expiring)

        // pA has a freezer lot and an expiring lot, but no lot that is both — where a
        // lot sits and when it expires belong to that lot, so the card drops.
        val apart = groupsOf(
            listOf(
                lot("a1", productId = "pA", expiresInDays = 60, location = PantryLocation.Freezer),
                lot("a2", productId = "pA", expiresInDays = 1, location = PantryLocation.Pantry),
            ),
        )
        assertTrue(applyPantryFilters(apart, coldAndExpiring).isEmpty())

        // Move the expiring lot into the freezer and one lot satisfies both.
        val together = groupsOf(
            listOf(
                lot("a1", productId = "pA", expiresInDays = 60, location = PantryLocation.Freezer),
                lot("a2", productId = "pA", expiresInDays = 1, location = PantryLocation.Freezer),
            ),
        )
        assertEquals(listOf("pA"), applyPantryFilters(together, coldAndExpiring).ids())
    }

    @Test
    fun `product-level stock ANDs against a lot-level location`() {
        // Both products are low overall (two on hand against a threshold of three);
        // only pA keeps any of it in the freezer.
        val groups = groupsOf(
            listOf(
                lot("a1", productId = "pA", expiresInDays = 2, quantity = 1, location = PantryLocation.Freezer, lowStockThreshold = 3),
                lot("a2", productId = "pA", expiresInDays = 5, quantity = 1, lowStockThreshold = 3),
                lot("b1", productId = "pB", expiresInDays = 2, quantity = 1, lowStockThreshold = 3),
                lot("b2", productId = "pB", expiresInDays = 5, quantity = 1, lowStockThreshold = 3),
            ),
        )

        assertEquals(
            listOf("pA", "pB"),
            applyPantryFilters(groups, setOf(PantryDashboardFilter.RunningLow)).ids(),
        )
        assertEquals(
            listOf("pA"),
            applyPantryFilters(
                groups,
                setOf(PantryDashboardFilter.RunningLow, PantryDashboardFilter.Freezer),
            ).ids(),
        )
    }
}
