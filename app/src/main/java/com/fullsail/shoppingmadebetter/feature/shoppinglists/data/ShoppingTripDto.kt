package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingTripDto(
    @SerialName("shopping_list_id") val shoppingListId: String,
    @SerialName("list_name") val listName: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("store_name") val storeName: String,
    @SerialName("item_count") val itemCount: Int,
    @SerialName("total_cost") val totalCost: Double,
)