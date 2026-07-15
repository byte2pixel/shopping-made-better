package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingTripDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCaseImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class GetShoppingTripsUseCaseTest {

    private class FakeRepo(
        private val result: List<ShoppingTripDto> = emptyList(),
        private val error: Throwable? = null,
    ) : ShoppingListRepository {
        override suspend fun getTrips() = error?.let { throw it } ?: result
    }

    @Test
    fun `maps DTOs to domain trips on success`() = runTest {
        val dtos = listOf(
            ShoppingTripDto("l1", "ALDI Weekly", "s1", "ALDI", itemCount = 5, totalCost = 12.34),
        )
        val useCase = GetShoppingTripsUseCaseImpl(FakeRepo(result = dtos))

        val out = useCase.execute(Unit)

        assertTrue(out is GetShoppingTripsUseCase.Output.Success)
        val trips = (out as GetShoppingTripsUseCase.Output.Success).trips
        assertEquals(1, trips.size)
        assertEquals("ALDI", trips[0].storeName)
        assertEquals(5, trips[0].itemCount)
        assertEquals(12.34, trips[0].totalCost, 0.001)
    }

    @Test
    fun `returns Failure when the repository throws`() = runTest {
        val boom = IOException("network down")
        val out = GetShoppingTripsUseCaseImpl(FakeRepo(error = boom)).execute(Unit)
        assertTrue(out is GetShoppingTripsUseCase.Output.Failure)
        assertSame(boom, (out as GetShoppingTripsUseCase.Output.Failure).error)
    }
}
