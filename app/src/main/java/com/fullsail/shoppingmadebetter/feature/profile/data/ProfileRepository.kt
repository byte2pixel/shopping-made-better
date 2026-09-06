package com.fullsail.shoppingmadebetter.feature.profile.data

interface ProfileRepository {
    suspend fun changePassword(newPassword: String)
    suspend fun updateContactInfo(email: String?, phone: String?)

    /** Reads `profiles.auto_adjust_enabled` for the signed-in user. */
    suspend fun getAutoAdjustEnabled(): Boolean

    /** Writes `profiles.auto_adjust_enabled` for the signed-in user. */
    suspend fun setAutoAdjustEnabled(enabled: Boolean)
}
