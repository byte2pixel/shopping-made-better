package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.feature.history.data.HistoryQuery
import com.fullsail.shoppingmadebetter.feature.history.data.HistoryRepository
import com.fullsail.shoppingmadebetter.feature.history.data.PurchaseHistoryRowDto
import com.fullsail.shoppingmadebetter.feature.history.data.PurchaseTripSummaryDto
import kotlinx.datetime.LocalDate

/**
 * Fake repository. [rows] backs the detail read, [summaries] backs the paged list
 * read, and [error] makes either throw.
 *
 * The page read really slices [summaries] rather than returning them whole, so a
 * test that gets its paging boundaries wrong fails here instead of passing against
 * a fake that ignores offset and limit.
 *
 * It filters before it slices, the way the server does. That ordering is the whole
 * point of pushing filters down, so the fake has to honour it: a fake that sliced
 * first would let a broken implementation pass.
 */
internal class FakeHistoryRepository(
    private val rows: List<PurchaseHistoryRowDto> = emptyList(),
    private val summaries: List<PurchaseTripSummaryDto> = emptyList(),
    private val error: Throwable? = null,
) : HistoryRepository {
    /** Every (offset, limit) the fake was asked for, in order. */
    val requestedPages = mutableListOf<Pair<Int, Int>>()

    /** Every query the fake was asked with, in order, aligned with [requestedPages]. */
    val requestedQueries = mutableListOf<HistoryQuery>()

    override suspend fun getPurchaseHistoryPage(
        offset: Int,
        limit: Int,
        query: HistoryQuery,
    ): List<PurchaseTripSummaryDto> {
        requestedPages += offset to limit
        requestedQueries += query
        error?.let { throw it }
        val matching = summaries.filter { it.matches(query) }
        if (offset >= matching.size) return emptyList()
        return matching.subList(offset, minOf(offset + limit, matching.size)).toList()
    }

    /** The server's narrowing, in Kotlin: an empty store list matches everything. */
    private fun PurchaseTripSummaryDto.matches(query: HistoryQuery): Boolean =
        query.storeIds.isEmpty() || storeId in query.storeIds

    override suspend fun getPurchase(purchaseId: String): List<PurchaseHistoryRowDto> =
        error?.let { throw it } ?: rows.filter { it.purchaseId == purchaseId }
}

/** A detail-view row with sensible defaults; override only what a test is about. */
internal fun row(
    purchaseId: String,
    id: String = "line-$purchaseId",
    purchasedOn: LocalDate = LocalDate(2026, 8, 12),
    purchasedAtEpoch: Long = 1_786_504_429L,
    storeName: String? = "ALDI",
    totalAmount: Double? = 10.0,
    productId: String = "product-$id",
    productName: String = "Product $id",
    brand: String = "Some Brand",
    size: String = "1 ea",
    imageUrl: String = "",
    quantity: Double = 1.0,
    pricePaid: Double = 2.50,
) = PurchaseHistoryRowDto(
    id = id,
    purchaseId = purchaseId,
    purchasedOn = purchasedOn,
    purchasedAtEpoch = purchasedAtEpoch,
    storeName = storeName,
    totalAmount = totalAmount,
    productId = productId,
    productName = productName,
    brand = brand,
    size = size,
    imageUrl = imageUrl,
    quantity = quantity,
    pricePaid = pricePaid,
)

/** A summary-view row with sensible defaults; override only what a test is about. */
internal fun summaryRow(
    id: String,
    purchasedOn: LocalDate = LocalDate(2026, 8, 12),
    purchasedAtEpoch: Long = 1_786_504_429L,
    storeId: String? = "store-aldi",
    storeName: String? = "ALDI",
    totalAmount: Double? = 10.0,
    lineTotal: Double = 10.0,
    itemCount: Int = 2,
) = PurchaseTripSummaryDto(
    id = id,
    purchasedOn = purchasedOn,
    purchasedAtEpoch = purchasedAtEpoch,
    storeId = storeId,
    storeName = storeName,
    totalAmount = totalAmount,
    lineTotal = lineTotal,
    itemCount = itemCount,
)

/** [count] summary rows, ids `trip-0`..`trip-<count-1>`, newest first. */
internal fun summaryRows(count: Int): List<PurchaseTripSummaryDto> =
    List(count) { index ->
        summaryRow(id = "trip-$index", purchasedAtEpoch = (count - index).toLong())
    }
