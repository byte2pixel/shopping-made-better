package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.GetShoppingListItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListItems
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.CompleteShoppingTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ShoppingListItemsState {
    data object Loading : ShoppingListItemsState
    data class Success(val items: List<ShoppingListItems>) : ShoppingListItemsState
    data object Error : ShoppingListItemsState
    data object DeleteSuccess : ShoppingListItemsState
}

@HiltViewModel
class ShoppingListItemsViewModel @Inject constructor(
    private val getShoppingListItemsUseCase: GetShoppingListItemsUseCase,
    private val getDeleteItemsUseCase: DeleteItemsUseCase,
    private val completeShoppingTripUseCase: CompleteShoppingTripUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShoppingListItemsState>(ShoppingListItemsState.Loading)
    val uiState: StateFlow<ShoppingListItemsState> = _uiState.asStateFlow()

    init { }

    fun getItems(input : String)  {
        _uiState.value = ShoppingListItemsState.Loading
        viewModelScope.launch {
            _uiState.value = when (val out = getShoppingListItemsUseCase.execute(input)) {
                is GetShoppingListItemsUseCase.Output.Success ->
                    ShoppingListItemsState.Success(out.input)
                is GetShoppingListItemsUseCase.Output.Failure ->
                    ShoppingListItemsState.Error
            }
        }
    }
    fun deleteItems(input : String, listId : String? = null)
    {
        _uiState.value = ShoppingListItemsState.Loading
        viewModelScope.launch {
            _uiState.value = when (val out = getDeleteItemsUseCase.execute(input)) {
                is DeleteItemsUseCase.Output.Success -> {
                    if (listId != null)
                    {
                        getItems(listId)
                    }
                    ShoppingListItemsState.DeleteSuccess
                }


                is DeleteItemsUseCase.Output.Failure ->
                    ShoppingListItemsState.Error
            }
        }

    }

    fun markAllPurchased(listId : String) {
        _uiState.value = ShoppingListItemsState.Loading
        viewModelScope.launch {
            when (completeShoppingTripUseCase.execute(listId)) {
                is CompleteShoppingTripUseCase.Output.Success ->
                    getItems(listId) // refresh the list since now it should be empty.
                is CompleteShoppingTripUseCase.Output.Failure ->
                    _uiState.value = ShoppingListItemsState.Error
            }
        }
    }
}
