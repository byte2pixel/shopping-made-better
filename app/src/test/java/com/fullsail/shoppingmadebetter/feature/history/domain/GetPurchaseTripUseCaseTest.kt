package com.fullsail.shoppingmadebetter.feature.history.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [GetPurchaseTripUseCaseImpl] using a hand-written
 * [FakeHistoryRepository].
 */
class GetPurchaseTripUseCaseTest {

    @Test
    fun `execute returns the requested trip with its line items`() = runTest {
        val useCase = GetPurchaseTripUseCaseImpl(
            FakeHistoryRepository(
                rows = listOf(
                    row(purchaseId = "wanted", id = "a", storeName = "Publix"),
                    row(purchaseId = "wanted", id = "b", storeName = "Publix"),
                    row(purchaseId = "other", id = "c"),
                ),
            ),
        )

        val output = useCase.execute("wanted")

        assertTrue("expected Success but was $output", output is GetPurchaseTripUseCase.Output.Success)
        val trip = (output as GetPurchaseTripUseCase.Output.Success).trip
        assertEquals("wanted", trip.id)
        assertEquals("Publix", trip.storeName)
        assertEquals(listOf("a", "b"), trip.items.map { it.id })
    }

    @Test
    fun `execute returns not found when no trip matches`() = runTest {
        val useCase = GetPurchaseTripUseCaseImpl(
            FakeHistoryRepository(rows = listOf(row(purchaseId = "other"))),
        )

        assertEquals(GetPurchaseTripUseCase.Output.NotFound, useCase.execute("missing"))
    }

    @Test
    fun `execute returns failure carrying the repository error`() = runTest {
        val boom = IOException("network down")
        val useCase = GetPurchaseTripUseCaseImpl(FakeHistoryRepository(error = boom))

        val output = useCase.execute("wanted")

        assertTrue("expected Failure but was $output", output is GetPurchaseTripUseCase.Output.Failure)
        assertSame(boom, (output as GetPurchaseTripUseCase.Output.Failure).error)
    }
}
