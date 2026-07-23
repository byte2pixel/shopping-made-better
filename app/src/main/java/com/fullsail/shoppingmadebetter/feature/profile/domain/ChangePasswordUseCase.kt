package com.fullsail.shoppingmadebetter.feature.profile.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface ChangePasswordUseCase : UseCase<ChangePasswordUseCase.Input, ChangePasswordUseCase.Output> {
    data class Input(val newPassword: String)

    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}