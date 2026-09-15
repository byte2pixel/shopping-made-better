package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Timestamp
import kotlin.time.Instant

@Serializable
data class ShoppingTripDto(
    @SerialName("shopping_list_id") val shoppingListId: String,
    @SerialName("list_name") val listName: String,
    @SerialName("store_id") val storeId: String,
    @SerialName("store_name") val storeName: String,
    @SerialName("item_count") val itemCount: Int,
    @SerialName("total_cost") val totalCost: Double,
    @SerialName("sort_order") val sortOrder : Int,
    @SerialName("created_at") val createdDate : Instant,
    @SerialName("updated_at") val updatedDate : Instant,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("is_own") val isOwn: Boolean = true,
)