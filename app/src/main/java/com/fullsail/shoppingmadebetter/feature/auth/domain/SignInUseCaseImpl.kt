package com.fullsail.shoppingmadebetter.feature.auth.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.auth.data.AuthRepository
import javax.inject.Inject

/**
 * Default [SignInUseCase]: delegates to the [AuthRepository] and translates any
 * failure into [Output.Failure].
 */
class SignInUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
) : SignInUseCase {

    override suspend fun execute(input: AuthCredentials): SignInUseCase.Output =
        try {
            authRepository.signIn(input.email, input.password)
            SignInUseCase.Output.Success
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}", e)
            SignInUseCase.Output.Failure(e)
        }

    private companion object {
        const val TAG = "SignInUseCase"
    }
}
