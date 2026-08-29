package com.fullsail.shoppingmadebetter.feature.history.ui

import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.AddToShoppingListSheet
import com.fullsail.shoppingmadebetter.core.ui.LabelChip
import com.fullsail.shoppingmadebetter.core.ui.ProductImage
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseLineItem
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTrip
import com.fullsail.shoppingmadebetter.feature.history.domain.StoreBasketCost
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlin.math.roundToInt

/**
 * One completed trip: its date, store and total, followed by every line item bought
 * on it, each tickable so the whole basket — or part of it — can be put back on a
 * shopping list with "Buy again". Reached by tapping a card on the History tab.
 * @param onProductClick opens the product behind a line item.
 * @param onTitleChange supplies the top-bar title once the trip is known.
 */
@Composable
fun PurchaseTripDetailScreen(
    purchaseId: String,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onTitleChange: (String) -> Unit = {},
    viewModel: PurchaseTripDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedProductIds by viewModel.selectedProductIds.collectAsState()
    val sheetState by viewModel.buyAgainSheet.collectAsState()
    val storeCosts by viewModel.storeCosts.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(purchaseId) { viewModel.load(purchaseId) }

    val state = uiState
    val unknownStore = stringResource(R.string.history_unknown_store)
    if (state is PurchaseTripDetailUiState.Success) {
        LaunchedEffect(state.trip.storeName) {
            onTitleChange(state.trip.storeName ?: unknownStore)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            snackbarHostState.showSnackbar(event.message(resources))
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

            is PurchaseTripDetailUiState.Success -> PurchaseTripDetailContent(
                trip = state.trip,
                selectedProductIds = selectedProductIds,
                storeCosts = storeCosts,
                onItemToggled = viewModel::onItemToggled,
                onItemClick = onProductClick,
                onBuyAgainClick = viewModel::onBuyAgainClicked,
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    (sheetState as? BuyAgainSheetState.Visible)?.let { visible ->
        AddToShoppingListSheet(
            title = pluralStringResource(
                R.plurals.history_buy_again_title,
                visible.selectedCount,
                visible.selectedCount,
            ),
            lists = visible.lists,
            onDismiss = viewModel::dismissBuyAgainSheet,
            onListChosen = viewModel::onListChosen,
        )
    }
}

/** The snackbar text for a finished "buy again". */
private fun TripDetailEvent.message(resources: Resources): String = when (this) {
    is TripDetailEvent.ItemsAdded -> if (skipped > 0) {
        resources.getQuantityString(
            R.plurals.history_buy_again_skipped, added, added, listName, skipped,
        )
    } else {
        resources.getQuantityString(R.plurals.history_buy_again_added, added, added, listName)
    }

    is TripDetailEvent.AddPartiallyFailed ->
        resources.getString(R.string.history_buy_again_partial, added, failed)

    TripDetailEvent.AddFailed -> resources.getString(R.string.history_buy_again_failed)
}

@Composable
private fun PurchaseTripDetailContent(
    trip: PurchaseTrip,
    selectedProductIds: Set<String>,
    storeCosts: List<StoreBasketCost>,
    onItemToggled: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onBuyAgainClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            TripSummary(
                trip = trip,
                buyAgainEnabled = selectedProductIds.isNotEmpty(),
                onBuyAgainClick = onBuyAgainClick,
            )
            // Absent when only this trip's own store prices the whole basket.
            if (storeCosts.isNotEmpty()) {
                HorizontalDivider()
                StoreCostComparison(storeCosts)
            }
            HorizontalDivider()
        }
        items(trip.items, key = { it.id }) { item ->
            PurchaseLineItemRow(
                item = item,
                isSelected = item.productId in selectedProductIds,
                onToggle = { onItemToggled(item.productId) },
                onClick = { onItemClick(item.productId) },
            )
        }
    }
}

/** The trip header: what was bought where, when, for how much, and "Buy again". */
@Composable
private fun TripSummary(
    trip: PurchaseTrip,
    buyAgainEnabled: Boolean,
    onBuyAgainClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        FilledTonalButton(onClick = onBuyAgainClick, enabled = buyAgainEnabled) {
            Icon(
                painter = painterResource(R.drawable.ic_shopping_cart),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.history_buy_again))
        }
    }
}

/** The same basket at every store that stocks all of it, cheapest first. */
@Composable
private fun StoreCostComparison(costs: List<StoreBasketCost>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.history_detail_cost_elsewhere),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        costs.forEachIndexed { index, cost ->
            StoreCostRow(cost = cost, isCheapest = index == 0)
        }
    }
}

@Composable
private fun StoreCostRow(cost: StoreBasketCost, isCheapest: Boolean) {
    // Rounded to cents first: a sub-cent difference reads as "same", not "$0.00 less".
    val rounded = (cost.difference * 100).roundToInt()
    val difference = when {
        rounded > 0 -> stringResource(
            R.string.history_detail_cost_cheaper,
            formatPrice(cost.difference),
        )

        rounded < 0 -> stringResource(
            R.string.history_detail_cost_dearer,
            formatPrice(-cost.difference),
        )

        else -> stringResource(R.string.history_detail_cost_same)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cost.storeName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCheapest) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = difference,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = formatPrice(cost.cost), style = MaterialTheme.typography.bodyLarge)
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

/**
 * One purchased product: what it was, how many, and what the line cost.
 *
 * The row carries two actions. Tapping it opens the product; the leading checkbox
 * ticks the item into the next "buy again".
 */
@Composable
private fun PurchaseLineItemRow(
    item: PurchaseLineItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectLabel = stringResource(R.string.history_buy_again_select, item.productName)
    val openLabel = stringResource(R.string.history_line_item_open)
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Gives the toggle strip a bounded height to fill; without it fillMaxHeight
            // is a no-op in a wrap-content Row.
            .height(IntrinsicSize.Min)
            .clickable(onClickLabel = openLabel, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .toggleable(
                    value = isSelected,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                )
                .semantics { contentDescription = selectLabel }
                // Applied after toggleable, so the padding is part of the touch target.
                .padding(start = 16.dp, end = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Checkbox(checked = isSelected, onCheckedChange = null)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductImage(imageUrl = item.imageUrl, contentDescription = null, size = 40.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = item.productName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = listOf(item.brand, item.size)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Only the marked items carry a chip; the rest are left unlabelled.
                if (item.addedToInventory) {
                    LabelChip(
                        label = stringResource(R.string.history_line_item_pantry),
                        accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconRes = R.drawable.ic_pantry,
                        contentDescription = stringResource(R.string.history_line_item_pantry_desc),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = formatPrice(item.lineTotal),
                    style = MaterialTheme.typography.bodyMedium,
                )
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
}

@Preview(showBackground = true, name = "Trip detail")
@Composable
private fun PurchaseTripDetailContentPreview() {
    val trip = previewTrip()
    ShoppingMadeBetterTheme {
        PurchaseTripDetailContent(
            trip = trip,
            selectedProductIds = trip.items.map { it.productId }.toSet(),
            storeCosts = previewStoreCosts,
            onItemToggled = {},
            onItemClick = {},
            onBuyAgainClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Deleted store, fractional quantity, partial selection")
@Composable
private fun PurchaseTripDetailContentUnknownStorePreview() {
    val trip = previewTrip(
        storeName = null,
        recordedTotal = null,
        items = listOf(
            previewLineItem("1", quantity = 1.5),
            previewLineItem("2", productName = "Roma Tomatoes", quantity = 0.75, pricePaid = 4.40),
        ),
    )
    ShoppingMadeBetterTheme {
        PurchaseTripDetailContent(
            trip = trip,
            // Only the first item ticked, to show both checkbox states.
            selectedProductIds = setOf(trip.items.first().productId),
            // No store prices the whole basket, so the comparison is absent.
            storeCosts = emptyList(),
            onItemToggled = {},
            onItemClick = {},
            onBuyAgainClick = {},
        )
    }
}

/** Cheapest first, spanning cheaper / same / dearer than the trip cost. */
private val previewStoreCosts = listOf(
    StoreBasketCost("s-2", "ALDI", 40.20, 2.12),
    StoreBasketCost("s-3", "Publix", 42.32, 0.0),
    StoreBasketCost("s-1", "Whole Foods", 44.44, -2.12),
)

@Preview(showBackground = true, name = "Trip not found")
@Composable
private fun PurchaseTripDetailNotFoundPreview() {
    ShoppingMadeBetterTheme {
        HistoryMessage(message = stringResource(R.string.history_detail_not_found))
    }
}
