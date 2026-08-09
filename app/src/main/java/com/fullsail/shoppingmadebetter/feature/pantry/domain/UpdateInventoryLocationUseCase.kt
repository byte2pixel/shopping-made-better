package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** The inventory row [id] to update and the new [location] to store. */
data class UpdateInventoryLocation(val id: String, val location: PantryLocation)

/** Sets the storage location for an inventory item. */
interface UpdateInventoryLocationUseCase :
    UseCase<UpdateInventoryLocation, UpdateInventoryLocationUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}
