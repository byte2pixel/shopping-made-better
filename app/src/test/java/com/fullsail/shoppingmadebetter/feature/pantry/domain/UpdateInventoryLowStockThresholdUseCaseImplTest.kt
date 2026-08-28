package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Unit tests for [UpdateInventoryLowStockThresholdUseCaseImpl]. */
class UpdateInventoryLowStockThresholdUseCaseImplTest {

    /** Fake repository: records the threshold it was asked to persist, or throws [error]. */
    private class FakePantryRepository(
        private val error: Throwable? = null,
    ) : PantryRepository {
        var lastProductId: String? = null
        var lastThreshold: Int? = null
        var called = false

        override suspend fun getInventoryItems(): List<InventoryItemDto> = emptyList()
        override suspend fun deleteInventoryItem(id: String) = Unit
        override suspend fun updateQuantity(id: String, quantity: Int) = Unit
        override suspend fun updateLocation(id: String, location: String) = Unit
        override suspend fun updateExpiry(id: String, expiresAt: LocalDate) = Unit
        override suspend fun updateLowStockThreshold(productId: String, threshold: Int?) {
            error?.let { throw it }
            called = true
            lastProductId = productId
            lastThreshold = threshold
        }
    }

    @Test
    fun `persists a set threshold`() = runTest {
        val repo = FakePantryRepository()
        val useCase = UpdateInventoryLowStockThresholdUseCaseImpl(repo)

        val out = useCase.execute(UpdateInventoryLowStockThreshold(productId = "p1", threshold = 3))

        assertTrue(out is UpdateInventoryLowStockThresholdUseCase.Output.Success)
        assertEquals("p1", repo.lastProductId)
        assertEquals(3, repo.lastThreshold)
    }

    @Test
    fun `persists a null threshold to clear it`() = runTest {
        val repo = FakePantryRepository()
        val useCase = UpdateInventoryLowStockThresholdUseCaseImpl(repo)

        useCase.execute(UpdateInventoryLowStockThreshold(productId = "p1", threshold = null))

        assertTrue(repo.called)
        assertNull(repo.lastThreshold)
    }

    @Test
    fun `returns Failure when the repository throws`() = runTest {
        val repo = FakePantryRepository(error = IOException("boom"))
        val useCase = UpdateInventoryLowStockThresholdUseCaseImpl(repo)

        val out = useCase.execute(UpdateInventoryLowStockThreshold(productId = "p1", threshold = 3))

        assertTrue(out is UpdateInventoryLowStockThresholdUseCase.Output.Failure)
    }
}
