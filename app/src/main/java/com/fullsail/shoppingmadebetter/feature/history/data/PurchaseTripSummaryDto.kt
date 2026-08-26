package com.fullsail.shoppingmadebetter.feature.history.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * One row of the `purchase_history_summary` view
 */
@Serializable
data class PurchaseTripSummaryDto(
    val id: String,
    val purchasedOn: LocalDate,
    /** Sort key: the trip's `purchased_at` as epoch seconds. Never displayed. */
    val purchasedAtEpoch: Long,
    /** `null` when the store the trip was made at has since been deleted. */
    val storeName: String? = null,
    /** `purchase_history.total_amount` as recorded; `null` when none was. */
    val totalAmount: Double? = null,
    /** `sum(quantity * price_paid)` over the trip's items; `0.0` when it has none. */
    val lineTotal: Double = 0.0,
    val itemCount: Int = 0,
)
