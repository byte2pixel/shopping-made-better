package com.fullsail.shoppingmadebetter.feature.history.domain

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * Feeds the History list one page of trips at a time. The key is a row offset,
 * so it maps straight onto the `limit`/`offset` the summary view is read with.
 *
 * Append-only: the list starts at the newest trip and only ever loads older ones,
 * so [LoadResult.Page.prevKey] is always null.
 *
 * One source pages one [filter]: the offsets it hands out are positions within
 * that filter's results, so they mean nothing under a different one. Changing the
 * filter builds a new source rather than invalidating this one — see
 * `HistoryViewModel`.
 */
class PurchaseHistoryPagingSource(
    private val getPurchaseHistoryUseCase: GetPurchaseHistoryUseCase,
    private val filter: HistoryFilter = HistoryFilter(),
) : PagingSource<Int, PurchaseTripSummary>() {

    override suspend fun load(
        params: LoadParams<Int>,
    ): LoadResult<Int, PurchaseTripSummary> {
        val offset = params.key ?: 0
        val output = getPurchaseHistoryUseCase.execute(
            GetPurchaseHistoryUseCase.Input(
                offset = offset,
                limit = params.loadSize,
                filter = filter,
            ),
        )
        return when (output) {
            is GetPurchaseHistoryUseCase.Output.Success -> LoadResult.Page(
                data = output.trips,
                prevKey = null,
                // Advance by what actually arrived, not by loadSize: the two differ
                // on the first load, where Paging asks for more than one page.
                nextKey = if (output.endReached || output.trips.isEmpty()) {
                    null
                } else {
                    offset + output.trips.size
                },
            )

            is GetPurchaseHistoryUseCase.Output.Failure -> LoadResult.Error(output.error)
        }
    }

    /**
     * Always refreshes from the newest trip. Anchoring to the visible position is
     * wrong here: a trip completed on the shopping-list tab is prepended, which
     * would shift every offset and re-show rows the user already scrolled past.
     */
    override fun getRefreshKey(state: PagingState<Int, PurchaseTripSummary>): Int? = null
}
