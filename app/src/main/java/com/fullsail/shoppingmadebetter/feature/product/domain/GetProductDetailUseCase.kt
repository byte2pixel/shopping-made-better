package com.fullsail.shoppingmadebetter.feature.product.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** Loads one product by id for the detail screen, held in the pantry or not. */
interface GetProductDetailUseCase : UseCase<String, GetProductDetailUseCase.Output> {
    sealed interface Output {
        data class Success(val product: ProductDetail) : Output
        data object NotFound : Output
        data class Failure(val error: Throwable) : Output
    }
}
