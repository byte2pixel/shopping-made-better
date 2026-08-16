package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface CompleteShoppingTripUseCase : UseCase<String, CompleteShoppingTripUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}