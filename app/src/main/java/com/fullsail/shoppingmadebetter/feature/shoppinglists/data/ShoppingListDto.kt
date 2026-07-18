package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingListDto (
    @SerialName("store_id") val storeId: String,
    @SerialName("name") val name: String,
    @SerialName("is_shared") val shared: Boolean,


)