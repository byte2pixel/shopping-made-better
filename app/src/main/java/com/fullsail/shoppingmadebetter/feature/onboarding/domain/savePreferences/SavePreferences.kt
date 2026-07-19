package com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences

data class SavePreferences(
    val dietaryRestrictions: List<String>,
    val topCategories: List<String>,
    val primaryGoal: String
)