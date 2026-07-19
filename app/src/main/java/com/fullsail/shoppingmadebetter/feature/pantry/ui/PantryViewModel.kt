package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PantryUiState {
    data object Loading : PantryUiState
    data class Success(val inventoryItems: List<InventoryItem>) : PantryUiState
    data object Error : PantryUiState
}

/** State of the "add to shopping list" bottom sheet. */
sealed interface AddToListSheetState {
    data object Hidden : AddToListSheetState
    data class Visible(val item: InventoryItem, val lists: Lists) : AddToListSheetState

    /** The user's shopping lists loaded for the picker. */
    sealed interface Lists {
        data object Loading : Lists
        data class Loaded(val trips: List<ShoppingTrip>) : Lists
        data object Empty : Lists
        data object Error : Lists
    }
}

/** One-shot outcomes surfaced to the user as a snackbar. */
sealed interface PantryEvent {
    data class ItemAdded(val itemName: String, val listName: String) : PantryEvent
    data class AddFailed(val itemName: String) : PantryEvent
}

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val getInventoryUseCase: GetInventoryUseCase,
    private val getShoppingTripsUseCase: GetShoppingTripsUseCase,
    private val insertItemUseCase: InsertItemUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PantryUiState>(PantryUiState.Loading)
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    private val _addToListSheet = MutableStateFlow<AddToListSheetState>(AddToListSheetState.Hidden)
    val addToListSheet: StateFlow<AddToListSheetState> = _addToListSheet.asStateFlow()

    private val _events = Channel<PantryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadInventory()
    }

    /**
     * Loads the user's pantry inventory.
     */
    fun loadInventory() {
        _uiState.value = PantryUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val out = getInventoryUseCase.execute(Unit)) {
                is GetInventoryUseCase.Output.Success -> PantryUiState.Success(out.inventoryItems)
                is GetInventoryUseCase.Output.Failure -> PantryUiState.Error
            }
        }
    }

    /** Opens the sheet for [item] and loads the user's shopping lists to pick from. */
    fun onAddToListClicked(item: InventoryItem) {
        _addToListSheet.value = AddToListSheetState.Visible(item, AddToListSheetState.Lists.Loading)
        viewModelScope.launch {
            val lists = when (val out = getShoppingTripsUseCase.execute(Unit)) {
                is GetShoppingTripsUseCase.Output.Success -> if (out.trips.isEmpty()) AddToListSheetState.Lists.Empty
                else AddToListSheetState.Lists.Loaded(out.trips)

                is GetShoppingTripsUseCase.Output.Failure -> AddToListSheetState.Lists.Error
            }
            // Only apply if the sheet is still open for the same item.
            val current = _addToListSheet.value
            if (current is AddToListSheetState.Visible && current.item.id == item.id) {
                _addToListSheet.value = current.copy(lists = lists)
            }
        }
    }

    /** Adds the sheet's item to [trip]'s shopping list, then reports the outcome. */
    fun onListChosen(trip: ShoppingTrip) {
        val current = _addToListSheet.value
        if (current !is AddToListSheetState.Visible) return
        val item = current.item
        _addToListSheet.value = AddToListSheetState.Hidden
        viewModelScope.launch {
            val out = insertItemUseCase.execute(
                InsertItem(
                    shoppingListId = trip.shoppingListId,
                    productId = item.productId,
                    quantity = 1,
                    note = "",
                    isChecked = false,
                    addInventory = true,
                )
            )
            val event = when (out) {
                is InsertItemUseCase.Output.Success -> PantryEvent.ItemAdded(
                    item.name,
                    trip.listName
                )

                is InsertItemUseCase.Output.Failure -> PantryEvent.AddFailed(item.name)
            }
            _events.send(event)
        }
    }

    fun dismissAddToListSheet() {
        _addToListSheet.value = AddToListSheetState.Hidden
    }
}
