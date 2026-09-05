package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.feature.pantry.data.AdjustmentDigestEntryDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryAdjustmentResultDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant

class GetAdjustmentDigestUseCaseImplTest {

    private class FakePantryRepository(
        private val entries: List<AdjustmentDigestEntryDto> = emptyList(),
        private val error: Throwable? = null,
    ) : PantryRepository {
        override suspend fun getInventoryItems(): List<InventoryItemDto> = emptyList()
        override suspend fun deleteInventoryItem(id: String) = Unit
        override suspend fun updateLocation(id: String, location: String) = Unit
        override suspend fun updateExpiry(id: String, expiresAt: LocalDate) = Unit
        override suspend fun updateLowStockThreshold(productId: String, threshold: Int?) = Unit
        override suspend fun applyInventoryAdjustment(id: String, delta: Int, reason: String) =
            InventoryAdjustmentResultDto(inventoryItemId = id, delta = 0.0, newQuantity = 0.0)
        override suspend fun undoInventoryAdjustment(adjustmentId: String) =
            InventoryAdjustmentResultDto(inventoryItemId = "", delta = 0.0, newQuantity = 0.0)
        override suspend fun getAdjustmentDigest(): List<AdjustmentDigestEntryDto> =
            error?.let { throw it } ?: entries
    }

    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-09-04T12:00:00Z")
    }
    private val timeZone = TimeZone.currentSystemDefault()
    private val today = fixedClock.todayIn(timeZone)

    /** Midday local, [daysAgo] days before today, so a DST shift cannot move the date. */
    private fun epochDaysAgo(daysAgo: Int): Long =
        today.minus(daysAgo, DateTimeUnit.DAY).atTime(12, 0).toInstant(timeZone).epochSeconds

    private fun dto(
        adjustmentId: String = "a1",
        inventoryItemId: String = "lot1",
        estimateSource: String? = "history",
        createdAtEpoch: Long = epochDaysAgo(0),
    ) = AdjustmentDigestEntryDto(
        adjustmentId = adjustmentId,
        inventoryItemId = inventoryItemId,
        productId = "p1",
        productName = "Jasmine Rice",
        imageUrl = "http://img/rice.png",
        delta = -1,
        quantityNow = 2,
        productQuantity = 4,
        lowStockThreshold = 3,
        estimateSource = estimateSource,
        createdAtEpoch = createdAtEpoch,
    )

    private fun GetAdjustmentDigestUseCase.Output.entries(): List<AdjustmentDigestEntry> {
        assertTrue(this is GetAdjustmentDigestUseCase.Output.Success)
        return (this as GetAdjustmentDigestUseCase.Output.Success).entries
    }

    @Test
    fun `execute maps every field of a digest row`() = runTest {
        val repo = FakePantryRepository(entries = listOf(dto()))
        val useCase = GetAdjustmentDigestUseCaseImpl(repo, fixedClock)

        val entry = useCase.execute(Unit).entries().single()

        assertEquals("a1", entry.adjustmentId)
        assertEquals("lot1", entry.lotId)
        assertEquals("p1", entry.productId)
        assertEquals("Jasmine Rice", entry.productName)
        assertEquals("http://img/rice.png", entry.imageUrl)
        assertEquals(-1, entry.delta)
        assertEquals(2, entry.quantityNow)
        assertEquals(4, entry.productQuantity)
        assertEquals(3, entry.lowStockThreshold)
        assertEquals(EstimateSource.History, entry.source)
    }

    @Test
    fun `execute maps the estimate source, unknown and missing both to null`() = runTest {
        val dtos = listOf(
            dto(adjustmentId = "history", estimateSource = "history"),
            dto(adjustmentId = "shelfLife", estimateSource = "shelf_life"),
            dto(adjustmentId = "manual", estimateSource = "manual"),
            dto(adjustmentId = "none", estimateSource = null),
            dto(adjustmentId = "bogus", estimateSource = "guesswork"),
        )
        val useCase = GetAdjustmentDigestUseCaseImpl(FakePantryRepository(entries = dtos), fixedClock)

        val bySource = useCase.execute(Unit).entries().associateBy { it.adjustmentId }

        assertEquals(EstimateSource.History, bySource.getValue("history").source)
        assertEquals(EstimateSource.ShelfLife, bySource.getValue("shelfLife").source)
        assertEquals(EstimateSource.Manual, bySource.getValue("manual").source)
        assertNull(bySource.getValue("none").source)
        assertNull(bySource.getValue("bogus").source)
    }

    @Test
    fun `execute derives daysAgo from the row timestamp relative to today`() = runTest {
        val dtos = listOf(
            dto(adjustmentId = "today", createdAtEpoch = epochDaysAgo(0)),
            dto(adjustmentId = "yesterday", createdAtEpoch = epochDaysAgo(1)),
            dto(adjustmentId = "threeDays", createdAtEpoch = epochDaysAgo(3)),
        )
        val useCase = GetAdjustmentDigestUseCaseImpl(FakePantryRepository(entries = dtos), fixedClock)

        val byId = useCase.execute(Unit).entries().associateBy { it.adjustmentId }

        assertEquals(0, byId.getValue("today").daysAgo)
        assertEquals(1, byId.getValue("yesterday").daysAgo)
        assertEquals(3, byId.getValue("threeDays").daysAgo)
    }

    @Test
    fun `execute keeps the order the view returned`() = runTest {
        val dtos = listOf(
            dto(adjustmentId = "newest", createdAtEpoch = epochDaysAgo(0)),
            dto(adjustmentId = "middle", createdAtEpoch = epochDaysAgo(2)),
            dto(adjustmentId = "oldest", createdAtEpoch = epochDaysAgo(5)),
        )
        val useCase = GetAdjustmentDigestUseCaseImpl(FakePantryRepository(entries = dtos), fixedClock)

        val ids = useCase.execute(Unit).entries().map { it.adjustmentId }

        assertEquals(listOf("newest", "middle", "oldest"), ids)
    }

    @Test
    fun `execute returns an empty list when nothing was adjusted this week`() = runTest {
        val useCase = GetAdjustmentDigestUseCaseImpl(FakePantryRepository(), fixedClock)

        assertTrue(useCase.execute(Unit).entries().isEmpty())
    }

    @Test
    fun `execute returns Failure carrying the error when the repository throws`() = runTest {
        val boom = IOException("network down")
        val useCase = GetAdjustmentDigestUseCaseImpl(FakePantryRepository(error = boom), fixedClock)

        val output = useCase.execute(Unit)

        assertTrue(output is GetAdjustmentDigestUseCase.Output.Failure)
        assertSame(boom, (output as GetAdjustmentDigestUseCase.Output.Failure).error)
    }
}
