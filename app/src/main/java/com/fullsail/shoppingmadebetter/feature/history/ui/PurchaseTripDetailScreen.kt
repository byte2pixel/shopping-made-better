package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.ProductImage
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseLineItem
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTrip
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * One completed trip: its date, store and total, followed by every line item bought
 * on it. Reached by tapping a card on the History tab.
 * @param onTitleChange supplies the top-bar title once the trip is known.
 */
@Composable
fun PurchaseTripDetailScreen(
    purchaseId: String,
    modifier: Modifier = Modifier,
    onTitleChange: (String) -> Unit = {},
    viewModel: PurchaseTripDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(purchaseId) { viewModel.load(purchaseId) }

    val state = uiState
    val unknownStore = stringResource(R.string.history_unknown_store)
    if (state is PurchaseTripDetailUiState.Success) {
        LaunchedEffect(state.trip.storeName) {
            onTitleChange(state.trip.storeName ?: unknownStore)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            PurchaseTripDetailUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            PurchaseTripDetailUiState.Error -> HistoryMessage(
                message = stringResource(R.string.history_error),
                actionLabel = stringResource(R.string.history_retry),
                onAction = { viewModel.load(purchaseId) },
            )

            PurchaseTripDetailUiState.NotFound -> HistoryMessage(
                message = stringResource(R.string.history_detail_not_found),
            )

            is PurchaseTripDetailUiState.Success -> PurchaseTripDetailContent(trip = state.trip)
        }
    }
}

@Composable
private fun PurchaseTripDetailContent(trip: PurchaseTrip, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            TripSummary(trip = trip)
            HorizontalDivider()
        }
        items(trip.items, key = { it.id }) { item ->
            PurchaseLineItemRow(item = item)
        }
    }
}

/** The trip header: what was bought where, when, and for how much. */
@Composable
private fun TripSummary(trip: PurchaseTrip, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryField(
            label = stringResource(R.string.history_detail_store),
            value = trip.storeName ?: stringResource(R.string.history_unknown_store),
        )
        SummaryField(
            label = stringResource(R.string.history_detail_date),
            value = formatTripDate(trip.purchasedOn),
        )
        SummaryField(
            label = stringResource(R.string.history_detail_total),
            value = formatPrice(trip.total),
        )
        SummaryField(
            label = stringResource(R.string.history_detail_items),
            value = pluralStringResource(
                R.plurals.history_trip_item_count,
                trip.itemCount,
                trip.itemCount,
            ),
        )
    }
}

@Composable
private fun SummaryField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** One purchased product: what it was, how many, and what the line cost. */
@Composable
private fun PurchaseLineItemRow(item: PurchaseLineItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(imageUrl = item.imageUrl, contentDescription = null, size = 40.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = item.productName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = listOf(item.brand, item.size).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = formatPrice(item.lineTotal), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(
                    R.string.history_line_quantity_price,
                    formatQuantity(item.quantity),
                    formatPrice(item.pricePaid),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, name = "Trip detail")
@Composable
private fun PurchaseTripDetailContentPreview() {
    ShoppingMadeBetterTheme {
        PurchaseTripDetailContent(trip = previewTrip())
    }
}

@Preview(showBackground = true, name = "Deleted store, fractional quantity")
@Composable
private fun PurchaseTripDetailContentUnknownStorePreview() {
    ShoppingMadeBetterTheme {
        PurchaseTripDetailContent(
            trip = previewTrip(
                storeName = null,
                recordedTotal = null,
                items = listOf(
                    previewLineItem("1", quantity = 1.5),
                    previewLineItem("2", productName = "Roma Tomatoes", quantity = 0.75, pricePaid = 4.40),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, name = "Trip not found")
@Composable
private fun PurchaseTripDetailNotFoundPreview() {
    ShoppingMadeBetterTheme {
        HistoryMessage(message = stringResource(R.string.history_detail_not_found))
    }
}
