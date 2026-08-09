package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import javax.inject.Inject

class UpdateInventoryQuantityUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository,
) : UpdateInventoryQuantityUseCase {
    override suspend fun execute(input: UpdateInventoryQuantity): UpdateInventoryQuantityUseCase.Output = try {
        pantryRepository.updateQuantity(input.id, input.quantity)
        UpdateInventoryQuantityUseCase.Output.Success
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update quantity for inventory item ${input.id}: ${e.message}", e)
        UpdateInventoryQuantityUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "UpdateInventoryQuantity"
    }
}
