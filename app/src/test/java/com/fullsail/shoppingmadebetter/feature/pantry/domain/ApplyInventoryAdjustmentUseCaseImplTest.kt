package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryAdjustmentResultDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Unit tests for [ApplyInventoryAdjustmentUseCaseImpl]. */
class ApplyInventoryAdjustmentUseCaseImplTest {

    /** Fake repository: records the adjustment it was asked to apply and returns [result], or throws [error]. */
    private class FakePantryRepository(
        private val result: InventoryAdjustmentResultDto = InventoryAdjustmentResultDto("i1", -1.0, 2.0),
        private val error: Throwable? = null,
    ) : PantryRepository {
        var lastId: String? = null
        var lastDelta: Int? = null
        var lastReason: String? = null

        override suspend fun getInventoryItems(): List<InventoryItemDto> = emptyList()
        override suspend fun deleteInventoryItem(id: String) = Unit
        override suspend fun updateLocation(id: String, location: String) = Unit
        override suspend fun updateExpiry(id: String, expiresAt: LocalDate) = Unit
        override suspend fun updateLowStockThreshold(productId: String, threshold: Int?) = Unit
        override suspend fun applyInventoryAdjustment(
            id: String,
            delta: Int,
            reason: String,
        ): InventoryAdjustmentResultDto {
            error?.let { throw it }
            lastId = id
            lastDelta = delta
            lastReason = reason
            return result
        }
    }

    @Test
    fun `applies the delta under the db reason and maps the result`() = runTest {
        val repo = FakePantryRepository()
        val useCase = ApplyInventoryAdjustmentUseCaseImpl(repo)

        val out = useCase.execute(ApplyInventoryAdjustment(id = "i1", delta = -1, reason = AdjustmentReason.Confirmed))

        assertEquals(ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 2, appliedDelta = -1), out)
        assertEquals("i1", repo.lastId)
        assertEquals(-1, repo.lastDelta)
        assertEquals("confirmed", repo.lastReason)
    }

    @Test
    fun `surfaces the effective delta when the floor clamps it`() = runTest {
        val repo = FakePantryRepository(result = InventoryAdjustmentResultDto("i1", -2.0, 0.0))
        val useCase = ApplyInventoryAdjustmentUseCaseImpl(repo)

        val out = useCase.execute(ApplyInventoryAdjustment(id = "i1", delta = -5, reason = AdjustmentReason.Confirmed))

        assertEquals(ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 0, appliedDelta = -2), out)
        assertEquals(-5, repo.lastDelta)
    }

    @Test
    fun `sends a positive delta for undo`() = runTest {
        val repo = FakePantryRepository(result = InventoryAdjustmentResultDto("i1", 1.0, 3.0))
        val useCase = ApplyInventoryAdjustmentUseCaseImpl(repo)

        val out = useCase.execute(ApplyInventoryAdjustment(id = "i1", delta = 1, reason = AdjustmentReason.Undo))

        assertEquals(ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 3, appliedDelta = 1), out)
        assertEquals("undo", repo.lastReason)
    }

    @Test
    fun `returns Failure when the repository throws`() = runTest {
        val repo = FakePantryRepository(error = IOException("boom"))
        val useCase = ApplyInventoryAdjustmentUseCaseImpl(repo)

        val out = useCase.execute(ApplyInventoryAdjustment(id = "i1", delta = -1, reason = AdjustmentReason.Confirmed))

        assertTrue(out is ApplyInventoryAdjustmentUseCase.Output.Failure)
    }
}
