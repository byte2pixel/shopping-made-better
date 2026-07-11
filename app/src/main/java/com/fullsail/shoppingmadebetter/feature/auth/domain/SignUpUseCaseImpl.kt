package com.fullsail.shoppingmadebetter.feature.auth.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.auth.data.AuthRepository
import javax.inject.Inject

/**
 * Default [SignUpUseCase]: delegates to the [AuthRepository] and translates any
 * failure into [Output.Failure].
 */
class SignUpUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
) : SignUpUseCase {

    override suspend fun execute(input: AuthCredentials): SignUpUseCase.Output =
        try {
            authRepository.signUp(input.email, input.password)
            SignUpUseCase.Output.Success
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed: ${e.message}", e)
            SignUpUseCase.Output.Failure(e)
        }

    private companion object {
        const val TAG = "SignUpUseCase"
    }
}
