package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui


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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearch
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing.StoreProductPricing
import com.fullsail.shoppingmadebetter.navigation.Dest


@Composable
fun ShoppingListItemComparisonScreen(
    onItemComparison :() -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemComparisonViewmodel = hiltViewModel(),
    onInfoScreen :(dest : Dest) -> Unit,

) {
    var selectedProduct by rememberSaveable {mutableStateOf<String?>(null)}
    val uiState by viewModel.uiState.collectAsState()


    if (selectedProduct == null)
    {
        Column{
            SimpleSearchBar(  onSearch = {product-> selectedProduct = product
                                         viewModel.showProducts(product)}, viewModel, onInfoScreen)
        }
    }
    else
    {

        Box(modifier.fillMaxSize())
        {
            when (val state = uiState)
            {
                ItemComparisonUIState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                ItemComparisonUIState.Error ->
                    Text("Couldn't load your lists", Modifier.align(Alignment.Center))

                is ItemComparisonUIState.PriceSuccess ->

                    if (state.price.isEmpty()) {
                        Text("No Prices Available yet", Modifier.align(Alignment.Center))
                    }

                      else  {

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.price.filter { it.productTitle == selectedProduct }, key = {it.productId + it.storeId }) {ItemCard(it, viewModel, onItemComparison) }
            }

              }

                else -> {}
            }
    }
    }

}
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ItemCard(product: StoreProductPricing, viewModel : ItemComparisonViewmodel, onItemComparison: () -> Unit)
    {
        val storeList  by viewModel.shoppingLists.collectAsState()
        var showDialog by remember {mutableStateOf(false)}
        var pickList by remember {mutableStateOf(false)}
        var listName by remember {mutableStateOf("")}
        var list : ShoppingTrip? = null
        fun onAddClicked()
        {
           // val list = storeList.firstOrNull { it.storeId == product.storeId }
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

        fun onAddNewListClicked()
        {
                showDialog = true
        }

        if (pickList)
        {
            ModalBottomSheet(onDismissRequest = { pickList = false }) {
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
                    }
                }

            }
        }

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

        OutlinedCard(
            onClick = {
               onAddClicked()

            },


            Modifier.fillMaxWidth()
        ) {


            Column(modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth())
            {

                Text(product.storeName, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(product.productTitle, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Text(
                    product.price + " - x miles away",
                    style = MaterialTheme.typography.bodyMedium,
                )
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

                    IconButton(
                        onClick =
                            {
                                onAddNewListClicked()
                            },
                    )
                    {
                        Icon(painterResource(id = R.drawable.ic_add_shopping_cart), contentDescription = "add to new cart", Modifier.size(24.dp))
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
        var searchResults by remember { mutableStateOf(emptyList<ProductSearch>()) }
        val modifier = Modifier
        val uiState by viewModel.uiState.collectAsState()
        var expanded by rememberSaveable { mutableStateOf(true) }
        val textFieldState =  rememberTextFieldState("")
        var filteredResults = searchResults.filter {
            it.productName.contains(textFieldState.text, ignoreCase = true)
        }

        when (val state = uiState) {

            ItemComparisonUIState.Error ->
                Text("Search suggestions couldn't be loaded")
            is ItemComparisonUIState.SearchSuccess -> {
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
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (filteredResults.isEmpty() && textFieldState.text != "")
                {
                    Text("No results could be found")
                }
                else {


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