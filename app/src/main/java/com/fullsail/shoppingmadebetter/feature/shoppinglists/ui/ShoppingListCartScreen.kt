package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListItems

@Composable
fun ShoppingListCartScreen(
    modifier: Modifier = Modifier,
    viewModel: ShoppingListItemsViewModel = hiltViewModel(),
    listId : String

) {

    val uiState by viewModel.uiState.collectAsState()
    val checkedItems by viewModel.checkedItems.collectAsState()


    Box() {
       LaunchedEffect(listId) { viewModel.getItems(listId)}
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

                        items(state.items, key = { it.id }) { CartRow(it,viewModel, onItemCrossed = {
                            viewModel.toggleItemCheck(it.id)
                        }, onItemUncrossed = {
                            viewModel.toggleItemCheck(it.id)
                        }) }
                    }
                }


            else -> {}
        }
        FloatingActionButton(onClick = {
            viewModel.markAllPurchased(listId)
            checkedItems.forEach { viewModel.deleteItems(it, listId) }
            viewModel.clearCheckedItems(listId)

        }, Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding( 20.dp)
        ) { Text("Complete List")}
    }
}
@Composable
fun CartRow(item : ShoppingListItems, viewModel: ShoppingListItemsViewModel, onItemCrossed: () -> Unit, onItemUncrossed : () -> Unit) {

    var clicked by remember { mutableStateOf(false)}
    Card(Modifier.fillMaxWidth().clickable(onClick = {
        clicked = !clicked
        if (clicked)
        onItemCrossed()
        else {
        onItemUncrossed()
        }

    }), colors = CardDefaults.cardColors(

        if (!clicked)
        {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        }
    ))
    {

        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically)
        {
        if (!clicked)
        {
            Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)

        } else {
            Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, textDecoration = TextDecoration.LineThrough)
        }


        }
    }
}


