package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface GetShoppingListItemsUseCase: UseCase<String, GetShoppingListItemsUseCase.Output> {
    sealed interface Output {
        data class Success(val input: List<ShoppingListItems>) : Output
        data class Failure(val error: Throwable) : Output
    }
}