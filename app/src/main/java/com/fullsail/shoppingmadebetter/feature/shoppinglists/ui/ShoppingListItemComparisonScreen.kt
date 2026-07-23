package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing.StoreProductPricing


@Composable
fun ShoppingListItemComparisonScreen(
    onItemComparison :() -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemComparisonViewmodel = hiltViewModel(),

) {
    var selectedProduct by rememberSaveable {mutableStateOf<String?>(null)}
    val uiState by viewModel.uiState.collectAsState()


    if (selectedProduct == null)
    {
        Column{
            SimpleSearchBar(  onSearch = {product-> selectedProduct = product
                                         viewModel.showProducts(product)}, viewModel)
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
        var listName by remember {mutableStateOf("")}

        fun onAddClicked()
        {
            val list = storeList.firstOrNull { it.storeId == product.storeId }
            if (list != null)
            {
                showDialog = false
                viewModel.addItem(InsertItem(list.shoppingListId, product.productId, 1, "", false, true))
                onItemComparison()
            }
            else {
                showDialog = true
            }


        }


        if (showDialog)
        {
               BasicAlertDialog(onDismissRequest = {}, Modifier.fillMaxWidth(), DialogProperties(),
                {
                    Column(modifier = Modifier.fillMaxWidth().background( color = MaterialTheme.colorScheme.surface).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    OutlinedTextField(modifier = Modifier.fillMaxWidth(),value = listName, onValueChange = {listName = it},label= { Text("Enter list name") })
                    Spacer(modifier = Modifier.height(50.dp))
                    Button(enabled= listName.isNotBlank(),
                            onClick = {
                                showDialog = false
                        viewModel.createListAddItem(product,listName)
                        onItemComparison()
                    }
                        )
                    {
                        Text("Ok")
                    }

                    }
            })





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

                Button(
                    onClick =
                        {
                            onAddClicked()

                        },
                    Modifier.align(Alignment.End)
                )
                {
                    Text("Add")
                }
            }
        }

    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SimpleSearchBar(
        onSearch: (String) -> Unit,
        viewModel: ItemComparisonViewmodel

        ) {
        var searchResults by remember { mutableStateOf(emptyList<String>()) }
        val modifier = Modifier
        val uiState by viewModel.uiState.collectAsState()
        var expanded by rememberSaveable { mutableStateOf(true) }
        val textFieldState =  rememberTextFieldState("")
        var filteredResults = searchResults.filter {
            it.contains(textFieldState.text, ignoreCase = true)
        }

        when (val state = uiState) {

            ItemComparisonUIState.Loading ->
                CircularProgressIndicator()

            ItemComparisonUIState.Error ->
                Text("Search suggestions couldn't be loaded")
            is ItemComparisonUIState.SearchSuccess -> {

                searchResults = state.products.map { it.productName }
                filteredResults = searchResults.filter {
                    it.contains(textFieldState.text, ignoreCase = true)
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
                filteredResults.forEach { result ->
                    ListItem(
                        headlineContent = { Text(result) },
                        modifier = Modifier
                            .clickable {
                                textFieldState.edit { replace(0, length, result) }
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