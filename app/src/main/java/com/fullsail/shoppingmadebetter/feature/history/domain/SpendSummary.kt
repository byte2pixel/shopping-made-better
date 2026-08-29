package com.fullsail.shoppingmadebetter.feature.history.domain

import kotlinx.datetime.LocalDate

/** One month's total spend across every store. */
data class MonthlySpend(
    val monthStart: LocalDate,
    val total: Double,
    val tripCount: Int,
)

/** One store's share of a month's spend. */
data class StoreSpend(
    val storeId: String?,
    /** `null` when the store has since been deleted; the UI names it "Unknown store". */
    val storeName: String?,
    val total: Double,
    /** This store's fraction of the month, 0..1, for the bar width. */
    val share: Double,
)

/** The store that would have cost least for what the user actually bought. */
data class CheapestStore(
    val storeId: String,
    val storeName: String,
    /** The compared baskets at this store's current prices. */
    val cost: Double,
    /** What those same baskets actually cost, minus [cost]. Always positive. */
    val saving: Double,
)

/** One store's price for a single past basket. */
data class StoreBasketCost(
    val storeId: String,
    val storeName: String,
    val cost: Double,
    /** Paid minus [cost]; negative where this store is dearer than the trip was. */
    val difference: Double,
)

/**
 * The History tab's insights: this month against last, where the money went, and
 * whether another store would have been cheaper.
 */
data class SpendSummary(
    val thisMonth: MonthlySpend,
    /** `null` when there were no trips the month before. */
    val lastMonth: MonthlySpend?,
    /** [thisMonth] split by store, biggest first. */
    val byStore: List<StoreSpend>,
    /** `null` when nothing is comparable — see [cheapestStore]. */
    val cheapest: CheapestStore?,
) {
    /** Nothing worth showing; the tab hides the whole section. */
    val isEmpty: Boolean =
        thisMonth.tripCount == 0 && lastMonth == null && byStore.isEmpty() && cheapest == null
}
