package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface GetPurchaseHistoryUseCase :
    UseCase<GetPurchaseHistoryUseCase.Input, GetPurchaseHistoryUseCase.Output> {

    /**
     * One page of history.
     * @param offset how many trips to skip; 0 is the newest trip.
     * @param limit how many trips to ask for.
     */
    data class Input(val offset: Int, val limit: Int)

    sealed interface Output {
        /**
         * [trips] is newest first and at most `limit` long. [endReached] is true
         * when the page came back short, i.e. there is nothing older to load.
         */
        data class Success(
            val trips: List<PurchaseTripSummary>,
            val endReached: Boolean,
        ) : Output

        data class Failure(val error: Throwable) : Output
    }
}
