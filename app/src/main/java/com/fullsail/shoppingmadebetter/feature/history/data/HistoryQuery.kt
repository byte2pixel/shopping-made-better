package com.fullsail.shoppingmadebetter.feature.history.data

import kotlinx.datetime.LocalDate

/**
 * The narrowing to apply to a page of `purchase_history_summary`, in the shape the
 * view is queried in rather than the shape the user picked it in.
 *
 * This is to a PostgREST filter what a DTO is to a row: the wire-side type. The
 * domain's `HistoryFilter` maps onto it, so the repository never has to know what a
 * "date preset" or a too-short search term means — by the time a query reaches
 * here, every field is either a value to send or absent.
 *
 * An all-defaults query sends no filter at all and reads the whole history.
 */
data class HistoryQuery(
    /**
     * Match trips made at any of these stores; empty means every store. The ids
     * OR-join, so adding one widens the result rather than narrowing it.
     */
    val storeIds: List<String> = emptyList(),
    /**
     * Match trips on or after this date; null sends no lower bound. A `LocalDate`
     * rather than a string because that is the type `"purchasedOn"` decodes as —
     * this file is shaped like the DTO it narrows.
     */
    val from: LocalDate? = null,
    /** Match trips on or before this date; null sends no upper bound. */
    val to: LocalDate? = null,
    /**
     * Match trips whose products contain this text, case-insensitively; null sends
     * no search. Already trimmed, already long enough to be worth running, and
     * already escaped for `LIKE` — the repository only wraps it in wildcards.
     */
    val productSearch: String? = null,
)
