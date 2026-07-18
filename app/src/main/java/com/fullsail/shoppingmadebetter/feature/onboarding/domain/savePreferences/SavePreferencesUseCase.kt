package com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences

interface SavePreferencesUseCase {
    suspend operator fun invoke(preferences: SavePreferences)
}