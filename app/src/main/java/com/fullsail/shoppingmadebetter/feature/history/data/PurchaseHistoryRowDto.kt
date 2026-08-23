package com.fullsail.shoppingmadebetter.feature.history.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * One row of the `purchase_history_detail` view: a single purchased line item,
 * carrying its trip's header columns (date, store, total) on every row.
 */
@Serializable
data class PurchaseHistoryRowDto(
    val id: String,
    val purchaseId: String,
    val purchasedOn: LocalDate,
    /** Sort key: the trip's `purchased_at` as epoch seconds. Never displayed. */
    val purchasedAtEpoch: Long,
    /** `null` when the store the trip was made at has since been deleted. */
    val storeName: String? = null,
    /** `null` when the trip was recorded without a total. */
    val totalAmount: Double? = null,
    val productId: String,
    val productName: String,
    val brand: String,
    val size: String,
    val imageUrl: String,
    val quantity: Double,
    val pricePaid: Double,
)
