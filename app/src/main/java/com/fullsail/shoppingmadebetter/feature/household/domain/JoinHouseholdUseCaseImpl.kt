package com.fullsail.shoppingmadebetter.feature.household.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdRepository
import com.fullsail.shoppingmadebetter.feature.household.domain.HouseholdErrors.mentions
import javax.inject.Inject

class JoinHouseholdUseCaseImpl @Inject constructor(
    private val householdRepository: HouseholdRepository,
) : JoinHouseholdUseCase {
    override suspend fun execute(input: String): JoinHouseholdUseCase.Output = try {
        JoinHouseholdUseCase.Output.Success(householdRepository.joinHousehold(input.trim()).toDomain())
    } catch (e: Exception) {
        Log.e(TAG, "Failed to join household: ${e.message}", e)
        when {
            e.mentions(HouseholdErrors.INVALID_CODE) -> JoinHouseholdUseCase.Output.InvalidCode
            e.mentions(HouseholdErrors.ALREADY_IN_HOUSEHOLD) -> JoinHouseholdUseCase.Output.AlreadyInHousehold
            else -> JoinHouseholdUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "JoinHouseholdUseCase"
    }
}
