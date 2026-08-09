package com.fullsail.shoppingmadebetter.feature.meals.domain

import com.fullsail.shoppingmadebetter.feature.meals.data.MealsRepository
import javax.inject.Inject

class SelectMealUseCaseImpl @Inject constructor(
    private val repository: MealsRepository
) : SelectMealUseCase {

    override suspend fun execute(input: SelectMealUseCase.Input): SelectMealUseCase.Output {
        return try {
            repository.selectMealPlan(input.mealId)
            SelectMealUseCase.Output.Success
        } catch (e: Exception) {
            SelectMealUseCase.Output.Failure(e)
        }
    }
}