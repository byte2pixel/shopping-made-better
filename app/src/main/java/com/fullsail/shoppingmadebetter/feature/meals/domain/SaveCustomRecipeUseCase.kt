package com.fullsail.shoppingmadebetter.feature.meals.domain

import com.fullsail.shoppingmadebetter.feature.meals.data.MealDto
import com.fullsail.shoppingmadebetter.feature.meals.data.MealsRepository
import java.util.UUID
import javax.inject.Inject

class SaveCustomRecipeUseCase @Inject constructor(
    private val repository: MealsRepository
) {
    suspend operator fun invoke(title: String, category: String, ingredients: String): Result<Unit> {

        if (title.isBlank()) {
            return Result.failure(IllegalArgumentException("Recipe title cannot be empty."))
        }
        if (ingredients.isBlank()) {
            return Result.failure(IllegalArgumentException("Please provide at least one ingredient."))
        }


        val mealDto = MealDto(
            id = UUID.randomUUID().toString(),
            title = title,
            category = category,
            itemCount = ingredients.split(",").filter { it.isNotBlank() }.size,
            matchPercentage = "100% Match",
            totalPrice = "$0.00"
        )


        return repository.saveCustomRecipe(mealDto)
    }
}