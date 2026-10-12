package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing

data class StoreProductPricing(
     val storeId: String,
     val storeName: String,
     val productId: String,
     val price: Double,
     val displayPrice: String,
     val productTitle: String,
     val productBrand: String? = null,
     //Package sizing used for helping to differentiate items of different prices and identical names
     val packageSizing: String?,
) {
}