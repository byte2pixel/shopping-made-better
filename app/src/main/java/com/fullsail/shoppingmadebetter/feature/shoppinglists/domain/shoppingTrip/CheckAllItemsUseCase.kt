package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/**
 * Flags every item on a shopping list as checked.
 */
interface CheckAllItemsUseCase : UseCase<String, CheckAllItemsUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}
