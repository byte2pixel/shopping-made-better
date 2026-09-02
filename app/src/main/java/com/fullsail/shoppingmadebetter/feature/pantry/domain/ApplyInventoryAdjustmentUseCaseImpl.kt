package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import javax.inject.Inject
import kotlin.math.roundToInt

class ApplyInventoryAdjustmentUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository,
) : ApplyInventoryAdjustmentUseCase {
    override suspend fun execute(input: ApplyInventoryAdjustment): ApplyInventoryAdjustmentUseCase.Output = try {
        val result = pantryRepository.applyInventoryAdjustment(
            id = input.id,
            delta = input.delta,
            reason = input.reason.toDbValue(),
        )
        ApplyInventoryAdjustmentUseCase.Output.Success(
            newQuantity = result.newQuantity.roundToInt(),
            appliedDelta = result.delta.roundToInt(),
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to apply adjustment to inventory item ${input.id}: ${e.message}", e)
        ApplyInventoryAdjustmentUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "ApplyInventoryAdjustment"
    }
}
