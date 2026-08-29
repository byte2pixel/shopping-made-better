package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.feature.history.data.SpendByMonthStoreDto
import com.fullsail.shoppingmadebetter.feature.history.data.TripCostByStoreDto
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus

/**
 * Turning spend rows into the tab's insight cards.
 */

/** The first of the month, today falls in, which is how the view keys its rows. */
internal fun LocalDate.startOfMonth(): LocalDate = LocalDate(year, month, 1)

/** The first of the month before [this]. */
internal fun LocalDate.previousMonthStart(): LocalDate =
    startOfMonth().minus(DatePeriod(months = 1))

/** Every store's spend in [month] added into one total. */
internal fun List<SpendByMonthStoreDto>.monthTotal(month: LocalDate): MonthlySpend {
    val rows = filter { it.monthStart == month }
    return MonthlySpend(
        monthStart = month,
        total = rows.sumOf { it.total },
        tripCount = rows.sumOf { it.tripCount },
    )
}

/**
 * [month] split by store, biggest first, each carrying its share of the month.
 * Empty when nothing was spent (avoid div by zero.)
 */
internal fun List<SpendByMonthStoreDto>.storeBreakdown(month: LocalDate): List<StoreSpend> {
    val rows = filter { it.monthStart == month && it.total > 0 }
    val total = rows.sumOf { it.total }
    if (total <= 0) return emptyList()
    return rows
        .map { StoreSpend(it.storeId, it.storeName, it.total, it.total / total) }
        .sortedByDescending { it.total }
}

/**
 * The store that would have cost least across [costs], or null when no comparison
 * matches all items. A winner only counts if it actually beats what was paid.
 */
internal fun cheapestStore(costs: List<TripCostByStoreDto>): CheapestStore? {
    if (costs.isEmpty()) return null
    val trips = costs.map { it.purchaseId }.toSet()
    val candidates = costs
        .groupBy { it.storeId }
        // Full coverage on every trip, or this store's total is over a smaller
        // basket than the one it is being compared against.
        .filterValues { rows ->
            rows.size == trips.size && rows.all { it.itemsPriced == it.itemsTotal }
        }
    // One store on its own is not a comparison.
    if (candidates.size < 2) return null

    val winner = candidates.values.minByOrNull { rows -> rows.sumOf { it.costHere } } ?: return null
    val cost = winner.sumOf { it.costHere }
    val saving = winner.sumOf { it.paidForSameItems } - cost
    if (saving <= 0) return null
    return CheapestStore(
        storeId = winner.first().storeId,
        storeName = winner.first().storeName,
        cost = cost,
        saving = saving,
    )
}

/**
 * One trip's basket priced at each store that prices all of it, cheapest first.
 * Empty when only the trip's own store can price it — nothing to compare against.
 */
internal fun tripComparison(costs: List<TripCostByStoreDto>): List<StoreBasketCost> {
    val priced = costs.filter { it.itemsPriced == it.itemsTotal && it.itemsTotal > 0 }
    if (priced.size < 2) return emptyList()
    return priced
        .map {
            StoreBasketCost(
                storeId = it.storeId,
                storeName = it.storeName,
                cost = it.costHere,
                difference = it.paidForSameItems - it.costHere,
            )
        }
        .sortedBy { it.cost }
}
