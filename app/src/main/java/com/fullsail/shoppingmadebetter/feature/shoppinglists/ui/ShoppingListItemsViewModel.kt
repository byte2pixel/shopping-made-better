package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.GetShoppingListItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListItems
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.UpdateQuantityUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.isChecked
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.isCheckedUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.QuantityUpdate
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
    private val updateQuantityUseCase: UpdateQuantityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShoppingListItemsState>(ShoppingListItemsState.Loading)
    val uiState: StateFlow<ShoppingListItemsState> = _uiState.asStateFlow()

    //Handles items that are checked
    private val _checkedItems = MutableStateFlow<List<String>>(emptyList())
    val checkedItems: StateFlow<List<String>> = _checkedItems.asStateFlow()
//Handles when list is purchased even state or when the purchase fails
    private val _events = Channel<ShoppingListItemsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()


    //Adds item to the list, allowing the user to clone an item in their list,
    fun addItem(item : InsertItem, listId : String, itemName : String)
    {
        viewModelScope.launch {
           when( val addedItem = insertItemUseCase.execute(item))
           {
               is InsertItemUseCase.Output.Success-> {
                   val currentState = _uiState.value
                   if (currentState is ShoppingListItemsState.Success)
                   {
                       _uiState.value = ShoppingListItemsState.Success(currentState.items +
                               ShoppingListItems(addedItem.insertedItemId, listId, item.productId, item.quantity, itemName, false))
                   }
           }
                  // getItems(listId) // refresh the list since now it should be empty.
               is InsertItemUseCase.Output.Failure ->
                   _uiState.value = ShoppingListItemsState.Error
           }

        }
    }
    //Allows the user to update the quantity of items in their list.
    fun updateQuantity(id : String, newQuantity:Int )
    {
        viewModelScope.launch {
            when(updateQuantityUseCase.execute(QuantityUpdate(id, newQuantity))){
                is UpdateQuantityUseCase.Output.Success-> {
                    val currentState = _uiState.value
                    if (currentState is ShoppingListItemsState.Success)
                    {
                        val newItemList = currentState.items.map{
                            if (it.id == id)
                            {
                                it.copy(quantity = newQuantity)
                            }
                            else {
                                it
                            }
                        }
                        _uiState.value = ShoppingListItemsState.Success(newItemList)
                    }
                }
                is UpdateQuantityUseCase.Output.Failure ->{}
            }
        }
    }
    //Controls plus button to increase the items quantity when pressed.
    fun increaseQuantity(item : ShoppingListItems)
    {
        updateQuantity(item.id , item.quantity + 1)
    }
    //Controls plus button to decrease the items quantity when pressed.
    fun decreaseQuantity(item:ShoppingListItems)
    {
        if (item.quantity > 1){
            updateQuantity(item.id, item.quantity - 1)
        }

    }
    //Toggles the checked state on a cart item
    fun toggleItemCheck(itemId: String) {
        val current = _checkedItems.value.toMutableList()
        if (itemId in current) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _checkedItems.value = current
    }
//Clears the checked items list and empties it
    fun clearCheckedItems() {
        _checkedItems.value.forEach { deleteItems(it) }
        _checkedItems.value = emptyList()
    }
    //Get the checked items into the _checkedItems state and refreshes the uiState value with the regotten items
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
//Sets the item from its id's checkmart state to whatever states value is
    fun checkItem(id : String, state : Boolean){
        viewModelScope.launch{
            when (getIsCheckedUseCase.execute(isChecked(id, state))) {
                is isCheckedUseCase.Output.Success ->
                {
                    //Manually updates the list to not cause screen flickering or database calls
                    val currentState = _uiState.value
                    if (currentState is ShoppingListItemsState.Success) {
                        val newCheckedItems = currentState.items.map{
                            if (it.id == id)
                            {
                                it.copy(checked=state)
                            }
                            else{
                                it
                            }
                        }
                        _uiState.value = ShoppingListItemsState.Success(newCheckedItems)
                    }


                }
                is isCheckedUseCase.Output.Failure ->
                    ShoppingListItemsState.Error


            }
        }
    }
//Delete an item off of the list with its given id
    fun deleteItems(input : String)
    {
        viewModelScope.launch {
             when (getDeleteItemsUseCase.execute(input)) {
                is DeleteItemsUseCase.Output.Success -> {
                    //When success manually  update the list to avoid refreshing from database
                    val currentState = _uiState.value
                    if (currentState is ShoppingListItemsState.Success) {
                        val postDeletedList = currentState.items.filter { it.id != input }
                        _uiState.value = ShoppingListItemsState.Success(postDeletedList)
                    }

                    }
                 is DeleteItemsUseCase.Output.Failure ->
                     ShoppingListItemsState.Error
            }
        }

    }

    //Calls an RPC to mark all purchase items and ask to push it to pantry
    //This removes all checked items from the below list ID and may or may not push them to the pantry
    //Based on user input
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
