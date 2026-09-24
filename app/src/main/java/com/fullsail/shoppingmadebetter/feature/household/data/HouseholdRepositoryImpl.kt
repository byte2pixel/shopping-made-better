package com.fullsail.shoppingmadebetter.feature.household.data

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class HouseholdRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : HouseholdRepository {
    override suspend fun getHousehold(): HouseholdDto? = withContext(Dispatchers.IO) {
        postgrest.from("households").select().decodeSingleOrNull<HouseholdDto>()
    }

    override suspend fun getMembers(): List<HouseholdMemberDto> = withContext(Dispatchers.IO) {
        postgrest.from("household_members").select().decodeList<HouseholdMemberDto>()
    }

    override suspend fun createHousehold(name: String): HouseholdDto = withContext(Dispatchers.IO) {
        postgrest.rpc("create_household", buildJsonObject { put("p_name", name) })
            .decodeSingle<HouseholdDto>()
    }

    override suspend fun joinHousehold(inviteCode: String): HouseholdDto = withContext(Dispatchers.IO) {
        postgrest.rpc("join_household", buildJsonObject { put("p_invite_code", inviteCode) })
            .decodeSingle<HouseholdDto>()
    }

    override suspend fun leaveHousehold() = withContext(Dispatchers.IO) {
        postgrest.rpc("leave_household")
        Unit
    }

    override suspend fun renameHousehold(householdId: String, name: String) = withContext(Dispatchers.IO) {
        postgrest.from("households").update({ set("name", name) }) {
            filter { eq("id", householdId) }
        }
        Unit
    }

    override suspend fun transferHead(memberId: String) = withContext(Dispatchers.IO) {
        postgrest.rpc("transfer_household_head", buildJsonObject { put("p_member", memberId) })
        Unit
    }

    override suspend fun removeMember(memberId: String) = withContext(Dispatchers.IO) {
        postgrest.rpc("remove_household_member", buildJsonObject { put("p_member", memberId) })
        Unit
    }

    // The RPC returns text, which PostgREST serialises as a bare JSON string.
    override suspend fun regenerateInviteCode(): String = withContext(Dispatchers.IO) {
        postgrest.rpc("regenerate_invite_code").decodeAs<String>()
    }
}
