package com.fullsail.shoppingmadebetter.feature.pantry.domain

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

        override suspend fun getInventoryItem(id: String): InventoryItemDto? =
            error?.let { throw it } ?: items.firstOrNull { it.id == id }

        override suspend fun deleteInventoryItem(id: String) = Unit
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

        val output = useCase.execute(Unit)

        assertTrue(output is GetInventoryUseCase.Output.Success)
        val items = (output as GetInventoryUseCase.Output.Success).inventoryItems
        assertEquals(2, items.size)
        // Every field maps across, in order.
        val milk = items[0]
        assertEquals("1", milk.id)
        assertEquals("p1", milk.productId)
        assertEquals("Milk", milk.name)
        assertEquals("Dairy Co", milk.brand)
        assertEquals("2% milk", milk.description)
        assertEquals("1 gal", milk.size)
        assertEquals("http://img/milk.png", milk.imageUrl)
        assertEquals(2, milk.quantity)
        assertEquals("Bread", items[1].name)
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

        val items = (useCase.execute(Unit) as GetInventoryUseCase.Output.Success).inventoryItems

        assertEquals(5, items[0].expiresInDays)   // future -> days remaining
        assertEquals(0, items[1].expiresInDays)   // due today
        assertEquals(-3, items[2].expiresInDays)  // overdue -> negative
        assertNull(items[3].expiresInDays)        // no date -> null
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

            val items = (useCase.execute(Unit) as GetInventoryUseCase.Output.Success).inventoryItems

            assertEquals(PantryLocation.Freezer, items[0].location)
            assertEquals(PantryLocation.Fridge, items[1].location)
            assertEquals(PantryLocation.Pantry, items[2].location)
            assertEquals(PantryLocation.Pantry, items[3].location) // unrecognized -> Pantry
        }

    @Test
    fun `execute returns an empty list when the repository has no items`() = runTest {
        val useCase = GetInventoryUseCaseImpl(FakePantryRepository(items = emptyList()), fixedClock)

        val output = useCase.execute(Unit)

        assertTrue(output is GetInventoryUseCase.Output.Success)
        assertTrue((output as GetInventoryUseCase.Output.Success).inventoryItems.isEmpty())
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
