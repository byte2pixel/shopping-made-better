package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/** Persists the set of selected dashboard filters across configuration changes. */
private val filterSetSaver = listSaver<Set<PantryDashboardFilter>, String>(
    save = { selected -> selected.map { it.name } },
    restore = { names -> names.map { PantryDashboardFilter.valueOf(it) }.toSet() },
)

/**
 * Items with a known expiration date within this many days — including
 * already-expired ones — count as "expiring soon". Hard-coded for now; a future
 * task may let the user configure it (e.g. by long-pressing the dashboard card).
 */
internal const val EXPIRING_SOON_DAYS = 5

/**
 * Narrows [items] to those matching every active, wired-up dashboard filter in
 * [filters]. Filters without a [PantryDashboardFilter.predicate] yet are ignored;
 * when no active filter has a predicate, [items] is returned unchanged. Multiple
 * wired filters compose as an intersection — an item must match them all.
 */
internal fun applyPantryFilters(
    items: List<InventoryItem>,
    filters: Set<PantryDashboardFilter>,
): List<InventoryItem> {
    val predicates = filters.mapNotNull { it.predicate }
    return if (predicates.isEmpty()) {
        items
    } else {
        items.filter { item -> predicates.all { predicate -> predicate(item) } }
    }
}

/**
 * Whether this item's expiry chip reads as a warning — red, orange, or yellow
 * rather than gray.
 */
internal fun InventoryItem.isExpiringSoon(): Boolean =
    expiresInDays != null && expiryBucket(expiresInDays) != ExpiryBucket.Later

/**
 * Whether this item is running low: it has a per-item low threshold set and its
 * quantity sits between 1 and that threshold. Quantity 0 is "out", not "low".
 * Items without a threshold ([lowStockThreshold] null) are
 * never low.
 */
internal fun InventoryItem.isLowStock(): Boolean =
    stockLevel(quantity, lowStockThreshold) == StockLevel.Low

/**
 * Whether this item is out of stock: nothing on hand ([quantity] 0).
 */
internal fun InventoryItem.isOutOfStock(): Boolean =
    stockLevel(quantity, lowStockThreshold) == StockLevel.Out

@Composable
fun PantryScreen(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PantryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState by viewModel.addToListSheet.collectAsState()
    val removeConfirm by viewModel.removeConfirm.collectAsState()
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
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PantryContent(
            uiState = uiState,
            onRetry = viewModel::loadInventory,
            onItemClick = onItemClick,
            onAddToListClick = viewModel::onAddToListClicked,
            onRemoveClick = viewModel::onRemoveClicked,
            onQuantityChange = viewModel::onQuantityChanged,
            onLocationChange = viewModel::onLocationChanged,
            onExpiryChange = viewModel::onExpiryChanged,
            onLowStockThresholdChange = viewModel::onLowStockThresholdChanged,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    (sheetState as? AddToListSheetState.Visible)?.let { visible ->
        AddToShoppingListSheet(
            state = visible,
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
}

@Composable
private fun PantryContent(
    uiState: PantryUiState,
    onRetry: () -> Unit,
    onItemClick: (String) -> Unit,
    onAddToListClick: (InventoryItem) -> Unit,
    onRemoveClick: (InventoryItem) -> Unit,
    onQuantityChange: (InventoryItem, Int) -> Unit,
    onLocationChange: (InventoryItem, PantryLocation) -> Unit,
    onExpiryChange: (InventoryItem, Int) -> Unit,
    onLowStockThresholdChange: (InventoryItem, Int?) -> Unit,
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
                val dashboardCards = remember(uiState.inventoryItems) {
                    pantryDashboardCards(uiState.inventoryItems)
                }
                var selectedFilters by rememberSaveable(stateSaver = filterSetSaver) {
                    mutableStateOf(emptySet<PantryDashboardFilter>())
                }

                val visibleItems = remember(uiState.inventoryItems, selectedFilters) {
                    applyPantryFilters(uiState.inventoryItems, selectedFilters)
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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(visibleItems, key = { it.id }) { inventoryItem ->
                            InventoryItemCard(
                                item = inventoryItem,
                                onClick = { onItemClick(inventoryItem.id) },
                                onAddToList = { onAddToListClick(inventoryItem) },
                                onRemove = { onRemoveClick(inventoryItem) },
                                onQuantityChange = { newQty ->
                                    onQuantityChange(inventoryItem, newQty)
                                },
                                onLocationChange = { newLocation ->
                                    onLocationChange(inventoryItem, newLocation)
                                },
                                onExpiryChange = { newDays ->
                                    onExpiryChange(inventoryItem, newDays)
                                },
                                onLowStockThresholdChange = { newThreshold ->
                                    onLowStockThresholdChange(inventoryItem, newThreshold)
                                },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToShoppingListSheet(
    state: AddToListSheetState.Visible,
    onDismiss: () -> Unit,
    onListChosen: (ShoppingTrip) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.add_to_list_title, state.item.name),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            when (val lists = state.lists) {
                AddToListSheetState.Lists.Loading -> CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                )

                AddToListSheetState.Lists.Empty -> Text(
                    text = stringResource(R.string.add_to_list_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                AddToListSheetState.Lists.Error -> Text(
                    text = stringResource(R.string.add_to_list_error),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                is AddToListSheetState.Lists.Loaded -> lists.trips.forEach { trip ->
                    ShoppingListRow(trip = trip, onClick = { onListChosen(trip) })
                }
            }
        }
    }
}

@Composable
private fun ShoppingListRow(trip: ShoppingTrip, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = trip.listName, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = pluralStringResource(
                R.plurals.add_to_list_item_count,
                trip.itemCount,
                trip.itemCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantryScreenPreview() {
    ShoppingMadeBetterTheme {
        PantryScreen(onItemClick = {})
    }
}
