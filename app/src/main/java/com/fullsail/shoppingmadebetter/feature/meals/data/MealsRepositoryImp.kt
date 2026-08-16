package com.fullsail.shoppingmadebetter.feature.meals.data

import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListItemsDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject

@Serializable
private data class DbMeal(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null
)

class MealsRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val pantryRepository: PantryRepository
) : MealsRepository {

    override suspend fun fetchMeals(): List<MealDto> {
        return try {

            val dbMeals = supabaseClient.postgrest["meals"]
                .select()
                .decodeList<DbMeal>()

            val pantryItems = try {
                pantryRepository.getInventoryItems()
            } catch (e: Exception) {
                emptyList()
            }
            val pantryProductIds = pantryItems.mapNotNull { it.productId }.toSet()

            dbMeals.map { dbMeal ->
                val ingredients = fetchIngredientsForMeal(dbMeal.id)
                val totalIngredients = ingredients.size

                // Count how many ingredients the user already has in their pantry
                val ownedCount = ingredients.count { it.productId != null && pantryProductIds.contains(it.productId) }

                val matchPercentage = if (totalIngredients > 0) {
                    val percent = (ownedCount.toFloat() / totalIngredients) * 100
                    "${percent.toInt()}% Match"
                } else {
                    "0% Match"
                }

                val missingCount = totalIngredients - ownedCount
                val category = if (missingCount == 0) "Can Make" else "Almost There"

                MealDto(
                    id = dbMeal.id,
                    title = dbMeal.name,
                    matchPercentage = matchPercentage,
                    itemCount = totalIngredients,
                    totalPrice = "$0.00",
                    category = category
                )
            }
        } catch (e: Exception) {
            println("Supabase Error fetching meals: ${e.message}")
            emptyList()
        }
    }

    override suspend fun fetchIngredientsForMeal(mealId: String): List<MealIngredientDto> {
        return try {
            supabaseClient.postgrest["meal_ingredients"]
                .select { filter { eq("meal_id", mealId) } }
                .decodeList<MealIngredientDto>()
        } catch (e: Exception) {
            println("Supabase Error fetching ingredients: ${e.message}")
            emptyList()
        }
    }

    override suspend fun selectMealPlan(mealId: String) {

    }

    override suspend fun addIngredientToShoppingList(ingredientId: String, title: String, activeListId: String) {
        try {
            val newItem = ShoppingListItemsDto(
                id = UUID.randomUUID().toString(),
                shoppingListId = activeListId,
                productId = ingredientId,
                quantity = 1,
                title = title
            )

            supabaseClient.postgrest["shopping_list_items"].insert(newItem)
            println("Successfully inserted $title into shopping list.")
        } catch (e: Exception) {
            println("Supabase Error adding ingredient: ${e.message}")
        }
    }

    override suspend fun addMealToShoppingList(mealId: String, activeListId: String) {
        try {
            val mealIngredients = fetchIngredientsForMeal(mealId)

            if (mealIngredients.isNotEmpty()) {
                val shoppingListItems = mealIngredients.map { ingredient ->


                    val safeQuantity: Double = ingredient.quantity ?: 1.0
                    val safeUnit: String = ingredient.unit ?: ""


                    val displayTitle = if (safeUnit.isNotBlank()) {
                        "$safeQuantity $safeUnit of ${ingredient.ingredientName}"
                    } else {
                        ingredient.ingredientName
                    }

                    ShoppingListItemsDto(
                        id = UUID.randomUUID().toString(),
                        shoppingListId = activeListId,
                        productId = ingredient.productId ?: UUID.randomUUID().toString(),
                        quantity = if (safeQuantity < 1.0) 1 else safeQuantity.toInt(),
                        title = displayTitle
                    )
                }

                supabaseClient.postgrest["shopping_list_items"].insert(shoppingListItems)
                println("Successfully added ${shoppingListItems.size} ingredients to shopping list.")
            }
        } catch (e: Exception) {
            println("Supabase Error adding meal to list: ${e.message}")
        }
    }
}