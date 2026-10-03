package com.fullsail.shoppingmadebetter.feature.onboarding.data

import com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences.SavePreferences

interface OnboardingRepository {
    suspend fun savePreferences(preferences: SavePreferences)

    /** The signed-in user's saved answers, or null when their profile row is missing. */
    suspend fun getPreferences(): SavePreferences?
}
