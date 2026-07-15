package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InsertItemDto
    (
    @SerialName("shopping_list_id") val shoppingListId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("note") val note: String,
    @SerialName("is_checked") val isChecked: Boolean,
    @SerialName("add_to_inventory") val addInventory: Boolean
            ){
}