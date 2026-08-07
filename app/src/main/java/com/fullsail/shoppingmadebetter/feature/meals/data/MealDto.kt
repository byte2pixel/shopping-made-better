package com.fullsail.shoppingmadebetter.feature.meals.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MealDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("match_percentage") val matchPercentage: String,
    @SerialName("item_count") val itemCount: Int,
    @SerialName("total_price") val totalPrice: String,
    @SerialName("category") val category: String

)