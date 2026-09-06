package com.fullsail.shoppingmadebetter.feature.pantry.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [GetPantryEstimateAlertsUseCaseImpl]. */
class GetPantryEstimateAlertsUseCaseImplTest {

    private val useCase = GetPantryEstimateAlertsUseCaseImpl()

    private fun lot(
        id: String,
        productId: String = "p1",
        quantity: Int = 0,
        reason: AdjustmentReason? = AdjustmentReason.Auto,
        expiresInDays: Int? = null,
    ) = InventoryItem(
        id = id,
        productId = productId,
        name = "name-$productId",
        brand = "brand",
        description = "desc",
        size = "size",
        imageUrl = "",
        quantity = quantity,
        expiresInDays = expiresInDays,
        lastAdjustmentReason = reason,
    )

    private suspend fun alertsFor(vararg lots: InventoryItem) =
        useCase.execute(GetPantryEstimateAlerts(groupInventoryByProduct(lots.toList())))

    @Test
    fun `an auto-adjusted lot at zero is alerted`() = runTest {
        assertEquals(listOf("a"), alertsFor(lot("a")).map { it.id })
    }

    @Test
    fun `an auto-adjusted lot still in stock is not alerted`() = runTest {
        assertTrue(alertsFor(lot("a", quantity = 2)).isEmpty())
    }

    @Test
    fun `a lot at zero is not alerted unless its latest reason is auto`() = runTest {
        val reasons = listOf(
            AdjustmentReason.Confirmed,
            AdjustmentReason.Dismissed,
            AdjustmentReason.Manual,
            AdjustmentReason.Undo,
            null,
        )
        reasons.forEach { reason ->
            assertTrue("$reason", alertsFor(lot("a", reason = reason)).isEmpty())
        }
    }

    @Test
    fun `alerts across several groups come back in display order`() = runTest {
        val alerts = alertsFor(
            lot("late", productId = "p2", expiresInDays = 5),
            lot("soon", productId = "p1", expiresInDays = 1),
            lot("stocked", productId = "p1", quantity = 3, expiresInDays = 2),
            lot("soon2", productId = "p1", expiresInDays = 3),
        )

        assertEquals(listOf("soon", "soon2", "late"), alerts.map { it.id })
    }

    @Test
    fun `an empty pantry yields no alerts`() = runTest {
        assertTrue(alertsFor().isEmpty())
    }
}
