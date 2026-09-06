package com.fullsail.shoppingmadebetter.feature.profile.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    override suspend fun getAutoAdjustEnabled(): Boolean = withContext(Dispatchers.IO) {
        supabaseClient.postgrest.from("profiles")
            .select(Columns.list("auto_adjust_enabled")) { filter { eq("id", currentUserId()) } }
            .decodeSingle<AutoAdjustSettingDto>()
            .autoAdjustEnabled
    }

    override suspend fun setAutoAdjustEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        supabaseClient.postgrest.from("profiles")
            .update({ set("auto_adjust_enabled", enabled) }) { filter { eq("id", currentUserId()) } }
        Unit
    }

    private fun currentUserId(): String =
        supabaseClient.auth.currentUserOrNull()?.id ?: error("Not signed in")
}
