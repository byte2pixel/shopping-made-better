package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseHistoryUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryFilter
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseHistoryPagingSource
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTripSummary
import com.fullsail.shoppingmadebetter.feature.stores.domain.GetStoresUseCase
import com.fullsail.shoppingmadebetter.feature.stores.domain.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getPurchaseHistoryUseCase: GetPurchaseHistoryUseCase,
    private val getStoresUseCase: GetStoresUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * The stores offered as filter chips.
     *
     * Every store, not only the ones the user has shopped at: answering that would
     * cost its own query, and a chip that finds nothing is honest — it says so with
     * the "no matches" message.
     *
     * A failure leaves the list empty, so the row collapses to nothing and the
     * unfiltered history still reads normally. Losing the filter is worth less than
     * an error screen over a list that loaded fine.
     */
    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    /**
     * The active filter, kept in [SavedStateHandle] rather than `rememberSaveable`:
     * the pager is built from it, so it has to live where the pager does, and this
     * way it survives the process being killed in the background as well as a
     * rotation.
     */
    val filter: StateFlow<HistoryFilter> = savedStateHandle
        .getStateFlow(KEY_STORE_IDS, emptyList<String>())
        .map { HistoryFilter(storeIds = it.toSet()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HistoryFilter(storeIds = storeIds().toSet()),
        )

    /**
     * The user's completed trips matching [filter], newest first, a page at a time.
     *
     * A new filter means a new pager, not an invalidated one: a `PagingSource`'s keys
     * are row offsets *within its own filter's results*, so offset 20 under "ALDI"
     * and offset 20 unfiltered are different trips. `flatMapLatest` drops the old
     * source's in-flight pages on the floor, which is what should happen — they
     * answer a question the user has moved on from.
     *
     * `cachedIn` keeps the loaded pages across configuration changes and tab
     * switches — the History tab keeps its own back stack, so this ViewModel
     * outlives the screen and re-paging from scratch on every return would undo
     * the point of paging. The screen still calls `refresh()` on entry so a trip
     * completed on the shopping-list tab shows up.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val trips: Flow<PagingData<PurchaseTripSummary>> = filter
        // No distinctUntilChanged: StateFlow already drops a re-emission of an equal
        // value, so re-selecting the same chips cannot rebuild the pager.
        .flatMapLatest { active ->
            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    // Pin the first load to one page. The default asks for 3x, which
                    // for a new user is three times the rows to render one screen of
                    // cards.
                    initialLoadSize = PAGE_SIZE,
                    enablePlaceholders = false,
                ),
                // A fresh source per generation: a PagingSource is single-use, so
                // Paging asks again after every invalidation (including a
                // pull-to-refresh from the screen) and after every filter change.
                pagingSourceFactory = {
                    PurchaseHistoryPagingSource(getPurchaseHistoryUseCase, active)
                },
            ).flow
        }
        .cachedIn(viewModelScope)

    init {
        loadStores()
    }

    /** Adds [storeId] to the filter, or drops it if it is already on. */
    fun toggleStore(storeId: String) {
        val current = storeIds()
        savedStateHandle[KEY_STORE_IDS] = if (storeId in current) {
            current - storeId
        } else {
            current + storeId
        }
    }

    private fun storeIds(): List<String> =
        savedStateHandle.get<List<String>>(KEY_STORE_IDS).orEmpty()

    private fun loadStores() {
        viewModelScope.launch {
            when (val output = getStoresUseCase.execute(Unit)) {
                is GetStoresUseCase.Output.Success -> _stores.value = output.stores
                is GetStoresUseCase.Output.Failure -> _stores.value = emptyList()
            }
        }
    }

    private companion object {
        /** Comfortably more than one screen of trip cards, so the next page is
         *  already loading by the time the user reaches the bottom. */
        const val PAGE_SIZE = 20

        /** Saved-state key. A `List` because `SavedStateHandle` cannot hold a `Set`. */
        const val KEY_STORE_IDS = "history-filter-store-ids"
    }
}
