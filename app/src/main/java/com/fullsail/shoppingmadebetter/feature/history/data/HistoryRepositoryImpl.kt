package com.fullsail.shoppingmadebetter.feature.history.data

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : HistoryRepository {
    /**
     * Orders explicitly rather than leaning on an ORDER BY inside the view:
     * Postgres does not promise an inner ordering survives an outer LIMIT/OFFSET,
     * and a page boundary that shifts between requests would drop or duplicate
     * trips. `id` breaks ties so two trips recorded at the same instant always
     * land on the same side of a boundary.
     */
    override suspend fun getPurchaseHistoryPage(
        offset: Int,
        limit: Int,
    ): List<PurchaseTripSummaryDto> = withContext(Dispatchers.IO) {
        postgrest.from(PURCHASE_HISTORY_SUMMARY)
            .select {
                order("purchasedAtEpoch", Order.DESCENDING)
                order("id", Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }
            .decodeList<PurchaseTripSummaryDto>()
    }

    override suspend fun getPurchase(purchaseId: String): List<PurchaseHistoryRowDto> =
        withContext(Dispatchers.IO) {
            postgrest.from(PURCHASE_HISTORY_DETAIL)
                .select { filter { eq("purchaseId", purchaseId) } }
                .decodeList<PurchaseHistoryRowDto>()
        }

    private companion object {
        /** One row per trip, for the paged list. */
        const val PURCHASE_HISTORY_SUMMARY = "purchase_history_summary"

        /** One row per purchased line item; read whole, for one trip at a time. */
        const val PURCHASE_HISTORY_DETAIL = "purchase_history_detail"
    }
}
