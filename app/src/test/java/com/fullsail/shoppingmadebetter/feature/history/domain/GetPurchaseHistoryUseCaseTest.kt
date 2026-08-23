package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.feature.history.data.PurchaseHistoryRowDto
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [GetPurchaseHistoryUseCaseImpl] using a hand-written
 * [FakeHistoryRepository].
 */
class GetPurchaseHistoryUseCaseTest {

    private fun useCase(vararg rows: PurchaseHistoryRowDto) =
        GetPurchaseHistoryUseCaseImpl(FakeHistoryRepository(rows = rows.toList()))

    /** The trips from a successful run; fails the test when the output was not a success. */
    private fun GetPurchaseHistoryUseCase.Output.trips(): List<PurchaseTrip> {
        assertTrue("expected Success but was $this", this is GetPurchaseHistoryUseCase.Output.Success)
        return (this as GetPurchaseHistoryUseCase.Output.Success).trips
    }

    @Test
    fun `execute maps a row onto a trip and its line item`() = runTest {
        val useCase = useCase(
            row(
                purchaseId = "trip-1",
                id = "line-1",
                purchasedOn = LocalDate(2026, 8, 19),
                storeName = "ALDI",
                totalAmount = 10.44,
                productId = "prod-1",
                productName = "Havarti Cheese Slices",
                brand = "Happy Farms",
                size = "200 g",
                imageUrl = "https://example.test/havarti.png",
                quantity = 2.0,
                pricePaid = 5.22,
            ),
        )

        val trip = useCase.execute(Unit).trips().single()

        assertEquals("trip-1", trip.id)
        assertEquals(LocalDate(2026, 8, 19), trip.purchasedOn)
        assertEquals("ALDI", trip.storeName)
        assertEquals(1, trip.itemCount)

        val item = trip.items.single()
        assertEquals("line-1", item.id)
        assertEquals("prod-1", item.productId)
        assertEquals("Havarti Cheese Slices", item.productName)
        assertEquals("Happy Farms", item.brand)
        assertEquals("200 g", item.size)
        assertEquals("https://example.test/havarti.png", item.imageUrl)
        assertEquals(2.0, item.quantity, DELTA)
        assertEquals(5.22, item.pricePaid, DELTA)
        assertEquals(10.44, item.lineTotal, DELTA)
    }

    @Test
    fun `execute groups rows into one trip per purchase, keeping line order`() = runTest {
        val useCase = useCase(
            row(purchaseId = "trip-1", id = "a"),
            row(purchaseId = "trip-1", id = "b"),
            row(purchaseId = "trip-1", id = "c"),
            row(purchaseId = "trip-2", id = "d", purchasedAtEpoch = 100L),
            row(purchaseId = "trip-2", id = "e", purchasedAtEpoch = 100L),
        )

        val trips = useCase.execute(Unit).trips().associateBy { it.id }

        assertEquals(2, trips.size)
        assertEquals(3, trips.getValue("trip-1").itemCount)
        assertEquals(2, trips.getValue("trip-2").itemCount)
        assertEquals(listOf("a", "b", "c"), trips.getValue("trip-1").items.map { it.id })
        assertEquals(listOf("d", "e"), trips.getValue("trip-2").items.map { it.id })
    }

    @Test
    fun `execute returns trips newest first even when rows arrive oldest first`() = runTest {
        val useCase = useCase(
            row(purchaseId = "oldest", purchasedAtEpoch = 100L),
            row(purchaseId = "newest", purchasedAtEpoch = 300L),
            row(purchaseId = "middle", purchasedAtEpoch = 200L),
        )

        assertEquals(
            listOf("newest", "middle", "oldest"),
            useCase.execute(Unit).trips().map { it.id },
        )
    }

    @Test
    fun `execute breaks a same-day tie by timestamp`() = runTest {
        val sameDay = LocalDate(2026, 8, 19)
        val useCase = useCase(
            row(purchaseId = "morning", purchasedOn = sameDay, purchasedAtEpoch = 1_000L),
            row(purchaseId = "evening", purchasedOn = sameDay, purchasedAtEpoch = 40_000L),
        )

        assertEquals(listOf("evening", "morning"), useCase.execute(Unit).trips().map { it.id })
    }

    @Test
    fun `execute keeps repository order for trips recorded at the same instant`() = runTest {
        val useCase = useCase(
            row(purchaseId = "first", purchasedAtEpoch = 500L),
            row(purchaseId = "second", purchasedAtEpoch = 500L),
        )

        assertEquals(listOf("first", "second"), useCase.execute(Unit).trips().map { it.id })
    }

    @Test
    fun `execute prefers the recorded total over the sum of the lines`() = runTest {
        // A recorded total that disagrees with the lines must survive: it is what the
        // user actually paid, and recomputing it silently would hide the mismatch.
        val useCase = useCase(
            row(purchaseId = "trip-1", id = "a", totalAmount = 99.0, quantity = 1.0, pricePaid = 2.0),
            row(purchaseId = "trip-1", id = "b", totalAmount = 99.0, quantity = 1.0, pricePaid = 3.0),
        )

        val trip = useCase.execute(Unit).trips().single()

        assertEquals(99.0, trip.total, DELTA)
        assertEquals(99.0, trip.recordedTotal!!, DELTA)
    }

    @Test
    fun `execute falls back to the line sum when no total was recorded`() = runTest {
        val useCase = useCase(
            row(purchaseId = "trip-1", id = "a", totalAmount = null, quantity = 2.0, pricePaid = 3.50),
            row(purchaseId = "trip-1", id = "b", totalAmount = null, quantity = 1.5, pricePaid = 2.00),
        )

        val trip = useCase.execute(Unit).trips().single()

        assertNull(trip.recordedTotal)
        assertEquals(10.0, trip.total, DELTA)
    }

    @Test
    fun `execute maps a deleted store to a null store name`() = runTest {
        val useCase = useCase(row(purchaseId = "trip-1", storeName = null))

        assertNull(useCase.execute(Unit).trips().single().storeName)
    }

    @Test
    fun `execute returns an empty success when there is no history`() = runTest {
        val useCase = GetPurchaseHistoryUseCaseImpl(FakeHistoryRepository())

        assertTrue(useCase.execute(Unit).trips().isEmpty())
    }

    @Test
    fun `execute returns failure carrying the repository error`() = runTest {
        val boom = IOException("network down")
        val useCase = GetPurchaseHistoryUseCaseImpl(FakeHistoryRepository(error = boom))

        val output = useCase.execute(Unit)

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
