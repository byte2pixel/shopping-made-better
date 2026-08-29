package com.fullsail.shoppingmadebetter.feature.history.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Unit tests for [GetTripCostComparisonUseCaseImpl]. */
class GetTripCostComparisonUseCaseTest {

    private fun useCase(repository: FakeSpendRepository) =
        GetTripCostComparisonUseCaseImpl(repository)

    @Test
    fun `stores come back cheapest first`() = runTest {
        val repository = FakeSpendRepository(
            tripCosts = listOf(
                costRow(purchaseId = "t-1", storeId = "s-1", storeName = "ALDI", costHere = 44.0),
                costRow(purchaseId = "t-1", storeId = "s-2", storeName = "Publix", costHere = 40.0),
            ),
        )

        val output = useCase(repository).execute("t-1")

        assertTrue(output is GetTripCostComparisonUseCase.Output.Success)
        assertEquals(
            listOf("Publix", "ALDI"),
            (output as GetTripCostComparisonUseCase.Output.Success).stores.map { it.storeName },
        )
    }

    @Test
    fun `only the asked-for trip is compared`() = runTest {
        val repository = FakeSpendRepository(
            tripCosts = listOf(
                costRow(purchaseId = "t-1", storeId = "s-1"),
                costRow(purchaseId = "t-1", storeId = "s-2"),
                costRow(purchaseId = "t-2", storeId = "s-1"),
                costRow(purchaseId = "t-2", storeId = "s-2"),
            ),
        )

        val output = useCase(repository).execute("t-1")

        assertEquals(
            2,
            (output as GetTripCostComparisonUseCase.Output.Success).stores.size,
        )
    }

    @Test
    fun `an unknown trip compares nothing`() = runTest {
        val output = useCase(FakeSpendRepository()).execute("missing")

        assertTrue((output as GetTripCostComparisonUseCase.Output.Success).stores.isEmpty())
    }

    @Test
    fun `a failure is reported rather than thrown`() = runTest {
        val output = useCase(FakeSpendRepository(error = IOException("no network")))
            .execute("t-1")

        assertTrue(output is GetTripCostComparisonUseCase.Output.Failure)
    }
}
