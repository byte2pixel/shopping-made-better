package com.fullsail.shoppingmadebetter.feature.profile.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : ProfileRepository {

    override suspend fun changePassword(newPassword: String) {
        supabaseClient.auth.updateUser {
            password = newPassword
        }
    }

    override suspend fun updateContactInfo(email: String?, phone: String?) {
        supabaseClient.auth.updateUser {
            if (!email.isNullOrBlank()) this.email = email
            if (!phone.isNullOrBlank()) this.phone = phone
        }
    }
}