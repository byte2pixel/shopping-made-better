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

    private companion object {
        /** Money and quantities are Doubles; compare them with a tolerance. */
        const val DELTA = 0.0001
    }
}
