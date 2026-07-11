package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing

import android.util.Log

import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.storeProductPricing.StoreProductPricingDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.storeProductPricing.StoreProductPricingRepository
import javax.inject.Inject
import kotlin.String


class StoreProductPricingUseCaseImpl @Inject constructor(
    private val repository: StoreProductPricingRepository,
) : StoreProductPricingUseCase {
    //Used to get the price of a particular product
    override suspend fun execute(input: String): StoreProductPricingUseCase.Output =
        try
        {
            StoreProductPricingUseCase.Output.Success(repository.getStores(input).map { it.toDomain() })
        } catch (e: Exception)
        {
            Log.e(TAG, "Failed to fetch prices: ${e.message}", e)
            StoreProductPricingUseCase.Output.Failure(e)
        }

    private fun StoreProductPricingDto.toDomain() = StoreProductPricing(
         storeId =storeId,
         storeName = storeName,
         productId = productId,
         price = price,
         productTitle = productTitle,
         productBrand = productBrand,

    )

    private companion object { const val TAG = "StoreProductPricingUseCase" }
}
