package com.fullsail.shoppingmadebetter.feature.household.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdRepository
import com.fullsail.shoppingmadebetter.feature.household.domain.HouseholdErrors.mentions
import javax.inject.Inject

class RegenerateInviteCodeUseCaseImpl @Inject constructor(
    private val householdRepository: HouseholdRepository,
) : RegenerateInviteCodeUseCase {
    override suspend fun execute(input: Unit): RegenerateInviteCodeUseCase.Output = try {
        RegenerateInviteCodeUseCase.Output.Success(householdRepository.regenerateInviteCode())
    } catch (e: Exception) {
        Log.e(TAG, "Failed to regenerate invite code: ${e.message}", e)
        if (e.mentions(HouseholdErrors.NOT_THE_HEAD)) {
            RegenerateInviteCodeUseCase.Output.NotHead
        } else {
            RegenerateInviteCodeUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "RegenerateInviteCodeUseCase"
    }
}
