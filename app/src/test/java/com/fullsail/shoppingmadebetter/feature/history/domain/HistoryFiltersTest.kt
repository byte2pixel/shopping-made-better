package com.fullsail.shoppingmadebetter.feature.history.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the History filter rules — [isActive] and [toQuery], the pure
 * translation from what the user picked into what the summary view is asked.
 *
 * These run without Postgres or Compose on purpose: the rules are where filtering
 * can quietly go wrong, and they are worth pinning down apart from both.
 */
class HistoryFiltersTest {

    @Test
    fun `a default filter is not active`() {
        // What the tab opens on. Nothing is being hidden, so an empty list means the
        // user has no purchases — not that their purchases were filtered away.
        assertFalse(HistoryFilter().isActive)
    }

    @Test
    fun `a filter with a store is active`() {
        assertTrue(HistoryFilter(storeIds = setOf("store-aldi")).isActive)
    }

    @Test
    fun `a default filter queries every store`() {
        // Empty, not "all the ids": an empty list tells the repository to leave the
        // filter off the request entirely.
        assertEquals(emptyList<String>(), HistoryFilter().toQuery().storeIds)
    }

    @Test
    fun `selected stores reach the query`() {
        val query = HistoryFilter(storeIds = setOf("store-aldi")).toQuery().storeIds

        assertEquals(listOf("store-aldi"), query)
    }

    @Test
    fun `every selected store reaches the query`() {
        val filter = HistoryFilter(storeIds = setOf("store-aldi", "store-publix"))

        // Order is not part of the contract — the ids OR-join — so compare as sets.
        assertEquals(setOf("store-aldi", "store-publix"), filter.toQuery().storeIds.toSet())
    }

    @Test
    fun `the query carries no duplicates`() {
        // The set already guarantees this; asserted so a later change to a list-backed
        // filter cannot start sending the same id twice unnoticed.
        val filter = HistoryFilter(storeIds = setOf("store-aldi"))

        assertEquals(1, filter.toQuery().storeIds.size)
    }
}
