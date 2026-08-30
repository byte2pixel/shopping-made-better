package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.GetShoppingListItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListItems
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.isChecked
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.isCheckedUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.CheckAllItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.CompleteShoppingTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ShoppingListItemsState {
    data object Loading : ShoppingListItemsState
    data class Success(val items: List<ShoppingListItems>) : ShoppingListItemsState
    data object Error : ShoppingListItemsState
    data object DeleteSuccess : ShoppingListItemsState
}

/**
 * One-shot outcomes surfaced to the user as a snackbar.
 *
 * The bottom-bar "mark all as purchased" button is rendered from [ShoppingListItemsState],
 * so reporting a failure by setting [ShoppingListItemsState.Error] just made the button
 * disappear with no explanation. These events are separate from the list state for that
 * reason, minor fix so it is clear an error happened.
 */
sealed interface ShoppingListItemsEvent {
    /** The whole list was purchased: it is now in purchase history and the pantry. */
    data object ListPurchased : ShoppingListItemsEvent

    /** Purchasing the list failed. Nothing was bought and the list is untouched. */
    data object PurchaseFailed : ShoppingListItemsEvent
}

@HiltViewModel
class ShoppingListItemsViewModel @Inject constructor(
    private val getShoppingListItemsUseCase: GetShoppingListItemsUseCase,
    private val getDeleteItemsUseCase: DeleteItemsUseCase,
    private val completeShoppingTripUseCase: CompleteShoppingTripUseCase,
    private val getIsCheckedUseCase : isCheckedUseCase,
    private val insertItemUseCase: InsertItemUseCase,
    private val checkAllItemsUseCase: CheckAllItemsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShoppingListItemsState>(ShoppingListItemsState.Loading)
    val uiState: StateFlow<ShoppingListItemsState> = _uiState.asStateFlow()

    private val _checkedItems = MutableStateFlow<List<String>>(emptyList())
    val checkedItems: StateFlow<List<String>> = _checkedItems.asStateFlow()

    private val _events = Channel<ShoppingListItemsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()


    init { }
    fun addItem(item : InsertItem, listId : String)
    {
        //_uiState.value = ShoppingListItemsState.Loading
        viewModelScope.launch {
           when( insertItemUseCase.execute(item))
           {
               is InsertItemUseCase.Output.Success ->
                   getItems(listId) // refresh the list since now it should be empty.
               is InsertItemUseCase.Output.Failure ->
                   _uiState.value = ShoppingListItemsState.Error
           }

        }
    }
    fun toggleItemCheck(itemId: String) {
        val current = _checkedItems.value.toMutableList()
        if (itemId in current) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _checkedItems.value = current
    }

    fun clearCheckedItems(listId: String) {
        _checkedItems.value.forEach { deleteItems(it, listId) }
        _checkedItems.value = emptyList()
    }
    fun getItems(input : String)  {
        _uiState.value = ShoppingListItemsState.Loading
        viewModelScope.launch {
         when (val out = getShoppingListItemsUseCase.execute(input)) {
                is GetShoppingListItemsUseCase.Output.Success ->{
                    _checkedItems.value = out.input.filter{
                        it.checked
                    }.map{it.id}
                    _uiState.value = ShoppingListItemsState.Success(out.input)}
                is GetShoppingListItemsUseCase.Output.Failure ->
                    _uiState.value = ShoppingListItemsState.Error
            }
        }
    }

    fun checkItem(id : String, state : Boolean, listId : String){
        viewModelScope.launch{
            when (val out = getIsCheckedUseCase.execute(isChecked(id, state))) {
                is isCheckedUseCase.Output.Success ->
                {
                    getItems(listId)
                }
                is isCheckedUseCase.Output.Failure ->
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

    /**
     * Buys the whole list: flags every item as checked, then completes the trip.

     * Deliberately separate from [markAllPurchased], which the cart screen uses now for
     * partial completion: checking everything there would silently buy the items the
     * user chose to leave behind.
     *
     * Nothing is deleted client-side. The RPC removes the purchased rows itself in one
     * transaction, so a failure anywhere leaves the list exactly as it was.
     */
    fun purchaseWholeList(listId: String) {
        val items = (_uiState.value as? ShoppingListItemsState.Success)?.items.orEmpty()
        // An empty list would only make the RPC raise; there is nothing to buy.
        if (items.isEmpty()) return

        _uiState.value = ShoppingListItemsState.Loading
        viewModelScope.launch {
            if (checkAllItemsUseCase.execute(listId) is CheckAllItemsUseCase.Output.Failure) {
                _events.send(ShoppingListItemsEvent.PurchaseFailed)
                getItems(listId)
                return@launch
            }

            when (completeShoppingTripUseCase.execute(listId)) {
                is CompleteShoppingTripUseCase.Output.Success -> {
                    _events.send(ShoppingListItemsEvent.ListPurchased)
                    getItems(listId) // refresh the list since now it should be empty.
                }

                is CompleteShoppingTripUseCase.Output.Failure -> {
                    _events.send(ShoppingListItemsEvent.PurchaseFailed)
                    getItems(listId) // nothing was bought; show the list as it still is.
                }
            }
        }
    }
}
