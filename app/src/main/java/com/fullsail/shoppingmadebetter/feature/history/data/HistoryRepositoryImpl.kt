package com.fullsail.shoppingmadebetter.feature.history.data

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Narrows a summary-view read to [query].
 *
 * Its own function so the parameters it builds can be asserted without a server —
 * the rules below are about how this client encodes a filter, which a fake
 * repository cannot exercise.
 */
internal fun PostgrestFilterBuilder.applyHistoryQuery(query: HistoryQuery) {
    // Omitted entirely when empty — `isIn` on no values matches nothing, which
    // would read as "you have no history".
    if (query.storeIds.isNotEmpty()) {
        isIn("storeId", query.storeIds)
    }

    // Both bounds inclusive, and each omitted when absent so a one-sided range
    // stays one-sided. `toString()` is the ISO form the `date` column compares
    // against.
    //
    // A full range goes inside `and` because only the *first* filter per column
    // survives — the client collapses its parameters with `mapToFirstValue` — so a
    // plain gte/lte pair on "purchasedOn" loses the lte and the range silently runs
    // open-ended. The group travels as one parameter, so both bounds arrive.
    val from = query.from
    val to = query.to
    when {
        from != null && to != null -> and {
            gte("purchasedOn", from.toString())
            lte("purchasedOn", to.toString())
        }

        from != null -> gte("purchasedOn", from.toString())
        to != null -> lte("purchasedOn", to.toString())
    }
}

class HistoryRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : HistoryRepository {
    /**
     * Orders explicitly rather than leaning on an ORDER BY inside the view:
     * Postgres does not promise an inner ordering survives an outer LIMIT/OFFSET,
     * and a page boundary that shifts between requests would drop or duplicate
     * trips. `id` breaks ties so two trips recorded at the same instant always
     * land on the same side of a boundary.
     */
    override suspend fun getPurchaseHistoryPage(
        offset: Int,
        limit: Int,
        query: HistoryQuery,
    ): List<PurchaseTripSummaryDto> = withContext(Dispatchers.IO) {
        postgrest.from(PURCHASE_HISTORY_SUMMARY)
            .select {
                // Filtering runs on the server, before the range: the list is paged,
                // so narrowing the loaded page instead would only ever search the
                // trips already on screen and call the rest a miss.
                filter { applyHistoryQuery(query) }
                order("purchasedAtEpoch", Order.DESCENDING)
                order("id", Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }
            .decodeList<PurchaseTripSummaryDto>()
    }

    override suspend fun getPurchase(purchaseId: String): List<PurchaseHistoryRowDto> =
        withContext(Dispatchers.IO) {
            postgrest.from(PURCHASE_HISTORY_DETAIL)
                .select { filter { eq("purchaseId", purchaseId) } }
                .decodeList<PurchaseHistoryRowDto>()
        }

    private companion object {
        /** One row per trip, for the paged list. */
        const val PURCHASE_HISTORY_SUMMARY = "purchase_history_summary"

        /** One row per purchased line item; read whole, for one trip at a time. */
        const val PURCHASE_HISTORY_DETAIL = "purchase_history_detail"
    }
}
