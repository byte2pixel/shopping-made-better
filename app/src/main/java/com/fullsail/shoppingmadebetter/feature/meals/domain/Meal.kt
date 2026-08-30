package com.fullsail.shoppingmadebetter.feature.meals.domain

data class Ingredient(
    val id: String,
    val name: String,
    val quantity: String,
    val price: String
)

data class Meal(
    val id: String,
    val title: String,
    val matchPercentage: Int,
    val itemCount: Int,
    val totalPrice: String,
    val category: String,
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: List<String> = emptyList(),
    val imageUrl: String? = null,
    val calories: String? = null,
    val protein: String? = null,
    val carbs: String? = null,
    val fat: String? = null
)