package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListItems

@Composable
fun ShoppingListItemsScreen(
    modifier: Modifier = Modifier,
    viewModel: ShoppingListItemsViewModel = hiltViewModel(),
    listId : String

    ) {

    val uiState by viewModel.uiState.collectAsState()



    Box() {
        viewModel.getItems(listId)
        when (val state = uiState) {
            ShoppingTripsUiState.Loading ->
                CircularProgressIndicator(Modifier.align(Alignment.Center))

            ShoppingTripsUiState.Error ->
                Text("Couldn't load your lists", Modifier.align(Alignment.Center))

            is ShoppingListItemsState.Success ->
                if (state.items.isEmpty()) {
                    Text("No items added to list", Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {

                        items(state.items, key = { it.id }) { ListRow(it,viewModel) }
                    }
                }


            else -> {}
        }
    }
}
@Composable
fun ListRow(item : ShoppingListItems, viewModel: ShoppingListItemsViewModel) {

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant))
    {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically)
        {
            Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = {
                viewModel.deleteItems(item.id)
                viewModel.getItems(item.shoppingListId)
            })
            {
                Icon(painterResource(id = R.drawable.ic_delete), contentDescription = "delete")
            }
    }
    }
}

