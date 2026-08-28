package com.fullsail.shoppingmadebetter.feature.history.domain

import kotlinx.datetime.LocalDate

/**
 * What the user has narrowed the History tab to.
 *
 * Filters of different kinds AND together; the values within one kind OR-join,
 * A default instance means "show everything", which is what the tab opens on.
 * See [toQuery] for how this becomes something the summary view can be asked.
 */
data class HistoryFilter(
    /** Ids of the stores to keep; empty means every store. */
    val storeIds: Set<String> = emptySet(),
    /**
     * Oldest trip date to keep, inclusive; null means no lower bound.
     */
    val from: LocalDate? = null,
    /** Newest trip date to keep, inclusive; null means no upper bound. */
    val to: LocalDate? = null,
    /**
     * Raw text to match against what was bought, exactly as typed. Blank means no
     * search, and so does anything shorter than two characters once trimmed — see
     * [searchTerm], which is the only thing that should read this.
     */
    val search: String = "",
)
