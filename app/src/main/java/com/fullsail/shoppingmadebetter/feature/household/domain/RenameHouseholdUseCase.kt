package com.fullsail.shoppingmadebetter.feature.household.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

data class RenameHousehold(val householdId: String, val name: String)

/** Renames the caller's household. Head only; a blank name is a failure. */
interface RenameHouseholdUseCase : UseCase<RenameHousehold, RenameHouseholdUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}
