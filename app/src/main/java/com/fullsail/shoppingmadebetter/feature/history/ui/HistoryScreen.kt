package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * The History tab: every completed shopping trip, newest first. Tapping a trip
 * opens its line items via [onTripClick].
 */
@Composable
fun HistoryScreen(
    onTripClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Re-fetch on every entry so a trip completed on the shopping-list tab shows up
    // without restarting the app.
    LaunchedEffect(Unit) { viewModel.load() }

    HistoryContent(
        uiState = uiState,
        onRetry = viewModel::load,
        onTripClick = onTripClick,
        modifier = modifier,
    )
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onRetry: () -> Unit,
    onTripClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            HistoryUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            HistoryUiState.Error -> HistoryMessage(
                message = stringResource(R.string.history_error),
                actionLabel = stringResource(R.string.history_retry),
                onAction = onRetry,
            )

            is HistoryUiState.Success ->
                if (uiState.trips.isEmpty()) {
                    HistoryMessage(message = stringResource(R.string.history_empty))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.trips, key = { it.id }) { trip ->
                            PurchaseTripCard(
                                trip = trip,
                                onClick = { onTripClick(trip.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
        }
    }
}

@Preview(showBackground = true, name = "Trips")
@Composable
private fun HistoryContentPreview() {
    ShoppingMadeBetterTheme {
        HistoryContent(
            uiState = HistoryUiState.Success(
                listOf(
                    previewTrip(),
                    previewTrip(
                        id = "trip-2",
                        storeName = "Publix",
                        recordedTotal = 26.96,
                        items = listOf(previewLineItem("5", productName = "Spiced Apple Herbal Tea")),
                    ),
                ),
            ),
            onRetry = {},
            onTripClick = {},
        )
    }
}

@Preview(showBackground = true, name = "No purchases yet")
@Composable
private fun HistoryContentEmptyPreview() {
    ShoppingMadeBetterTheme {
        HistoryContent(
            uiState = HistoryUiState.Success(emptyList()),
            onRetry = {},
            onTripClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun HistoryContentErrorPreview() {
    ShoppingMadeBetterTheme {
        HistoryContent(uiState = HistoryUiState.Error, onRetry = {}, onTripClick = {})
    }
}
