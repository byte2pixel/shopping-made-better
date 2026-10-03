package com.fullsail.shoppingmadebetter.feature.meals.domain

import com.fullsail.shoppingmadebetter.feature.meals.data.MealsRepository
import javax.inject.Inject

class DeleteCustomRecipeUseCase @Inject constructor(
    private val repository: MealsRepository
) {
    suspend operator fun invoke(mealId: String): Result<Unit> {
        return repository.deleteCustomRecipe(mealId)
    }
}