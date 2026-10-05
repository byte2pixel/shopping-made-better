package com.fullsail.shoppingmadebetter.feature.meals.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectMealUseCaseTest {
    @Test
    fun `execute returns success when valid mealId is provided`() = runBlocking {
        val useCase = object : SelectMealUseCase {
            override suspend fun execute(input: SelectMealUseCase.Input): SelectMealUseCase.Output {
                return SelectMealUseCase.Output.Success
            }
        }

        val result = useCase.execute(SelectMealUseCase.Input("test-id-123"))

        assertTrue(result is SelectMealUseCase.Output.Success)
    }
}