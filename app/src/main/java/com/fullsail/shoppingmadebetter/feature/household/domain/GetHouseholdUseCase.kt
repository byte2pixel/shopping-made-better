package com.fullsail.shoppingmadebetter.feature.household.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** The caller's household and its members, head first. */
interface GetHouseholdUseCase : UseCase<Unit, GetHouseholdUseCase.Output> {
    sealed interface Output {
        data object None : Output
        data class Member(val household: Household, val members: List<HouseholdMember>) : Output
        data class Failure(val error: Throwable) : Output
    }
}
