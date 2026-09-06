package com.fullsail.shoppingmadebetter.feature.profile.domain

import com.fullsail.shoppingmadebetter.feature.profile.data.ProfileRepository
import javax.inject.Inject

class GetAutoAdjustEnabledUseCaseImpl @Inject constructor(
    private val repository: ProfileRepository
) : GetAutoAdjustEnabledUseCase {

    override suspend fun execute(input: Unit): GetAutoAdjustEnabledUseCase.Output {
        return try {
            GetAutoAdjustEnabledUseCase.Output.Success(repository.getAutoAdjustEnabled())
        } catch (e: Exception) {
            GetAutoAdjustEnabledUseCase.Output.Failure(e)
        }
    }
}
