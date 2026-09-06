package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** The `inventory_adjustments` row [adjustmentId] to reverse. */
data class UndoInventoryAdjustment(val adjustmentId: String)

/**
 * Reverses one automatic adjustment by applying its negated delta as an `undo` audit row.
 * Only `auto` rows can be undone, and each only once.
 */
interface UndoInventoryAdjustmentUseCase :
    UseCase<UndoInventoryAdjustment, UndoInventoryAdjustmentUseCase.Output> {
    sealed interface Output {
        data class Success(val newQuantity: Int, val appliedDelta: Int) : Output
        data class Failure(val error: Throwable) : Output
    }
}
