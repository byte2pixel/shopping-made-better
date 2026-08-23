package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface GetPurchaseHistoryUseCase : UseCase<Unit, GetPurchaseHistoryUseCase.Output> {
    sealed interface Output {
        data class Success(val trips: List<PurchaseTrip>) : Output
        data class Failure(val error: Throwable) : Output
    }
}
