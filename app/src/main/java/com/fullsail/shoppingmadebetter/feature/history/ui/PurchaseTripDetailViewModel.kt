package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.core.ui.ShoppingListPickerState
import com.fullsail.shoppingmadebetter.feature.history.domain.AddTripToList
import com.fullsail.shoppingmadebetter.feature.history.domain.AddTripToListUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseTripUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.GetTripCostComparisonUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTrip
import com.fullsail.shoppingmadebetter.feature.history.domain.StoreBasketCost
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListUseCase
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
        /** Stores a new list can be created at; empty when that load failed. */
        val stores: List<Store> = emptyList(),
        /** The trip's own store, preselected for a new list. */
        val defaultStoreId: String? = null,
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
    private val getTripCostComparisonUseCase: GetTripCostComparisonUseCase,
    private val shoppingListUseCase: ShoppingListUseCase,
    private val getStoresUseCase: GetStoresUseCase,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<PurchaseTripDetailUiState>(PurchaseTripDetailUiState.Loading)
    val uiState: StateFlow<PurchaseTripDetailUiState> = _uiState.asStateFlow()

    /**
     * This basket priced at every store that stocks all of it, cheapest first, or
     * empty when there is nothing to compare. A failure leaves it empty rather than
     * failing the screen — the trip itself loaded.
     */
    private val _storeCosts = MutableStateFlow<List<StoreBasketCost>>(emptyList())
    val storeCosts: StateFlow<List<StoreBasketCost>> = _storeCosts.asStateFlow()

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
        _storeCosts.value = emptyList()
        loadStoreCosts(purchaseId)
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

    private fun loadStoreCosts(purchaseId: String) {
        viewModelScope.launch {
            _storeCosts.value = when (
                val out = getTripCostComparisonUseCase.execute(purchaseId)
            ) {
                is GetTripCostComparisonUseCase.Output.Success -> out.stores
                is GetTripCostComparisonUseCase.Output.Failure -> emptyList()
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

    /**
     * Opens the picker and loads the user's shopping lists, plus the stores a new list
     * could be created at. The two run together, and a failed store load only disables
     * creating — picking an existing list still works.
     */
    fun onBuyAgainClicked() {
        val selectedCount = _selectedProductIds.value.size
        if (selectedCount == 0) return
        _buyAgainSheet.value = BuyAgainSheetState.Visible(
            selectedCount = selectedCount,
            lists = ShoppingListPickerState.Loading,
            defaultStoreId = (_uiState.value as? PurchaseTripDetailUiState.Success)?.trip?.storeId,
        )
        viewModelScope.launch {
            val stores = async { stores() }
            val lists = when (val out = getShoppingTripsUseCase.execute(Unit)) {
                is GetShoppingTripsUseCase.Output.Success ->
                    if (out.trips.isEmpty()) ShoppingListPickerState.Empty
                    else ShoppingListPickerState.Loaded(out.trips)

                is GetShoppingTripsUseCase.Output.Failure -> ShoppingListPickerState.Error
            }
            // Only apply if the sheet is still open — the user may have dismissed it.
            val current = _buyAgainSheet.value
            if (current is BuyAgainSheetState.Visible) {
                _buyAgainSheet.value = current.copy(lists = lists, stores = stores.await())
            }
        }
    }

    /** Every store, or none when the fetch fails — the sheet treats empty as "can't create". */
    private suspend fun stores(): List<Store> =
        when (val out = getStoresUseCase.execute(Unit)) {
            is GetStoresUseCase.Output.Success -> out.stores
            is GetStoresUseCase.Output.Failure -> emptyList()
        }

    /** Copies the selected items onto [trip]'s list, then reports the outcome. */
    fun onListChosen(trip: ShoppingTrip) {
        if (_buyAgainSheet.value !is BuyAgainSheetState.Visible) return
        val purchaseId = this.purchaseId ?: return
        val productIds = _selectedProductIds.value
        _buyAgainSheet.value = BuyAgainSheetState.Hidden
        if (productIds.isEmpty()) return
        viewModelScope.launch {
            addTripToList(trip.shoppingListId, trip.listName, purchaseId, productIds)
        }
    }

    /**
     * Creates a list called [name] at [storeId], then copies the selected items onto
     * it — the point of creating it from here. A failed create reports the same
     * add-failed snackbar as a failed add; either way nothing reached the list.
     */
    fun onCreateList(name: String, storeId: String) {
        if (_buyAgainSheet.value !is BuyAgainSheetState.Visible) return
        val purchaseId = this.purchaseId ?: return
        val productIds = _selectedProductIds.value
        _buyAgainSheet.value = BuyAgainSheetState.Hidden
        if (productIds.isEmpty()) return
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
                _events.send(TripDetailEvent.AddFailed)
            } else {
                addTripToList(listId, listName, purchaseId, productIds)
            }
        }
    }

    /** Copies [productIds] from [purchaseId] onto the list [listId] and reports the outcome. */
    private suspend fun addTripToList(
        listId: String,
        listName: String,
        purchaseId: String,
        productIds: Set<String>,
    ) {
        val out = addTripToListUseCase.execute(
            AddTripToList(
                purchaseId = purchaseId,
                shoppingListId = listId,
                productIds = productIds,
            )
        )
        val event = when (out) {
            is AddTripToListUseCase.Output.Success -> TripDetailEvent.ItemsAdded(
                added = out.added,
                listName = listName,
                skipped = out.skipped,
            )

            is AddTripToListUseCase.Output.PartialFailure ->
                TripDetailEvent.AddPartiallyFailed(added = out.added, failed = out.failed)

            is AddTripToListUseCase.Output.Failure -> TripDetailEvent.AddFailed
        }
        _events.send(event)
    }

    fun dismissBuyAgainSheet() {
        _buyAgainSheet.value = BuyAgainSheetState.Hidden
    }
}
