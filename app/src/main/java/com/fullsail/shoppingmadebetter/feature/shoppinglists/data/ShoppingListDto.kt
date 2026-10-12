package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingListDto (
    @SerialName("id") val shoppingListId: String? = null,
    @SerialName("store_id") val storeId: String,
    @SerialName("name") val name: String,
    //If shopping is shared across family
    @SerialName("is_shared") val shared: Boolean,


)