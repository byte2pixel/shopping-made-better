package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    var showDialog by remember {mutableStateOf(false)}
    var listForDeletion by remember {mutableStateOf<String?>(null)}

    if (showDialog)
    {
        BasicAlertDialog(onDismissRequest = {}, Modifier.fillMaxWidth(), DialogProperties(),
            {
                Row(modifier = Modifier.fillMaxWidth().background( color = MaterialTheme.colorScheme.surface).padding(24.dp)) {

                    Button(
                        onClick = {
                            showDialog = false

                        }
                    )
                    {
                        Text("Keep List")
                    }
                    OutlinedButton (
                        onClick = {
                            showDialog = false
                            listForDeletion?.let {
                                viewModel.removeList(
                                    it
                                )
                            }


                        }
                    )
                    {
                        Text("Delete List")
                    }

                }
            })
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

                                    showDialog = true
                                    listForDeletion = it.shoppingListId

                                }, onItemComparison = onItemComparison)
                                }
                            }
                        }


                    else -> {}
                }
            }
            Box(modifier = Modifier.fillMaxSize())
            {
                OutlinedButton(
                    onClick = { onItemComparison(Dest.ShoppingListItemComparison) },
                    modifier = Modifier.align(Alignment.BottomEnd),
                    shape = CircleShape
                ) {
                    Text("+")
                }
            }

        }



@Composable
private fun TripCard(trip: ShoppingTrip, onDelete: () -> Unit, onItemComparison :(dest : Dest) -> Unit)
{
    OutlinedCard(
        onClick = {

            //Add viewing shopping list logic
        },
        Modifier.fillMaxWidth())
    {
        Column(Modifier.padding(16.dp).fillMaxWidth())
        {
            Text(trip.listName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(trip.storeName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${trip.itemCount} items · \$${"%.2f".format(trip.totalCost)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier= Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End)
            {
                OutlinedButton(onClick = {onDelete()})
                {
                    Text("Delete")
                }
                Button(onClick = {onItemComparison(Dest.ShoppingListItemsScreen(trip.shoppingListId))})
                {
                    Text("Details")
                }
            }
        }
    }
}


