package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface GetShoppingTripsUseCase : UseCase<Unit, GetShoppingTripsUseCase.Output> {
    sealed interface Output
    {
        data class Success(val trips: List<ShoppingTrip>) : Output
        data class Failure(val error: Throwable) : Output
    }
}
