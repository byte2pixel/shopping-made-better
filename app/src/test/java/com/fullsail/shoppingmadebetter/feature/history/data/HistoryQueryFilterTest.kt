package com.fullsail.shoppingmadebetter.feature.history.data

import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [applyHistoryQuery]: the PostgREST parameters a [HistoryQuery]
 * actually turns into.
 *
 * `FakeHistoryRepository` re-implements the server's narrowing in Kotlin, so it can
 * only prove that a *correct* query narrows correctly — it never sees how the query
 * is encoded. These assert the encoding instead, against the same collapse the
 * client performs before sending: `params` is a value *list* per column, but only
 * the first of each is put on the wire, so a second filter on one column is
 * dropped in silence. That is exactly how a date range lost its upper bound and
 * ran open-ended.
 */
class HistoryQueryFilterTest {

    /** The parameters that would be sent, collapsed the way the client collapses them. */
    private fun sentParams(query: HistoryQuery): Map<String, String> {
        val builder = PostgrestFilterBuilder(PropertyConversionMethod.NONE)
        builder.applyHistoryQuery(query)
        return builder.params.mapValues { (_, values) -> values.first() }
    }

    @Test
    fun `an empty query sends no filter at all`() {
        assertEquals(emptyMap<String, String>(), sentParams(HistoryQuery()))
    }

    @Test
    fun `a store filter sends the ids`() {
        val sent = sentParams(HistoryQuery(storeIds = listOf("store-aldi", "store-publix")))

        val storeId = sent.getValue("storeId")
        assertTrue(storeId, storeId.contains("store-aldi"))
        assertTrue(storeId, storeId.contains("store-publix"))
    }

    @Test
    fun `both ends of a date range survive to the wire`() {
        // The regression. Two filters on one column collapse to the first, so a
        // gte/lte pair sends only the gte and every trip after the end date comes
        // back anyway. Both bounds have to reach the server together.
        val sent = sentParams(
            HistoryQuery(from = LocalDate(2026, 8, 18), to = LocalDate(2026, 8, 20)),
        )

        val encoded = sent.values.joinToString(" ")
        assertTrue("lower bound missing from $encoded", encoded.contains("gte.2026-08-18"))
        assertTrue("upper bound missing from $encoded", encoded.contains("lte.2026-08-20"))
    }

    @Test
    fun `a lower bound alone is sent alone`() {
        val sent = sentParams(HistoryQuery(from = LocalDate(2026, 8, 18)))

        val encoded = sent.values.joinToString(" ")
        assertTrue(encoded, encoded.contains("gte.2026-08-18"))
        assertTrue("unexpected upper bound in $encoded", !encoded.contains("lte."))
    }

    @Test
    fun `an upper bound alone is sent alone`() {
        val sent = sentParams(HistoryQuery(to = LocalDate(2026, 8, 20)))

        val encoded = sent.values.joinToString(" ")
        assertTrue(encoded, encoded.contains("lte.2026-08-20"))
        assertTrue("unexpected lower bound in $encoded", !encoded.contains("gte."))
    }

    @Test
    fun `a search is sent as a wrapped ilike`() {
        val sent = sentParams(HistoryQuery(productSearch = "oats"))

        // Wildcards belong to the repository; the term itself arrives escaped.
        assertEquals("ilike.%oats%", sent.getValue("productSearch"))
    }

    @Test
    fun `an escaped search term keeps its escapes on the wire`() {
        // The escaping is only worth anything if it survives encoding.
        val sent = sentParams(HistoryQuery(productSearch = "100\\% oats"))

        assertEquals("ilike.%100\\% oats%", sent.getValue("productSearch"))
    }

    @Test
    fun `all three filters are sent together`() {
        val sent = sentParams(
            HistoryQuery(
                storeIds = listOf("store-aldi"),
                from = LocalDate(2026, 8, 18),
                to = LocalDate(2026, 8, 20),
                productSearch = "oats",
            ),
        )

        assertTrue(sent.getValue("storeId").contains("store-aldi"))
        assertEquals("ilike.%oats%", sent.getValue("productSearch"))
        val encoded = sent.values.joinToString(" ")
        assertTrue(encoded, encoded.contains("gte.2026-08-18"))
        assertTrue(encoded, encoded.contains("lte.2026-08-20"))
    }

    @Test
    fun `a store and a date range are both sent`() {
        // Different columns, so these cannot collapse into each other — asserted so
        // that stays true if the date bounds are ever re-encoded.
        val sent = sentParams(
            HistoryQuery(
                storeIds = listOf("store-aldi"),
                from = LocalDate(2026, 8, 18),
                to = LocalDate(2026, 8, 20),
            ),
        )

        assertTrue(sent.getValue("storeId").contains("store-aldi"))
        val encoded = sent.values.joinToString(" ")
        assertTrue(encoded, encoded.contains("gte.2026-08-18"))
        assertTrue(encoded, encoded.contains("lte.2026-08-20"))
    }
}
