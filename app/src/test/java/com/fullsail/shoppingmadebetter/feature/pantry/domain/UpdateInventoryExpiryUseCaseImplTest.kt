package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Unit tests for [UpdateInventoryExpiryUseCaseImpl], focused on the days→date
 * conversion done against a fixed [Clock].
 */
class UpdateInventoryExpiryUseCaseImplTest {

    /** Fake repository: records the expiry it was asked to persist, or throws [error]. */
    private class FakePantryRepository(
        private val error: Throwable? = null,
    ) : PantryRepository {
        var lastId: String? = null
        var lastExpiresAt: LocalDate? = null

        override suspend fun getInventoryItems(): List<InventoryItemDto> = emptyList()
        override suspend fun getInventoryItem(id: String): InventoryItemDto? = null
        override suspend fun deleteInventoryItem(id: String) = Unit
        override suspend fun updateQuantity(id: String, quantity: Int) = Unit
        override suspend fun updateLocation(id: String, location: String) = Unit
        override suspend fun updateExpiry(id: String, expiresAt: LocalDate) {
            error?.let { throw it }
            lastId = id
            lastExpiresAt = expiresAt
        }
    }

    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-08T12:00:00Z")
    }
    private val today = fixedClock.todayIn(TimeZone.currentSystemDefault())

    @Test
    fun `converts a positive day offset to today plus that many days`() = runTest {
        val repo = FakePantryRepository()
        val useCase = UpdateInventoryExpiryUseCaseImpl(repo, fixedClock)

        val out = useCase.execute(UpdateInventoryExpiry(id = "i1", expiresInDays = 7))

        assertTrue(out is UpdateInventoryExpiryUseCase.Output.Success)
        assertEquals("i1", repo.lastId)
        assertEquals(LocalDate(2026, 8, 15), repo.lastExpiresAt)
    }

    @Test
    fun `zero days maps to today`() = runTest {
        val repo = FakePantryRepository()
        val useCase = UpdateInventoryExpiryUseCaseImpl(repo, fixedClock)

        useCase.execute(UpdateInventoryExpiry(id = "i1", expiresInDays = 0))

        assertEquals(today, repo.lastExpiresAt)
    }

    @Test
    fun `negative days maps to a date in the past`() = runTest {
        val repo = FakePantryRepository()
        val useCase = UpdateInventoryExpiryUseCaseImpl(repo, fixedClock)

        useCase.execute(UpdateInventoryExpiry(id = "i1", expiresInDays = -2))

        assertEquals(LocalDate(2026, 8, 6), repo.lastExpiresAt)
    }

    @Test
    fun `returns Failure when the repository throws`() = runTest {
        val repo = FakePantryRepository(error = IOException("boom"))
        val useCase = UpdateInventoryExpiryUseCaseImpl(repo, fixedClock)

        val out = useCase.execute(UpdateInventoryExpiry(id = "i1", expiresInDays = 3))

        assertTrue(out is UpdateInventoryExpiryUseCase.Output.Failure)
    }
}
