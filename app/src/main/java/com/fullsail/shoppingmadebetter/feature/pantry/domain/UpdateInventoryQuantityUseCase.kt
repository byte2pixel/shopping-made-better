package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** The inventory row [id] to update and the new [quantity] to store. */
data class UpdateInventoryQuantity(val id: String, val quantity: Int)

/** Sets the on-hand quantity for an inventory item. */
interface UpdateInventoryQuantityUseCase :
    UseCase<UpdateInventoryQuantity, UpdateInventoryQuantityUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}
