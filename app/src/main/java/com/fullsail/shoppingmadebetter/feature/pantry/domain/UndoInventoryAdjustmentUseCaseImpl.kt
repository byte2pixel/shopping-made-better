package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import javax.inject.Inject
import kotlin.math.roundToInt

class UndoInventoryAdjustmentUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository,
) : UndoInventoryAdjustmentUseCase {
    override suspend fun execute(input: UndoInventoryAdjustment): UndoInventoryAdjustmentUseCase.Output = try {
        val result = pantryRepository.undoInventoryAdjustment(input.adjustmentId)
        UndoInventoryAdjustmentUseCase.Output.Success(
            newQuantity = result.newQuantity.roundToInt(),
            appliedDelta = result.delta.roundToInt(),
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to undo adjustment ${input.adjustmentId}: ${e.message}", e)
        UndoInventoryAdjustmentUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "UndoInventoryAdjustment"
    }
}
