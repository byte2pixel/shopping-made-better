package com.fullsail.shoppingmadebetter.feature.household.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** Replaces the household's invite code; the old one stops working at once. Head only. */
interface RegenerateInviteCodeUseCase : UseCase<Unit, RegenerateInviteCodeUseCase.Output> {
    sealed interface Output {
        data class Success(val code: String) : Output
        data object NotHead : Output
        data class Failure(val error: Throwable) : Output
    }
}
