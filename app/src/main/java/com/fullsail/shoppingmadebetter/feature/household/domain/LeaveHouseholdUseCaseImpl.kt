package com.fullsail.shoppingmadebetter.feature.household.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdRepository
import com.fullsail.shoppingmadebetter.feature.household.domain.HouseholdErrors.mentions
import javax.inject.Inject

class LeaveHouseholdUseCaseImpl @Inject constructor(
    private val householdRepository: HouseholdRepository,
) : LeaveHouseholdUseCase {
    override suspend fun execute(input: Unit): LeaveHouseholdUseCase.Output = try {
        householdRepository.leaveHousehold()
        LeaveHouseholdUseCase.Output.Success
    } catch (e: Exception) {
        Log.e(TAG, "Failed to leave household: ${e.message}", e)
        when {
            e.mentions(HouseholdErrors.HEAD_CANNOT_LEAVE) -> LeaveHouseholdUseCase.Output.HeadWithMembers
            // Already out, which is the state the caller asked for.
            e.mentions(HouseholdErrors.NOT_IN_HOUSEHOLD) -> LeaveHouseholdUseCase.Output.Success
            else -> LeaveHouseholdUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "LeaveHouseholdUseCase"
    }
}
