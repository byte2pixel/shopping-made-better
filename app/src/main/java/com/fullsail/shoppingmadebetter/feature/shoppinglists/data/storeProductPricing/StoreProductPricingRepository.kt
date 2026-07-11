package com.fullsail.shoppingmadebetter.feature.shoppinglists.data.storeProductPricing


interface StoreProductPricingRepository {
    suspend fun getStores(productName : String): List<StoreProductPricingDto>
}