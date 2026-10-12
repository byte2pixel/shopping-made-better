package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListItems
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.navigation.Dest

@Composable
fun ShoppingListCartScreen(
    modifier: Modifier = Modifier,
    viewModel: ShoppingListItemsViewModel = hiltViewModel(),
    listId : String,
    onInfoScreen :(dest : Dest) -> Unit,
) {

    val uiState by viewModel.uiState.collectAsState()
    //list of checked items from the viewmodel
    val checkedItems by viewModel.checkedItems.collectAsState()
    //Controlling if showing the push checked items to pantry dialog.
    var showDialog by remember {mutableStateOf(false)}
    //Toggle if showing filtering by unchecked and checked
    var toggledFilter by remember {mutableStateOf(false)}
    //Adds confirmation for when the list is being deleted
    var toggledDelete by remember {mutableStateOf(false)}
    //When true prompts the user if theyd like to send their checked items ot the pantry and does so.
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
                    checkedItems.forEach { viewModel.deleteItems(it) }
                    viewModel.clearCheckedItems()}) {
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
                        Row(modifier = Modifier.align(Alignment.Start)) {
                            //Toggles the check marked to be filtered into individual lists
                            IconButton(onClick = {
                                toggledFilter = !toggledFilter
                            }, )
                            {
                                Icon(
                                    painterResource(id = R.drawable.ic_filter_list),
                                    contentDescription = "Filter List",
                                    Modifier.size(24.dp)
                                )
                            }
                            IconButton(onClick = {
                               toggledDelete = !toggledDelete
                                //Sets up visible deletion toggling so the user can see what parts of the list they are deleting
                            }, modifier = if (toggledDelete) {
                                Modifier.background(Color.Red ,RoundedCornerShape(12.dp))
                            } else {
                                Modifier
                            }
                            )
                            {
                                Icon(
                                    painterResource(id = R.drawable.ic_delete),
                                    contentDescription = "Edit and Delete List",
                                    Modifier.size(24.dp)
                                )
                            }
                        }

//Splits the list if the toggledfilter state is set
                        if (toggledFilter) {
                        Text("Unchecked")
                        LazyColumn(
                            Modifier.weight(1f).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                                //Iterates through items that are not checked
                            items(state.items.filter { !it.checked }, key = { it.id }) {
            //Sets up cart rows  to display each row
                                CartRow(it, viewModel, onItemCrossed = {
                                    if (toggledDelete)
                                    {
                                        viewModel.deleteItems(it.id)
                                    }else
                                    {
                                        viewModel.toggleItemCheck(it.id)
                                        viewModel.checkItem(it.id, true)
                                    }

                                }, onItemUncrossed = {
                                    if (toggledDelete)
                                    {
                                        viewModel.deleteItems(it.id)
                                    }else
                                    {
                                        viewModel.toggleItemCheck(it.id)
                                        viewModel.checkItem(it.id, false)
                                    }
                                }, listId,onInfoScreen)
                            }
                        }
            //Check column
                            Text("Checked")
                            LazyColumn(Modifier.weight(1f).padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),) {
                                items(state.items.filter { it.checked }, key = { it.id }) {
                                    //iterates through items that are filtered into checked to set up rows for each checked item
                                    CartRow(it, viewModel, onItemCrossed = {
                                        if (toggledDelete)
                                        {
                                            viewModel.deleteItems(it.id )
                                        }else
                                        {
                                            viewModel.toggleItemCheck(it.id)
                                            viewModel.checkItem(it.id, true)
                                        }

                                    }, onItemUncrossed = {
                                        if (toggledDelete)
                                        {
                                            viewModel.deleteItems(it.id )
                                        }else {
                                            viewModel.toggleItemCheck(it.id)
                                            viewModel.checkItem(it.id, false)
                                        }
                                    }, listId,onInfoScreen)
                                }
                            }
                        }
                        else
                        {
                            //Unchecked and checked combined into one unordered list.
                            LazyColumn(
                                Modifier.weight(1f).padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {

                                items(state.items, key = { it.id }) {
                        //iterates through items and sets up cart row for each one
                                    CartRow(it, viewModel, onItemCrossed = {
                                        if (toggledDelete)
                                        {
                                            viewModel.deleteItems(it.id )
                                        }
                                        else {
                                            viewModel.toggleItemCheck(it.id)
                                            viewModel.checkItem(it.id, true)
                                        }
                                    }, onItemUncrossed = {
                                        if (toggledDelete)
                                        {
                                            viewModel.deleteItems(it.id )
                                        }
                                        else{
                                            viewModel.toggleItemCheck(it.id)
                                            viewModel.checkItem(it.id, false)
                                        }
                                    }, listId,onInfoScreen)
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
            if (checkedItems.isNotEmpty()){
                showDialog = true
            }


        }, modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding( 20.dp)
        ) { Text("Complete List")}
    }
}
//Builds out the cart row the user uses to see each cart item.
@Composable
fun CartRow(item : ShoppingListItems, viewModel: ShoppingListItemsViewModel, onItemCrossed: () -> Unit, onItemUncrossed : () -> Unit, listId : String, onInfoScreen :(dest : Dest) -> Unit) {
//Show the options panel state
    var optionsShown by remember{ mutableStateOf(false)}
    Card(Modifier.fillMaxWidth().clickable(onClick = {
//Sets up the item being crossed when it is checked or not
        if (!item.checked)
        onItemCrossed()

        else {
        onItemUncrossed()
        }
//controls colors based on checked state
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

//Controls icon shown while the items are not checked
        if (!item.checked)
        {
            Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            //Icon button to the information menu
            IconButton(onClick = {
                onInfoScreen(Dest.InformationScreen(item.productId))
            })
            {
                Icon(
                    painterResource(id = R.drawable.ic_info),
                    contentDescription = "information",
                    Modifier.size(24.dp)
                )
            }
            //Opens the options menu allowing further control
            if(optionsShown) {
                //button to decrease quantity
                IconButton(onClick = {
                    viewModel.decreaseQuantity(item)
                })
                {
                    Icon(
                        painterResource(id = R.drawable.ic_remove),
                        contentDescription = "minus",
                        Modifier.size(24.dp)
                    )
                }
                //Shows quantity
                Text(item.quantity.toString(), style = MaterialTheme.typography.bodyLarge)
                //Button to increase o quantity
                IconButton(onClick = {
                        viewModel.increaseQuantity(item)
                })
                {
                    Icon(
                        painterResource(id = R.drawable.ic_add),
                        contentDescription = "add",
                        Modifier.size(24.dp)
                    )
                }
                //Clones the item allowing users to have niche control over their list and how it checks
                IconButton(onClick = {
                    val clonedItem = InsertItem(listId, item.productId, 1, "", item.checked, true)
                    viewModel.addItem(clonedItem, listId, item.title)

                })
                {
                    Icon(
                        painterResource(id = R.drawable.ic_clone),
                        contentDescription = "clone",
                        Modifier.size(24.dp)
                    )
                }//Re toggles the options shown to false when the user is done
                IconButton(onClick = {
                    optionsShown = false
                })
                {
                    Icon(
                        painterResource(id = R.drawable.ic_check),
                        contentDescription = "check",
                        Modifier.size(24.dp)
                    )
                }
            }
            //Shows options pertaining to when the items are checked
            else {
                Text(item.quantity.toString(), style = MaterialTheme.typography.bodyLarge)
                IconButton(onClick = {
                    //Opens up the options menu
                    optionsShown = true

                })
                {
                    Icon(
                        painterResource(id = R.drawable.ic_edit),
                        contentDescription = "showEdit",
                        Modifier.size(24.dp)
                    )
                }
            }
        } else {
            //Shows item name and information and draws the information screen access button
            Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, textDecoration = TextDecoration.LineThrough)
            IconButton(onClick = {
                onInfoScreen(Dest.InformationScreen(item.productId))
            })
            {
                Icon(
                    painterResource(id = R.drawable.ic_info),
                    contentDescription = "information",
                    Modifier.size(24.dp)
                )
            }

        }
        }
    }
}


