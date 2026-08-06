package com.fullsail.shoppingmadebetter.feature.profile.domain

import com.fullsail.shoppingmadebetter.feature.profile.data.ProfileRepository
import javax.inject.Inject

class UpdateProfileInfoUseCaseImpl @Inject constructor(
    private val repository: ProfileRepository
) : UpdateProfileInfoUseCase {

    override suspend fun execute(input: UpdateProfileInfoUseCase.Input): UpdateProfileInfoUseCase.Output {
        return try {
            repository.updateContactInfo(email = input.newEmail, phone = input.newPhone)
            UpdateProfileInfoUseCase.Output.Success
        } catch (e: Exception) {
            UpdateProfileInfoUseCase.Output.Failure(e)
        }
    }
}