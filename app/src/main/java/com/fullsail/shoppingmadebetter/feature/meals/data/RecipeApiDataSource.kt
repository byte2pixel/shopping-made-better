package com.fullsail.shoppingmadebetter.feature.meals.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import javax.inject.Inject

@Serializable
data class MealDbApiResponse(
    val meals: List<MealDbItem?>?
)

@Serializable
data class MealDbItem(
    val idMeal: String,
    val strMeal: String,
    val strCategory: String?,
    val strMealThumb: String?,
    val strInstructions: String?
)

class RecipeApiDataSource @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }

    fun fetchRecipesFromApi(): List<MealDto> {
        val fetchedMeals = mutableListOf<MealDto>()
        try {

            for (i in 1..5) {
                val responseString = URL("https://www.themealdb.com/api/json/v1/1/random.php").readText()
                val apiResponse = json.decodeFromString<MealDbApiResponse>(responseString)

                apiResponse.meals?.firstOrNull()?.let { item ->
                    fetchedMeals.add(
                        MealDto(
                            id = item.idMeal,
                            title = item.strMeal,
                            matchPercentage = "100% Match",
                            itemCount = 4,
                            totalPrice = "$12.99",
                            category = item.strCategory ?: "Recommended"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("API Fetch Error: ${e.message}")
        }
        return fetchedMeals
    }
}