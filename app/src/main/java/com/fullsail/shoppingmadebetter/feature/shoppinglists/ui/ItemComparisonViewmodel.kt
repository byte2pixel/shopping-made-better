package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ProductGroup
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.GetStoreInformationUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.StoreAddressInformation
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearch
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearchUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing.StoreProductPricing
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.storeProductPricing.StoreProductPricingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
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
    private val getStoreInformationUseCase: GetStoreInformationUseCase,
    private val getInventoryUseCase: GetInventoryUseCase,
    @ApplicationContext private val context: Context

    ) : ViewModel()
{

    private val _shoppingLists = MutableStateFlow<List<ShoppingTrip>>(listOf())
    val shoppingLists = _shoppingLists.asStateFlow()
    private val _itemInformation = MutableStateFlow<List<ProductGroup>>(listOf())
    val itemInformation = _itemInformation.asStateFlow()
    val storeMap =  MutableStateFlow<Map<String, StoreAddressInformation>>(emptyMap())
    private val _uiState = MutableStateFlow<ItemComparisonUIState>(ItemComparisonUIState.Loading)
    val uiState: StateFlow<ItemComparisonUIState> = _uiState.asStateFlow()

    init { getShoppingLists()
        getPantryItemInformation() }

    fun getPantryItemInformation()
    {
        viewModelScope.launch {
            when (val out = getInventoryUseCase.execute(Unit))
            {
                is GetInventoryUseCase.Output.Success ->{
                    _itemInformation.value = out.productGroups.filter { it.totalQuantity== 0 || it.earliestExpiresInDays == 0 }
                }
                is GetInventoryUseCase.Output.Failure ->
                    ItemComparisonUIState.Error


            }

        }
    }

    fun getStoreInfo(id : String)
    {
        viewModelScope.launch {
            when (val out = getStoreInformationUseCase.execute(id))
            {
                is GetStoreInformationUseCase.Output.Success ->{
                    storeMap.value += (id to out.input)

                }
                is GetStoreInformationUseCase.Output.Failure ->
                    ItemComparisonUIState.Error


            }

        }
    }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun addressToCoordinates(storeLocation : String) : String
    {
        val getUserLocation = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location =
            getUserLocation.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: return "error"

        val geocoder =  Geocoder(context, Locale.getDefault())
        val addressLocation = geocoder.getFromLocationName(storeLocation , 1)?.firstOrNull()
        if (addressLocation != null) {

            val distance = FloatArray(1)
            Location.distanceBetween(
                location.latitude,
                location.longitude,
                addressLocation.latitude,
                addressLocation.longitude,
                distance
            )
            val milesAway = distance[0] * 0.00062137
            Log.d("DISTANCE", (milesAway).toString())
            return String.format(Locale.getDefault(),"%.1f",milesAway)

        }
        return "error"

        }

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

    fun createListAddItem(product : StoreProductPricing, listName : String)
    {
        viewModelScope.launch {
            when (val out = getShoppingListUseCase.execute(ShoppingList( null,product.storeId, listName, false)))
            {
                is ShoppingListUseCase.Output.Success ->{
                    if (out.list.shoppingListId != null)
                    {
                        addItem(InsertItem(out.list.shoppingListId, product.productId, 1, "", false, true))

                    }

                }
                is ShoppingListUseCase.Output.Failure ->
                    ItemComparisonUIState.Error
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
