package com.fullsail.shoppingmadebetter.feature.household.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** Creates a household named by the input and makes the caller its head. */
interface CreateHouseholdUseCase : UseCase<String, CreateHouseholdUseCase.Output> {
    sealed interface Output {
        data class Success(val household: Household) : Output
        data object AlreadyInHousehold : Output
        data class Failure(val error: Throwable) : Output
    }
}
