package com.fullsail.shoppingmadebetter.feature.pantry.data

import kotlinx.serialization.Serializable

/** One row of `inventory_adjustments_detail`: an automatic adjustment made this week. */
@Serializable
data class AdjustmentDigestEntryDto(
    val adjustmentId: String,
    val inventoryItemId: String,
    val productId: String,
    val productName: String,
    val imageUrl: String,
    /** Signed change the job applied, in whole units. */
    val delta: Int,
    /** The lot's quantity after the adjustment. */
    val quantityNow: Int,
    /** The product's total across the user's lots, which the stock marker reads. */
    val productQuantity: Int,
    val lowStockThreshold: Int? = null,
    /** What the estimate was based on: `history`, `shelf_life` or `manual`. */
    val estimateSource: String? = null,
    /** When the adjustment was written, epoch seconds. */
    val createdAtEpoch: Long,
)
