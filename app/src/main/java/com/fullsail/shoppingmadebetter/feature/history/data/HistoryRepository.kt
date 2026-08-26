package com.fullsail.shoppingmadebetter.feature.history.data

interface HistoryRepository {
    /**
     * One page of the user's completed trips, newest first, as summaries without
     * line items. Reads `purchase_history_summary`, so the cost is one row per
     * card rather than one per purchased item.
     *
     * @param offset how many trips to skip; 0 is the newest trip.
     * @param limit how many trips to return. A shorter result means the end of
     *   the history was reached.
     */
    suspend fun getPurchaseHistoryPage(offset: Int, limit: Int): List<PurchaseTripSummaryDto>

    /**
     * The line items of the trip [purchaseId], or an empty list when no such trip
     * is visible. RLS scopes the rows to the current user.
     */
    suspend fun getPurchase(purchaseId: String): List<PurchaseHistoryRowDto>
}
