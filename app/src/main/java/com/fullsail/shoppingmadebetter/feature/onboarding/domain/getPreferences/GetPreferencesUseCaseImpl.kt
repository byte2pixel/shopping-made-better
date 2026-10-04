package com.fullsail.shoppingmadebetter.feature.onboarding.domain.getPreferences

import com.fullsail.shoppingmadebetter.feature.onboarding.data.OnboardingRepository
import com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences.SavePreferences
import javax.inject.Inject

class GetPreferencesUseCaseImpl @Inject constructor(
    private val repository: OnboardingRepository
) : GetPreferencesUseCase {

    override suspend fun invoke(): SavePreferences? = repository.getPreferences()
}
