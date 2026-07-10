package com.fullsail.shoppingmadebetter.feature.auth.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/**
 * Signs a user in with email + password ([AuthCredentials]). Returns a sealed
 * [Output] so callers handle success and failure exhaustively without try/catch.
 */
interface SignInUseCase : UseCase<AuthCredentials, SignInUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}