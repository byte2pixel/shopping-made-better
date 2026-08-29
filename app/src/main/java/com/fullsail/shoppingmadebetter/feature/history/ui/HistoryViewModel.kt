package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseHistoryUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryDatePreset
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryFilter
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseHistoryPagingSource
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTripSummary
import com.fullsail.shoppingmadebetter.feature.history.domain.rangeFrom
import com.fullsail.shoppingmadebetter.feature.history.domain.selectedPreset
import com.fullsail.shoppingmadebetter.feature.stores.domain.GetStoresUseCase
import com.fullsail.shoppingmadebetter.feature.stores.domain.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getPurchaseHistoryUseCase: GetPurchaseHistoryUseCase,
    private val getStoresUseCase: GetStoresUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock,
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
     *
     * The dates go in as ISO strings, which `SavedStateHandle` holds directly; a
     * null key is "no bound" rather than any particular date.
     */
    val filter: StateFlow<HistoryFilter> = combine(
        savedStateHandle.getStateFlow(KEY_STORE_IDS, emptyList<String>()),
        savedStateHandle.getStateFlow<String?>(KEY_FROM, null),
        savedStateHandle.getStateFlow<String?>(KEY_TO, null),
        savedStateHandle.getStateFlow(KEY_SEARCH, ""),
    ) { ids, from, to, search ->
        HistoryFilter(
            storeIds = ids.toSet(),
            from = from?.let(LocalDate::parse),
            to = to?.let(LocalDate::parse),
            search = search,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = currentFilter(),
    )

    /**
     * What the search field shows, updated on every keystroke.
     * Separate from the term in [filter], which lags it by [SEARCH_DEBOUNCE]:
     */
    private val _searchInput = MutableStateFlow(savedStateHandle[KEY_SEARCH] ?: "")
    val searchInput: StateFlow<String> = _searchInput.asStateFlow()

    /**
     * Which date chip reads as selected, derived rather than stored so it can never
     * disagree with the dates actually being filtered on.
     */
    val selectedDatePreset: StateFlow<HistoryDatePreset?> = filter
        .map { it.selectedPreset(today()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = filter.value.selectedPreset(today()),
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
        commitSearchAfterPauses()
    }

    /** Records the search field's text; the filter follows once typing pauses. */
    fun setSearch(text: String) {
        _searchInput.value = text
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

    /**
     * Applies [preset], or clears the date range when it is already the active one.
     *
     * Single-select, unlike the stores: a trip falls in one range, so a second
     * preset replaces the first rather than widening anything.
     */
    fun selectDatePreset(preset: HistoryDatePreset) {
        val today = today()
        if (filter.value.selectedPreset(today) == preset) {
            setDates(from = null, to = null)
            return
        }
        val range = preset.rangeFrom(today)
        setDates(from = range.start, to = range.endInclusive)
    }

    /** Applies a hand-picked range; both ends inclusive. */
    fun setCustomRange(from: LocalDate, to: LocalDate) = setDates(from, to)

    /** Drops every filter at once, returning the tab to the full history. */
    fun clearFilters() {
        savedStateHandle[KEY_STORE_IDS] = emptyList<String>()
        setDates(from = null, to = null)
        // Both, not just the input: waiting out the debounce would leave the list
        // filtered for a moment after the user asked for it not to be.
        _searchInput.value = ""
        savedStateHandle[KEY_SEARCH] = ""
    }

    /**
     * Feeds the settled search text into the filter, and so into a new pager.
     *
     * `debounce` waits for a pause instead of firing per keystroke, and
     * `distinctUntilChanged` drops a pause that ended on the text it started with
     * typing a letter and deleting it should not rebuild the pager.
     */
    @OptIn(FlowPreview::class)
    private fun commitSearchAfterPauses() {
        viewModelScope.launch {
            _searchInput
                .debounce(SEARCH_DEBOUNCE)
                .distinctUntilChanged()
                .collect { savedStateHandle[KEY_SEARCH] = it }
        }
    }

    private fun setDates(from: LocalDate?, to: LocalDate?) {
        savedStateHandle[KEY_FROM] = from?.toString()
        savedStateHandle[KEY_TO] = to?.toString()
    }

    private fun today(): LocalDate = clock.todayIn(TimeZone.currentSystemDefault())

    /** The saved filter read straight through, for a StateFlow's initial value. */
    private fun currentFilter() = HistoryFilter(
        storeIds = storeIds().toSet(),
        from = savedStateHandle.get<String?>(KEY_FROM)?.let(LocalDate::parse),
        to = savedStateHandle.get<String?>(KEY_TO)?.let(LocalDate::parse),
        search = savedStateHandle[KEY_SEARCH] ?: "",
    )

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

        /** Saved-state keys for the date range, as ISO strings; null means unbounded. */
        const val KEY_FROM = "history-filter-from"
        const val KEY_TO = "history-filter-to"

        /** Saved-state key for the settled search text, never the in-flight one. */
        const val KEY_SEARCH = "history-filter-search"

        /** Long enough to cover typing, short enough to feel immediate. */
        val SEARCH_DEBOUNCE = 300.milliseconds
    }
}
