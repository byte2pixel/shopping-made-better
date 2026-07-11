package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.productSearch.ProductSearchDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.productSearch.ProductSearchRepository
import javax.inject.Inject


class ProductSearchUseCaseImpl @Inject constructor(
    private val repository: ProductSearchRepository,

) : ProductSearchUseCase
{
//Gets the searched product to match against the original
    override suspend fun execute(input: String): ProductSearchUseCase.Output =
        try {
            ProductSearchUseCase.Output.Success(repository.getProduct(input).map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch trips: ${e.message}", e)
            ProductSearchUseCase.Output.Failure(e)
        }

    private fun ProductSearchDto.toDomain() = ProductSearch(
         productId = productId,
         productName = productName
    )



    private companion object { const val TAG = "ProductSearchUseCase" }
}