package com.fullsail.shoppingmadebetter.feature.onboarding.domain.getPreferences

import com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences.SavePreferences

/** Reads the signed-in user's saved onboarding answers, or null when there is no profile row. */
interface GetPreferencesUseCase {
    suspend operator fun invoke(): SavePreferences?
}
