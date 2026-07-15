package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface InsertItemUseCase : UseCase<InsertItem, InsertItemUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}
