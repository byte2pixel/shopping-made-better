package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface GetItemDetailsUseCase: UseCase<String, GetItemDetailsUseCase.Output> {
    sealed interface Output {
        data class Success(val id: ItemDetails) : Output
        data class Failure(val error: Throwable) : Output
    }
}