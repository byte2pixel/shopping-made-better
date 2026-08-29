package com.fullsail.shoppingmadebetter.feature.history.domain

/**
 * What the user has narrowed the History tab to, as they expressed it.
 *
 * Filters of different kinds AND together; the values within one kind OR-join,
 * the way the pantry's location cards do — picking a second store widens the
 * result to the union rather than emptying it, since a trip has only one store.
 *
 * A default instance means "show everything", which is what the tab opens on.
 * See [toQuery] for how this becomes something the summary view can be asked.
 */
data class HistoryFilter(
    /** Ids of the stores to keep; empty means every store. */
    val storeIds: Set<String> = emptySet(),
)
