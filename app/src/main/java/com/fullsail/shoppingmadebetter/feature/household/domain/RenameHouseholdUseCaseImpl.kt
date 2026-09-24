package com.fullsail.shoppingmadebetter.feature.household.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdRepository
import javax.inject.Inject

class RenameHouseholdUseCaseImpl @Inject constructor(
    private val householdRepository: HouseholdRepository,
) : RenameHouseholdUseCase {
    override suspend fun execute(input: RenameHousehold): RenameHouseholdUseCase.Output {
        val name = input.name.trim()
        if (name.isEmpty()) {
            return RenameHouseholdUseCase.Output.Failure(IllegalArgumentException("Household name is blank"))
        }
        return try {
            householdRepository.renameHousehold(input.householdId, name)
            RenameHouseholdUseCase.Output.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename household: ${e.message}", e)
            RenameHouseholdUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "RenameHouseholdUseCase"
    }
}
