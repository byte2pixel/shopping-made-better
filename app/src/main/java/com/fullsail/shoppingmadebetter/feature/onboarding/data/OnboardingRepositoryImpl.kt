package com.fullsail.shoppingmadebetter.feature.onboarding.data

import com.fullsail.shoppingmadebetter.feature.onboarding.domain.savePreferences.SavePreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject


@Serializable
private data class ProfileUpdateDto(
    @SerialName("dietary_preferences") val dietaryPreferences: List<String>,
    @SerialName("category_preferences") val categoryPreferences: List<String>,
    @SerialName("primary_goal") val primaryGoal: String,
    @SerialName("auto_adjust_enabled") val autoAdjustEnabled: Boolean,
)

class OnboardingRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : OnboardingRepository {

    override suspend fun savePreferences(preferences: SavePreferences) {

        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("User must be logged in to save preferences.")


        val updateData = ProfileUpdateDto(
            dietaryPreferences = preferences.dietaryRestrictions,
            categoryPreferences = preferences.topCategories,
            primaryGoal = preferences.primaryGoal,
            autoAdjustEnabled = preferences.autoAdjustEnabled
        )


        supabaseClient.postgrest["profiles"].update(updateData) {
            filter {
                eq(column = "id", value = userId)
            }
        }
    }
}