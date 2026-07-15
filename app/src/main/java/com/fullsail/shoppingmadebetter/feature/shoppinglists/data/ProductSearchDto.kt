package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductSearchDto (
    @SerialName("id") val productId: String,
    @SerialName("title") val productName: String
    )
{

}