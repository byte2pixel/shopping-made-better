package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListDto

interface ShoppingListUseCase: UseCase<ShoppingList, ShoppingListUseCase.Output> {
    sealed interface Output {
        data class Success(val shoppingList: ShoppingListDto) : Output
        data class Failure(val error: Throwable) : Output
    }
}