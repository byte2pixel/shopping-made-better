package com.fullsail.shoppingmadebetter.feature.history.data

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class SpendRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : SpendRepository {
    override suspend fun getSpendByMonth(
        sinceMonth: LocalDate,
    ): List<SpendByMonthStoreDto> = withContext(Dispatchers.IO) {
        postgrest.from(SPEND_BY_MONTH_STORE)
            .select { filter { gte("monthStart", sinceMonth.toString()) } }
            .decodeList<SpendByMonthStoreDto>()
    }

    override suspend fun getTripCostByStore(
        purchaseId: String,
    ): List<TripCostByStoreDto> = withContext(Dispatchers.IO) {
        postgrest.from(TRIP_COST_BY_STORE)
            .select { filter { eq("purchaseId", purchaseId) } }
            .decodeList<TripCostByStoreDto>()
    }

    override suspend fun getTripCostsSince(
        from: LocalDate,
    ): List<TripCostByStoreDto> = withContext(Dispatchers.IO) {
        postgrest.from(TRIP_COST_BY_STORE)
            .select { filter { gte("purchasedOn", from.toString()) } }
            .decodeList<TripCostByStoreDto>()
    }

    private companion object {
        /** One row per month per store. */
        const val SPEND_BY_MONTH_STORE = "purchase_spend_by_month_store"

        /** One row per trip per store, priced at today's prices. */
        const val TRIP_COST_BY_STORE = "purchase_trip_cost_by_store"
    }
}
