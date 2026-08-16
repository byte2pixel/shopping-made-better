package com.fullsail.shoppingmadebetter.feature.pantry.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Boundary tests for [stockLevel], the pure quantity/threshold to chip severity mapping. */
class StockLevelTest {

    @Test
    fun `zero or negative quantity is out, threshold irrelevant`() {
        assertEquals(StockLevel.Out, stockLevel(0, lowStockThreshold = 3))
        assertEquals(StockLevel.Out, stockLevel(0, lowStockThreshold = null))
        assertEquals(StockLevel.Out, stockLevel(-1, lowStockThreshold = 3))
    }

    @Test
    fun `quantity from one up to the threshold is low`() {
        assertEquals(StockLevel.Low, stockLevel(1, lowStockThreshold = 3))
        assertEquals(StockLevel.Low, stockLevel(2, lowStockThreshold = 3))
        // The threshold value itself still counts as low.
        assertEquals(StockLevel.Low, stockLevel(3, lowStockThreshold = 3))
    }

    @Test
    fun `quantity above the threshold is ok`() {
        assertEquals(StockLevel.Ok, stockLevel(4, lowStockThreshold = 3))
        assertEquals(StockLevel.Ok, stockLevel(100, lowStockThreshold = 3))
    }

    @Test
    fun `positive quantity without a threshold is ok, never low`() {
        assertEquals(StockLevel.Ok, stockLevel(1, lowStockThreshold = null))
        assertEquals(StockLevel.Ok, stockLevel(50, lowStockThreshold = null))
    }
}
