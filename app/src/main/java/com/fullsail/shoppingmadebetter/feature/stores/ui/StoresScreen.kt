package com.fullsail.shoppingmadebetter.feature.stores.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.stores.domain.Store
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * Dev example screen (SCRUM-79): lists the stores in the local Supabase database
 * with their names and addresses, demonstrating the
 * Screen -> ViewModel -> UseCase -> Repository -> Supabase flow end to end.
 */
@Composable
fun StoresScreen(
    modifier: Modifier = Modifier,
    viewModel: StoresViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    StoresContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun StoresContent(uiState: StoresUiState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            StoresUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            StoresUiState.Error ->
                Text(
                    text = stringResource(R.string.stores_error),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )

            is StoresUiState.Success ->
                if (uiState.stores.isEmpty()) {
                    Text(
                        text = stringResource(R.string.stores_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.stores, key = { it.id }) { store ->
                            StoreRow(store)
                            HorizontalDivider()
                        }
                    }
                }
        }
    }
}

@Composable
private fun StoreRow(store: Store) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = store.name, style = MaterialTheme.typography.titleMedium)
        Text(text = store.address, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "${store.city}, ${store.state} ${store.postalCode}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StoresContentPreview() {
    ShoppingMadeBetterTheme {
        StoresContent(
            uiState = StoresUiState.Success(
                listOf(
                    Store("1", "Corner Market", "100 Main St", "Austin", "TX", "78701", null),
                    Store("2", "Green Grocer", "200 Oak Ave", "Austin", "TX", "78702", null),
                ),
            ),
        )
    }
}
