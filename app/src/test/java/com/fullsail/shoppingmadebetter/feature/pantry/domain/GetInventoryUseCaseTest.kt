package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryAdjustmentResultDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Unit tests for [GetInventoryUseCaseImpl] using a handwritten fake
 * [PantryRepository].
 */
class GetInventoryUseCaseTest {

    /** Fake repository: returns [items] for the list call, or throws [error]. */
    private class FakePantryRepository(
        private val items: List<InventoryItemDto> = emptyList(),
        private val error: Throwable? = null,
    ) : PantryRepository {
        override suspend fun getInventoryItems(): List<InventoryItemDto> =
            error?.let { throw it } ?: items

        override suspend fun deleteInventoryItem(id: String) = Unit

        override suspend fun updateLocation(id: String, location: String) = Unit

        override suspend fun updateExpiry(id: String, expiresAt: LocalDate) = Unit

        override suspend fun updateLowStockThreshold(productId: String, threshold: Int?) = Unit

        override suspend fun applyInventoryAdjustment(id: String, delta: Int, reason: String) =
            InventoryAdjustmentResultDto(inventoryItemId = id, delta = 0.0, newQuantity = 0.0)
    }

    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-07-24T12:00:00Z")
    }
    private val today = fixedClock.todayIn(TimeZone.currentSystemDefault())

    private fun dto(id: String, expiryDate: LocalDate?) = InventoryItemDto(
        id = id,
        productId = "p-$id",
        name = "name-$id",
        brand = "brand",
        description = "desc",
        size = "size",
        quantity = 1,
        imageUrl = "",
        expiryDate = expiryDate,
    )

    /** The groups from a successful run, keyed by product id for order-independent lookup. */
    private fun GetInventoryUseCase.Output.groupsByProduct(): Map<String, ProductGroup> {
        assertTrue(this is GetInventoryUseCase.Output.Success)
        return (this as GetInventoryUseCase.Output.Success).productGroups.associateBy { it.productId }
    }

    @Test
    fun `execute maps DTOs to domain items on success`() = runTest {
        val dtos = listOf(
            InventoryItemDto(
                id = "1",
                productId = "p1",
                name = "Milk",
                brand = "Dairy Co",
                description = "2% milk",
                size = "1 gal",
                quantity = 2,
                imageUrl = "http://img/milk.png",
                expiryDate = null,
            ),
            InventoryItemDto(
                id = "2",
                productId = "p2",
                name = "Bread",
                brand = "Bakery Co",
                description = "Whole wheat",
                size = "24 oz",
                quantity = 1,
                imageUrl = "http://img/bread.png",
                expiryDate = null,
            ),
        )
        val useCase = GetInventoryUseCaseImpl(FakePantryRepository(items = dtos), fixedClock)

        val groups = useCase.execute(Unit).groupsByProduct()

        assertEquals(2, groups.size)
        // Every field maps across, onto the product's single lot.
        val milk = groups.getValue("p1").lots.single()
        assertEquals("1", milk.id)
        assertEquals("p1", milk.productId)
        assertEquals("Milk", milk.name)
        assertEquals("Dairy Co", milk.brand)
        assertEquals("2% milk", milk.description)
        assertEquals("1 gal", milk.size)
        assertEquals("http://img/milk.png", milk.imageUrl)
        assertEquals(2, milk.quantity)
        assertEquals("Bread", groups.getValue("p2").lots.single().name)
    }

    @Test
    fun `execute derives expiresInDays from expiryDate relative to today`() = runTest {
        val dtos = listOf(
            dto("future", today.plus(5, DateTimeUnit.DAY)),
            dto("today", today),
            dto("expired", today.minus(3, DateTimeUnit.DAY)),
            dto("undated", null),
        )
        val useCase = GetInventoryUseCaseImpl(FakePantryRepository(items = dtos), fixedClock)

        val groups = useCase.execute(Unit).groupsByProduct()

        assertEquals(5, groups.getValue("p-future").lots.single().expiresInDays)   // days remaining
        assertEquals(0, groups.getValue("p-today").lots.single().expiresInDays)    // due today
        assertEquals(-3, groups.getValue("p-expired").lots.single().expiresInDays) // overdue -> negative
        assertNull(groups.getValue("p-undated").lots.single().expiresInDays)       // no date -> null
    }

    @Test
    fun `execute maps the location string to a PantryLocation, unknown falling back to Pantry`() =
        runTest {
            val dtos = listOf(
                dto("freezer", null).copy(location = "freezer"),
                dto("fridge", null).copy(location = "fridge"),
                dto("pantry", null).copy(location = "pantry"),
                dto("unknown", null).copy(location = "garage"),
            )
            val useCase = GetInventoryUseCaseImpl(FakePantryRepository(items = dtos), fixedClock)

            val groups = useCase.execute(Unit).groupsByProduct()

            assertEquals(PantryLocation.Freezer, groups.getValue("p-freezer").lots.single().location)
            assertEquals(PantryLocation.Fridge, groups.getValue("p-fridge").lots.single().location)
            assertEquals(PantryLocation.Pantry, groups.getValue("p-pantry").lots.single().location)
            // Unrecognized -> Pantry.
            assertEquals(PantryLocation.Pantry, groups.getValue("p-unknown").lots.single().location)
        }

    @Test
    fun `execute marks a lot estimated only when its latest adjustment reason is auto`() = runTest {
        val dtos = listOf(
            dto("auto", null).copy(lastAdjustmentReason = "auto", estimateSource = "history"),
            dto("confirmed", null).copy(lastAdjustmentReason = "confirmed", estimateSource = "shelf_life"),
            dto("manual", null).copy(lastAdjustmentReason = "manual", estimateSource = "manual"),
            dto("none", null),
        )
        val useCase = GetInventoryUseCaseImpl(FakePantryRepository(items = dtos), fixedClock)

        val groups = useCase.execute(Unit).groupsByProduct()

        val auto = groups.getValue("p-auto").lots.single()
        assertTrue(auto.estimated)
        assertEquals(EstimateSource.History, auto.estimateSource)
        val confirmed = groups.getValue("p-confirmed").lots.single()
        assertFalse(confirmed.estimated)
        assertEquals(EstimateSource.ShelfLife, confirmed.estimateSource)
        val manual = groups.getValue("p-manual").lots.single()
        assertFalse(manual.estimated)
        assertEquals(EstimateSource.Manual, manual.estimateSource)
        val none = groups.getValue("p-none").lots.single()
        assertFalse(none.estimated)
        assertNull(none.estimateSource)
    }

    @Test
    fun `execute groups repeat purchases of one product into a single group`() = runTest {
        val dtos = listOf(
            dto("old", today.plus(2, DateTimeUnit.DAY)).copy(productId = "milk", quantity = 1),
            dto("new", today.plus(9, DateTimeUnit.DAY)).copy(productId = "milk", quantity = 3),
            dto("bread", null).copy(productId = "bread"),
        )
        val useCase = GetInventoryUseCaseImpl(FakePantryRepository(items = dtos), fixedClock)

        val groups = (useCase.execute(Unit) as GetInventoryUseCase.Output.Success).productGroups

        assertEquals(2, groups.size)
        // Soonest-expiring product first; the undated one last.
        assertEquals(listOf("milk", "bread"), groups.map { it.productId })
        val milk = groups.first()
        assertEquals(listOf("old", "new"), milk.lots.map { it.id })
        assertEquals(4, milk.totalQuantity)
        assertEquals(2, milk.earliestExpiresInDays)
    }

    @Test
    fun `execute returns an empty list when the repository has no items`() = runTest {
        val useCase = GetInventoryUseCaseImpl(FakePantryRepository(items = emptyList()), fixedClock)

        val output = useCase.execute(Unit)

        assertTrue(output is GetInventoryUseCase.Output.Success)
        assertTrue((output as GetInventoryUseCase.Output.Success).productGroups.isEmpty())
    }

    @Test
    fun `execute returns Failure carrying the error when the repository throws`() = runTest {
        val boom = IOException("network down")
        val useCase = GetInventoryUseCaseImpl(FakePantryRepository(error = boom), fixedClock)

        val output = useCase.execute(Unit)

        assertTrue(output is GetInventoryUseCase.Output.Failure)
        assertSame(boom, (output as GetInventoryUseCase.Output.Failure).error)
    }
}
