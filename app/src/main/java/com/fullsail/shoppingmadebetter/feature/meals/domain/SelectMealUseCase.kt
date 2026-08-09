package com.fullsail.shoppingmadebetter.feature.meals.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface SelectMealUseCase : UseCase<SelectMealUseCase.Input, SelectMealUseCase.Output> {
    data class Input(val mealId: String)

    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}