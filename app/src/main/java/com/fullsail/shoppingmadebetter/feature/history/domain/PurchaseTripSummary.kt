package com.fullsail.shoppingmadebetter.feature.history.domain

import kotlinx.datetime.LocalDate

/**
 * One completed trip as the History list shows it: when, where, what it cost and
 * how many items, with no line items attached. Built from a
 * `purchase_history_summary` row so the list can be paged; open a trip to get its
 * [PurchaseTrip] with items.
 */
data class PurchaseTripSummary(
    val id: String,
    val purchasedOn: LocalDate,
    /** Sort key carried over from the view; never displayed. Keeps "newest first"
     *  exact for two trips made on the same day */
    val purchasedAtEpoch: Long,
    val storeName: String?,
    /** `purchase_history.total_amount` as stored, or `null` when none was recorded. */
    val recordedTotal: Double?,
    /** Sum of the trip's lines, computed by the view. */
    val lineTotal: Double,
    val itemCount: Int,
) {
    /**
     * What the trip cost: the recorded total, falling back to the sum of the lines
     * when the trip was recorded without one.
     */
    val total: Double = recordedTotal ?: lineTotal
}
