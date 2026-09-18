package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface GetStoreInformationUseCase : UseCase<String, GetStoreInformationUseCase.Output> {
    sealed interface Output {
        data class Success(val input: StoreAddressInformation) : Output
        data class Failure(val error: Throwable) : Output
    }
}