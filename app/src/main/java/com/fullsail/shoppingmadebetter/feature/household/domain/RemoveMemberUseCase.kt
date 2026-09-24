package com.fullsail.shoppingmadebetter.feature.household.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** Drops the member with the given id from the caller's household. Head only. */
interface RemoveMemberUseCase : UseCase<String, RemoveMemberUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data object NotHead : Output
        data object MemberNotInHousehold : Output
        data object CannotRemoveSelf : Output
        data class Failure(val error: Throwable) : Output
    }
}
