package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListItems
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem

@Composable
fun ShoppingListCartScreen(
    modifier: Modifier = Modifier,
    viewModel: ShoppingListItemsViewModel = hiltViewModel(),
    listId : String

) {

    val uiState by viewModel.uiState.collectAsState()
    val checkedItems by viewModel.checkedItems.collectAsState()
    var showDialog by remember {mutableStateOf(false)}
    var toggled by remember {mutableStateOf(false)}
    if (showDialog)
    {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "Would you like to push checked items to pantry?:  ") },
            text = {
            },
            confirmButton = {

                TextButton(modifier = Modifier.fillMaxWidth(), onClick = {  showDialog = false
                    viewModel.markAllPurchased(listId)
                    checkedItems.forEach { viewModel.deleteItems(it, listId) }
                    viewModel.clearCheckedItems(listId)}) {
                    Text(text = "Yes", textAlign = TextAlign.Right)
                }
            },
            dismissButton = {
                TextButton(modifier = Modifier.fillMaxWidth(),onClick = {  showDialog = false
                viewModel.getItems(listId)}) {
                    Text(text = "No", textAlign = TextAlign.Left  )
                }
            },

            )
    }

    Box(Modifier.fillMaxSize()) {
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
                    Column(Modifier.fillMaxWidth()) {
                        IconButton(onClick = {
                            toggled = !toggled
                        })
                        {
                            Icon(
                                painterResource(id = R.drawable.ic_filter_list),
                                contentDescription = "Filter List",
                                Modifier.size(24.dp)
                            )
                        }

                        if (toggled) {
                        Text("Unchecked")
                        LazyColumn(
                            Modifier.weight(1f).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {

                            items(state.items.filter { !it.checked }, key = { it.id }) {

                                CartRow(it, viewModel, onItemCrossed = {
                                    viewModel.toggleItemCheck(it.id)
                                    viewModel.checkItem(it.id, true, listId)
                                }, onItemUncrossed = {
                                    viewModel.toggleItemCheck(it.id)
                                    viewModel.checkItem(it.id, false, listId)
                                }, listId)
                            }
                        }

                            Text("Checked")
                            LazyColumn(Modifier.weight(1f).padding(16.dp)) {
                                items(state.items.filter { it.checked }, key = { it.id }) {
                                    CartRow(it, viewModel, onItemCrossed = {
                                        viewModel.toggleItemCheck(it.id)
                                        viewModel.checkItem(it.id, true, listId)
                                    }, onItemUncrossed = {
                                        viewModel.toggleItemCheck(it.id)
                                        viewModel.checkItem(it.id, false, listId)
                                    }, listId)
                                }
                            }
                        }
                        else
                        {
                            LazyColumn(
                                Modifier.weight(1f).padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {

                                items(state.items, key = { it.id }) {

                                    CartRow(it, viewModel, onItemCrossed = {
                                        viewModel.toggleItemCheck(it.id)
                                        viewModel.checkItem(it.id, true, listId)
                                    }, onItemUncrossed = {
                                        viewModel.toggleItemCheck(it.id)
                                        viewModel.checkItem(it.id, false, listId)
                                    }, listId)
                                }
                            }
                        }
                    }
                }



            else -> {}
        }
        FloatingActionButton(containerColor = if (viewModel.checkedItems.collectAsState().value.isEmpty())
        {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        },onClick = {
            showDialog = true

        }, modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding( 20.dp)
        ) { Text("Complete List")}
    }
}
@Composable
fun CartRow(item : ShoppingListItems, viewModel: ShoppingListItemsViewModel, onItemCrossed: () -> Unit, onItemUncrossed : () -> Unit, listId : String) {


    Card(Modifier.fillMaxWidth().clickable(onClick = {

        if (!item.checked)
        onItemCrossed()

        else {
        onItemUncrossed()
        }

    }), colors = CardDefaults.cardColors(

        if (!item.checked)
        {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        }
    ))
    {

        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically)
        {
        if (!item.checked)
        {
            Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = {
                val clonedItem : InsertItem = InsertItem(listId, item.productId, 1, "", item.checked, true)
                viewModel.addItem(clonedItem, listId)
            })
            {
                Icon(
                    painterResource(id = R.drawable.ic_add),
                    contentDescription = "add",
                    Modifier.size(24.dp)
                )
            }
        } else {
            Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, textDecoration = TextDecoration.LineThrough)
            IconButton(onClick = {
            })
            {
                Icon(
                    painterResource(id = R.drawable.ic_add),
                    contentDescription = "add",
                    Modifier.size(24.dp)
                )
            }
        }
        }
    }
}


