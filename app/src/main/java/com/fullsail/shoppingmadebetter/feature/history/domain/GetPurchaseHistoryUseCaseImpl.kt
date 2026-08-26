package com.fullsail.shoppingmadebetter.feature.history.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.history.data.HistoryRepository
import com.fullsail.shoppingmadebetter.feature.history.data.PurchaseTripSummaryDto
import javax.inject.Inject

class GetPurchaseHistoryUseCaseImpl @Inject constructor(
    private val historyRepository: HistoryRepository,
) : GetPurchaseHistoryUseCase {
    override suspend fun execute(
        input: GetPurchaseHistoryUseCase.Input,
    ): GetPurchaseHistoryUseCase.Output = try {
        val page = historyRepository.getPurchaseHistoryPage(
            offset = input.offset,
            limit = input.limit,
        )
        GetPurchaseHistoryUseCase.Output.Success(
            trips = page.map { it.toSummary() },
            // A short page means the history ran out. A page that is exactly
            // `limit` long may still be the last one; the next request comes back
            // empty and ends it there, which costs one extra call but never stops
            // short of a trip the user has.
            endReached = page.size < input.limit,
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch purchase history page at ${input.offset}: ${e.message}", e)
        GetPurchaseHistoryUseCase.Output.Failure(e)
    }

    private fun PurchaseTripSummaryDto.toSummary() = PurchaseTripSummary(
        id = id,
        purchasedOn = purchasedOn,
        purchasedAtEpoch = purchasedAtEpoch,
        storeName = storeName,
        recordedTotal = totalAmount,
        lineTotal = lineTotal,
        itemCount = itemCount,
    )

    private companion object {
        const val TAG = "GetPurchaseHistoryUseCase"
    }
}
