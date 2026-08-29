package com.fullsail.shoppingmadebetter.feature.history.domain

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the History filter rules — [isActive], [toQuery], the date presets
 * and the picker's millis conversion: the pure translation from what the user
 * picked into what the summary view is asked.
 *
 * These run without Postgres or Compose on purpose: the rules are where filtering
 * can quietly go wrong, and they are worth pinning down apart from both. Every date
 * test resolves against a fixed [TODAY] rather than a clock, so a preset's bounds
 * are asserted outright instead of recomputed by the assertion.
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

    @Test
    fun `a filter with only dates is active`() {
        // Every filter kind has to reach isActive, or an empty list under a date
        // filter would read as "no purchases yet".
        assertTrue(HistoryFilter(from = LocalDate(2026, 8, 1)).isActive)
        assertTrue(HistoryFilter(to = LocalDate(2026, 8, 28)).isActive)
    }

    @Test
    fun `both date bounds reach the query`() {
        val filter = HistoryFilter(from = LocalDate(2026, 8, 1), to = LocalDate(2026, 8, 28))

        val query = filter.toQuery()

        assertEquals(LocalDate(2026, 8, 1), query.from)
        assertEquals(LocalDate(2026, 8, 28), query.to)
    }

    @Test
    fun `one bound alone stays one-sided`() {
        // "Everything since August" is a real filter; the missing end must not be
        // filled in with today, which would hide a trip dated in the future.
        val query = HistoryFilter(from = LocalDate(2026, 8, 1)).toQuery()

        assertEquals(LocalDate(2026, 8, 1), query.from)
        assertNull(query.to)
    }

    @Test
    fun `a store and a date travel in one query`() {
        val filter = HistoryFilter(
            storeIds = setOf("store-aldi"),
            from = LocalDate(2026, 8, 1),
            to = LocalDate(2026, 8, 28),
        )

        val query = filter.toQuery()

        // Kinds AND together, so both have to survive the trip down.
        assertEquals(listOf("store-aldi"), query.storeIds)
        assertEquals(LocalDate(2026, 8, 1), query.from)
        assertEquals(LocalDate(2026, 8, 28), query.to)
    }

    @Test
    fun `last 30 days is a 30-day window ending today`() {
        val range = HistoryDatePreset.Last30Days.rangeFrom(TODAY)

        // Today counts as one of the 30, so the window opens 29 days back.
        assertEquals(LocalDate(2026, 7, 30), range.start)
        assertEquals(TODAY, range.endInclusive)
    }

    @Test
    fun `last 3 months counts calendar months back`() {
        val range = HistoryDatePreset.Last3Months.rangeFrom(TODAY)

        assertEquals(LocalDate(2026, 5, 28), range.start)
        assertEquals(TODAY, range.endInclusive)
    }

    @Test
    fun `last 3 months crosses a year boundary`() {
        val range = HistoryDatePreset.Last3Months.rangeFrom(LocalDate(2027, 1, 15))

        assertEquals(LocalDate(2026, 10, 15), range.start)
    }

    @Test
    fun `this year starts on January 1st`() {
        val range = HistoryDatePreset.ThisYear.rangeFrom(TODAY)

        assertEquals(LocalDate(2026, 1, 1), range.start)
        assertEquals(TODAY, range.endInclusive)
    }

    @Test
    fun `this year is a single day on January 1st`() {
        // The year has one day in it so far. An empty or backwards range here would
        // match nothing at all.
        val range = HistoryDatePreset.ThisYear.rangeFrom(LocalDate(2026, 1, 1))

        assertEquals(LocalDate(2026, 1, 1), range.start)
        assertEquals(LocalDate(2026, 1, 1), range.endInclusive)
    }

    @Test
    fun `a preset's own range reads back as that preset`() {
        // This is what highlights a chip, so every preset has to survive the round
        // trip through the dates it resolved to.
        HistoryDatePreset.entries.forEach { preset ->
            val range = preset.rangeFrom(TODAY)
            val filter = HistoryFilter(from = range.start, to = range.endInclusive)

            assertEquals(preset, filter.selectedPreset(TODAY))
        }
    }

    @Test
    fun `a hand-picked range matches no preset`() {
        val filter = HistoryFilter(from = LocalDate(2026, 8, 19), to = TODAY)

        assertNull(filter.selectedPreset(TODAY))
        assertTrue(filter.hasCustomRange(preset = null))
    }

    @Test
    fun `an unfiltered date range is neither a preset nor custom`() {
        val filter = HistoryFilter()

        assertNull(filter.selectedPreset(TODAY))
        assertFalse(filter.hasCustomRange(preset = null))
    }

    @Test
    fun `a date survives the picker's millis round trip`() {
        HistoryDatePreset.entries.forEach { preset ->
            val range = preset.rangeFrom(TODAY)

            assertEquals(range.start, localDateFromUtcMillis(range.start.toUtcMillis()))
        }
    }

    @Test
    fun `a millis value inside a day lands on that day`() {
        // The picker hands back start-of-day, but flooring is what keeps any other
        // instant on its own date instead of rounding it forward.
        val noon = TODAY.toUtcMillis() + 12 * 60 * 60 * 1000

        assertEquals(TODAY, localDateFromUtcMillis(noon))
    }

    @Test
    fun `a pre-epoch date survives the round trip`() {
        // Negative millis are where a plain division would land a day late.
        val date = LocalDate(1969, 7, 20)

        assertEquals(date, localDateFromUtcMillis(date.toUtcMillis()))
    }

    private companion object {
        /** Fixed, never a real clock — presets have to be deterministic. */
        val TODAY = LocalDate(2026, 8, 28)
    }
}
