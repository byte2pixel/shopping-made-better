package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class InsertItemDto
    (
    @SerialName("shopping_list_id") val shoppingListId: String,
    @SerialName("product_id") val productId: String,
    //Quantity of the item
    @SerialName("quantity") val quantity: Int,
    //Unused
    @SerialName("note") val note: String,
    //Controls if an item is crossed off or not
    @SerialName("is_checked") val isChecked: Boolean,
    //unused
    @SerialName("add_to_inventory") val addInventory: Boolean
            ){
}