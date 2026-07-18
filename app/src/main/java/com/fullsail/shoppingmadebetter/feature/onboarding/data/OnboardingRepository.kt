package com.fullsail.shoppingmadebetter.feature.onboarding.data

import com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences.SavePreferences

interface OnboardingRepository {
    suspend fun savePreferences(preferences: SavePreferences)
}