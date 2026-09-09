package com.fullsail.shoppingmadebetter.feature.household.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdRepository
import com.fullsail.shoppingmadebetter.feature.household.domain.HouseholdErrors.mentions
import javax.inject.Inject

class CreateHouseholdUseCaseImpl @Inject constructor(
    private val householdRepository: HouseholdRepository,
) : CreateHouseholdUseCase {
    override suspend fun execute(input: String): CreateHouseholdUseCase.Output = try {
        CreateHouseholdUseCase.Output.Success(householdRepository.createHousehold(input.trim()).toDomain())
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create household: ${e.message}", e)
        if (e.mentions(HouseholdErrors.ALREADY_IN_HOUSEHOLD)) {
            CreateHouseholdUseCase.Output.AlreadyInHousehold
        } else {
            CreateHouseholdUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "CreateHouseholdUseCase"
    }
}
