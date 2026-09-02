package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** The inventory row [id] to adjust, the signed [delta] to apply, and the [reason] to audit it under. */
data class ApplyInventoryAdjustment(val id: String, val delta: Int, val reason: AdjustmentReason)

/**
 * Applies a signed quantity change to an inventory item and records it in the audit trail.
 * The quantity floors at zero, so [Output.Success.appliedDelta] can be smaller in magnitude
 * than the delta requested.
 */
interface ApplyInventoryAdjustmentUseCase :
    UseCase<ApplyInventoryAdjustment, ApplyInventoryAdjustmentUseCase.Output> {
    sealed interface Output {
        data class Success(val newQuantity: Int, val appliedDelta: Int) : Output
        data class Failure(val error: Throwable) : Output
    }
}
