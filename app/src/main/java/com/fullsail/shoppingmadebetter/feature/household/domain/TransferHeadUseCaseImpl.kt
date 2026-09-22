package com.fullsail.shoppingmadebetter.feature.household.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdRepository
import com.fullsail.shoppingmadebetter.feature.household.domain.HouseholdErrors.mentions
import javax.inject.Inject

class TransferHeadUseCaseImpl @Inject constructor(
    private val householdRepository: HouseholdRepository,
) : TransferHeadUseCase {
    override suspend fun execute(input: String): TransferHeadUseCase.Output = try {
        householdRepository.transferHead(input)
        TransferHeadUseCase.Output.Success
    } catch (e: Exception) {
        Log.e(TAG, "Failed to transfer household head: ${e.message}", e)
        when {
            e.mentions(HouseholdErrors.NOT_THE_HEAD) -> TransferHeadUseCase.Output.NotHead
            e.mentions(HouseholdErrors.MEMBER_NOT_IN_HOUSEHOLD) -> TransferHeadUseCase.Output.MemberNotInHousehold
            else -> TransferHeadUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "TransferHeadUseCase"
    }
}
