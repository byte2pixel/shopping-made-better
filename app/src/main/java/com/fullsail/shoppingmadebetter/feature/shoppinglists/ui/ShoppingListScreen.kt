package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.RenameList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.navigation.Dest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListsScreen(
    onItemComparison :(dest : Dest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShoppingTripsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    var showDeleteDialog by remember {mutableStateOf(false)}
    var showRenameDialog by remember {mutableStateOf(false)}
    var selectedList by remember {mutableStateOf<String?>(null)}

    if (showRenameDialog)
    {
        var listName by remember {mutableStateOf("")}

        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "Enter new list name:") },
            text = {
                OutlinedTextField(modifier = Modifier.fillMaxWidth(),value = listName, onValueChange = {listName = it},label= { Text("Enter list name") })
                Spacer(modifier = Modifier.height(50.dp))

            },
            confirmButton = {
                TextButton(onClick = {  showRenameDialog = false
                    selectedList?.let {
                        viewModel.renameList(
                            RenameList(it, listName)
                        )
                    }
                    }) {
                    Text(text = "Ok")
                }
            },   dismissButton = {
                TextButton(onClick = {  showRenameDialog = false }) {
                    Text(text = "Back")
                }
            },

            )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "DELETE selected list?") },
            text = {

            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    selectedList?.let {
                        viewModel.removeList(
                            it
                        )
                    }
                }) {
                    Text(text = "Delete list")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                }) { Text("Keep list") }
            }

        )
    }


    DisposableEffect(lifecycleOwner)
    {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME)
            {
                viewModel.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

            Box(modifier.fillMaxSize())
            {
                when (val state = uiState)
                {
                    ShoppingTripsUiState.Loading ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))

                    ShoppingTripsUiState.Error ->
                        Text("Couldn't load your lists", Modifier.align(Alignment.Center))

                    is ShoppingTripsUiState.Success ->
                        if (state.trips.isEmpty())
                        {
                            Text("No shopping trips yet", Modifier.align(Alignment.Center))
                        } else
                        {
                            LazyColumn(
                                Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {

                                items(state.trips, key = { it.shoppingListId }) { TripCard(it, onDelete =  {

                                    showDeleteDialog = true
                                    selectedList = it.shoppingListId

                                }, onRename = {
                                    showRenameDialog = true
                                    selectedList = it.shoppingListId
                                } ,onItemComparison = onItemComparison)
                                }
                            }
                        }


                    else -> {}
                }
                FloatingActionButton(onClick = {onItemComparison(Dest.ShoppingListItemComparison)}, modifier = Modifier.align(
                    Alignment.BottomEnd).padding(bottom = 70.dp, end = 16.dp).size(60.dp), shape = CircleShape ) {
                    Icon(painterResource(id = R.drawable.ic_add), contentDescription = "add", Modifier.size(24.dp))}
            }
        }

@Composable
private fun TripCard(trip: ShoppingTrip, onDelete: () -> Unit, onItemComparison :(dest : Dest) -> Unit, onRename: () -> Unit )
{
    OutlinedCard(
        onClick = {

            //Add viewing shopping list logic
        },
        Modifier.fillMaxWidth())
    {
        Column(Modifier.padding(16.dp).fillMaxWidth())
        {
            Row(verticalAlignment = Alignment.CenterVertically){
                Text(trip.listName, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = {
                    onRename()
                }) { Icon(painterResource(id = R.drawable.ic_edit), contentDescription = "rename", Modifier.size(24.dp))}
            }
            Spacer(Modifier.height(4.dp))
            Text(trip.storeName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                $$"$${trip.itemCount} items · $$${"%.2f".format(trip.totalCost)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier= Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End)
            {
                IconButton(onClick = {onDelete()})
                {
                    Icon(painterResource(id = R.drawable.ic_delete), contentDescription = "delete", Modifier.size(24.dp))
                }
                IconButton(onClick = {onItemComparison(Dest.ShoppingListItemsScreen(trip.shoppingListId))})
                {
                    Icon(painterResource(id = R.drawable.ic_shopping_lists), contentDescription = "details", Modifier.size(24.dp))

                }
                IconButton(onClick = {onItemComparison(Dest.ShoppingListCartScreen(trip.shoppingListId))})
                {
                    Icon(painterResource(id = R.drawable.ic_cart), contentDescription = "Cart", Modifier.size(24.dp))

                }
            }
        }
    }
}


