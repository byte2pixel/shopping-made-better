package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import javax.inject.Inject

class AddInventoryItemUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository,
) : AddInventoryItemUseCase {
    override suspend fun execute(input: AddInventoryItem): AddInventoryItemUseCase.Output = try {
        AddInventoryItemUseCase.Output.Success(
            pantryRepository.addInventoryItem(
                productId = input.productId,
                quantity = input.quantity,
                // Null travels all the way to the RPC, which leaves the location to the trigger.
                location = input.location?.toDbValue(),
            ),
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to add product ${input.productId} to the pantry: ${e.message}", e)
        AddInventoryItemUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "AddInventoryItemUseCase"
    }
}
