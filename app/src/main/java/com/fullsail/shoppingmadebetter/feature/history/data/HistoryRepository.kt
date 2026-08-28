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
     * @param query which trips count as history at all. Offset and limit apply to
     *   the filtered result, so paging stays exact under a filter.
     */
    suspend fun getPurchaseHistoryPage(
        offset: Int,
        limit: Int,
        query: HistoryQuery = HistoryQuery(),
    ): List<PurchaseTripSummaryDto>

    /**
     * The line items of the trip [purchaseId], or an empty list when no such trip
     * is visible. RLS scopes the rows to the current user.
     */
    suspend fun getPurchase(purchaseId: String): List<PurchaseHistoryRowDto>
}
