package com.fullsail.shoppingmadebetter.feature.meals.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class InsertMealRow(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null
)

class CustomRecipeSupabaseDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    suspend fun saveRecipe(mealDto: MealDto): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Map the UI model to the strict database row format
                val dbRow = InsertMealRow(
                    id = mealDto.id,
                    name = mealDto.title,
                    description = "Custom Recipe: ${mealDto.category}",
                    imageUrl = null
                )

                supabaseClient.postgrest["meals"].insert(dbRow)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}