package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/**
 * The [productId] whose low-stock [threshold] to set for the current user. `null` clears the
 * threshold, so the product never counts as "low"; a set value flags it once its on-hand
 * quantity is at or below that number. Stored per user + product, so it applies to the
 * product across pantry entries and survives remove/re-add.
 */
data class UpdateInventoryLowStockThreshold(val productId: String, val threshold: Int?)

/** Sets (or clears) the current user's per-product low-stock threshold. */
interface UpdateInventoryLowStockThresholdUseCase :
    UseCase<UpdateInventoryLowStockThreshold, UpdateInventoryLowStockThresholdUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}
