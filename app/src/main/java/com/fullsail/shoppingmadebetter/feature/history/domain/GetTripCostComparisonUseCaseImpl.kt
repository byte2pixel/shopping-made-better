package com.fullsail.shoppingmadebetter.feature.history.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.history.data.SpendRepository
import javax.inject.Inject

class GetTripCostComparisonUseCaseImpl @Inject constructor(
    private val spendRepository: SpendRepository,
) : GetTripCostComparisonUseCase {
    override suspend fun execute(input: String): GetTripCostComparisonUseCase.Output = try {
        GetTripCostComparisonUseCase.Output.Success(
            tripComparison(spendRepository.getTripCostByStore(input)),
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to compare trip $input across stores: ${e.message}", e)
        GetTripCostComparisonUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "GetTripCostComparisonUseCase"
    }
}
