package com.fullsail.shoppingmadebetter.feature.meals.domain

import com.fullsail.shoppingmadebetter.feature.meals.data.MealsRepository
import javax.inject.Inject

class SaveCustomRecipeUseCase @Inject constructor(
    private val repository: MealsRepository
) {
    suspend operator fun invoke(title: String, category: String, ingredients: String) {
        // Business logic and validation happens here before hitting the database
        if (title.isNotBlank() && ingredients.isNotBlank()) {
            repository.saveCustomMeal(title, category, ingredients)
        }
    }
}