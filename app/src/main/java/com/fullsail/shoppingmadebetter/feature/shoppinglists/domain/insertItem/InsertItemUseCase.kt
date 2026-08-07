package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface InsertItemUseCase : UseCase<InsertItem, InsertItemUseCase.Output> {
    sealed interface Output {
        data class Success(val insertedItemId: String) : Output
        data class Failure(val error: Throwable) : Output
    }
}
