package com.fullsail.shoppingmadebetter.feature.profile.data

interface ProfileRepository {
    suspend fun changePassword(newPassword: String)
    suspend fun updateContactInfo(email: String?, phone: String?)
}