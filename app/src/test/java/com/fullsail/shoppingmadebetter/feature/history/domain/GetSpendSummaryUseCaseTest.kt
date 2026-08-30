package com.fullsail.shoppingmadebetter.feature.history.domain

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Unit tests for [GetSpendSummaryUseCaseImpl] — which windows it asks for, and how
 * a month with no trips before it is reported.
 */
class GetSpendSummaryUseCaseTest {

    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-28T12:00:00Z")
    }
    private val today = fixedClock.todayIn(TimeZone.currentSystemDefault())
    private val thisMonth = today.startOfMonth()
    private val lastMonth = today.previousMonthStart()

    private fun useCase(repository: FakeSpendRepository) =
        GetSpendSummaryUseCaseImpl(repository, fixedClock)

    /** Runs the use case and unwraps a success, failing the test if it wasn't one. */
    private suspend fun summaryOf(repository: FakeSpendRepository): SpendSummary =
        when (val output = useCase(repository).execute(Unit)) {
            is GetSpendSummaryUseCase.Output.Success -> output.summary
            is GetSpendSummaryUseCase.Output.Failure -> error("expected success, got ${output.error}")
        }

    @Test
    fun `both months are reported`() = runTest {
        val summary = summaryOf(
            FakeSpendRepository(
                months = listOf(
                    monthRow(thisMonth, total = 100.0, tripCount = 3),
                    monthRow(lastMonth, total = 80.0, tripCount = 2),
                ),
            ),
        )

        assertEquals(100.0, summary.thisMonth.total, EPSILON)
        assertEquals(80.0, summary.lastMonth?.total ?: 0.0, EPSILON)
    }

    @Test
    fun `a first month has no previous month`() = runTest {
        // The hero card drops its delta pill rather than showing a 100% rise.
        val summary = summaryOf(
            FakeSpendRepository(months = listOf(monthRow(thisMonth, total = 100.0))),
        )

        assertNull(summary.lastMonth)
    }

    @Test
    fun `this month is broken down by store`() = runTest {
        val summary = summaryOf(
            FakeSpendRepository(
                months = listOf(
                    monthRow(thisMonth, storeId = "s-1", storeName = "ALDI", total = 60.0),
                    monthRow(thisMonth, storeId = "s-2", storeName = "Publix", total = 40.0),
                    monthRow(lastMonth, storeId = "s-1", storeName = "ALDI", total = 99.0),
                ),
            ),
        )

        assertEquals(listOf("ALDI", "Publix"), summary.byStore.map { it.storeName })
        assertEquals(0.6, summary.byStore[0].share, EPSILON)
    }

    @Test
    fun `it asks for six months of history`() = runTest {
        val repository = FakeSpendRepository()
        summaryOf(repository)

        assertEquals(LocalDate(2026, 3, 1), repository.requestedSinceMonth)
    }

    @Test
    fun `it asks for a thirty day savings window including today`() = runTest {
        val repository = FakeSpendRepository()
        summaryOf(repository)

        assertEquals(LocalDate(2026, 7, 30), repository.requestedCostsSince)
    }

    @Test
    fun `a user with no history gets an empty summary`() = runTest {
        // The tab hides the whole section rather than showing a row of zeros.
        assertTrue(summaryOf(FakeSpendRepository()).isEmpty)
    }

    @Test
    fun `the cheapest store comes from the savings window`() = runTest {
        val summary = summaryOf(
            FakeSpendRepository(
                months = listOf(monthRow(thisMonth, total = 50.0)),
                tripCosts = listOf(
                    costRow(storeId = "s-1", storeName = "ALDI", costHere = 40.0, paidForSameItems = 50.0),
                    costRow(storeId = "s-2", storeName = "Publix", costHere = 48.0, paidForSameItems = 50.0),
                ),
            ),
        )

        assertEquals("ALDI", summary.cheapest?.storeName)
        assertEquals(10.0, summary.cheapest?.saving ?: 0.0, EPSILON)
    }

    @Test
    fun `trips older than the window are not compared`() = runTest {
        val summary = summaryOf(
            FakeSpendRepository(
                tripCosts = listOf(
                    costRow(purchasedOn = LocalDate(2026, 1, 1), storeId = "s-1"),
                    costRow(purchasedOn = LocalDate(2026, 1, 1), storeId = "s-2"),
                ),
            ),
        )

        assertNull(summary.cheapest)
    }

    @Test
    fun `a failure is reported rather than thrown`() = runTest {
        val output = useCase(FakeSpendRepository(error = IOException("no network")))
            .execute(Unit)

        assertTrue(output is GetSpendSummaryUseCase.Output.Failure)
    }

    private companion object {
        const val EPSILON = 0.001
    }
}
