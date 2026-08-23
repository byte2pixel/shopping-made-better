package com.fullsail.shoppingmadebetter.feature.history.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.history.data.HistoryRepository
import javax.inject.Inject

class GetPurchaseTripUseCaseImpl @Inject constructor(
    private val historyRepository: HistoryRepository,
) : GetPurchaseTripUseCase {
    override suspend fun execute(input: String): GetPurchaseTripUseCase.Output = try {
        when (val trip = historyRepository.getPurchase(input).toTrips().firstOrNull()) {
            null -> GetPurchaseTripUseCase.Output.NotFound
            else -> GetPurchaseTripUseCase.Output.Success(trip)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch purchase $input: ${e.message}", e)
        GetPurchaseTripUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "GetPurchaseTripUseCase"
    }
}
