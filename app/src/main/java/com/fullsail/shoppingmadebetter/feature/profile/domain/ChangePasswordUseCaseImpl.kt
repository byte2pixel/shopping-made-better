package com.fullsail.shoppingmadebetter.feature.profile.domain

import com.fullsail.shoppingmadebetter.feature.profile.data.ProfileRepository
import javax.inject.Inject

class ChangePasswordUseCaseImpl @Inject constructor(
    private val repository: ProfileRepository
) : ChangePasswordUseCase {

    override suspend fun invoke(newPassword: String): Result<Unit> {
        return if (newPassword.isBlank() || newPassword.length < 6) {
            Result.failure(IllegalArgumentException("Password must be at least 6 characters long."))
        } else {
            repository.changePassword(newPassword)
        }
    }
}