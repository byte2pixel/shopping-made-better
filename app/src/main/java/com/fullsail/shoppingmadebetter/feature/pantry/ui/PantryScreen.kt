package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

@Composable
fun PantryScreen(
    modifier: Modifier = Modifier,
    viewModel: PantryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    PantryContent(uiState = uiState, onRetry = { viewModel.loadInventory() }, modifier = modifier)
}

@Composable
private fun PantryContent(uiState: PantryUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            PantryUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            PantryUiState.Error ->
                Column(
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
                    OutlinedButton(onClick = { onRetry() }) {
                        Text(text = stringResource(R.string.pantry_retry))
                    }
                }
            is PantryUiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.inventoryItems, key = { it.id }) { inventoryItem ->
                        InventoryItemRow(inventoryItem)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryItemRow(inventoryItem: InventoryItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = inventoryItem.name, style = MaterialTheme.typography.titleMedium)
        Text(text = inventoryItem.brand, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "${inventoryItem.quantity} ${inventoryItem.size}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PantryScreenPreview() {
    ShoppingMadeBetterTheme {
        PantryScreen()
    }
}
