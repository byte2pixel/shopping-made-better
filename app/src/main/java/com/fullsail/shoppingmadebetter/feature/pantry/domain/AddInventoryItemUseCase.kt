package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/**
 * A product to put in the pantry by hand. [location] is what the user picked, or null
 * to let the product's category decide (fridge for milk, freezer for frozen, and so on).
 */
data class AddInventoryItem(
    val productId: String,
    val quantity: Int,
    val location: PantryLocation?,
)

/** Adds a lot to the user's pantry, returning the new row's id so an undo can remove it. */
interface AddInventoryItemUseCase : UseCase<AddInventoryItem, AddInventoryItemUseCase.Output> {
    sealed interface Output {
        data class Success(val lotId: String) : Output
        data class Failure(val error: Throwable) : Output
    }
}
