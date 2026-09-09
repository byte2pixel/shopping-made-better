package com.fullsail.shoppingmadebetter.feature.household.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** Removes the caller from their household. A head can only leave once alone. */
interface LeaveHouseholdUseCase : UseCase<Unit, LeaveHouseholdUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data object HeadWithMembers : Output
        data class Failure(val error: Throwable) : Output
    }
}
