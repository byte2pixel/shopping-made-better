package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import javax.inject.Inject

class UpdateInventoryLowStockThresholdUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository,
) : UpdateInventoryLowStockThresholdUseCase {
    override suspend fun execute(
        input: UpdateInventoryLowStockThreshold,
    ): UpdateInventoryLowStockThresholdUseCase.Output = try {
        pantryRepository.updateLowStockThreshold(input.productId, input.threshold)
        UpdateInventoryLowStockThresholdUseCase.Output.Success
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update low stock threshold for product ${input.productId}: ${e.message}", e)
        UpdateInventoryLowStockThresholdUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "UpdateInventoryThreshold"
    }
}
