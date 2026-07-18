package com.fullsail.shoppingmadebetter.feature.onboarding.data

import com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences.SavePreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : OnboardingRepository {

    override suspend fun savePreferences(preferences: SavePreferences) {
        // We update the 'profiles' table for the current user session
        supabaseClient.postgrest["profiles"].update(
            mapOf(
                "dietary_preferences" to preferences.dietaryRestrictions,
                "category_preferences" to preferences.topCategories,
                "primary_goal" to preferences.primaryGoal
            )
        ) {

            filter {
                eq("id", supabaseClient.postgrest.config.toString())
            }
        }
    }
}