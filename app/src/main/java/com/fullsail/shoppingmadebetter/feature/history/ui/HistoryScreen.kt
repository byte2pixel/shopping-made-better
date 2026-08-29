package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryDatePreset
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryFilter
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTripSummary
import com.fullsail.shoppingmadebetter.feature.history.domain.isActive
import com.fullsail.shoppingmadebetter.feature.stores.domain.Store
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate

/**
 * The History tab: every completed shopping trip, newest first, a page at a time,
 * narrowed by the filter row above the list. Tapping a trip opens its line items
 * via [onTripClick].
 */
@Composable
fun HistoryScreen(
    onTripClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val trips = viewModel.trips.collectAsLazyPagingItems()
    val stores by viewModel.stores.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.selectedDatePreset.collectAsStateWithLifecycle()

    // Re-fetch on every entry so a trip completed on the shopping-list tab shows up
    // without restarting the app. Already-loaded pages stay on screen while the
    // refresh runs, so this doesn't flash a spinner over a populated list.
    LaunchedEffect(Unit) { trips.refresh() }

    HistoryContent(
        trips = trips,
        stores = stores,
        filter = filter,
        selectedPreset = selectedPreset,
        onToggleStore = viewModel::toggleStore,
        onSelectPreset = viewModel::selectDatePreset,
        onCustomRange = viewModel::setCustomRange,
        onClearFilters = viewModel::clearFilters,
        onTripClick = onTripClick,
        modifier = modifier,
    )
}

@Composable
private fun HistoryContent(
    trips: LazyPagingItems<PurchaseTripSummary>,
    stores: List<Store>,
    filter: HistoryFilter,
    selectedPreset: HistoryDatePreset?,
    onToggleStore: (String) -> Unit,
    onSelectPreset: (HistoryDatePreset) -> Unit,
    onCustomRange: (LocalDate, LocalDate) -> Unit,
    onClearFilters: () -> Unit,
    onTripClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        HistoryFilterRow(
            stores = stores,
            filter = filter,
            selectedPreset = selectedPreset,
            onToggleStore = onToggleStore,
            onSelectPreset = onSelectPreset,
            onCustomRange = onCustomRange,
            onClearFilters = onClearFilters,
        )
        // Unconditional: the date row renders even when no store loaded.
        HorizontalDivider()

        HistoryList(
            trips = trips,
            // Asked of the filter itself, so the later search filter picks the same
            // message up without this screen having to learn about it.
            isFiltered = filter.isActive,
            onTripClick = onTripClick,
        )
    }
}

@Composable
private fun HistoryList(
    trips: LazyPagingItems<PurchaseTripSummary>,
    isFiltered: Boolean,
    onTripClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refreshState = trips.loadState.refresh
    val isEmpty = trips.itemCount == 0

    Box(modifier = modifier.fillMaxSize()) {
        when {
            // Loading and Error only take over the screen when there is nothing to
            // show yet. Once trips are loaded they stay put through a background
            // refresh, failed or not — same rule the pre-paging screen followed.
            isEmpty && refreshState is LoadState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            isEmpty && refreshState is LoadState.Error -> HistoryMessage(
                message = stringResource(R.string.history_error),
                actionLabel = stringResource(R.string.history_retry),
                onAction = trips::retry,
            )

            // An empty list means two different things, and the filter is what
            // tells them apart: nothing bought yet, or nothing matching.
            isEmpty && isFiltered -> HistoryMessage(
                message = stringResource(R.string.history_no_matches),
            )

            isEmpty -> HistoryMessage(message = stringResource(R.string.history_empty))

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    count = trips.itemCount,
                    key = trips.itemKey { it.id },
                ) { index ->
                    // Null only with placeholders enabled, which this pager disables.
                    trips[index]?.let { trip ->
                        PurchaseTripCard(
                            trip = trip,
                            onClick = { onTripClick(trip.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                // Footer for the next page: a spinner while it loads, a retry when
                // it failed. Nothing once the oldest trip has been reached.
                when (trips.loadState.append) {
                    is LoadState.Loading -> item(key = APPEND_FOOTER_KEY) { AppendSpinner() }

                    is LoadState.Error -> item(key = APPEND_FOOTER_KEY) {
                        AppendError(onRetry = trips::retry)
                    }

                    is LoadState.NotLoading -> Unit
                }
            }
        }
    }
}

/** The "loading the next page" footer under the last card. */
@Composable
private fun AppendSpinner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

/** The "next page failed" footer: says so, and offers to try again. */
@Composable
private fun AppendError(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick = onRetry) {
            Text(
                text = stringResource(R.string.history_load_more_error),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private const val APPEND_FOOTER_KEY = "history-append-footer"

/**
 * A pager for previews: [trips] already loaded, with [refresh] and [append] load
 * states so the loading / error footers render in the IDE.
 */
private fun previewPager(
    trips: List<PurchaseTripSummary> = emptyList(),
    refresh: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
    append: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
): Flow<PagingData<PurchaseTripSummary>> = flowOf(
    PagingData.from(
        data = trips,
        sourceLoadStates = LoadStates(
            refresh = refresh,
            prepend = LoadState.NotLoading(endOfPaginationReached = true),
            append = append,
        ),
    ),
)

@Composable
private fun HistoryContentPreviewHost(
    pager: Flow<PagingData<PurchaseTripSummary>>,
    stores: List<Store> = previewStores,
    filter: HistoryFilter = HistoryFilter(),
    selectedPreset: HistoryDatePreset? = null,
) {
    ShoppingMadeBetterTheme {
        HistoryContent(
            trips = pager.collectAsLazyPagingItems(),
            stores = stores,
            filter = filter,
            selectedPreset = selectedPreset,
            onToggleStore = {},
            onSelectPreset = {},
            onCustomRange = { _, _ -> },
            onClearFilters = {},
            onTripClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Trips")
@Composable
private fun HistoryContentPreview() {
    HistoryContentPreviewHost(
        previewPager(
            listOf(
                previewTripSummary(),
                previewTripSummary(
                    id = "trip-2",
                    storeName = "Publix",
                    recordedTotal = 26.96,
                    itemCount = 1,
                ),
            ),
        ),
    )
}

@Preview(showBackground = true, name = "Loading the next page")
@Composable
private fun HistoryContentAppendingPreview() {
    HistoryContentPreviewHost(
        previewPager(
            trips = listOf(previewTripSummary()),
            append = LoadState.Loading,
        ),
    )
}

@Preview(showBackground = true, name = "Next page failed")
@Composable
private fun HistoryContentAppendErrorPreview() {
    HistoryContentPreviewHost(
        previewPager(
            trips = listOf(previewTripSummary()),
            append = LoadState.Error(IllegalStateException("no network")),
        ),
    )
}

@Preview(showBackground = true, name = "No purchases yet")
@Composable
private fun HistoryContentEmptyPreview() {
    HistoryContentPreviewHost(previewPager())
}

@Preview(showBackground = true, name = "Filtered, no matches")
@Composable
private fun HistoryContentNoMatchesPreview() {
    HistoryContentPreviewHost(
        previewPager(),
        filter = HistoryFilter(storeIds = setOf("s-2")),
    )
}

@Preview(showBackground = true, name = "Filtered by store")
@Composable
private fun HistoryContentFilteredPreview() {
    HistoryContentPreviewHost(
        previewPager(listOf(previewTripSummary(storeName = "ALDI"))),
        filter = HistoryFilter(storeIds = setOf("s-2")),
    )
}

@Preview(showBackground = true, name = "Filtered by date")
@Composable
private fun HistoryContentDateFilteredPreview() {
    HistoryContentPreviewHost(
        previewPager(listOf(previewTripSummary())),
        filter = HistoryFilter(from = LocalDate(2026, 7, 30), to = LocalDate(2026, 8, 28)),
        selectedPreset = HistoryDatePreset.Last30Days,
    )
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun HistoryContentErrorPreview() {
    HistoryContentPreviewHost(
        previewPager(refresh = LoadState.Error(IllegalStateException("no network"))),
    )
}
