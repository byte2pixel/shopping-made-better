package com.fullsail.shoppingmadebetter.feature.profile.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface UpdateProfileInfoUseCase : UseCase<UpdateProfileInfoUseCase.Input, UpdateProfileInfoUseCase.Output> {
    data class Input(
        val newEmail: String?,
        val newPhone: String?
    )

    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}