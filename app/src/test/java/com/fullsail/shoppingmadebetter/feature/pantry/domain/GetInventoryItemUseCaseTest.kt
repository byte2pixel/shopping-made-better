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
    }

    private val dto = InventoryItemDto(
        id = "i1",
        productId = "p1",
        name = "Milk",
        brand = "Dairy Co",
        description = "2% milk",
        size = "1 gal",
        quantity = 2,
        imageUrl = "http://img/milk.png",
    )

    @Test
    fun `execute maps the DTO to a domain item and forwards the id on success`() = runTest {
        val repo = FakePantryRepository(item = dto)
        val useCase = GetInventoryItemUseCaseImpl(repo)

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
    fun `execute returns NotFound when the repository returns null`() = runTest {
        val useCase = GetInventoryItemUseCaseImpl(FakePantryRepository(item = null))

        val output = useCase.execute("missing")

        assertTrue(output is GetInventoryItemUseCase.Output.NotFound)
    }

    @Test
    fun `execute returns Failure carrying the error when the repository throws`() = runTest {
        val boom = IOException("network down")
        val useCase = GetInventoryItemUseCaseImpl(FakePantryRepository(error = boom))

        val output = useCase.execute("i1")

        assertTrue(output is GetInventoryItemUseCase.Output.Failure)
        assertSame(boom, (output as GetInventoryItemUseCase.Output.Failure).error)
    }
}