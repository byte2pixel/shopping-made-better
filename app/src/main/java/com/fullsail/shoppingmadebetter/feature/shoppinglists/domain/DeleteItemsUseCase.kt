package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface DeleteItemsUseCase: UseCase<String, DeleteItemsUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}