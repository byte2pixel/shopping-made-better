package com.fullsail.shoppingmadebetter.feature.pantry.domain

data class InventoryItem(
    val id: String,
    val productId: String,
    val name: String,
    val brand: String,
    val description: String,
    val size: String,
    val imageUrl: String,
    val quantity: Int,
    val expiresInDays: Int?,
    val location: PantryLocation = PantryLocation.Pantry,
    val lowStockThreshold: Int? = null,
    /** Reason on the lot's latest audit row; `null` when it has none. */
    val lastAdjustmentReason: AdjustmentReason? = null,
    val estimateSource: EstimateSource? = null,
) {
    /** True while the quantity is an unconfirmed estimate (`auto`, or `dismissed` without confirming). */
    val estimated: Boolean
        get() = lastAdjustmentReason == AdjustmentReason.Auto ||
            lastAdjustmentReason == AdjustmentReason.Dismissed
}
