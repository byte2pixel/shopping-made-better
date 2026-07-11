package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface StoreProductPricingUseCase : UseCase<String, StoreProductPricingUseCase.Output> {

    sealed interface Output
    {
        data class Success(val prices: List<StoreProductPricing>) : Output
        data class Failure(val error: Throwable) : Output
    }
}