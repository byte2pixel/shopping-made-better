package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreProductPricingDto(
    @SerialName("store_id") val storeId: String,
    @SerialName("store_name") val storeName: String,
    @SerialName("product_id") val productId: String,
    @SerialName("display_price") val price: String,
    @SerialName("product_title") val productTitle: String,
    @SerialName("product_brand") val productBrand: String?,
) {

}