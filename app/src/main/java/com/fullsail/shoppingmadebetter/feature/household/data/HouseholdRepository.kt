package com.fullsail.shoppingmadebetter.feature.household.data

interface HouseholdRepository {
    /** The caller's household, or null when they have none. */
    suspend fun getHousehold(): HouseholdDto?

    /** Members of the caller's household; empty when they have none. */
    suspend fun getMembers(): List<HouseholdMemberDto>

    suspend fun createHousehold(name: String): HouseholdDto

    suspend fun joinHousehold(inviteCode: String): HouseholdDto

    suspend fun leaveHousehold()
}
