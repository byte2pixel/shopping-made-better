package com.fullsail.shoppingmadebetter.feature.profile.domain

import com.fullsail.shoppingmadebetter.feature.profile.data.ProfileRepository
import javax.inject.Inject

class SetAutoAdjustEnabledUseCaseImpl @Inject constructor(
    private val repository: ProfileRepository
) : SetAutoAdjustEnabledUseCase {

    override suspend fun execute(input: SetAutoAdjustEnabledUseCase.Input): SetAutoAdjustEnabledUseCase.Output {
        return try {
            repository.setAutoAdjustEnabled(input.enabled)
            SetAutoAdjustEnabledUseCase.Output.Success
        } catch (e: Exception) {
            SetAutoAdjustEnabledUseCase.Output.Failure(e)
        }
    }
}
