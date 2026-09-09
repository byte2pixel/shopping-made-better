package com.fullsail.shoppingmadebetter.feature.household.domain

import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdDto
import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdMemberDto

data class Household(val id: String, val name: String, val inviteCode: String)

data class HouseholdMember(
    val id: String,
    val displayName: String,
    val isHead: Boolean,
    val isSelf: Boolean,
)

/** Messages raised by the household RPCs; `households_test.sql` pins the exact text. */
object HouseholdErrors {
    const val INVALID_CODE = "invalid invite code"
    const val ALREADY_IN_HOUSEHOLD = "already in a household"
    const val HEAD_CANNOT_LEAVE = "head cannot leave while other members remain"
    const val NOT_IN_HOUSEHOLD = "not in a household"
    const val NAME_REQUIRED = "name required"

    fun Throwable.mentions(token: String): Boolean = message?.contains(token) == true
}

internal fun HouseholdDto.toDomain() = Household(id = id, name = name, inviteCode = inviteCode)

internal fun HouseholdMemberDto.toDomain() =
    HouseholdMember(id = id, displayName = displayName, isHead = isHead, isSelf = isSelf)
