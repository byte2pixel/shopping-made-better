package com.fullsail.shoppingmadebetter.feature.meals.data

interface MealsRepository {
    suspend fun fetchMeals(): List<MealDto>
    suspend fun fetchIngredientsForMeal(mealId: String): List<MealIngredientDto>
    suspend fun selectMealPlan(mealId: String)
    suspend fun addIngredientToShoppingList(ingredientId: String, title: String, activeListId: String)
    suspend fun addMealToShoppingList(mealId: String, activeListId: String)
    suspend fun toggleFavoriteMeal(mealId: String, isFavorite: Boolean)
}