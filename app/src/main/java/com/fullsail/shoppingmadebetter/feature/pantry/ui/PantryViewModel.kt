package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.core.ui.ShoppingListPickerState
import com.fullsail.shoppingmadebetter.feature.pantry.domain.AdjustmentReason
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ApplyInventoryAdjustment
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ApplyInventoryAdjustmentUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.DeleteInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetPantryEstimateAlerts
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetPantryEstimateAlertsUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetSkipRemoveConfirmationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ProductGroup
import com.fullsail.shoppingmadebetter.feature.pantry.domain.SetSkipRemoveConfirmationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryExpiry
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryExpiryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLocationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThreshold
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThresholdUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PantryUiState {
    data object Loading : PantryUiState
    data class Success(val productGroups: List<ProductGroup>) : PantryUiState
    data object Error : PantryUiState
}

/** State of the "add to shopping list" bottom sheet. */
sealed interface AddToListSheetState {
    data object Hidden : AddToListSheetState
    data class Visible(
        val item: InventoryItem,
        val lists: ShoppingListPickerState,
    ) : AddToListSheetState
}

/** One-shot outcomes surfaced to the user as a snackbar. */
sealed interface PantryEvent {
    data class ItemAdded(
        val itemName: String,
        val listName: String,
        val insertedItemId: String,
    ) : PantryEvent

    data class AddFailed(val itemName: String) : PantryEvent

    /** The just-added item was removed via Undo. */
    data class ItemRemoved(val itemName: String) : PantryEvent

    /** Undo failed to remove the just-added item. */
    data class UndoFailed(val itemName: String) : PantryEvent

    /** An item was removed from the pantry (via the card's remove action). */
    data class RemovedFromPantry(val itemName: String) : PantryEvent

    /** Removing an item from the pantry failed. */
    data class RemoveFailed(val itemName: String) : PantryEvent

    /** A quick-action edit to an item (quantity/location/expiry) failed to save. */
    data class UpdateFailed(val itemName: String) : PantryEvent

    /** A background refresh failed while items were already on screen. */
    data object RefreshFailed : PantryEvent
}

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val getInventoryUseCase: GetInventoryUseCase,
    private val getShoppingTripsUseCase: GetShoppingTripsUseCase,
    private val insertItemUseCase: InsertItemUseCase,
    private val deleteItemsUseCase: DeleteItemsUseCase,
    private val deleteInventoryItemUseCase: DeleteInventoryItemUseCase,
    private val getSkipRemoveConfirmationUseCase: GetSkipRemoveConfirmationUseCase,
    private val setSkipRemoveConfirmationUseCase: SetSkipRemoveConfirmationUseCase,
    private val applyInventoryAdjustmentUseCase: ApplyInventoryAdjustmentUseCase,
    private val updateInventoryLocationUseCase: UpdateInventoryLocationUseCase,
    private val updateInventoryExpiryUseCase: UpdateInventoryExpiryUseCase,
    private val updateInventoryLowStockThresholdUseCase: UpdateInventoryLowStockThresholdUseCase,
    private val getPantryEstimateAlertsUseCase: GetPantryEstimateAlertsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PantryUiState>(PantryUiState.Loading)
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    private val _addToListSheet = MutableStateFlow<AddToListSheetState>(AddToListSheetState.Hidden)
    val addToListSheet: StateFlow<AddToListSheetState> = _addToListSheet.asStateFlow()

    private val _removeConfirm = MutableStateFlow<InventoryItem?>(null)
    val removeConfirm: StateFlow<InventoryItem?> = _removeConfirm.asStateFlow()

    /** Lots answered this session, so a failed write or a stale reload cannot re-prompt. */
    private val _handledAlertLotIds = MutableStateFlow<Set<String>>(emptySet())

    /** The zero-stock estimate to confirm now, if any; hidden while the sheet or remove dialog is up. */
    val zeroStockAlert: StateFlow<InventoryItem?> = combine(
        uiState, _handledAlertLotIds, addToListSheet, removeConfirm,
    ) { state, handled, sheet, remove ->
        if (state !is PantryUiState.Success || sheet !is AddToListSheetState.Hidden || remove != null) {
            null
        } else {
            getPantryEstimateAlertsUseCase
                .execute(GetPantryEstimateAlerts(state.productGroups))
                .firstOrNull { it.id !in handled }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _events = Channel<PantryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadInventory()
    }

    /**
     * Loads the user's pantry inventory. Safe to call as a background refresh: when
     * items are already on screen it keeps them visible instead of flashing the
     * spinner, and a failed refresh leaves the existing list in place and says so with
     * a [PantryEvent.RefreshFailed]. Only shows Loading/Error when there is nothing to
     * display yet (e.g. the first load).
     */
    fun loadInventory() {
        if (_uiState.value !is PantryUiState.Success) {
            _uiState.value = PantryUiState.Loading
        }
        viewModelScope.launch {
            when (val out = getInventoryUseCase.execute(Unit)) {
                is GetInventoryUseCase.Output.Success ->
                    _uiState.value = PantryUiState.Success(out.productGroups)

                is GetInventoryUseCase.Output.Failure ->
                    if (_uiState.value !is PantryUiState.Success) {
                        _uiState.value = PantryUiState.Error
                    } else {
                        _events.send(PantryEvent.RefreshFailed)
                    }
            }
        }
    }

    /** Opens the sheet for [item] and loads the user's shopping lists to pick from. */
    fun onAddToListClicked(item: InventoryItem) {
        _addToListSheet.value = AddToListSheetState.Visible(item, ShoppingListPickerState.Loading)
        viewModelScope.launch {
            val lists = when (val out = getShoppingTripsUseCase.execute(Unit)) {
                is GetShoppingTripsUseCase.Output.Success -> if (out.trips.isEmpty()) ShoppingListPickerState.Empty
                else ShoppingListPickerState.Loaded(out.trips)

                is GetShoppingTripsUseCase.Output.Failure -> ShoppingListPickerState.Error
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
                    itemName = item.name,
                    listName = trip.listName,
                    insertedItemId = out.insertedItemId,
                )

                is InsertItemUseCase.Output.Failure -> PantryEvent.AddFailed(item.name)
            }
            _events.send(event)
        }
    }

    /**
     * Undoes an add by removing the just-created shopping-list item [insertedItemId],
     * then reports the outcome. [itemName] is only used to label the resulting snackbar.
     */
    fun undoAdd(insertedItemId: String, itemName: String) {
        viewModelScope.launch {
            val event = when (deleteItemsUseCase.execute(insertedItemId)) {
                is DeleteItemsUseCase.Output.Success -> PantryEvent.ItemRemoved(itemName)
                is DeleteItemsUseCase.Output.Failure -> PantryEvent.UndoFailed(itemName)
            }
            _events.send(event)
        }
    }

    fun dismissAddToListSheet() {
        _addToListSheet.value = AddToListSheetState.Hidden
    }

    /**
     * Shows the confirmation dialog unless the user previously chose "don't ask again".
     */
    fun onRemoveClicked(item: InventoryItem) {
        viewModelScope.launch {
            if (getSkipRemoveConfirmationUseCase.execute(Unit)) {
                deleteItem(item)
            } else {
                _removeConfirm.value = item
            }
        }
    }

    fun dismissRemove() {
        _removeConfirm.value = null
    }

    /**
     * Confirms removal of the item currently in the dialog: closes the dialog,
     * persists the "don't ask again" choice when [dontAskAgain] is set, then
     * deletes the item and reports the outcome via a snackbar.
     */
    fun confirmRemove(dontAskAgain: Boolean) {
        val item = _removeConfirm.value ?: return
        _removeConfirm.value = null
        viewModelScope.launch {
            if (dontAskAgain) setSkipRemoveConfirmationUseCase.execute(true)
            deleteItem(item)
        }
    }

    /**
     * Deletes [item] — a single lot — from the pantry, drops it from its product
     * group on success, and reports the outcome via a snackbar event.
     */
    private suspend fun deleteItem(item: InventoryItem) {
        val event = when (deleteInventoryItemUseCase.execute(item.id)) {
            is DeleteInventoryItemUseCase.Output.Success -> {
                removeLotFromState(item.id)
                PantryEvent.RemovedFromPantry(item.name)
            }

            is DeleteInventoryItemUseCase.Output.Failure -> PantryEvent.RemoveFailed(item.name)
        }
        _events.send(event)
    }

    /**
     * Persists a manual quantity edit as an audit-tracked `manual` adjustment.
     * No-op when the value is unchanged.
     */
    fun onQuantityChanged(item: InventoryItem, newQuantity: Int) {
        if (newQuantity == item.quantity) return
        applyAdjustment(item, newQuantity, AdjustmentReason.Manual)
    }

    /** Confirms [item]'s auto-adjusted quantity as-is (a zero-delta `confirmed` adjustment). */
    fun onConfirmEstimate(item: InventoryItem) {
        applyAdjustment(item, item.quantity, AdjustmentReason.Confirmed)
    }

    /** Replaces [item]'s auto-adjusted quantity with the user's count as a `confirmed` adjustment. */
    fun onCorrectEstimate(item: InventoryItem, newQuantity: Int) {
        applyAdjustment(item, newQuantity, AdjustmentReason.Confirmed)
    }

    /** "Add to list": confirms [item] at zero and opens the add-to-list sheet for it. */
    fun onZeroStockOut(item: InventoryItem) {
        onAddToListClicked(item)
        markAlertHandled(item)
        onConfirmEstimate(item)
    }

    /** "Still have some": replaces the zero with the user's [count] as a `confirmed` adjustment. */
    fun onZeroStockStillHave(item: InventoryItem, count: Int) {
        markAlertHandled(item)
        onCorrectEstimate(item, count)
    }

    /** "Not now": a zero-delta `dismissed` adjustment, so the lot is not asked about again. */
    fun onZeroStockDismissed(item: InventoryItem) {
        markAlertHandled(item)
        applyAdjustment(item, item.quantity, AdjustmentReason.Dismissed)
    }

    private fun markAlertHandled(item: InventoryItem) {
        _handledAlertLotIds.value = _handledAlertLotIds.value + item.id
    }

    /**
     * Optimistically sets [item]'s quantity to [newQuantity] and latest reason to [reason],
     * persists the change, and reverts with a snackbar if the save fails. On success the
     * quantity reconciles to what the backend applied, since it floors at zero.
     */
    private fun applyAdjustment(item: InventoryItem, newQuantity: Int, reason: AdjustmentReason) {
        updateLotInState(item.id) { it.copy(quantity = newQuantity, lastAdjustmentReason = reason) }
        viewModelScope.launch {
            val out = applyInventoryAdjustmentUseCase.execute(
                ApplyInventoryAdjustment(item.id, newQuantity - item.quantity, reason),
            )
            when (out) {
                is ApplyInventoryAdjustmentUseCase.Output.Success ->
                    updateLotInState(item.id) { it.copy(quantity = out.newQuantity) }

                is ApplyInventoryAdjustmentUseCase.Output.Failure -> {
                    updateLotInState(item.id) { item }
                    _events.send(PantryEvent.UpdateFailed(item.name))
                }
            }
        }
    }

    /**
     * Optimistically sets [item]'s storage location to [newLocation] in the list, persists
     * it, and reverts with a snackbar if the save fails. No-op when the value is unchanged.
     */
    fun onLocationChanged(item: InventoryItem, newLocation: PantryLocation) {
        if (newLocation == item.location) return
        updateLotInState(item.id) { it.copy(location = newLocation) }
        viewModelScope.launch {
            val out = updateInventoryLocationUseCase.execute(
                UpdateInventoryLocation(item.id, newLocation),
            )
            if (out is UpdateInventoryLocationUseCase.Output.Failure) {
                updateLotInState(item.id) { item }
                _events.send(PantryEvent.UpdateFailed(item.name))
            }
        }
    }

    /**
     * Optimistically sets [item]'s shelf life to [newExpiresInDays] (days from today) in the
     * list, persists it, and reverts with a snackbar if the save fails. No-ops when unchanged.
     */
    fun onExpiryChanged(item: InventoryItem, newExpiresInDays: Int) {
        if (newExpiresInDays == item.expiresInDays) return
        updateLotInState(item.id) { it.copy(expiresInDays = newExpiresInDays) }
        viewModelScope.launch {
            val out = updateInventoryExpiryUseCase.execute(
                UpdateInventoryExpiry(item.id, newExpiresInDays),
            )
            if (out is UpdateInventoryExpiryUseCase.Output.Failure) {
                updateLotInState(item.id) { item }
                _events.send(PantryEvent.UpdateFailed(item.name))
            }
        }
    }

    /**
     * Optimistically sets the low-stock [newThreshold] (`null` clears it) for [item]'s product,
     * persists it, and reverts with a snackbar if the save fails. The threshold is per-product,
     * so every pantry entry of the same product updates together. No-op when unchanged.
     */
    fun onLowStockThresholdChanged(item: InventoryItem, newThreshold: Int?) {
        if (newThreshold == item.lowStockThreshold) return
        val previous = item.lowStockThreshold
        updateLotsByProductId(item.productId) { it.copy(lowStockThreshold = newThreshold) }
        viewModelScope.launch {
            val out = updateInventoryLowStockThresholdUseCase.execute(
                UpdateInventoryLowStockThreshold(item.productId, newThreshold),
            )
            if (out is UpdateInventoryLowStockThresholdUseCase.Output.Failure) {
                updateLotsByProductId(item.productId) { it.copy(lowStockThreshold = previous) }
                _events.send(PantryEvent.UpdateFailed(item.name))
            }
        }
    }

    /**
     * Drops the lot [lotId] from its product group, removing the group entirely
     * when that was its last lot.
     */
    private fun removeLotFromState(lotId: String) {
        val current = _uiState.value
        if (current is PantryUiState.Success) {
            _uiState.value = current.copy(
                productGroups = current.productGroups.mapNotNull { group ->
                    val remaining = group.lots.filterNot { it.id == lotId }
                    when {
                        remaining.size == group.lots.size -> group
                        remaining.isEmpty() -> null
                        else -> group.copy(lots = remaining)
                    }
                },
            )
        }
    }

    /**
     * Replaces the lot [lotId] inside its product group with [transform] applied to it,
     * leaving every other lot untouched. The group's derived aggregates (total quantity,
     * earliest expiry, locations) recompute. Group and lot order are kept
     * as-is, the next full load re-sorts. No-ops unless the state is [PantryUiState.Success].
     */
    private fun updateLotInState(lotId: String, transform: (InventoryItem) -> InventoryItem) {
        val current = _uiState.value
        if (current is PantryUiState.Success) {
            _uiState.value = current.copy(
                productGroups = current.productGroups.map { group ->
                    if (group.lots.none { it.id == lotId }) {
                        group
                    } else {
                        group.copy(
                            lots = group.lots.map { lot ->
                                if (lot.id == lotId) transform(lot) else lot
                            },
                        )
                    }
                },
            )
        }
    }

    /**
     * Applies [transform] to every lot of the product [productId], leaving other groups
     * untouched. Used for per-product settings (the low-stock threshold), which every lot
     * of a product carries together. No-ops unless the state is [PantryUiState.Success].
     */
    private fun updateLotsByProductId(
        productId: String,
        transform: (InventoryItem) -> InventoryItem,
    ) {
        val current = _uiState.value
        if (current is PantryUiState.Success) {
            _uiState.value = current.copy(
                productGroups = current.productGroups.map { group ->
                    if (group.productId == productId) {
                        group.copy(lots = group.lots.map(transform))
                    } else {
                        group
                    }
                },
            )
        }
    }
}
