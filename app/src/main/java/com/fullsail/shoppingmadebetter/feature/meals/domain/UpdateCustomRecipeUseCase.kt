package com.fullsail.shoppingmadebetter.feature.meals.domain

import com.fullsail.shoppingmadebetter.feature.meals.data.MealsRepository
import javax.inject.Inject

class UpdateCustomRecipeUseCase @Inject constructor(
    private val repository: MealsRepository
) {
    suspend operator fun invoke(mealId: String, title: String, category: String, ingredients: String): Result<Unit> {
        if (title.isBlank() || ingredients.isBlank()) {
            return Result.failure(Exception("Title and ingredients cannot be blank."))
        }
        return repository.updateCustomRecipe(mealId, title, category, ingredients)
    }
}