package com.fullsail.shoppingmadebetter.feature.history.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.history.data.HistoryRepository
import javax.inject.Inject

class GetPurchaseHistoryUseCaseImpl @Inject constructor(
    private val historyRepository: HistoryRepository,
) : GetPurchaseHistoryUseCase {
    override suspend fun execute(input: Unit): GetPurchaseHistoryUseCase.Output = try {
        GetPurchaseHistoryUseCase.Output.Success(historyRepository.getPurchaseHistory().toTrips())
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch purchase history: ${e.message}", e)
        GetPurchaseHistoryUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "GetPurchaseHistoryUseCase"
    }
}
