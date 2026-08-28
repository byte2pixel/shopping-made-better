package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface GetPurchaseHistoryUseCase :
    UseCase<GetPurchaseHistoryUseCase.Input, GetPurchaseHistoryUseCase.Output> {

    /**
     * One page of history.
     * @param offset how many trips to skip; 0 is the newest trip.
     * @param limit how many trips to ask for.
     * @param filter which trips count as history at all. Offset and limit apply to
     *   the filtered result, so page 2 of a filtered list holds the 21st match --
     *   not the 21st trip, filtered.
     */
    data class Input(
        val offset: Int,
        val limit: Int,
        val filter: HistoryFilter = HistoryFilter(),
    )

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
