package com.fullsail.shoppingmadebetter.feature.history.domain

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [PurchaseHistoryPagingSource]. These drive `load` directly, which
 * is where the offset arithmetic that decides whether a trip is ever shown lives.
 */
class PurchaseHistoryPagingSourceTest {

    private fun pagingSource(
        repository: FakeHistoryRepository,
        filter: HistoryFilter = HistoryFilter(),
    ) = PurchaseHistoryPagingSource(GetPurchaseHistoryUseCaseImpl(repository), filter)

    private fun refresh(loadSize: Int) = PagingSource.LoadParams.Refresh<Int>(
        key = null,
        loadSize = loadSize,
        placeholdersEnabled = false,
    )

    private fun append(key: Int, loadSize: Int) = PagingSource.LoadParams.Append(
        key = key,
        loadSize = loadSize,
        placeholdersEnabled = false,
    )

    /** The loaded page; fails the test when the load did not produce one. */
    private fun PagingSource.LoadResult<Int, PurchaseTripSummary>.page():
        PagingSource.LoadResult.Page<Int, PurchaseTripSummary> {
        assertTrue(
            "expected Page but was $this",
            this is PagingSource.LoadResult.Page,
        )
        return this as PagingSource.LoadResult.Page
    }

    @Test
    fun `first load starts at the newest trip and points at the next page`() = runTest {
        val source = pagingSource(FakeHistoryRepository(summaries = summaryRows(50)))

        val page = source.load(refresh(loadSize = 20)).page()

        assertEquals(20, page.data.size)
        assertEquals("trip-0", page.data.first().id)
        assertNull("the list only ever loads older trips", page.prevKey)
        assertEquals(20, page.nextKey)
    }

    @Test
    fun `append continues from its key`() = runTest {
        val source = pagingSource(FakeHistoryRepository(summaries = summaryRows(50)))

        val page = source.load(append(key = 20, loadSize = 20)).page()

        assertEquals("trip-20", page.data.first().id)
        assertEquals("trip-39", page.data.last().id)
        assertEquals(40, page.nextKey)
    }

    @Test
    fun `a full page keeps paging even when it is the last one`() = runTest {
        // 40 trips, 20 at a time: the second page is full, so it cannot be known to
        // be the last. Stopping here would silently hide a 41st trip.
        val source = pagingSource(FakeHistoryRepository(summaries = summaryRows(40)))

        val page = source.load(append(key = 20, loadSize = 20)).page()

        assertEquals(20, page.data.size)
        assertEquals(40, page.nextKey)
    }

    @Test
    fun `a short page ends the list`() = runTest {
        val source = pagingSource(FakeHistoryRepository(summaries = summaryRows(25)))

        val page = source.load(append(key = 20, loadSize = 20)).page()

        assertEquals(5, page.data.size)
        assertNull(page.nextKey)
    }

    @Test
    fun `an empty page past the end ends the list`() = runTest {
        val source = pagingSource(FakeHistoryRepository(summaries = summaryRows(40)))

        val page = source.load(append(key = 40, loadSize = 20)).page()

        assertTrue(page.data.isEmpty())
        assertNull(page.nextKey)
    }

    @Test
    fun `an empty history loads one empty page and stops`() = runTest {
        val source = pagingSource(FakeHistoryRepository())

        val page = source.load(refresh(loadSize = 20)).page()

        assertTrue(page.data.isEmpty())
        assertNull(page.nextKey)
        assertNull(page.prevKey)
    }

    @Test
    fun `paging through the whole history yields every trip exactly once`() = runTest {
        // The boundary arithmetic only really holds if walking it end to end covers
        // the history with no gap and no repeat.
        val source = pagingSource(FakeHistoryRepository(summaries = summaryRows(45)))

        val seen = mutableListOf<String>()
        var key: Int? = null
        do {
            val page = (key?.let { source.load(append(it, loadSize = 20)) }
                ?: source.load(refresh(loadSize = 20))).page()
            seen += page.data.map { it.id }
            key = page.nextKey
        } while (key != null)

        assertEquals(List(45) { "trip-$it" }, seen)
    }

    @Test
    fun `a failed load surfaces the error to Paging`() = runTest {
        val boom = IOException("network down")
        val source = pagingSource(FakeHistoryRepository(error = boom))

        val result = source.load(refresh(loadSize = 20))

        assertTrue(
            "expected Error but was $result",
            result is PagingSource.LoadResult.Error,
        )
        assertSame(boom, (result as PagingSource.LoadResult.Error).throwable)
    }

    @Test
    fun `a refresh restarts at the newest trip`() = runTest {
        // A trip completed on the shopping-list tab is prepended, so anchoring a
        // refresh to the scroll position would shift every offset under the user.
        val source = pagingSource(FakeHistoryRepository(summaries = summaryRows(50)))

        val state = PagingState(
            pages = emptyList<PagingSource.LoadResult.Page<Int, PurchaseTripSummary>>(),
            anchorPosition = 30,
            config = PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0,
        )

        assertNull(source.getRefreshKey(state))
    }

    @Test
    fun `first load honours an initial load size larger than a page`() = runTest {
        val repository = FakeHistoryRepository(summaries = summaryRows(100))
        val source = pagingSource(repository)

        val page = source.load(refresh(loadSize = 60)).page()

        assertEquals(listOf(0 to 60), repository.requestedPages)
        // Advances by what arrived, not by the page size, so the next append does
        // not re-read rows this load already delivered.
        assertEquals(60, page.nextKey)
    }

    @Test
    fun `the source carries its filter into every page it requests`() = runTest {
        val repository = FakeHistoryRepository(summaries = summaryRows(60))
        val filter = HistoryFilter(storeIds = setOf("store-aldi"))
        val source = pagingSource(repository, filter)

        source.load(refresh(loadSize = 20))
        source.load(append(key = 20, loadSize = 20))

        // Every page, not just the first: a source's offsets are positions within its
        // own filter's results, so dropping the filter on append would page a
        // different list than the one the first page came from.
        assertEquals(2, repository.requestedQueries.size)
        assertTrue(repository.requestedQueries.all { it.storeIds == listOf("store-aldi") })
    }

    @Test
    fun `paging walks the filtered history and nothing else`() = runTest {
        // Two stores interleaved: ALDI on the even ids, Publix on the odd ones.
        val repository = FakeHistoryRepository(
            summaries = List(50) { index ->
                summaryRow(
                    id = "trip-$index",
                    storeId = if (index % 2 == 0) "store-aldi" else "store-publix",
                    purchasedAtEpoch = (50 - index).toLong(),
                )
            },
        )
        val source = pagingSource(repository, HistoryFilter(storeIds = setOf("store-aldi")))

        val seen = mutableListOf<String>()
        var key: Int? = null
        do {
            val page = (key?.let { source.load(append(it, loadSize = 20)) }
                ?: source.load(refresh(loadSize = 20))).page()
            seen += page.data.map { it.id }
            key = page.nextKey
        } while (key != null)

        // Every ALDI trip exactly once, in order, across page boundaries — the case
        // that breaks when filtering happens after paging instead of before it.
        assertEquals(List(25) { "trip-${it * 2}" }, seen)
    }
}
