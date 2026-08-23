package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingListItemsDto(
    @SerialName("id") val id: String,
    @SerialName("shopping_id") val shoppingListId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("title") val title: String,
    @SerialName("is_checked") val checked: Boolean = false,

) {
}