package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface ProductSearchUseCase : UseCase<String, ProductSearchUseCase.Output>
{


    sealed interface Output
    {
        data class Success( val product: List<ProductSearch>) : Output
        data class Failure(val error: Throwable) : Output
    }
}
