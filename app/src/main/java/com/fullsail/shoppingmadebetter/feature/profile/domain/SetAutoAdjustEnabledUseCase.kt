package com.fullsail.shoppingmadebetter.feature.profile.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** Turns the nightly auto-adjustment on or off for the signed-in user. */
interface SetAutoAdjustEnabledUseCase :
    UseCase<SetAutoAdjustEnabledUseCase.Input, SetAutoAdjustEnabledUseCase.Output> {
    data class Input(val enabled: Boolean)

    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}
