package com.fullsail.shoppingmadebetter.feature.history.domain

import kotlinx.datetime.LocalDate

/** One product bought on a [PurchaseTrip], at the price paid that day. */
data class PurchaseLineItem(
    val id: String,
    val productId: String,
    val productName: String,
    val brand: String,
    val size: String,
    val imageUrl: String,
    val quantity: Double,
    val pricePaid: Double,
) {
    /** What this line cost: unit price times quantity. */
    val lineTotal: Double = quantity * pricePaid
}

/**
 * One completed shopping trip and everything bought on it, built from the flat
 * `purchase_history_detail` rows by [toTrips].
 */
data class PurchaseTrip(
    val id: String,
    val purchasedOn: LocalDate,
    /** Sort key carried over from the view; never displayed. Keeps "newest first"
     *  exact for two trips made on the same day, which a date alone cannot do. */
    val purchasedAtEpoch: Long,
    val storeName: String?,
    /** `purchase_history.total_amount` as stored, or `null` when none was recorded. */
    val recordedTotal: Double?,
    val items: List<PurchaseLineItem>,
) {
    val itemCount: Int = items.size

    /**
     * What the trip cost: the recorded total, falling back to the sum of the lines
     * when the trip was recorded without one. The recorded total always wins — it is
     * what the user actually paid, and recomputing it silently would hide a mismatch.
     */
    val total: Double = recordedTotal ?: items.sumOf { it.lineTotal }
}
