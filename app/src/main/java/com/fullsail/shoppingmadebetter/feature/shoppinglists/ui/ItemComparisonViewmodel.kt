package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearch
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearchUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing.StoreProductPricing
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing.StoreProductPricingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ItemComparisonUIState {
    data object Loading : ItemComparisonUIState
    data class SearchSuccess(val products: List<ProductSearch>) : ItemComparisonUIState
    data class PriceSuccess(val price: List<StoreProductPricing>) : ItemComparisonUIState
    data object Error : ItemComparisonUIState
}


@HiltViewModel
class ItemComparisonViewmodel @Inject constructor(
    private val getStoreProductPricingUseCase: StoreProductPricingUseCase,
    private val productSearchUseCase: ProductSearchUseCase,
    private val insertItemUseCase: InsertItemUseCase,
    private val getShoppingTripsUseCase: GetShoppingTripsUseCase,
    private val getShoppingListUseCase: ShoppingListUseCase,

    ) : ViewModel()
{
    private val _shoppingLists = MutableStateFlow<List<ShoppingTrip>>(listOf())
    val shoppingLists = _shoppingLists.asStateFlow()
    private val _uiState = MutableStateFlow<ItemComparisonUIState>(ItemComparisonUIState.Loading)
    val uiState: StateFlow<ItemComparisonUIState> = _uiState.asStateFlow()

    init { getShoppingLists() }

    fun search(search : String)
    {
        _uiState.value = ItemComparisonUIState.Loading
        viewModelScope.launch {
            _uiState.value = when (val out = productSearchUseCase.execute(search)) {
                is ProductSearchUseCase.Output.Success ->
                    ItemComparisonUIState.SearchSuccess(out.product)
                is ProductSearchUseCase.Output.Failure ->
                    ItemComparisonUIState.Error
            }
        }
    }
    fun addItem(item : InsertItem)
    {
        _uiState.value = ItemComparisonUIState.Loading
        viewModelScope.launch {
            insertItemUseCase.execute(item)
        }
    }

    fun createListAddItem(product : StoreProductPricing)
    {
        viewModelScope.launch {

            getShoppingListUseCase.execute(ShoppingList( product.storeId, product.storeName + " Weekly", false))
            getShoppingLists{
            val list = shoppingLists.value.first { it.storeId == product.storeId}
            addItem(InsertItem(list.shoppingListId, product.productId, 1, "", false, true))
            }
        }
    }


    fun showProducts(prodId : String)
    {
        _uiState.value = ItemComparisonUIState.Loading
        viewModelScope.launch {
            _uiState.value =  when (val out = getStoreProductPricingUseCase.execute(prodId)) {
                is StoreProductPricingUseCase.Output.Success ->
                    ItemComparisonUIState.PriceSuccess(out.prices)
                is StoreProductPricingUseCase.Output.Failure ->
                    ItemComparisonUIState.Error
            }
        }
    }



    fun getShoppingLists(waiting : () -> Unit = {})
    {
        viewModelScope.launch {
            Log.d("ShoppingList", "Refreshed Lists")
            when (val out = getShoppingTripsUseCase.execute(Unit))
            {
                is GetShoppingTripsUseCase.Output.Success ->{
                    _shoppingLists.value = out.trips
                    waiting()
                }
                is GetShoppingTripsUseCase.Output.Failure ->
                    ItemComparisonUIState.Error
            }
        }
    }
}
