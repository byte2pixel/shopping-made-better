package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface GetTripCostComparisonUseCase :
    UseCase<String, GetTripCostComparisonUseCase.Output> {

    sealed interface Output {
        /** [stores] is cheapest first, and empty when there is nothing to compare. */
        data class Success(val stores: List<StoreBasketCost>) : Output

        data class Failure(val error: Throwable) : Output
    }
}
