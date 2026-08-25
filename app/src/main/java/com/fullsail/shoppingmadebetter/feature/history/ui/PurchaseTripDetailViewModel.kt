package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.core.ui.ShoppingListPickerState
import com.fullsail.shoppingmadebetter.feature.history.domain.AddTripToList
import com.fullsail.shoppingmadebetter.feature.history.domain.AddTripToListUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseTripUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTrip
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

sealed interface PurchaseTripDetailUiState {
    data object Loading : PurchaseTripDetailUiState
    data class Success(val trip: PurchaseTrip) : PurchaseTripDetailUiState

    /** The trip was deleted, or never belonged to this user. */
    data object NotFound : PurchaseTripDetailUiState
    data object Error : PurchaseTripDetailUiState
}

/** State of the "buy again" shopping-list picker. */
sealed interface BuyAgainSheetState {
    data object Hidden : BuyAgainSheetState

    /** [selectedCount] labels the sheet ("Add 4 items to…"). */
    data class Visible(
        val selectedCount: Int,
        val lists: ShoppingListPickerState,
    ) : BuyAgainSheetState
}

/** One-shot outcomes of a "buy again", surfaced to the user as a snackbar. */
sealed interface TripDetailEvent {
    /** [skipped] products were selected but are no longer in the catalog. */
    data class ItemsAdded(val added: Int, val listName: String, val skipped: Int) : TripDetailEvent

    /** Some items made it onto the list and [failed] did not. */
    data class AddPartiallyFailed(val added: Int, val failed: Int) : TripDetailEvent

    /** Nothing was added. */
    data object AddFailed : TripDetailEvent
}

@HiltViewModel
class PurchaseTripDetailViewModel @Inject constructor(
    private val getPurchaseTripUseCase: GetPurchaseTripUseCase,
    private val getShoppingTripsUseCase: GetShoppingTripsUseCase,
    private val addTripToListUseCase: AddTripToListUseCase,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<PurchaseTripDetailUiState>(PurchaseTripDetailUiState.Loading)
    val uiState: StateFlow<PurchaseTripDetailUiState> = _uiState.asStateFlow()

    /** Products ticked for "buy again"; every item of the trip starts selected. */
    private val _selectedProductIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedProductIds: StateFlow<Set<String>> = _selectedProductIds.asStateFlow()

    private val _buyAgainSheet = MutableStateFlow<BuyAgainSheetState>(BuyAgainSheetState.Hidden)
    val buyAgainSheet: StateFlow<BuyAgainSheetState> = _buyAgainSheet.asStateFlow()

    private val _events = Channel<TripDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** The trip currently on screen; the id "buy again" copies from. */
    private var purchaseId: String? = null

    /** Loads the trip [purchaseId]; called from the screen, which owns the id. */
    fun load(purchaseId: String) {
        this.purchaseId = purchaseId
        _uiState.value = PurchaseTripDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val out = getPurchaseTripUseCase.execute(purchaseId)) {
                is GetPurchaseTripUseCase.Output.Success -> {
                    // Everything selected by default: repeat-buying the whole basket is
                    // the common case, and dropping a few is quicker than picking many.
                    _selectedProductIds.value = out.trip.items.map { it.productId }.toSet()
                    PurchaseTripDetailUiState.Success(out.trip)
                }

                GetPurchaseTripUseCase.Output.NotFound -> PurchaseTripDetailUiState.NotFound
                is GetPurchaseTripUseCase.Output.Failure -> PurchaseTripDetailUiState.Error
            }
        }
    }

    /** Ticks or unticks [productId] for the next "buy again". */
    fun onItemToggled(productId: String) {
        val current = _selectedProductIds.value
        _selectedProductIds.value = if (productId in current) {
            current - productId
        } else {
            current + productId
        }
    }

    /** Opens the picker and loads the user's shopping lists. No-op with nothing selected. */
    fun onBuyAgainClicked() {
        val selectedCount = _selectedProductIds.value.size
        if (selectedCount == 0) return
        _buyAgainSheet.value = BuyAgainSheetState.Visible(
            selectedCount = selectedCount,
            lists = ShoppingListPickerState.Loading,
        )
        viewModelScope.launch {
            val lists = when (val out = getShoppingTripsUseCase.execute(Unit)) {
                is GetShoppingTripsUseCase.Output.Success ->
                    if (out.trips.isEmpty()) ShoppingListPickerState.Empty
                    else ShoppingListPickerState.Loaded(out.trips)

                is GetShoppingTripsUseCase.Output.Failure -> ShoppingListPickerState.Error
            }
            // Only apply if the sheet is still open — the user may have dismissed it.
            val current = _buyAgainSheet.value
            if (current is BuyAgainSheetState.Visible) {
                _buyAgainSheet.value = current.copy(lists = lists)
            }
        }
    }

    /** Copies the selected items onto [trip]'s list, then reports the outcome. */
    fun onListChosen(trip: ShoppingTrip) {
        if (_buyAgainSheet.value !is BuyAgainSheetState.Visible) return
        val purchaseId = this.purchaseId ?: return
        val productIds = _selectedProductIds.value
        _buyAgainSheet.value = BuyAgainSheetState.Hidden
        if (productIds.isEmpty()) return
        viewModelScope.launch {
            val out = addTripToListUseCase.execute(
                AddTripToList(
                    purchaseId = purchaseId,
                    shoppingListId = trip.shoppingListId,
                    productIds = productIds,
                )
            )
            val event = when (out) {
                is AddTripToListUseCase.Output.Success -> TripDetailEvent.ItemsAdded(
                    added = out.added,
                    listName = trip.listName,
                    skipped = out.skipped,
                )

                is AddTripToListUseCase.Output.PartialFailure ->
                    TripDetailEvent.AddPartiallyFailed(added = out.added, failed = out.failed)

                is AddTripToListUseCase.Output.Failure -> TripDetailEvent.AddFailed
            }
            _events.send(event)
        }
    }

    fun dismissBuyAgainSheet() {
        _buyAgainSheet.value = BuyAgainSheetState.Hidden
    }
}
