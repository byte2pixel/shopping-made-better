package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui


import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.ContentSizedBottomSheet
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearch
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing.StoreProductPricing
import com.fullsail.shoppingmadebetter.navigation.Dest
import kotlin.collections.forEach


@Composable
fun ShoppingListItemComparisonScreen(
    onItemComparison :() -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemComparisonViewmodel = hiltViewModel(),
    onInfoScreen :(dest : Dest) -> Unit,

) {
//holds the selected product the user finds using the search bar for use in item comparison
    var selectedProduct by rememberSaveable {mutableStateOf<String?>(null)}
    //Controls which was the items are sorted
    var bAscending by remember { mutableStateOf(true)}
    val uiState by viewModel.uiState.collectAsState()
    //Ensures the user gets permissions required to use the location functionality
    val getPermissions = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions())
    {}
    if ((ActivityCompat.checkSelfPermission(
            LocalContext.current,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(
            LocalContext.current,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED)
    ) {
        LaunchedEffect(Unit) {
            getPermissions.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }

    }
    //Ensures that selected product == null and displays the search bar
    if (selectedProduct == null)
    {
        Column{
            SimpleSearchBar(  onSearch = {product-> selectedProduct = product
                                         viewModel.showProducts(product)}, viewModel, onInfoScreen)
        }
    }
    //Otherwise it shows informaiton for the selected item from  different stores
    else
    {

        Box(modifier.fillMaxSize())
        {
            when (val state = uiState)
            {
                //Handles the state of getting the information of the selected items.
                ItemComparisonUIState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                ItemComparisonUIState.Error ->
                    Text("Couldn't load your lists", Modifier.align(Alignment.Center))

                is ItemComparisonUIState.PriceSuccess ->

                    if (state.price.isEmpty()) {
                        Text("No Prices Available yet", Modifier.align(Alignment.Center))
                    }
                        //If everything goes well it continues the code to show to compared item at different stores
                      else  {
                        val prices = remember(state.price) {state.price.sortedBy { it.price }}
                        val highestPrice= prices.maxOf{it.price}
                          Column() {
                        Row()
                        {
                            //Sort icon button
                            IconButton( onClick = {
                                bAscending = !bAscending
                            }) { Icon(painterResource(id = R.drawable.ic_filter_list), contentDescription = "Sort Type", Modifier.size(24.dp))}


                        }
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                //Filters the prices based on the product title of the selected product
                val filtered = state.price.filter { it.productTitle == selectedProduct }
                //Sorts the lists based on the value of bAscending
                val sorted = if (bAscending) filtered.sortedBy { it.price } else filtered.sortedByDescending { it.price }
//Ensures the permission is gotten than asks again if it isnt.
                items(sorted, key = { it.productId + it.storeId }) {
                    if ((ActivityCompat.checkSelfPermission(
                            LocalContext.current,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(
                            LocalContext.current,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED)
                    ) {
                        LaunchedEffect(Unit) {
                            getPermissions.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }

                    }
                    //Display the item card
                    ItemCard(it, viewModel, onItemComparison, String.format("%.2f",100 - it.price / highestPrice * 100 ))
                }
            }
            }
              }

                else -> {}
            }
    }
    }

}
//Handles the drawing of all the required information for each item card.
    @OptIn(ExperimentalMaterial3Api::class)
    @androidx.annotation.RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        @Composable
    fun ItemCard(product: StoreProductPricing, viewModel : ItemComparisonViewmodel, onItemComparison: () -> Unit, highestPrice : String)
    {
        //Holds the store list
        val storeList  by viewModel.shoppingLists.collectAsState()
        //Controls showing the dialog when the user sets it to true
        var showDialog by remember {mutableStateOf(false)}
        //Controls picking a list dialog
        var pickList by remember {mutableStateOf(false)}
        //Holds the name of the list
        var listName by remember {mutableStateOf("")}
        var list : ShoppingTrip? = null
        //Holds information from the stores
        val storeInfo by viewModel.storeMap.collectAsState()



//Used whenever add is clicked for existing apps
        fun onAddClicked()
        {
            if (list != null)
            {
                showDialog = false
                viewModel.addItem(InsertItem(list!!.shoppingListId, product.productId, 1, "", false, true))
                onItemComparison()
            }
            else {
                showDialog = true
            }


        }
//Used when you wish to create a new dialog and sets the show dialog to true
        fun onAddNewListClicked()
        {
                showDialog = true
        }

        //Controls when a list is being picked through to add an item to a shopping list
        if (pickList)
        {
            ContentSizedBottomSheet(onDismissRequest = { pickList = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Add to list",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Column()
                    {
                        //iterates through the available lists for the item
                        val lists by viewModel.shoppingLists.collectAsState()
                        for (s in lists) {
                            if (s.storeId == product.storeId) {
                                Card (modifier = Modifier.fillMaxWidth(),colors = CardDefaults.cardColors( containerColor = BottomSheetDefaults.ContainerColor), onClick = {
                                    list =
                                        storeList.firstOrNull { it.shoppingListId == s.shoppingListId }
                                    onAddClicked()
                                }
                                )
                                {
                                    Row(Modifier.fillMaxWidth().padding(12.dp))
                                    {
                                        Text(s.listName + " " + s.storeName)
                                    }

                                }

                            }
                        }
                        //Create new list button
                        Card(colors = CardDefaults.cardColors( containerColor = BottomSheetDefaults.ContainerColor, contentColor = MaterialTheme.colorScheme.primary), onClick = {onAddNewListClicked()}) {
                           Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center)
                           {Text("Create new list",style = MaterialTheme.typography.titleMedium)}
                        }

                    }
                }

            }
        }
//Controls the show dialog button this is used to create  a new list and name it.
        if (showDialog)
        {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = "Enter list title:") },
                text = {
                    OutlinedTextField(modifier = Modifier.fillMaxWidth(),value = listName, onValueChange = {listName = it},label= { Text("Enter list name") })
                    Spacer(modifier = Modifier.height(50.dp))

                },
                confirmButton = {
                    TextButton(onClick = {  showDialog = false
                        viewModel.createListAddItem(product,listName)
                        onItemComparison() }) {
                        Text(text = "Ok")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {  showDialog = false }) {
                        Text(text = "Back")
                    }
                },
            )
        }
//When clicked opens the modal to allow the user to select their lists
        OutlinedCard(
            onClick = {
               pickList = true

            },
            colors = CardDefaults.outlinedCardColors(MaterialTheme.colorScheme.surfaceContainer),


            modifier = Modifier.fillMaxWidth()
        ) {

            Column(modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth())
            {
//Flag that changes color based on the discount price for some visual stimulation for the user
                val pricePercent = highestPrice.toDouble()
                val flagColor = when {
                    pricePercent == 0.0 -> Color.Red
                    pricePercent <= 9.0 -> Color.Yellow
                    else -> Color.Green
                }
                Row(){
                    Text(product.storeName, style = MaterialTheme.typography.titleMedium)
                    Icon(painterResource(id = R.drawable.ic_bookmark), tint= {flagColor},"Discount Price", Modifier.size(24.dp))

                }
                Spacer(Modifier.height(4.dp))
                //Display the percent off of the highest priced store.
                Row(){
                    Text(
                        "$highestPrice% off highest price store",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(Modifier.height(4.dp))
                //Shows the product title and the sizing unit to help differentiate items of same name but different size
                Text(
                    product.productTitle + "   " + product.packageSizing,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                //Gets store information for distance calculation
                LaunchedEffect(product.storeId) {
                    viewModel.getStoreInfo(product.storeId)
                }
                //Ensures store is not null
                val store = storeInfo[product.storeId]
                if (store != null) {
                    //then attempts to get the distance from it
                    val distance = viewModel.addressToCoordinates(store.address)
                    Text(
                        product.displayPrice + (distance?.let { "    $it miles away" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.padding(6.dp))
                    HorizontalDivider()


                } else {
                    Text(
                        product.displayPrice + "    loading distance...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.padding(6.dp))
                    HorizontalDivider()


                }

                //Provides user with  a shopping list button they can also press to open the add to cart modal
                Row(
                    Modifier.align(Alignment.End)
                ){

                    IconButton(
                        onClick =
                            {
                                pickList = true
                            },
                    )
                    {
                        Icon(painterResource(id = R.drawable.ic_shopping_cart), contentDescription = "Add to existing cart", Modifier.size(24.dp))
                    }
                }
            }
        }

    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SimpleSearchBar(
        onSearch: (String) -> Unit,
        viewModel: ItemComparisonViewmodel,
        onInfoScreen :(dest : Dest) -> Unit

        ) {
        //Holds the search results from the searchbar
        var searchResults by remember { mutableStateOf(emptyList<ProductSearch>()) }
        val modifier = Modifier
        val uiState by viewModel.uiState.collectAsState()
        var expanded by rememberSaveable { mutableStateOf(true) }
        //Holds the users text input
        val textFieldState =  rememberTextFieldState("")
        //Filters the search results based on the product name and textfield
        var filteredResults = searchResults.filter {
            it.productName.contains(textFieldState.text, ignoreCase = true)
        }
//Controls the state of the shopping list
        when (val state = uiState) {

            ItemComparisonUIState.Error ->
                Text("Search suggestions couldn't be loaded")
            is ItemComparisonUIState.SearchSuccess -> {
                //When search is successful it filters and suggests matching items to the user
                searchResults = state.products.map { it }
                filteredResults = searchResults.filter {
                    it.productName.contains(textFieldState.text, ignoreCase = true)
                }


            }
            else -> {}
        }
    Box(
        modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }
    ) {
        //Creates a search bar for the user to search items with
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = textFieldState.text.toString(),
                    onQueryChange = {
                        textFieldState.edit { replace(0, length, it) }
                        viewModel.search(textFieldState.text.toString())

                                    },
                    onSearch = {

                        onSearch(textFieldState.text.toString())

                        expanded = false
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text("Search") }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            //Shows when text field state isn't empty and results are empty
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (filteredResults.isEmpty() && textFieldState.text != "")
                {
                    Text("No results could be found")
                }
                //When text field state is empty show the user information about their pantry like when they are running low
                // Or when an item they have is expiring and suggest they buy it.
                else if(textFieldState.text == "")
                {
                    Row(Modifier.background(MaterialTheme.colorScheme.primaryContainer).fillMaxWidth().padding(12.dp))
                    {
                        Text("Running Low")
                    }

                    Column()
                    {
                        viewModel.itemInformation.collectAsState().value.forEach { result ->
                            if (result.lowStockThreshold == null) {
                                if ( result.totalQuantity != 0 )
                                {
                                    return@forEach
                                }
                            }
                            else if (result.totalQuantity > result.lowStockThreshold)
                            {
                                return@forEach
                            }
                            ListItem(
                                headlineContent = { Text(result.name + " quantity remaining: "+result.totalQuantity)
                                },
                                trailingContent = {
                                    IconButton(onClick = {
                                        onInfoScreen(Dest.InformationScreen(result.productId))
                                    })
                                    {
                                        Icon(
                                            painterResource(id = R.drawable.ic_info),
                                            contentDescription = "information",
                                            Modifier.size(24.dp)
                                        )
                                    }
                                },
                                modifier =Modifier
                                    .clickable {
                                        textFieldState.edit { replace(0, length, result.name) }
                                        expanded = false
                                        onSearch(textFieldState.text.toString())

                                    }
                                    .fillMaxWidth()
                            )

                        }
                    }
                    Row(Modifier.background(MaterialTheme.colorScheme.primaryContainer).fillMaxWidth().padding(12.dp))
                    {
                        Text("Expiring Soon")
                    }

                    Column()
                    {
                        val expirationDaysThreshold = 5
                        viewModel.itemInformation.collectAsState().value.forEach { result ->
                            if (result.earliestExpiresInDays == null)
                            {
                                return@forEach
                            }
                            else if (result.earliestExpiresInDays > expirationDaysThreshold )
                            {
                                return@forEach
                            }
                            val expired = if (result.earliestExpiresInDays > 0)  (result.earliestExpiresInDays.toString() + " days") else "expired"
                            ListItem(
                                headlineContent = { Text(result.name + " expiring in: " + expired )
                                },
                                trailingContent = {
                                    IconButton(onClick = {
                                        onInfoScreen(Dest.InformationScreen(result.productId))
                                    })
                                    {
                                        Icon(
                                            painterResource(id = R.drawable.ic_info),
                                            contentDescription = "information",
                                            Modifier.size(24.dp)
                                        )
                                    }
                                },
                                modifier =Modifier
                                    .clickable {
                                        textFieldState.edit { replace(0, length, result.name) }
                                        expanded = false
                                        onSearch(textFieldState.text.toString())

                                    }
                                    .fillMaxWidth()
                            )

                        }
                    }


                }
                else {
//Displays the filtered results for the items they searched allowing users to click on the items or their information to add to their cart or learn more.

                filteredResults.forEach { result ->
                    Row(Modifier.border(width = 1.dp , color = Color.Black)) {
                    ListItem(
                        headlineContent = { Text(result.productName)
                             },
                        trailingContent = {
                            IconButton(onClick = {
                                onInfoScreen(Dest.InformationScreen(result.productId))
                            })
                            {
                                Icon(
                                    painterResource(id = R.drawable.ic_info),
                                    contentDescription = "information",
                                    Modifier.size(24.dp)
                                )
                            }
                        },
                        modifier =Modifier
                            .clickable {
                                textFieldState.edit { replace(0, length, result.productName) }
                                expanded = false
                                onSearch(textFieldState.text.toString())

                            }
                            .fillMaxWidth()
                    )

                }
                }
                }
            }
        }
    }
}