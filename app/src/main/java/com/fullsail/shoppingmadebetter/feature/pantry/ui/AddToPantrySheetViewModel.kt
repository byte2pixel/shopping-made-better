package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.pantry.domain.AddInventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.AddInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.DeleteInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearch
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State of the "add to pantry" bottom sheet. It has two faces behind one state: a
 * catalog search until a product is picked, then the quantity and location controls
 * for it. [location] stays null unless the user picks one, which leaves the choice to
 * the product's category.
 *
 * [results] and [searchFailed] describe the last search that came back, not the one in
 * flight: while [searching] they are the previous keystroke's answer, still on screen,
 * because blanking them mid-type collapses the sheet and bounces it back.
 */
sealed interface AddToPantrySheetState {
    data object Hidden : AddToPantrySheetState
    data class Visible(
        val query: String = "",
        val results: List<ProductSearch> = emptyList(),
        val searching: Boolean = false,
        /** True when the last search failed, so the sheet says so instead of "no matches". */
        val searchFailed: Boolean = false,
        /** True once a search has returned, so "no matches" is only said when it was asked. */
        val hasSearched: Boolean = false,
        val selected: ProductSearch? = null,
        val quantity: Int = 1,
        val location: PantryLocation? = null,
    ) : AddToPantrySheetState
}

/** One-shot outcomes of the add-to-pantry sheet; [Added] and [Undone] change the inventory. */
sealed interface AddToPantrySheetEvent {
    /** A product was added to the pantry; [lotId] is what Undo would remove. */
    data class Added(val itemName: String, val lotId: String) : AddToPantrySheetEvent

    data class AddFailed(val itemName: String) : AddToPantrySheetEvent

    /** The just-added lot was removed via Undo. */
    data class Undone(val itemName: String) : AddToPantrySheetEvent

    /** Undo failed to remove the just-added lot. */
    data class UndoFailed(val itemName: String) : AddToPantrySheetEvent
}

/** The pantry's "add to pantry" sheet: a catalog search, then quantity and location for the pick. */
@HiltViewModel
class AddToPantrySheetViewModel @Inject constructor(
    private val productSearchUseCase: ProductSearchUseCase,
    private val addInventoryItemUseCase: AddInventoryItemUseCase,
    private val deleteInventoryItemUseCase: DeleteInventoryItemUseCase,
) : ViewModel() {
    private val _addToPantrySheet = MutableStateFlow<AddToPantrySheetState>(AddToPantrySheetState.Hidden)
    val addToPantrySheet: StateFlow<AddToPantrySheetState> = _addToPantrySheet.asStateFlow()

    /** The in-flight catalog search, cancelled on the next keystroke so only the last one runs. */
    private var searchJob: Job? = null

    private val _events = Channel<AddToPantrySheetEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Opens the sheet on an empty search. */
    fun onAddToPantryClicked() {
        _addToPantrySheet.value = AddToPantrySheetState.Visible()
    }

    /**
     * Searches the catalog for [query] after a short pause, so a fast typist sends one
     * request rather than one per letter. Below [MIN_SEARCH_LENGTH] characters nothing is
     * sent: a single letter matches most of the catalog and would only cost a round trip.
     */
    fun onAddToPantryQuery(query: String) {
        val current = _addToPantrySheet.value
        if (current !is AddToPantrySheetState.Visible) return
        searchJob?.cancel()
        val term = query.trim()
        if (term.length < MIN_SEARCH_LENGTH) {
            _addToPantrySheet.value = current.copy(
                query = query,
                results = emptyList(),
                searching = false,
                searchFailed = false,
                hasSearched = false,
            )
            return
        }
        // Only `searching` changes here. The previous results stay put until the new ones
        // land, so the sheet keeps its height instead of collapsing onto a spinner and
        // springing back on every keystroke.
        _addToPantrySheet.value = current.copy(query = query, searching = true)
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val out = productSearchUseCase.execute(term.escapedForLike())
            // The cancel above usually wins, but a result that outran it must not
            // overwrite what the user has typed since.
            val latest = _addToPantrySheet.value
            if (latest is AddToPantrySheetState.Visible && latest.query == query) {
                _addToPantrySheet.value = when (out) {
                    is ProductSearchUseCase.Output.Success -> latest.copy(
                        results = out.product,
                        searching = false,
                        searchFailed = false,
                        hasSearched = true,
                    )

                    is ProductSearchUseCase.Output.Failure -> latest.copy(
                        results = emptyList(),
                        searching = false,
                        searchFailed = true,
                        hasSearched = true,
                    )
                }
            }
        }
    }

    /** Picks [product] and moves the sheet on to the quantity and location controls. */
    fun onAddToPantryProductSelected(product: ProductSearch) {
        val current = _addToPantrySheet.value
        if (current !is AddToPantrySheetState.Visible) return
        searchJob?.cancel()
        _addToPantrySheet.value = current.copy(selected = product, searching = false)
    }

    /** Goes back to the results without losing the query, so a mis-tap is one tap to fix. */
    fun onAddToPantryProductCleared() {
        val current = _addToPantrySheet.value
        if (current !is AddToPantrySheetState.Visible) return
        _addToPantrySheet.value = current.copy(selected = null, quantity = 1, location = null)
    }

    fun onAddToPantryQuantity(quantity: Int) {
        val current = _addToPantrySheet.value
        if (current !is AddToPantrySheetState.Visible) return
        _addToPantrySheet.value =
            current.copy(quantity = quantity.coerceIn(1, MAX_PANTRY_QUANTITY))
    }

    /** Sets where the lot is stored; [location] null hands the choice back to the product. */
    fun onAddToPantryLocation(location: PantryLocation?) {
        val current = _addToPantrySheet.value
        if (current !is AddToPantrySheetState.Visible) return
        _addToPantrySheet.value = current.copy(location = location)
    }

    /**
     * Adds the picked product as a new lot. The event carries the new lot's id so Undo
     * can remove exactly what was added; the screen reloads the pantry on it.
     */
    fun onAddToPantryConfirm() {
        val current = _addToPantrySheet.value
        if (current !is AddToPantrySheetState.Visible) return
        val product = current.selected ?: return
        dismiss()
        viewModelScope.launch {
            val out = addInventoryItemUseCase.execute(
                AddInventoryItem(
                    productId = product.productId,
                    quantity = current.quantity,
                    location = current.location,
                ),
            )
            val event = when (out) {
                is AddInventoryItemUseCase.Output.Success ->
                    AddToPantrySheetEvent.Added(product.productName, out.lotId)

                is AddInventoryItemUseCase.Output.Failure ->
                    AddToPantrySheetEvent.AddFailed(product.productName)
            }
            _events.send(event)
        }
    }

    /** Undoes an add by deleting the lot [lotId] it created; the screen reloads on the event. */
    fun undoAddToPantry(lotId: String, itemName: String) {
        viewModelScope.launch {
            val event = when (deleteInventoryItemUseCase.execute(lotId)) {
                is DeleteInventoryItemUseCase.Output.Success -> AddToPantrySheetEvent.Undone(itemName)
                is DeleteInventoryItemUseCase.Output.Failure -> AddToPantrySheetEvent.UndoFailed(itemName)
            }
            _events.send(event)
        }
    }

    fun dismiss() {
        searchJob?.cancel()
        _addToPantrySheet.value = AddToPantrySheetState.Hidden
    }

    private companion object {
        /** Shortest query worth sending; below this most of the catalog matches. */
        const val MIN_SEARCH_LENGTH = 2

        /** How long typing has to pause before the search goes out. */
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}

/**
 * This text as something safe to hand to `LIKE`. `%` and `_` are wildcards and `\`
 * escapes them, so all three are escaped — otherwise searching for "100% Whole Grains"
 * would match anything starting with "100".
 */
private fun String.escapedForLike(): String =
    replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
