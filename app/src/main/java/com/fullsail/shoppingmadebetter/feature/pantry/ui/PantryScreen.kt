package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

@Composable
fun PantryScreen(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PantryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState by viewModel.addToListSheet.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is PantryEvent.ItemAdded -> resources.getString(
                    R.string.added_to_list, event.itemName, event.listName
                )

                is PantryEvent.AddFailed -> resources.getString(
                    R.string.add_to_list_failed, event.itemName
                )
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PantryContent(
            uiState = uiState,
            onRetry = viewModel::loadInventory,
            onItemClick = onItemClick,
            onAddToListClick = viewModel::onAddToListClicked,
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
}

@Composable
private fun PantryContent(
    uiState: PantryUiState,
    onRetry: () -> Unit,
    onItemClick: (String) -> Unit,
    onAddToListClick: (InventoryItem) -> Unit,
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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.inventoryItems, key = { it.id }) { inventoryItem ->
                        InventoryItemRow(
                            inventoryItem = inventoryItem,
                            onClick = { onItemClick(inventoryItem.id) },
                            onAddToList = { onAddToListClick(inventoryItem) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryItemRow(
    inventoryItem: InventoryItem,
    onClick: () -> Unit,
    onAddToList: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = inventoryItem.name, style = MaterialTheme.typography.titleMedium)
            Text(text = inventoryItem.brand, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${inventoryItem.quantity} ${inventoryItem.size}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        IconButton(onClick = onAddToList) {
            Icon(
                painter = painterResource(R.drawable.ic_shopping_cart),
                contentDescription = stringResource(R.string.pantry_add_to_list),
            )
        }
    }
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
