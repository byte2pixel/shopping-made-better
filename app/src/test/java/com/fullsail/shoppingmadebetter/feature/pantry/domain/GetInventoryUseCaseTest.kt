package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

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
            ),
        )
        val useCase = GetInventoryUseCaseImpl(FakePantryRepository(items = dtos))

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
    fun `execute returns an empty list when the repository has no items`() = runTest {
        val useCase = GetInventoryUseCaseImpl(FakePantryRepository(items = emptyList()))

        val output = useCase.execute(Unit)

        assertTrue(output is GetInventoryUseCase.Output.Success)
        assertTrue((output as GetInventoryUseCase.Output.Success).inventoryItems.isEmpty())
    }

    @Test
    fun `execute returns Failure carrying the error when the repository throws`() = runTest {
        val boom = IOException("network down")
        val useCase = GetInventoryUseCaseImpl(FakePantryRepository(error = boom))

        val output = useCase.execute(Unit)

        assertTrue(output is GetInventoryUseCase.Output.Failure)
        assertSame(boom, (output as GetInventoryUseCase.Output.Failure).error)
    }
}