package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.feature.history.data.HistoryQuery
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.minus

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
    get() = storeIds.isNotEmpty() || from != null || to != null || searchTerm() != null

/**
 * How many things this filter narrows on, for the collapsed panel's badge. Each
 * store counts separately so the badge matches the summary line; a date range counts
 * once whatever its bounds. Asks [searchTerm], so a half-typed letter counts for
 * nothing.
 */
internal val HistoryFilter.activeCount: Int
    get() = storeIds.size +
        (if (from != null || to != null) 1 else 0) +
        (if (searchTerm() != null) 1 else 0)

/**
 * This filter as the summary view's query.
 *
 * The set becomes a list only to fix an order for the request; the ids OR-join, so
 * which order they go in makes no difference to the result.
 */
internal fun HistoryFilter.toQuery(): HistoryQuery = HistoryQuery(
    storeIds = storeIds.toList(),
    from = from,
    to = to,
    productSearch = searchTerm(),
)

/**
 * The search text as something safe to hand to `LIKE`, or null when there is
 * nothing worth searching for.
 *
 * Below [MIN_SEARCH_LENGTH] characters this returns null rather than a pattern: a
 * single letter matches most of a history, so running it costs a request to say
 * nothing. That is also why [isActive] asks this rather than reading `search` —
 * a half-typed letter must not turn the empty list into "no trips match".
 *
 * `%` and `_` are wildcards to `LIKE`, and `\` escapes them, so all three are
 * escaped here — otherwise searching for a product literally named "100% Whole
 * Grains" would match anything starting with "100". The client does not do this
 * for us: outside a logical group it sends an `ilike` value through `toString()`
 * untouched, which is why the existing search at `ShoppingListRepositoryImpl`
 * over-matches. Backslash goes first, or it would escape its own replacements.
 */
internal fun HistoryFilter.searchTerm(): String? {
    val trimmed = search.trim()
    if (trimmed.length < MIN_SEARCH_LENGTH) return null
    return trimmed
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
}

/** Shortest search worth sending; below this the whole history would match. */
internal const val MIN_SEARCH_LENGTH = 2

/** The date ranges offered as one-tap chips, in the order they are shown. */
enum class HistoryDatePreset { Last30Days, Last3Months, ThisYear }

/**
 * This preset resolved against [today], both ends inclusive.
 *
 * [today] is a parameter rather than a clock read in here so the rules stay
 * deterministic in tests; the ViewModel supplies the real one.
 */
internal fun HistoryDatePreset.rangeFrom(today: LocalDate): LocalDateRange = when (this) {
    // 29, not 30: the window is 30 days long and today is one of them.
    HistoryDatePreset.Last30Days -> today.minus(DatePeriod(days = 29))..today
    HistoryDatePreset.Last3Months -> today.minus(DatePeriod(months = 3))..today
    HistoryDatePreset.ThisYear -> LocalDate(today.year, 1, 1)..today
}

/**
 * Which preset chip this filter's dates came from, or null for a hand-picked range.
 */
internal fun HistoryFilter.selectedPreset(today: LocalDate): HistoryDatePreset? =
    HistoryDatePreset.entries.firstOrNull { preset ->
        val range = preset.rangeFrom(today)
        from == range.start && to == range.endInclusive
    }

/** Whether the dates in force were picked by hand rather than by [preset]. */
internal fun HistoryFilter.hasCustomRange(preset: HistoryDatePreset?): Boolean =
    (from != null || to != null) && preset == null

/** A day as the UTC start-of-day milliseconds the M3 date pickers work in. */
internal fun LocalDate.toUtcMillis(): Long = toEpochDays() * MILLIS_PER_DAY

/**
 * The day [millis] falls on, reading it as UTC.
 */
internal fun localDateFromUtcMillis(millis: Long): LocalDate =
    LocalDate.fromEpochDays(Math.floorDiv(millis, MILLIS_PER_DAY))

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
