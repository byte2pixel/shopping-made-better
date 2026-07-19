package com.fullsail.shoppingmadebetter.feature.profile.domain

interface ChangePasswordUseCase {
    suspend operator fun invoke(newPassword: String): Result<Unit>
}