package com.fullsail.shoppingmadebetter.feature.household.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A `households` row, or the row returned by the create/join RPCs. */
@Serializable
data class HouseholdDto(
    val id: String,
    val name: String,
    @SerialName("invite_code") val inviteCode: String,
)

/** One row of the `household_members` view. */
@Serializable
data class HouseholdMemberDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("is_household_head") val isHead: Boolean,
    @SerialName("is_self") val isSelf: Boolean,
)
