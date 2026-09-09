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
}
