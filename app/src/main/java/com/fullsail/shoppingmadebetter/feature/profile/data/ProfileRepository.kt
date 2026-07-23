package com.fullsail.shoppingmadebetter.feature.profile.data

interface ProfileRepository {
    suspend fun changePassword(newPassword: String)
}