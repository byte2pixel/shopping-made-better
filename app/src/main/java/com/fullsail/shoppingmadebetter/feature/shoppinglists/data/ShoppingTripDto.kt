package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Timestamp
import kotlin.time.Instant

@Serializable
data class ShoppingTripDto(
    @SerialName("shopping_list_id") val shoppingListId: String,
    @SerialName("list_name") val listName: String,
    //Holds the stores id good for differentiating them
    @SerialName("store_id") val storeId: String,
    @SerialName("store_name") val storeName: String,
    //holds the amount of items in a given shopping list
    @SerialName("item_count") val itemCount: Int,
    //Shows the total cost of the list
    @SerialName("total_cost") val totalCost: Double,
    //Order of which the shopping list is displayed
    @SerialName("sort_order") val sortOrder : Int,

    @SerialName("created_at") val createdDate : Instant,
    @SerialName("updated_at") val updatedDate : Instant,
    //Shows who the list was created by
    @SerialName("created_by") val createdBy: String? = null,
    //True if is the users list false if family list
    @SerialName("is_own") val isOwn: Boolean = true,
)