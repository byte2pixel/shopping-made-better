package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.AddToShoppingListSheet
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/** Persists the set of selected dashboard filters across configuration changes. */
/** Stable key for the digest card, so it is not confused with a product row. */
private const val DIGEST_KEY = "adjustment_digest"

private val filterSetSaver = listSaver<Set<PantryDashboardFilter>, String>(
    save = { selected -> selected.map { it.name } },
    restore = { names -> names.map { PantryDashboardFilter.valueOf(it) }.toSet() },
)

/**
 * @param onProductClick opens the detail screen for the tapped lot's product. Lots of the
 *   same product share one detail screen
 * @param onReviewDigest opens this week's automatic adjustments
 */
@Composable
fun PantryScreen(
    onProductClick: (String) -> Unit,
    onReviewDigest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PantryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState by viewModel.addToListSheet.collectAsState()
    val removeConfirm by viewModel.removeConfirm.collectAsState()
    val zeroStockAlert by viewModel.zeroStockAlert.collectAsState()
    val digestLotCount by viewModel.digestLotCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    // Re-fetch on every entry so items purchased elsewhere — e.g. via
    // "mark all as purchased" on a shopping list show up, when re-visiting pantry.
    LaunchedEffect(Unit) { viewModel.loadInventory() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PantryEvent.ItemAdded -> {
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(
                            R.string.added_to_list, event.itemName, event.listName
                        ),
                        actionLabel = resources.getString(R.string.add_to_list_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoAdd(event.insertedItemId, event.itemName)
                    }
                }

                is PantryEvent.AddFailed -> snackbarHostState.showSnackbar(
                    resources.getString(R.string.add_to_list_failed, event.itemName)
                )

                is PantryEvent.ItemRemoved -> snackbarHostState.showSnackbar(
                    resources.getString(R.string.removed_from_list, event.itemName)
                )

                is PantryEvent.UndoFailed -> snackbarHostState.showSnackbar(
                    resources.getString(R.string.undo_failed, event.itemName)
                )

                is PantryEvent.RemovedFromPantry -> snackbarHostState.showSnackbar(
                    resources.getString(R.string.pantry_removed, event.itemName)
                )

                is PantryEvent.RemoveFailed -> snackbarHostState.showSnackbar(
                    resources.getString(R.string.pantry_remove_failed, event.itemName)
                )

                is PantryEvent.UpdateFailed -> snackbarHostState.showSnackbar(
                    resources.getString(R.string.pantry_update_failed, event.itemName)
                )

                PantryEvent.RefreshFailed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.pantry_refresh_failed),
                        actionLabel = resources.getString(R.string.pantry_retry),
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.loadInventory()
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PantryContent(
            uiState = uiState,
            onRetry = viewModel::loadInventory,
            onProductClick = onProductClick,
            digestLotCount = digestLotCount,
            onReviewDigest = onReviewDigest,
            onAddToListClick = viewModel::onAddToListClicked,
            onRemoveClick = viewModel::onRemoveClicked,
            onQuantityChange = viewModel::onQuantityChanged,
            onLocationChange = viewModel::onLocationChanged,
            onExpiryChange = viewModel::onExpiryChanged,
            onLowStockThresholdChange = viewModel::onLowStockThresholdChanged,
            onConfirmEstimate = viewModel::onConfirmEstimate,
            onCorrectEstimate = viewModel::onCorrectEstimate,
            onUndoEstimate = viewModel::onUndoEstimate,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    (sheetState as? AddToListSheetState.Visible)?.let { visible ->
        AddToShoppingListSheet(
            title = stringResource(R.string.add_to_list_title, visible.item.name),
            lists = visible.lists,
            onDismiss = viewModel::dismissAddToListSheet,
            onListChosen = viewModel::onListChosen,
        )
    }

    removeConfirm?.let { item ->
        RemoveFromPantryDialog(
            itemName = item.name,
            onConfirm = viewModel::confirmRemove,
            onDismiss = viewModel::dismissRemove,
        )
    }

    zeroStockAlert?.let { lot ->
        key(lot.id) {
            ZeroStockAlertDialog(
                lot = lot,
                onOut = { viewModel.onZeroStockOut(lot) },
                onStillHave = { count -> viewModel.onZeroStockStillHave(lot, count) },
                onDismiss = { viewModel.onZeroStockDismissed(lot) },
            )
        }
    }
}

@Composable
private fun PantryContent(
    uiState: PantryUiState,
    onRetry: () -> Unit,
    onProductClick: (String) -> Unit,
    digestLotCount: Int,
    onReviewDigest: () -> Unit,
    onAddToListClick: (InventoryItem) -> Unit,
    onRemoveClick: (InventoryItem) -> Unit,
    onQuantityChange: (InventoryItem, Int) -> Unit,
    onLocationChange: (InventoryItem, PantryLocation) -> Unit,
    onExpiryChange: (InventoryItem, Int) -> Unit,
    onLowStockThresholdChange: (InventoryItem, Int?) -> Unit,
    onConfirmEstimate: (InventoryItem) -> Unit,
    onCorrectEstimate: (InventoryItem, Int) -> Unit,
    onUndoEstimate: (InventoryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            PantryUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            PantryUiState.Error -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.pantry_error),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onRetry) {
                    Text(text = stringResource(R.string.pantry_retry))
                }
            }

            is PantryUiState.Success -> {
                val dashboardCards = remember(uiState.productGroups) {
                    pantryDashboardCards(uiState.productGroups)
                }
                var selectedFilters by rememberSaveable(stateSaver = filterSetSaver) {
                    mutableStateOf(emptySet<PantryDashboardFilter>())
                }

                val visibleGroups = remember(uiState.productGroups, selectedFilters) {
                    applyPantryFilters(uiState.productGroups, selectedFilters)
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    PantryDashboard(
                        cards = dashboardCards,
                        selected = selectedFilters,
                        onToggle = { filter ->
                            selectedFilters = if (filter in selectedFilters) {
                                selectedFilters - filter
                            } else {
                                selectedFilters + filter
                            }
                        },
                    )
                    HorizontalDivider()
                    LazyColumn(
                        // Clips animateItem fade-outs, which foundation draws outside the list's own
                        // scroll clip, so cards removed by a filter cannot paint over the dashboard.
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // The digest spans the whole pantry, so the filters do not apply to it.
                        if (digestLotCount > 0) {
                            item(key = DIGEST_KEY) {
                                AdjustmentDigestCard(
                                    lotCount = digestLotCount,
                                    onReview = onReviewDigest,
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                        items(visibleGroups, key = { it.productId }) { group ->
                            // UI-only state, keyed by the item key, survives scrolling away and config changes.
                            var isExpanded by rememberSaveable { mutableStateOf(false) }
                            ProductCard(
                                group = group,
                                isExpanded = isExpanded,
                                onExpandedChange = { isExpanded = it },
                                onLotClick = { lot -> onProductClick(lot.productId) },
                                onAddToList = { onAddToListClick(group.lots.first()) },
                                onRemoveLot = onRemoveClick,
                                onQuantityChange = onQuantityChange,
                                onLocationChange = onLocationChange,
                                onExpiryChange = onExpiryChange,
                                onLowStockThresholdChange = { newThreshold ->
                                    onLowStockThresholdChange(group.lots.first(), newThreshold)
                                },
                                onConfirmEstimate = onConfirmEstimate,
                                onCorrectEstimate = onCorrectEstimate,
                                onUndoEstimate = onUndoEstimate,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoveFromPantryDialog(
    itemName: String,
    onConfirm: (dontAskAgain: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var dontAskAgain by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.pantry_remove_confirm_title)) },
        text = {
            Column {
                Text(text = stringResource(R.string.pantry_remove_confirm_message, itemName))
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = dontAskAgain,
                            role = Role.Checkbox,
                            onValueChange = { dontAskAgain = it },
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Click handled by the Row's toggleable for one accessible target.
                    Checkbox(checked = dontAskAgain, onCheckedChange = null)
                    Text(text = stringResource(R.string.pantry_remove_dont_ask_again))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(dontAskAgain) }) {
                Text(text = stringResource(R.string.pantry_remove_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.pantry_remove_cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun PantryScreenPreview() {
    ShoppingMadeBetterTheme {
        PantryScreen(onProductClick = {}, onReviewDigest = {})
    }
}
