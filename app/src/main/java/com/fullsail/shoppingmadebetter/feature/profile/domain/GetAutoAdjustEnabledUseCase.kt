package com.fullsail.shoppingmadebetter.feature.profile.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** Reads whether the nightly auto-adjustment applies to the signed-in user's pantry. */
interface GetAutoAdjustEnabledUseCase : UseCase<Unit, GetAutoAdjustEnabledUseCase.Output> {
    sealed interface Output {
        data class Success(val enabled: Boolean) : Output
        data class Failure(val error: Throwable) : Output
    }
}
