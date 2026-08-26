package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseHistoryPagingSource
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTripSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class HistoryViewModel @Inject constructor(
    /**
     * A fresh source per generation: a PagingSource is single-use, so Paging asks
     * again after every invalidation (including a pull-to-refresh from the screen).
     */
    private val pagingSourceProvider: Provider<PurchaseHistoryPagingSource>,
) : ViewModel() {

    /**
     * The user's completed trips, newest first, one page at a time.
     *
     * `cachedIn` keeps the loaded pages across configuration changes and tab
     * switches — the History tab keeps its own back stack, so this ViewModel
     * outlives the screen and re-paging from scratch on every return would undo
     * the point of paging. The screen still calls `refresh()` on entry so a trip
     * completed on the shopping-list tab shows up.
     */
    val trips: Flow<PagingData<PurchaseTripSummary>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            // Pin the first load to one page. The default asks for 3x, which for a
            // new user is three times the rows to render one screen of cards.
            initialLoadSize = PAGE_SIZE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { pagingSourceProvider.get() },
    ).flow.cachedIn(viewModelScope)

    private companion object {
        /** Comfortably more than one screen of trip cards, so the next page is
         *  already loading by the time the user reaches the bottom. */
        const val PAGE_SIZE = 20
    }
}
