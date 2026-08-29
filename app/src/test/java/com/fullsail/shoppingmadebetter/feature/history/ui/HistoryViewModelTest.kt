package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseHistoryUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryDatePreset
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryFilter
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTripSummary
import com.fullsail.shoppingmadebetter.feature.history.domain.isActive
import com.fullsail.shoppingmadebetter.feature.history.domain.rangeFrom
import com.fullsail.shoppingmadebetter.feature.history.domain.searchTerm
import com.fullsail.shoppingmadebetter.feature.stores.domain.GetStoresUseCase
import com.fullsail.shoppingmadebetter.feature.stores.domain.Store
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Unit tests for [HistoryViewModel] — the filter state the pager is built from.
 *
 * Collaborators are hand-written fakes. The clock is fixed so a date preset resolves
 * to the same range every run, and every test shares [MainDispatcherRule]'s scheduler
 * with `runTest`, so the search debounce runs on virtual time and can be advanced
 * deliberately rather than waited out.
 *
 * These cover what the emulator could not: that a filter survives the process being
 * killed, and that a pause in typing is what commits a search.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-28T12:00:00Z")
    }
    private val today = fixedClock.todayIn(TimeZone.currentSystemDefault())

    private class FakeGetPurchaseHistoryUseCase : GetPurchaseHistoryUseCase {
        /** Every filter a page was asked for, in order. */
        val filters = mutableListOf<HistoryFilter>()

        override suspend fun execute(
            input: GetPurchaseHistoryUseCase.Input,
        ): GetPurchaseHistoryUseCase.Output {
            filters += input.filter
            return GetPurchaseHistoryUseCase.Output.Success(emptyList(), endReached = true)
        }
    }

    private class FakeGetStoresUseCase(
        private val output: GetStoresUseCase.Output =
            GetStoresUseCase.Output.Success(listOf(store("s-1"), store("s-2"))),
    ) : GetStoresUseCase {
        override suspend fun execute(input: Unit) = output
    }

    private fun viewModel(
        savedState: SavedStateHandle = SavedStateHandle(),
        stores: GetStoresUseCase = FakeGetStoresUseCase(),
        history: GetPurchaseHistoryUseCase = FakeGetPurchaseHistoryUseCase(),
    ) = HistoryViewModel(history, stores, savedState, fixedClock)

    /** Runs on the dispatcher rule's scheduler, so virtual time is shared. */
    private fun vmTest(body: suspend TestScope.() -> Unit) =
        runTest(mainDispatcherRule.testDispatcher.scheduler, testBody = body)

    // ---- stores -------------------------------------------------------------

    @Test
    fun `stores load for the filter chips`() = vmTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf("s-1", "s-2"), viewModel.stores.value.map { it.id })
    }

    @Test
    fun `a failed store load leaves the chips empty rather than erroring`() = vmTest {
        // The row collapses and the unfiltered history still reads normally: losing
        // the filter is worth less than an error screen over a list that loaded fine.
        val viewModel = viewModel(
            stores = FakeGetStoresUseCase(GetStoresUseCase.Output.Failure(IOException("no network"))),
        )

        advanceUntilIdle()

        assertTrue(viewModel.stores.value.isEmpty())
    }

    // ---- stores filter ------------------------------------------------------

    @Test
    fun `the tab opens on no filter at all`() = vmTest {
        val viewModel = viewModel()

        assertEquals(HistoryFilter(), viewModel.filter.value)
        assertFalse(viewModel.filter.value.isActive)
    }

    @Test
    fun `toggling a store adds it and toggling again removes it`() = vmTest {
        val viewModel = viewModel()

        viewModel.toggleStore("s-1")
        assertEquals(setOf("s-1"), viewModel.filter.value.storeIds)

        viewModel.toggleStore("s-1")
        assertEquals(emptySet<String>(), viewModel.filter.value.storeIds)
    }

    @Test
    fun `a second store widens the filter rather than replacing it`() = vmTest {
        val viewModel = viewModel()

        viewModel.toggleStore("s-1")
        viewModel.toggleStore("s-2")

        assertEquals(setOf("s-1", "s-2"), viewModel.filter.value.storeIds)
    }

    // ---- date filter --------------------------------------------------------

    @Test
    fun `a preset resolves against the clock and reads back as itself`() = vmTest {
        val viewModel = viewModel()

        viewModel.selectDatePreset(HistoryDatePreset.Last30Days)

        val expected = HistoryDatePreset.Last30Days.rangeFrom(today)
        assertEquals(expected.start, viewModel.filter.value.from)
        assertEquals(expected.endInclusive, viewModel.filter.value.to)
        assertEquals(HistoryDatePreset.Last30Days, viewModel.selectedDatePreset.value)
    }

    @Test
    fun `tapping the active preset clears the range`() = vmTest {
        val viewModel = viewModel()

        viewModel.selectDatePreset(HistoryDatePreset.ThisYear)
        viewModel.selectDatePreset(HistoryDatePreset.ThisYear)

        assertNull(viewModel.filter.value.from)
        assertNull(viewModel.filter.value.to)
        assertNull(viewModel.selectedDatePreset.value)
    }

    @Test
    fun `a second preset replaces the first`() = vmTest {
        // Single-select, unlike the stores: a trip falls in one range.
        val viewModel = viewModel()

        viewModel.selectDatePreset(HistoryDatePreset.Last30Days)
        viewModel.selectDatePreset(HistoryDatePreset.ThisYear)

        assertEquals(HistoryDatePreset.ThisYear, viewModel.selectedDatePreset.value)
        assertEquals(HistoryDatePreset.ThisYear.rangeFrom(today).start, viewModel.filter.value.from)
    }

    @Test
    fun `a hand-picked range belongs to no preset`() = vmTest {
        val viewModel = viewModel()

        viewModel.setCustomRange(LocalDate(2026, 8, 19), LocalDate(2026, 8, 21))

        assertEquals(LocalDate(2026, 8, 19), viewModel.filter.value.from)
        assertEquals(LocalDate(2026, 8, 21), viewModel.filter.value.to)
        assertNull(viewModel.selectedDatePreset.value)
    }

    // ---- search -------------------------------------------------------------

    @Test
    fun `the field updates immediately but the filter waits for a pause`() = vmTest {
        val viewModel = viewModel()

        viewModel.setSearch("oats")

        // The field is what the user is looking at, so it cannot lag.
        assertEquals("oats", viewModel.searchInput.value)
        assertEquals("", viewModel.filter.value.search)

        advanceUntilIdle()
        assertEquals("oats", viewModel.filter.value.search)
    }

    @Test
    fun `a pause shorter than the debounce commits nothing`() = vmTest {
        val viewModel = viewModel()

        viewModel.setSearch("oats")
        advanceTimeBy(200.milliseconds)

        assertEquals("", viewModel.filter.value.search)
    }

    @Test
    fun `typing through the debounce commits only the last text`() = vmTest {
        val viewModel = viewModel()

        // Each keystroke restarts the wait, so the half-typed words never travel.
        viewModel.setSearch("o")
        advanceTimeBy(100.milliseconds)
        viewModel.setSearch("oa")
        advanceTimeBy(100.milliseconds)
        viewModel.setSearch("oats")
        advanceUntilIdle()

        assertEquals("oats", viewModel.filter.value.search)
    }

    @Test
    fun `a single character never becomes a query term`() = vmTest {
        val viewModel = viewModel()

        viewModel.setSearch("o")
        advanceUntilIdle()

        // It reaches the filter, but not as something to send — and the filter must
        // not read as active, or the empty list would say "no trips match".
        assertEquals("o", viewModel.filter.value.search)
        assertNull(viewModel.filter.value.searchTerm())
        assertFalse(viewModel.filter.value.isActive)
    }

    @Test
    fun `a letter typed and deleted inside one pause never reaches the filter`() = vmTest {
        val viewModel = viewModel()
        val seen = mutableListOf<HistoryFilter>()
        val job = launch { viewModel.filter.toList(seen) }

        viewModel.setSearch("o")
        viewModel.setSearch("")
        advanceUntilIdle()
        job.cancel()

        // One emission, the initial one: the debounce coalesced both keystrokes, so
        // the pager was never rebuilt for text the user had already taken back.
        assertEquals(listOf(HistoryFilter()), seen)
    }

    // ---- combining and clearing --------------------------------------------

    @Test
    fun `filters of different kinds hold at once`() = vmTest {
        val viewModel = viewModel()

        viewModel.toggleStore("s-1")
        viewModel.selectDatePreset(HistoryDatePreset.ThisYear)
        viewModel.setSearch("oats")
        advanceUntilIdle()

        val filter = viewModel.filter.value
        assertEquals(setOf("s-1"), filter.storeIds)
        assertEquals(HistoryDatePreset.ThisYear.rangeFrom(today).start, filter.from)
        assertEquals("oats", filter.search)
        assertTrue(filter.isActive)
    }

    @Test
    fun `clearing drops every kind of filter at once`() = vmTest {
        val viewModel = viewModel()
        viewModel.toggleStore("s-1")
        viewModel.selectDatePreset(HistoryDatePreset.ThisYear)
        viewModel.setSearch("oats")
        advanceUntilIdle()

        viewModel.clearFilters()

        // Immediately, without waiting out the debounce: the list must not stay
        // filtered after the user asked for it not to be.
        assertEquals(HistoryFilter(), viewModel.filter.value)
        assertEquals("", viewModel.searchInput.value)
        assertFalse(viewModel.filter.value.isActive)

        // And the pending commit must not put the term back afterwards.
        advanceUntilIdle()
        assertEquals(HistoryFilter(), viewModel.filter.value)
    }

    // ---- surviving death ----------------------------------------------------

    @Test
    fun `every filter is restored from saved state`() = vmTest {
        // What a rotation or a background kill looks like: a new ViewModel over the
        // handle the old one wrote. None of this is held in the composition.
        val saved = SavedStateHandle(
            mapOf(
                "history-filter-store-ids" to listOf("s-2"),
                "history-filter-from" to "2026-08-01",
                "history-filter-to" to "2026-08-28",
                "history-filter-search" to "oats",
            ),
        )

        val viewModel = viewModel(savedState = saved)

        val filter = viewModel.filter.value
        assertEquals(setOf("s-2"), filter.storeIds)
        assertEquals(LocalDate(2026, 8, 1), filter.from)
        assertEquals(LocalDate(2026, 8, 28), filter.to)
        assertEquals("oats", filter.search)
        // The field has to come back filled in, or the list would look filtered by
        // nothing the user can see.
        assertEquals("oats", viewModel.searchInput.value)
    }

    @Test
    fun `a restored range still reads back as its preset`() = vmTest {
        val range = HistoryDatePreset.Last3Months.rangeFrom(today)
        val saved = SavedStateHandle(
            mapOf(
                "history-filter-from" to range.start.toString(),
                "history-filter-to" to range.endInclusive.toString(),
            ),
        )

        val viewModel = viewModel(savedState = saved)

        assertEquals(HistoryDatePreset.Last3Months, viewModel.selectedDatePreset.value)
    }

    // ---- the pager ----------------------------------------------------------

    @Test
    fun `a filter change builds a new pager`() = vmTest {
        // The whole reason the filter is a StateFlow: offsets are positions within
        // one filter's results, so a change has to start a new source, not reuse one.
        val viewModel = viewModel()
        val pagers = mutableListOf<PagingData<PurchaseTripSummary>>()
        val job: Job = launch { viewModel.trips.toList(pagers) }
        advanceUntilIdle()
        val before = pagers.size

        viewModel.toggleStore("s-1")
        advanceUntilIdle()
        job.cancel()

        assertTrue("expected a new pager, saw $before then ${pagers.size}", pagers.size > before)
    }

    private companion object {
        fun store(id: String) = Store(id, "Store $id", "1 Main St", "Orlando", "FL", "32801", null)
    }
}
