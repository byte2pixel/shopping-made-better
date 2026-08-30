package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.feature.history.data.SpendByMonthStoreDto
import com.fullsail.shoppingmadebetter.feature.history.data.SpendRepository
import com.fullsail.shoppingmadebetter.feature.history.data.TripCostByStoreDto
import kotlinx.datetime.LocalDate

/**
 * Fake spend repository. Really applies the date bounds the server would, so a use
 * case that asks for the wrong window fails here rather than passing against a fake
 * that returns everything.
 */
internal class FakeSpendRepository(
    private val months: List<SpendByMonthStoreDto> = emptyList(),
    private val tripCosts: List<TripCostByStoreDto> = emptyList(),
    private val error: Throwable? = null,
) : SpendRepository {
    /** The bounds the fake was asked with, for asserting the requested windows. */
    var requestedSinceMonth: LocalDate? = null
        private set
    var requestedCostsSince: LocalDate? = null
        private set

    override suspend fun getSpendByMonth(sinceMonth: LocalDate): List<SpendByMonthStoreDto> {
        requestedSinceMonth = sinceMonth
        error?.let { throw it }
        return months.filter { it.monthStart >= sinceMonth }
    }

    override suspend fun getTripCostByStore(purchaseId: String): List<TripCostByStoreDto> {
        error?.let { throw it }
        return tripCosts.filter { it.purchaseId == purchaseId }
    }

    override suspend fun getTripCostsSince(from: LocalDate): List<TripCostByStoreDto> {
        requestedCostsSince = from
        error?.let { throw it }
        return tripCosts.filter { it.purchasedOn >= from }
    }
}

/** A monthly-spend row; override only what a test is about. */
internal fun monthRow(
    monthStart: LocalDate,
    storeId: String? = "s-1",
    storeName: String? = "ALDI",
    total: Double = 10.0,
    tripCount: Int = 1,
) = SpendByMonthStoreDto(monthStart, storeId, storeName, total, tripCount)

/** A trip-cost row, fully priced unless a test says otherwise. */
internal fun costRow(
    purchaseId: String = "trip-1",
    purchasedOn: LocalDate = LocalDate(2026, 8, 20),
    storeId: String = "s-1",
    storeName: String = "ALDI",
    costHere: Double = 10.0,
    paidForSameItems: Double = 12.0,
    itemsPriced: Int = 4,
    itemsTotal: Int = 4,
) = TripCostByStoreDto(
    purchaseId = purchaseId,
    purchasedOn = purchasedOn,
    storeId = storeId,
    storeName = storeName,
    costHere = costHere,
    paidForSameItems = paidForSameItems,
    itemsPriced = itemsPriced,
    itemsTotal = itemsTotal,
)
