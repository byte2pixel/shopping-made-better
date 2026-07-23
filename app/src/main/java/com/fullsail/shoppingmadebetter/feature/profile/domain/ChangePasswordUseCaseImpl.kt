package com.fullsail.shoppingmadebetter.feature.profile.domain

import com.fullsail.shoppingmadebetter.feature.profile.data.ProfileRepository
import javax.inject.Inject

class ChangePasswordUseCaseImpl @Inject constructor(
    private val repository: ProfileRepository
) : ChangePasswordUseCase {

    override suspend fun execute(input: ChangePasswordUseCase.Input): ChangePasswordUseCase.Output {
        return try {
            repository.changePassword(input.newPassword)
            ChangePasswordUseCase.Output.Success
        } catch (e: Exception) {
            ChangePasswordUseCase.Output.Failure(e)
        }
    }
}