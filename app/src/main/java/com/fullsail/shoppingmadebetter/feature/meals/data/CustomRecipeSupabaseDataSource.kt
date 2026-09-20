package com.fullsail.shoppingmadebetter.feature.meals.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class `CustomRecipeSupabaseDataSource`(
    private val supabaseClient: SupabaseClient
) {
    suspend fun saveRecipe(mealDto: MealDto): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {

                supabaseClient.postgrest["meals"].insert(mealDto)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}