package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing

data class StoreProductPricing(
     val storeId: String,
     val storeName: String,
     val productId: String,
     val price: String,
     val productTitle: String,
     val productBrand: String? = null,
) {
}