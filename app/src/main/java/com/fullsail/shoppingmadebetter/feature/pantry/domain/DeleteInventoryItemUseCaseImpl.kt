package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import javax.inject.Inject

class DeleteInventoryItemUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository,
) : DeleteInventoryItemUseCase {
    override suspend fun execute(input: String): DeleteInventoryItemUseCase.Output = try {
        pantryRepository.deleteInventoryItem(input)
        DeleteInventoryItemUseCase.Output.Success
    } catch (e: Exception) {
        Log.e(TAG, "Failed to delete inventory item $input: ${e.message}", e)
        DeleteInventoryItemUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "DeleteInventoryItemUseCase"
    }
}