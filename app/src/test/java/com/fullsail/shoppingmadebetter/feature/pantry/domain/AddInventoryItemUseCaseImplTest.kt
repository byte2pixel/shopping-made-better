package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.feature.pantry.data.AdjustmentDigestEntryDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryAdjustmentResultDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Unit tests for [AddInventoryItemUseCaseImpl]. */
class AddInventoryItemUseCaseImplTest {

    /** Fake repository: records what it was asked to add, or throws [error]. */
    private class FakePantryRepository(
        private val error: Throwable? = null,
        private val lotId: String = "lot-1",
    ) : PantryRepository {
        var lastProductId: String? = null
        var lastQuantity: Int? = null
        var lastLocation: String? = null

        override suspend fun getInventoryItems(): List<InventoryItemDto> = emptyList()
        override suspend fun deleteInventoryItem(id: String) = Unit
        override suspend fun updateLocation(id: String, location: String) = Unit
        override suspend fun updateExpiry(id: String, expiresAt: LocalDate) = Unit
        override suspend fun updateLowStockThreshold(productId: String, threshold: Int?) = Unit
        override suspend fun applyInventoryAdjustment(id: String, delta: Int, reason: String) =
            InventoryAdjustmentResultDto(inventoryItemId = id, delta = 0.0, newQuantity = 0.0)
        override suspend fun undoInventoryAdjustment(adjustmentId: String) =
            InventoryAdjustmentResultDto(inventoryItemId = "", delta = 0.0, newQuantity = 0.0)
        override suspend fun getAdjustmentDigest(): List<AdjustmentDigestEntryDto> = emptyList()
        override suspend fun addInventoryItem(
            productId: String,
            quantity: Int,
            location: String?,
        ): String {
            error?.let { throw it }
            lastProductId = productId
            lastQuantity = quantity
            lastLocation = location
            return lotId
        }
    }

    @Test
    fun `a chosen location is sent as its database value`() = runTest {
        val repository = FakePantryRepository()

        val output = AddInventoryItemUseCaseImpl(repository)
            .execute(AddInventoryItem("p1", quantity = 2, location = PantryLocation.Fridge))

        assertEquals("p1", repository.lastProductId)
        assertEquals(2, repository.lastQuantity)
        assertEquals("fridge", repository.lastLocation)
        assertEquals(AddInventoryItemUseCase.Output.Success("lot-1"), output)
    }

    @Test
    fun `no chosen location leaves the database to derive one`() = runTest {
        val repository = FakePantryRepository()

        AddInventoryItemUseCaseImpl(repository)
            .execute(AddInventoryItem("p1", quantity = 1, location = null))

        // Null all the way down: the RPC defaults it and the insert trigger decides.
        assertNull(repository.lastLocation)
    }

    @Test
    fun `a failed add is reported as a failure`() = runTest {
        val repository = FakePantryRepository(error = IOException("no network"))

        val output = AddInventoryItemUseCaseImpl(repository)
            .execute(AddInventoryItem("p1", quantity = 1, location = null))

        assertTrue(output is AddInventoryItemUseCase.Output.Failure)
    }
}
