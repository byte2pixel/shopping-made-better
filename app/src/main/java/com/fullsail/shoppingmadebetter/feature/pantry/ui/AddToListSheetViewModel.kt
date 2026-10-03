package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.core.ui.ShoppingListPickerState
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.feature.stores.domain.GetStoresUseCase
import com.fullsail.shoppingmadebetter.feature.stores.domain.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State of the "add to shopping list" bottom sheet. */
sealed interface AddToListSheetState {
    data object Hidden : AddToListSheetState
    data class Visible(
        val item: InventoryItem,
        val lists: ShoppingListPickerState,
        /** Stores a new list can be created at; empty when that load failed. */
        val stores: List<Store> = emptyList(),
    ) : AddToListSheetState
}

/** One-shot outcomes of the add-to-list sheet, surfaced as a snackbar. */
sealed interface AddToListSheetEvent {
    data class ItemAdded(
        val itemName: String,
        val listName: String,
        val insertedItemId: String,
    ) : AddToListSheetEvent

    data class AddFailed(val itemName: String) : AddToListSheetEvent

    /** The just-added item was removed via Undo. */
    data class ItemRemoved(val itemName: String) : AddToListSheetEvent

    /** Undo failed to remove the just-added item. */
    data class UndoFailed(val itemName: String) : AddToListSheetEvent
}

/** The pantry's "add to shopping list" sheet: picks or creates a list for one inventory item. */
@HiltViewModel
class AddToListSheetViewModel @Inject constructor(
    private val getShoppingTripsUseCase: GetShoppingTripsUseCase,
    private val insertItemUseCase: InsertItemUseCase,
    private val deleteItemsUseCase: DeleteItemsUseCase,
    private val shoppingListUseCase: ShoppingListUseCase,
    private val getStoresUseCase: GetStoresUseCase,
) : ViewModel() {
    private val _addToListSheet = MutableStateFlow<AddToListSheetState>(AddToListSheetState.Hidden)
    val addToListSheet: StateFlow<AddToListSheetState> = _addToListSheet.asStateFlow()

    private val _events = Channel<AddToListSheetEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /**
     * Opens the sheet for [item] and loads the user's shopping lists to pick from,
     * plus the stores a brand-new list could be created at. The two run together, and
     * a failed store load only disables creating — picking an existing list still works.
     */
    fun onAddToListClicked(item: InventoryItem) {
        _addToListSheet.value = AddToListSheetState.Visible(item, ShoppingListPickerState.Loading)
        viewModelScope.launch {
            val stores = async { stores() }
            val lists = when (val out = getShoppingTripsUseCase.execute(Unit)) {
                is GetShoppingTripsUseCase.Output.Success -> if (out.trips.isEmpty()) ShoppingListPickerState.Empty
                else ShoppingListPickerState.Loaded(out.trips)

                is GetShoppingTripsUseCase.Output.Failure -> ShoppingListPickerState.Error
            }
            // Only apply if the sheet is still open for the same item.
            val current = _addToListSheet.value
            if (current is AddToListSheetState.Visible && current.item.id == item.id) {
                _addToListSheet.value = current.copy(lists = lists, stores = stores.await())
            }
        }
    }

    /** Every store, or none when the fetch fails — the sheet treats empty as "can't create". */
    private suspend fun stores(): List<Store> =
        when (val out = getStoresUseCase.execute(Unit)) {
            is GetStoresUseCase.Output.Success -> out.stores
            is GetStoresUseCase.Output.Failure -> emptyList()
        }

    /** Adds the sheet's item to [trip]'s shopping list, then reports the outcome. */
    fun onListChosen(trip: ShoppingTrip) {
        val current = _addToListSheet.value
        if (current !is AddToListSheetState.Visible) return
        val item = current.item
        _addToListSheet.value = AddToListSheetState.Hidden
        viewModelScope.launch {
            addToList(trip.shoppingListId, trip.listName, item)
        }
    }

    /**
     * Creates a list called [name] at [storeId], then adds the sheet's item to it —
     * the point of creating it from here. A failed create reports the same add-failed
     * snackbar as a failed add; either way nothing reached the list.
     */
    fun onCreateList(name: String, storeId: String) {
        val current = _addToListSheet.value
        if (current !is AddToListSheetState.Visible) return
        val item = current.item
        _addToListSheet.value = AddToListSheetState.Hidden
        val listName = name.trim()
        viewModelScope.launch {
            val out = shoppingListUseCase.execute(
                ShoppingList(
                    shoppingListId = null,
                    storeId = storeId,
                    name = listName,
                    // Sharing is household-wide through RLS; the column is unused.
                    shared = false,
                )
            )
            val listId = (out as? ShoppingListUseCase.Output.Success)?.list?.shoppingListId
            if (listId == null) {
                _events.send(AddToListSheetEvent.AddFailed(item.name))
            } else {
                addToList(listId, listName, item)
            }
        }
    }

    /** Puts [item] on the list [listId], reporting the outcome as a snackbar event. */
    private suspend fun addToList(listId: String, listName: String, item: InventoryItem) {
        val out = insertItemUseCase.execute(
            InsertItem(
                shoppingListId = listId,
                productId = item.productId,
                quantity = 1,
                note = "",
                isChecked = false,
                addInventory = true,
            )
        )
        val event = when (out) {
            is InsertItemUseCase.Output.Success -> AddToListSheetEvent.ItemAdded(
                itemName = item.name,
                listName = listName,
                insertedItemId = out.insertedItemId,
            )

            is InsertItemUseCase.Output.Failure -> AddToListSheetEvent.AddFailed(item.name)
        }
        _events.send(event)
    }

    /**
     * Undoes an add by removing the just-created shopping-list item [insertedItemId],
     * then reports the outcome. [itemName] is only used to label the resulting snackbar.
     */
    fun undoAdd(insertedItemId: String, itemName: String) {
        viewModelScope.launch {
            val event = when (deleteItemsUseCase.execute(insertedItemId)) {
                is DeleteItemsUseCase.Output.Success -> AddToListSheetEvent.ItemRemoved(itemName)
                is DeleteItemsUseCase.Output.Failure -> AddToListSheetEvent.UndoFailed(itemName)
            }
            _events.send(event)
        }
    }

    fun dismiss() {
        _addToListSheet.value = AddToListSheetState.Hidden
    }
}
