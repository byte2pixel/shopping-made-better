package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryAdjustmentResultDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Unit tests for [UndoInventoryAdjustmentUseCaseImpl]. */
class UndoInventoryAdjustmentUseCaseImplTest {

    /** Fake repository: records the adjustment it was asked to undo and returns [result], or throws [error]. */
    private class FakePantryRepository(
        private val result: InventoryAdjustmentResultDto = InventoryAdjustmentResultDto("i1", 2.0, 3.0),
        private val error: Throwable? = null,
    ) : PantryRepository {
        var lastAdjustmentId: String? = null

        override suspend fun getInventoryItems(): List<InventoryItemDto> = emptyList()
        override suspend fun deleteInventoryItem(id: String) = Unit
        override suspend fun updateLocation(id: String, location: String) = Unit
        override suspend fun updateExpiry(id: String, expiresAt: LocalDate) = Unit
        override suspend fun updateLowStockThreshold(productId: String, threshold: Int?) = Unit
        override suspend fun applyInventoryAdjustment(id: String, delta: Int, reason: String) =
            InventoryAdjustmentResultDto(inventoryItemId = id, delta = 0.0, newQuantity = 0.0)
        override suspend fun undoInventoryAdjustment(adjustmentId: String): InventoryAdjustmentResultDto {
            error?.let { throw it }
            lastAdjustmentId = adjustmentId
            return result
        }
    }

    @Test
    fun `undoes the adjustment by id and maps the result`() = runTest {
        val repo = FakePantryRepository()
        val useCase = UndoInventoryAdjustmentUseCaseImpl(repo)

        val out = useCase.execute(UndoInventoryAdjustment(adjustmentId = "a1"))

        assertEquals(UndoInventoryAdjustmentUseCase.Output.Success(newQuantity = 3, appliedDelta = 2), out)
        assertEquals("a1", repo.lastAdjustmentId)
    }

    @Test
    fun `returns Failure carrying the error when the repository throws`() = runTest {
        val error = IOException("boom")
        val repo = FakePantryRepository(error = error)
        val useCase = UndoInventoryAdjustmentUseCaseImpl(repo)

        val out = useCase.execute(UndoInventoryAdjustment(adjustmentId = "a1"))

        assertTrue(out is UndoInventoryAdjustmentUseCase.Output.Failure)
        assertSame(error, (out as UndoInventoryAdjustmentUseCase.Output.Failure).error)
    }
}
