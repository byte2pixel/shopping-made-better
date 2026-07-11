package com.fullsail.shoppingmadebetter.feature.auth.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/**
 * Registers a new user with email + password ([AuthCredentials]). Returns a
 * sealed [Output] so callers handle success and failure exhaustively without
 * try/catch. Password-confirmation is a UI concern and stays in the ViewModel.
 */
interface SignUpUseCase : UseCase<AuthCredentials, SignUpUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}