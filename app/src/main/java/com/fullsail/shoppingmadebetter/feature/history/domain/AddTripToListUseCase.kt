package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/**
 * Which of a past trip's line items to copy onto which shopping list.
 * [productIds] is the user's selection from the trip detail screen; ids no longer
 * in the catalog are skipped rather than failing the whole add.
 */
data class AddTripToList(
    val purchaseId: String,
    val shoppingListId: String,
    val productIds: Set<String>,
)

/**
 * Copies the selected line items of a past trip onto a shopping list, one
 * `shopping_list_items` insert per item, at the quantity originally bought.
 */
interface AddTripToListUseCase : UseCase<AddTripToList, AddTripToListUseCase.Output> {
    sealed interface Output {
        /**
         * Every item that could be added was added. [skipped] counts requested
         * products that are no longer on the trip (delisted from the catalog).
         */
        data class Success(val added: Int, val skipped: Int) : Output

        /** Some inserts landed and some failed; the list is left as-is for the failures. */
        data class PartialFailure(val added: Int, val failed: Int, val skipped: Int) : Output

        /** Nothing was added: the trip could not be read, or every insert failed. */
        data class Failure(val error: Throwable) : Output
    }
}
