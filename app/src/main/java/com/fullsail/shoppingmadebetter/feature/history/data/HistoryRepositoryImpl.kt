package com.fullsail.shoppingmadebetter.feature.history.data

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : HistoryRepository {
    override suspend fun getPurchaseHistory(): List<PurchaseHistoryRowDto> =
        withContext(Dispatchers.IO) {
            postgrest.from(PURCHASE_HISTORY_DETAIL).select().decodeList<PurchaseHistoryRowDto>()
        }

    override suspend fun getPurchase(purchaseId: String): List<PurchaseHistoryRowDto> =
        withContext(Dispatchers.IO) {
            postgrest.from(PURCHASE_HISTORY_DETAIL)
                .select { filter { eq("purchaseId", purchaseId) } }
                .decodeList<PurchaseHistoryRowDto>()
        }

    private companion object {
        /** Ordered newest-trip-first by the view itself, so neither call re-orders. */
        const val PURCHASE_HISTORY_DETAIL = "purchase_history_detail"
    }
}
