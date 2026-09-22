package com.fullsail.shoppingmadebetter.feature.household.domain

import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdDto
import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdMemberDto
import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdRepository

/**
 * In-memory household. Every call throws [failWith] when set, so a test can hand it an
 * exception carrying one of the [HouseholdErrors] tokens.
 */
internal class FakeHouseholdRepository(
    var household: HouseholdDto? = null,
    var members: List<HouseholdMemberDto> = emptyList(),
    var failWith: Throwable? = null,
) : HouseholdRepository {
    val createdNames = mutableListOf<String>()
    val joinedCodes = mutableListOf<String>()
    var leaveCalls = 0
    var renamedTo: Pair<String, String>? = null
    var transferredTo: String? = null
    var removed: String? = null
    var regenerated = 0

    override suspend fun getHousehold(): HouseholdDto? {
        failWith?.let { throw it }
        return household
    }

    override suspend fun getMembers(): List<HouseholdMemberDto> {
        failWith?.let { throw it }
        return members
    }

    override suspend fun createHousehold(name: String): HouseholdDto {
        createdNames += name
        failWith?.let { throw it }
        return HouseholdDto(id = "h-new", name = name, inviteCode = "NEW12345").also { household = it }
    }

    override suspend fun joinHousehold(inviteCode: String): HouseholdDto {
        joinedCodes += inviteCode
        failWith?.let { throw it }
        return (household ?: DEMO).also { household = it }
    }

    override suspend fun leaveHousehold() {
        leaveCalls++
        failWith?.let { throw it }
        household = null
        members = emptyList()
    }

    override suspend fun renameHousehold(householdId: String, name: String) {
        renamedTo = householdId to name
        failWith?.let { throw it }
        household = household?.copy(name = name)
    }

    override suspend fun transferHead(memberId: String) {
        transferredTo = memberId
        failWith?.let { throw it }
        members = members.map { it.copy(isHead = it.id == memberId) }
    }

    override suspend fun removeMember(memberId: String) {
        removed = memberId
        failWith?.let { throw it }
        members = members.filterNot { it.id == memberId }
    }

    override suspend fun regenerateInviteCode(): String {
        regenerated++
        failWith?.let { throw it }
        household = household?.copy(inviteCode = NEW_CODE)
        return NEW_CODE
    }

    companion object {
        const val NEW_CODE = "A1B2C3D4"
        val DEMO = HouseholdDto(id = "h1", name = "Demo Household", inviteCode = "DEMO2026")
        val HEAD = HouseholdMemberDto(id = "u1", displayName = "Demo Shopper", isHead = true, isSelf = false)
        val SELF = HouseholdMemberDto(id = "u2", displayName = "Demo Roommate", isHead = false, isSelf = true)
    }
}
