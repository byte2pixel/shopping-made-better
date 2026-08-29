package com.fullsail.shoppingmadebetter.feature.history.domain

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [GetPurchaseHistoryUseCaseImpl] using a hand-written
 * [FakeHistoryRepository] that really slices its rows by offset and limit.
 */
class GetPurchaseHistoryUseCaseTest {

    private fun useCase(repository: FakeHistoryRepository) =
        GetPurchaseHistoryUseCaseImpl(repository)

    /** The success output; fails the test when the output was not a success. */
    private fun GetPurchaseHistoryUseCase.Output.success():
        GetPurchaseHistoryUseCase.Output.Success {
        assertTrue(
            "expected Success but was $this",
            this is GetPurchaseHistoryUseCase.Output.Success,
        )
        return this as GetPurchaseHistoryUseCase.Output.Success
    }

    private suspend fun GetPurchaseHistoryUseCaseImpl.page(offset: Int, limit: Int) =
        execute(GetPurchaseHistoryUseCase.Input(offset = offset, limit = limit))

    @Test
    fun `execute maps a summary row onto a trip`() = runTest {
        val repository = FakeHistoryRepository(
            summaries = listOf(
                summaryRow(
                    id = "trip-1",
                    purchasedOn = LocalDate(2026, 8, 19),
                    purchasedAtEpoch = 1_787_109_229L,
                    storeName = "ALDI",
                    totalAmount = 42.32,
                    lineTotal = 40.00,
                    itemCount = 4,
                ),
            ),
        )

        val trip = useCase(repository).page(offset = 0, limit = 20).success().trips.single()

        assertEquals("trip-1", trip.id)
        assertEquals(LocalDate(2026, 8, 19), trip.purchasedOn)
        assertEquals(1_787_109_229L, trip.purchasedAtEpoch)
        assertEquals("ALDI", trip.storeName)
        assertEquals(42.32, trip.recordedTotal!!, DELTA)
        assertEquals(40.00, trip.lineTotal, DELTA)
        assertEquals(4, trip.itemCount)
    }

    @Test
    fun `execute passes offset and limit straight through to the repository`() = runTest {
        val repository = FakeHistoryRepository(summaries = summaryRows(50))

        useCase(repository).page(offset = 20, limit = 20)

        assertEquals(listOf(20 to 20), repository.requestedPages)
    }

    @Test
    fun `execute returns the requested window, newest first`() = runTest {
        val repository = FakeHistoryRepository(summaries = summaryRows(50))

        val trips = useCase(repository).page(offset = 20, limit = 5).success().trips

        assertEquals(
            listOf("trip-20", "trip-21", "trip-22", "trip-23", "trip-24"),
            trips.map { it.id },
        )
    }

    @Test
    fun `execute reports more to load when the page comes back full`() = runTest {
        val repository = FakeHistoryRepository(summaries = summaryRows(50))

        val output = useCase(repository).page(offset = 0, limit = 20).success()

        assertEquals(20, output.trips.size)
        assertFalse(output.endReached)
    }

    @Test
    fun `execute reports the end when the page comes back short`() = runTest {
        val repository = FakeHistoryRepository(summaries = summaryRows(25))

        val output = useCase(repository).page(offset = 20, limit = 20).success()

        assertEquals(5, output.trips.size)
        assertTrue(output.endReached)
    }

    @Test
    fun `execute does not call the last exactly-full page the end`() = runTest {
        // 40 trips read 20 at a time: the second page is full, so there is no way to
        // know it was the last one. Claiming the end here would hide a 41st trip.
        val useCase = useCase(FakeHistoryRepository(summaries = summaryRows(40)))

        val secondPage = useCase.page(offset = 20, limit = 20).success()

        assertEquals(20, secondPage.trips.size)
        assertFalse(secondPage.endReached)
    }

    @Test
    fun `execute ends on the empty page after an exactly-full last page`() = runTest {
        val useCase = useCase(FakeHistoryRepository(summaries = summaryRows(40)))

        val pastTheEnd = useCase.page(offset = 40, limit = 20).success()

        assertTrue(pastTheEnd.trips.isEmpty())
        assertTrue(pastTheEnd.endReached)
    }

    @Test
    fun `execute returns an empty end-reached page when there is no history`() = runTest {
        val output = useCase(FakeHistoryRepository()).page(offset = 0, limit = 20).success()

        assertTrue(output.trips.isEmpty())
        assertTrue(output.endReached)
    }

    @Test
    fun `execute prefers the recorded total over the sum of the lines`() = runTest {
        // A recorded total that disagrees with the lines must survive: it is what the
        // user actually paid, and recomputing it silently would hide the mismatch.
        val repository = FakeHistoryRepository(
            summaries = listOf(summaryRow(id = "trip-1", totalAmount = 99.0, lineTotal = 5.0)),
        )

        val trip = useCase(repository).page(offset = 0, limit = 20).success().trips.single()

        assertEquals(99.0, trip.total, DELTA)
        assertEquals(99.0, trip.recordedTotal!!, DELTA)
    }

    @Test
    fun `execute falls back to the line total when no total was recorded`() = runTest {
        val repository = FakeHistoryRepository(
            summaries = listOf(summaryRow(id = "trip-1", totalAmount = null, lineTotal = 10.0)),
        )

        val trip = useCase(repository).page(offset = 0, limit = 20).success().trips.single()

        assertNull(trip.recordedTotal)
        assertEquals(10.0, trip.total, DELTA)
    }

    @Test
    fun `execute maps a deleted store to a null store name`() = runTest {
        val repository = FakeHistoryRepository(
            summaries = listOf(summaryRow(id = "trip-1", storeName = null)),
        )

        val trip = useCase(repository).page(offset = 0, limit = 20).success().trips.single()

        assertNull(trip.storeName)
    }

    @Test
    fun `execute maps a trip recorded without items to an empty card`() = runTest {
        val repository = FakeHistoryRepository(
            summaries = listOf(
                summaryRow(id = "trip-1", totalAmount = null, lineTotal = 0.0, itemCount = 0),
            ),
        )

        val trip = useCase(repository).page(offset = 0, limit = 20).success().trips.single()

        assertEquals(0, trip.itemCount)
        assertEquals(0.0, trip.total, DELTA)
    }

    @Test
    fun `execute returns failure carrying the repository error`() = runTest {
        val boom = IOException("network down")
        val useCase = useCase(FakeHistoryRepository(error = boom))

        val output = useCase.page(offset = 0, limit = 20)

        assertTrue(
            "expected Failure but was $output",
            output is GetPurchaseHistoryUseCase.Output.Failure,
        )
        assertSame(boom, (output as GetPurchaseHistoryUseCase.Output.Failure).error)
    }

    @Test
    fun `the filter reaches the repository as a query`() = runTest {
        val repository = FakeHistoryRepository(summaries = summaryRows(3))

        useCase(repository).execute(
            GetPurchaseHistoryUseCase.Input(
                offset = 0,
                limit = 20,
                filter = HistoryFilter(storeIds = setOf("store-aldi")),
            ),
        )

        // The narrowing has to travel all the way down. Stopping short and filtering
        // the returned page would only ever search the trips already loaded.
        assertEquals(listOf("store-aldi"), repository.requestedQueries.single().storeIds)
    }

    @Test
    fun `an unfiltered read sends an empty query`() = runTest {
        val repository = FakeHistoryRepository(summaries = summaryRows(3))

        useCase(repository).page(offset = 0, limit = 20)

        // Empty rather than every id: the repository leaves the filter off the
        // request entirely, which is not the same as asking for all known stores.
        assertEquals(emptyList<String>(), repository.requestedQueries.single().storeIds)
    }

    @Test
    fun `a filtered read returns only matching trips`() = runTest {
        val repository = FakeHistoryRepository(
            summaries = listOf(
                summaryRow(id = "aldi-1", storeId = "store-aldi"),
                summaryRow(id = "publix-1", storeId = "store-publix"),
                summaryRow(id = "aldi-2", storeId = "store-aldi"),
            ),
        )

        val output = useCase(repository).execute(
            GetPurchaseHistoryUseCase.Input(
                offset = 0,
                limit = 20,
                filter = HistoryFilter(storeIds = setOf("store-aldi")),
            ),
        ).success()

        assertEquals(listOf("aldi-1", "aldi-2"), output.trips.map { it.id })
    }

    @Test
    fun `two stores widen the result to both`() = runTest {
        val repository = FakeHistoryRepository(
            summaries = listOf(
                summaryRow(id = "aldi-1", storeId = "store-aldi"),
                summaryRow(id = "publix-1", storeId = "store-publix"),
                summaryRow(id = "wf-1", storeId = "store-wf"),
            ),
        )

        val output = useCase(repository).execute(
            GetPurchaseHistoryUseCase.Input(
                offset = 0,
                limit = 20,
                filter = HistoryFilter(storeIds = setOf("store-aldi", "store-publix")),
            ),
        ).success()

        // A trip happens at one store, so selecting a second can only ever widen the
        // list. AND-joining them would empty it every time.
        assertEquals(listOf("aldi-1", "publix-1"), output.trips.map { it.id })
    }

    @Test
    fun `endReached is judged on the filtered result, not the whole history`() = runTest {
        val repository = FakeHistoryRepository(
            summaries = listOf(
                summaryRow(id = "aldi-1", storeId = "store-aldi"),
                summaryRow(id = "publix-1", storeId = "store-publix"),
                summaryRow(id = "publix-2", storeId = "store-publix"),
            ),
        )

        val output = useCase(repository).execute(
            GetPurchaseHistoryUseCase.Input(
                offset = 0,
                limit = 2,
                filter = HistoryFilter(storeIds = setOf("store-aldi")),
            ),
        ).success()

        // One ALDI trip against a limit of 2: the filtered history is exhausted even
        // though two unfiltered trips remain behind it.
        assertEquals(1, output.trips.size)
        assertTrue(output.endReached)
    }

    @Test
    fun `a date range reaches the repository as a query`() = runTest {
        val repository = FakeHistoryRepository(summaries = summaryRows(3))

        useCase(repository).execute(
            GetPurchaseHistoryUseCase.Input(
                offset = 0,
                limit = 20,
                filter = HistoryFilter(
                    from = LocalDate(2026, 8, 1),
                    to = LocalDate(2026, 8, 28),
                ),
            ),
        )

        val query = repository.requestedQueries.single()
        assertEquals(LocalDate(2026, 8, 1), query.from)
        assertEquals(LocalDate(2026, 8, 28), query.to)
    }

    @Test
    fun `a date-filtered read returns only trips in range`() = runTest {
        val repository = FakeHistoryRepository(
            summaries = listOf(
                summaryRow(id = "before", purchasedOn = LocalDate(2026, 7, 31)),
                summaryRow(id = "first-day", purchasedOn = LocalDate(2026, 8, 1)),
                summaryRow(id = "last-day", purchasedOn = LocalDate(2026, 8, 28)),
                summaryRow(id = "after", purchasedOn = LocalDate(2026, 8, 29)),
            ),
        )

        val output = useCase(repository).execute(
            GetPurchaseHistoryUseCase.Input(
                offset = 0,
                limit = 20,
                filter = HistoryFilter(
                    from = LocalDate(2026, 8, 1),
                    to = LocalDate(2026, 8, 28),
                ),
            ),
        ).success()

        // Both ends inclusive: a trip made on the first or last day of the range is
        // in it, which is what a user picking those days means.
        assertEquals(listOf("first-day", "last-day"), output.trips.map { it.id })
    }

    @Test
    fun `a store and a date range narrow together`() = runTest {
        val repository = FakeHistoryRepository(
            summaries = listOf(
                summaryRow(id = "aldi-in", storeId = "store-aldi", purchasedOn = LocalDate(2026, 8, 12)),
                summaryRow(id = "aldi-out", storeId = "store-aldi", purchasedOn = LocalDate(2026, 7, 1)),
                summaryRow(id = "publix-in", storeId = "store-publix", purchasedOn = LocalDate(2026, 8, 12)),
            ),
        )

        val output = useCase(repository).execute(
            GetPurchaseHistoryUseCase.Input(
                offset = 0,
                limit = 20,
                filter = HistoryFilter(
                    storeIds = setOf("store-aldi"),
                    from = LocalDate(2026, 8, 1),
                ),
            ),
        ).success()

        // Filter kinds AND: the ALDI trip outside the range and the in-range Publix
        // trip each fail one half.
        assertEquals(listOf("aldi-in"), output.trips.map { it.id })
    }

    @Test
    fun `endReached is judged on the date-filtered result`() = runTest {
        val repository = FakeHistoryRepository(
            summaries = listOf(
                summaryRow(id = "in-range", purchasedOn = LocalDate(2026, 8, 12)),
                summaryRow(id = "older-1", purchasedOn = LocalDate(2026, 1, 5)),
                summaryRow(id = "older-2", purchasedOn = LocalDate(2026, 1, 4)),
            ),
        )

        val output = useCase(repository).execute(
            GetPurchaseHistoryUseCase.Input(
                offset = 0,
                limit = 2,
                filter = HistoryFilter(from = LocalDate(2026, 8, 1)),
            ),
        ).success()

        assertEquals(1, output.trips.size)
        assertTrue(output.endReached)
    }

    @Test
    fun `a search reaches the repository as an escaped term`() = runTest {
        val repository = FakeHistoryRepository(summaries = summaryRows(3))

        useCase(repository).execute(
            GetPurchaseHistoryUseCase.Input(
                offset = 0,
                limit = 20,
                filter = HistoryFilter(search = " 100% oats "),
            ),
        )

        assertEquals("100\\% oats", repository.requestedQueries.single().productSearch)
    }

    @Test
    fun `a searched read returns only trips that bought it`() = runTest {
        val repository = FakeHistoryRepository(
            summaries = listOf(
                summaryRow(id = "oats", productSearch = "Minute Oats Robertsons"),
                summaryRow(id = "cheese", productSearch = "Havarti Cheese Slices Cracker Barrel"),
                summaryRow(id = "both", productSearch = "Steel Cut Oats Havarti Cheese"),
            ),
        )

        val output = useCase(repository).execute(
            GetPurchaseHistoryUseCase.Input(
                offset = 0,
                limit = 20,
                filter = HistoryFilter(search = "oats"),
            ),
        ).success()

        // Case-insensitive, and matching mid-string — the column runs every item's
        // title and brand together, so a term is almost never at its start.
        assertEquals(listOf("oats", "both"), output.trips.map { it.id })
    }

    @Test
    fun `a search combines with a store and a date`() = runTest {
        val repository = FakeHistoryRepository(
            summaries = listOf(
                summaryRow(
                    id = "wanted",
                    storeId = "store-aldi",
                    purchasedOn = LocalDate(2026, 8, 12),
                    productSearch = "Minute Oats",
                ),
                summaryRow(
                    id = "wrong-store",
                    storeId = "store-publix",
                    purchasedOn = LocalDate(2026, 8, 12),
                    productSearch = "Minute Oats",
                ),
                summaryRow(
                    id = "wrong-product",
                    storeId = "store-aldi",
                    purchasedOn = LocalDate(2026, 8, 12),
                    productSearch = "Havarti Cheese",
                ),
            ),
        )

        val output = useCase(repository).execute(
            GetPurchaseHistoryUseCase.Input(
                offset = 0,
                limit = 20,
                filter = HistoryFilter(
                    storeIds = setOf("store-aldi"),
                    from = LocalDate(2026, 8, 1),
                    search = "oats",
                ),
            ),
        ).success()

        assertEquals(listOf("wanted"), output.trips.map { it.id })
    }

    private companion object {
        /** Money and quantities are Doubles; compare them with a tolerance. */
        const val DELTA = 0.0001
    }
}
