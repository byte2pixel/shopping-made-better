package com.fullsail.shoppingmadebetter.feature.household.data

interface HouseholdRepository {
    /** The caller's household, or null when they have none. */
    suspend fun getHousehold(): HouseholdDto?

    /** Members of the caller's household; empty when they have none. */
    suspend fun getMembers(): List<HouseholdMemberDto>

    suspend fun createHousehold(name: String): HouseholdDto

    suspend fun joinHousehold(inviteCode: String): HouseholdDto

    suspend fun leaveHousehold()

    /** Head only; RLS makes a member's update a no-op. */
    suspend fun renameHousehold(householdId: String, name: String)

    /** Head only. Makes [memberId] the head and the caller a member. */
    suspend fun transferHead(memberId: String)

    /** Head only. Drops [memberId] from the household. */
    suspend fun removeMember(memberId: String)

    /** Head only. Replaces the invite code and returns the new one. */
    suspend fun regenerateInviteCode(): String
}
