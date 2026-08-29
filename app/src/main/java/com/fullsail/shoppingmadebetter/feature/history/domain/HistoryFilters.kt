package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.feature.history.data.HistoryQuery

/**
 * Turning a [HistoryFilter] into the query that answers it.
 *
 * These are pure functions over plain values — no Android, no coroutines, no
 * network — so the filtering rules can be tested on their own, apart from both
 * the UI and Postgres. That is the same split `PantryFilters.kt` makes; what
 * differs is where the result is applied. The pantry filters a list it already
 * holds in memory, while History is paged from the server, so these rules have to
 * reach the database rather than a loaded list. A trip on page 5 is still a match.
 */

/**
 * Whether anything is being filtered out right now.
 *
 * The empty list means two different things and this is what tells them apart:
 * with no filter active the user has no purchases yet, and with one active their
 * purchases simply do not match.
 */
internal val HistoryFilter.isActive: Boolean
    get() = storeIds.isNotEmpty()

/**
 * This filter as the summary view's query.
 *
 * The set becomes a list only to fix an order for the request; the ids OR-join, so
 * which order they go in makes no difference to the result.
 */
internal fun HistoryFilter.toQuery(): HistoryQuery = HistoryQuery(
    storeIds = storeIds.toList(),
)
