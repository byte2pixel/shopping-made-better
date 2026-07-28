package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
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
 * Unit tests for [GetInventoryItemUseCaseImpl] using a handwritten fake
 * [PantryRepository].
 */
class GetInventoryItemUseCaseTest {

    /** Fake repository: returns [item] for the single-item call, or throws [error]. */
    private class FakePantryRepository(
        private val item: InventoryItemDto? = null,
        private val error: Throwable? = null,
    ) : PantryRepository {
        var requestedId: String? = null

        override suspend fun getInventoryItems(): List<InventoryItemDto> =
            error?.let { throw it } ?: listOfNotNull(item)

        override suspend fun getInventoryItem(id: String): InventoryItemDto? {
            requestedId = id
            return error?.let { throw it } ?: item
        }

        override suspend fun deleteInventoryItem(id: String) = Unit
    }

    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-07-24T12:00:00Z")
    }
    private val today = fixedClock.todayIn(TimeZone.currentSystemDefault())

    private val dto = InventoryItemDto(
        id = "i1",
        productId = "p1",
        name = "Milk",
        brand = "Dairy Co",
        description = "2% milk",
        size = "1 gal",
        quantity = 2,
        imageUrl = "http://img/milk.png",
        expiryDate = null,
    )

    @Test
    fun `execute maps the DTO to a domain item and forwards the id on success`() = runTest {
        val repo = FakePantryRepository(item = dto)
        val useCase = GetInventoryItemUseCaseImpl(repo, fixedClock)

        val output = useCase.execute("i1")

        assertTrue(output is GetInventoryItemUseCase.Output.Success)
        val item = (output as GetInventoryItemUseCase.Output.Success).inventoryItem
        assertEquals("i1", item.id)
        assertEquals("p1", item.productId)
        assertEquals("Milk", item.name)
        assertEquals("Dairy Co", item.brand)
        assertEquals("2% milk", item.description)
        assertEquals("1 gal", item.size)
        assertEquals("http://img/milk.png", item.imageUrl)
        assertEquals(2, item.quantity)
        assertEquals("i1", repo.requestedId)
    }

    @Test
    fun `execute derives expiresInDays from expiryDate relative to today`() = runTest {
        val future = dto.copy(expiryDate = today.plus(5, DateTimeUnit.DAY))
        val expired = dto.copy(expiryDate = today.minus(3, DateTimeUnit.DAY))
        val dueToday = dto.copy(expiryDate = today)

        assertEquals(
            5,
            (GetInventoryItemUseCaseImpl(FakePantryRepository(item = future), fixedClock)
                .execute("i1") as GetInventoryItemUseCase.Output.Success).inventoryItem.expiresInDays,
        )
        assertEquals(
            0,
            (GetInventoryItemUseCaseImpl(FakePantryRepository(item = dueToday), fixedClock)
                .execute("i1") as GetInventoryItemUseCase.Output.Success).inventoryItem.expiresInDays,
        )
        assertEquals(
            -3,
            (GetInventoryItemUseCaseImpl(FakePantryRepository(item = expired), fixedClock)
                .execute("i1") as GetInventoryItemUseCase.Output.Success).inventoryItem.expiresInDays,
        )
        assertNull(
            (GetInventoryItemUseCaseImpl(FakePantryRepository(item = dto), fixedClock)
                .execute("i1") as GetInventoryItemUseCase.Output.Success).inventoryItem.expiresInDays,
        )
    }

    @Test
    fun `execute returns NotFound when the repository returns null`() = runTest {
        val useCase = GetInventoryItemUseCaseImpl(FakePantryRepository(item = null), fixedClock)

        val output = useCase.execute("missing")

        assertTrue(output is GetInventoryItemUseCase.Output.NotFound)
    }

    @Test
    fun `execute returns Failure carrying the error when the repository throws`() = runTest {
        val boom = IOException("network down")
        val useCase = GetInventoryItemUseCaseImpl(FakePantryRepository(error = boom), fixedClock)

        val output = useCase.execute("i1")

        assertTrue(output is GetInventoryItemUseCase.Output.Failure)
        assertSame(boom, (output as GetInventoryItemUseCase.Output.Failure).error)
    }
}