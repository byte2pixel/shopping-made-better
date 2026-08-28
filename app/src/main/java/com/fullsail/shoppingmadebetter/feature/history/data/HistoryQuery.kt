package com.fullsail.shoppingmadebetter.feature.history.data

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
)
