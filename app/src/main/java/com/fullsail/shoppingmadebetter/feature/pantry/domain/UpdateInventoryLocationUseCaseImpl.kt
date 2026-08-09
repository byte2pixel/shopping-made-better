package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import javax.inject.Inject

class UpdateInventoryLocationUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository,
) : UpdateInventoryLocationUseCase {
    override suspend fun execute(input: UpdateInventoryLocation): UpdateInventoryLocationUseCase.Output = try {
        pantryRepository.updateLocation(input.id, input.location.toDbValue())
        UpdateInventoryLocationUseCase.Output.Success
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update location for inventory item ${input.id}: ${e.message}", e)
        UpdateInventoryLocationUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "UpdateInventoryLocation"
    }
}
