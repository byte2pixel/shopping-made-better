package com.fullsail.shoppingmadebetter.feature.history.data

interface HistoryRepository {
    /**
     * Every purchased line item, newest trip first. RLS on `purchase_history`
     * scopes the rows to the current user.
     */
    suspend fun getPurchaseHistory(): List<PurchaseHistoryRowDto>

    /**
     * The line items of the trip [purchaseId], or an empty list when no such trip
     * is visible. RLS scopes the rows to the current user.
     */
    suspend fun getPurchase(purchaseId: String): List<PurchaseHistoryRowDto>
}
