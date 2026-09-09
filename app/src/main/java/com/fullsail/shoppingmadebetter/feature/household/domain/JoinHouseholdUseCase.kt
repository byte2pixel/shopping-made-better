package com.fullsail.shoppingmadebetter.feature.household.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** Joins the household whose invite code matches the input, case-insensitively. */
interface JoinHouseholdUseCase : UseCase<String, JoinHouseholdUseCase.Output> {
    sealed interface Output {
        data class Success(val household: Household) : Output
        data object InvalidCode : Output
        data object AlreadyInHousehold : Output
        data class Failure(val error: Throwable) : Output
    }
}
