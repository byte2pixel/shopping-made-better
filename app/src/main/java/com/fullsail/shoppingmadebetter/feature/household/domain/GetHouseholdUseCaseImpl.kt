package com.fullsail.shoppingmadebetter.feature.household.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdRepository
import javax.inject.Inject

class GetHouseholdUseCaseImpl @Inject constructor(
    private val householdRepository: HouseholdRepository,
) : GetHouseholdUseCase {
    override suspend fun execute(input: Unit): GetHouseholdUseCase.Output = try {
        val household = householdRepository.getHousehold()
        if (household == null) {
            GetHouseholdUseCase.Output.None
        } else {
            val members = householdRepository.getMembers()
                .map { it.toDomain() }
                .sortedWith(compareByDescending<HouseholdMember> { it.isHead }.thenBy { it.displayName.lowercase() })
            GetHouseholdUseCase.Output.Member(household.toDomain(), members)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load household: ${e.message}", e)
        GetHouseholdUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "GetHouseholdUseCase"
    }
}
