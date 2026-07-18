package com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences

import com.fullsail.shoppingmadebetter.feature.onboarding.data.OnboardingRepository
import javax.inject.Inject

class SavePreferencesUseCaseImpl @Inject constructor(
    private val repository: OnboardingRepository
) : SavePreferencesUseCase {

    override suspend fun invoke(preferences: SavePreferences) {
        repository.savePreferences(preferences)
    }
}