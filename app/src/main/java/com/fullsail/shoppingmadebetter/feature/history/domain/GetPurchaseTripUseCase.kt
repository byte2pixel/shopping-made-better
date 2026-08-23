package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** Loads one completed trip by its `purchase_history` id, for the detail screen. */
interface GetPurchaseTripUseCase : UseCase<String, GetPurchaseTripUseCase.Output> {
    sealed interface Output {
        data class Success(val trip: PurchaseTrip) : Output

        /** No trip with that id is visible to the current user. */
        data object NotFound : Output
        data class Failure(val error: Throwable) : Output
    }
}
