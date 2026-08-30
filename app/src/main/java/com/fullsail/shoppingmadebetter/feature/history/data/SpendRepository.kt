package com.fullsail.shoppingmadebetter.feature.history.data

import kotlinx.datetime.LocalDate

/**
 * Reads the spend aggregates behind the History tab's insights.
 * Answer what was spent.
 */
interface SpendRepository {
    /**
     * Monthly spend per store, for months starting on or after [sinceMonth].
     */
    suspend fun getSpendByMonth(sinceMonth: LocalDate): List<SpendByMonthStoreDto>

    /** What the trip [purchaseId]'s basket costs at each store today. */
    suspend fun getTripCostByStore(purchaseId: String): List<TripCostByStoreDto>

    /** The same, for every trip made on or after [from]. */
    suspend fun getTripCostsSince(from: LocalDate): List<TripCostByStoreDto>
}
