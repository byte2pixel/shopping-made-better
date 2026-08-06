package com.fullsail.shoppingmadebetter.feature.meals.data

import io.github.jan.supabase.SupabaseClient
import javax.inject.Inject

class MealsRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : MealsRepository {

    override suspend fun fetchMeals(): List<MealDto> {
        return listOf(
            MealDto(
                id = "1",
                title = "Chicken Alfredo",
                matchPercentage = "95% Match",
                itemCount = 4,
                totalPrice = "$34.19",
                category = "Recommended"
            ),
            MealDto(
                id = "2",
                title = "Beef Tacos",
                matchPercentage = "Missing: 3",
                itemCount = 5,
                totalPrice = "$28.50",
                category = "Almost There"
            )
        )
    }

    override suspend fun selectMealPlan(mealId: String) {

    }
}