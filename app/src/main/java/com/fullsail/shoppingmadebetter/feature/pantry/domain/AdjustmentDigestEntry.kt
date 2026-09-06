package com.fullsail.shoppingmadebetter.feature.pantry.domain

/**
 * One automatic adjustment in this week's digest.
 *
 * [quantityNow] is the lot's own quantity; [productQuantity] is the product's total across
 * the user's lots, which with [lowStockThreshold] gives the row's stock marker — the same
 * basis as the pantry card's.
 */
data class AdjustmentDigestEntry(
    val adjustmentId: String,
    val lotId: String,
    val productId: String,
    val productName: String,
    val imageUrl: String,
    val delta: Int,
    val quantityNow: Int,
    val productQuantity: Int,
    val lowStockThreshold: Int? = null,
    val source: EstimateSource? = null,
    /** Whole days between the adjustment and today; 0 means today. */
    val daysAgo: Int,
)
