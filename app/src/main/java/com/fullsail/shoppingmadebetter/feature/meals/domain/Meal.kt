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
    val matchPercentage: String,
    val itemCount: Int,
    val totalPrice: String,
    val category: String,
    val ingredients: List<Ingredient> = emptyList()
)