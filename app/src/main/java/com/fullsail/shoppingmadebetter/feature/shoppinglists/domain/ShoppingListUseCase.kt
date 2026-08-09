package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase
interface ShoppingListUseCase: UseCase<ShoppingList, ShoppingListUseCase.Output> {
    sealed interface Output {
        data class Success(val list : ShoppingList) : Output
        data class Failure(val error: Throwable) : Output
    }
}