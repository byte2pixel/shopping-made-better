package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.FakeShoppingListRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingTripDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCaseImpl
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import kotlin.time.Instant

class GetShoppingTripsUseCaseTest {

    private class FakeRepo(
        private val result: List<ShoppingTripDto> = emptyList(),
        private val error: Throwable? = null,
    ) : FakeShoppingListRepository() {
        override suspend fun getTrips() = error?.let { throw it } ?: result
    }

    @Test
    fun `maps DTOs to domain trips on success`() = runTest {
        val dtos = listOf(
            ShoppingTripDto(
                "l1",
                "ALDI Weekly",
                "s1",
                "ALDI",
                itemCount = 5,
                totalCost = 12.34,
                sortOrder = 0,
                createdDate = Instant.parse("2026-09-01T12:00:00Z"),
                updatedDate = Instant.parse("2026-09-01T12:00:00Z"),
            ),
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
    fun `maps a housemate's list through as not own`() = runTest {
        val dtos = listOf(
            ShoppingTripDto(
                "l2",
                "Whole Foods Weekly",
                "s2",
                "Whole Foods",
                itemCount = 3,
                totalCost = 20.0,
                sortOrder = 1,
                createdDate = Instant.parse("2026-09-01T12:00:00Z"),
                updatedDate = Instant.parse("2026-09-01T12:00:00Z"),
                createdBy = "Demo Roommate",
                isOwn = false,
            ),
        )

        val out = GetShoppingTripsUseCaseImpl(FakeRepo(result = dtos)).execute(Unit)

        val trips = (out as GetShoppingTripsUseCase.Output.Success).trips
        assertEquals("Demo Roommate", trips[0].createdBy)
        assertFalse(trips[0].isOwn)
    }

    // The view has returned created_by and is_own since SCRUM-286, but the defaults keep
    // an older payload decoding, and an unattributed list reads as the viewer's own.
    @Test
    fun `a DTO without the attribution fields is own and unattributed`() = runTest {
        val dto = Json.decodeFromString<ShoppingTripDto>(
            """
            {
              "shopping_list_id": "l1",
              "list_name": "ALDI Weekly",
              "store_id": "s1",
              "store_name": "ALDI",
              "item_count": 5,
              "total_cost": 12.34,
              "sort_order": 0,
              "created_at": "2026-09-01T12:00:00Z",
              "updated_at": "2026-09-01T12:00:00Z"
            }
            """.trimIndent()
        )

        val out = GetShoppingTripsUseCaseImpl(FakeRepo(result = listOf(dto))).execute(Unit)

        val trips = (out as GetShoppingTripsUseCase.Output.Success).trips
        assertNull(trips[0].createdBy)
        assertTrue(trips[0].isOwn)
    }

    @Test
    fun `returns Failure when the repository throws`() = runTest {
        val boom = IOException("network down")
        val out = GetShoppingTripsUseCaseImpl(FakeRepo(error = boom)).execute(Unit)
        assertTrue(out is GetShoppingTripsUseCase.Output.Failure)
        assertSame(boom, (out as GetShoppingTripsUseCase.Output.Failure).error)
    }
}
