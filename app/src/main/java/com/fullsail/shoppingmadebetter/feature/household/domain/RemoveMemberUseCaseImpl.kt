package com.fullsail.shoppingmadebetter.feature.household.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdRepository
import com.fullsail.shoppingmadebetter.feature.household.domain.HouseholdErrors.mentions
import javax.inject.Inject

class RemoveMemberUseCaseImpl @Inject constructor(
    private val householdRepository: HouseholdRepository,
) : RemoveMemberUseCase {
    override suspend fun execute(input: String): RemoveMemberUseCase.Output = try {
        householdRepository.removeMember(input)
        RemoveMemberUseCase.Output.Success
    } catch (e: Exception) {
        Log.e(TAG, "Failed to remove household member: ${e.message}", e)
        when {
            e.mentions(HouseholdErrors.NOT_THE_HEAD) -> RemoveMemberUseCase.Output.NotHead
            e.mentions(HouseholdErrors.MEMBER_NOT_IN_HOUSEHOLD) -> RemoveMemberUseCase.Output.MemberNotInHousehold
            e.mentions(HouseholdErrors.CANNOT_REMOVE_SELF) -> RemoveMemberUseCase.Output.CannotRemoveSelf
            else -> RemoveMemberUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "RemoveMemberUseCase"
    }
}
