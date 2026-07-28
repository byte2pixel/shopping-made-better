package com.fullsail.shoppingmadebetter.feature.meals.data

interface MealsRepository {
    suspend fun fetchMeals(): List<MealDto>
    suspend fun selectMealPlan(mealId: String)
}