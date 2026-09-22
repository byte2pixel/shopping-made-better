package com.fullsail.shoppingmadebetter.feature.household.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** Makes the member with the given id the head and the caller a member. Head only. */
interface TransferHeadUseCase : UseCase<String, TransferHeadUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data object NotHead : Output
        data object MemberNotInHousehold : Output
        data class Failure(val error: Throwable) : Output
    }
}
