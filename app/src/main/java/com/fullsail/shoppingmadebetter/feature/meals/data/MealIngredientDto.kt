package com.fullsail.shoppingmadebetter.feature.meals.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MealIngredientDto(
    @SerialName("id") val id: String,
    @SerialName("meal_id") val mealId: String,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("ingredient_name") val ingredientName: String,
    @SerialName("quantity") val quantity: Double? = 1.0,
    @SerialName("unit") val unit: String? = ""
)