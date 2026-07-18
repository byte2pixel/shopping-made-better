package com.fullsail.shoppingmadebetter.feature.onboarding.data

data class OnboardingPreferencesDto(
    val dietaryRestrictions: List<String>,
    val topCategories: List<String>,
    val primaryGoal: String
)