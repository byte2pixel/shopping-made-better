package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ProductGroup

/**
 * Items with a known expiration date within this many days, including
 * already-expired ones, count as "expiring soon". Hard-coded for now; a future
 * task may let the user configure it (e.g. by long-pressing the dashboard card).
 */
internal const val EXPIRING_SOON_DAYS = 5

/**
 * Whether this lot's expiry chip reads as a warning — red, orange, or yellow
 * rather than gray.
 */
internal fun InventoryItem.isExpiringSoon(): Boolean =
    expiresInDays != null && expiryBucket(expiresInDays) != ExpiryBucket.Later

/**
 * Whether this product is running low: it has a low-stock threshold set and its
 * total across every lot sits between 1 and that threshold. A total of 0 is "out",
 * not "low", and a product without a threshold is never low.
 *
 * Stock is judged on the total, never a single lot.
 */
internal fun ProductGroup.isLowStock(): Boolean =
    stockLevel(totalQuantity, lowStockThreshold) == StockLevel.Low

/** Whether this product is out of stock: nothing on hand across any lot. */
internal fun ProductGroup.isOutOfStock(): Boolean =
    stockLevel(totalQuantity, lowStockThreshold) == StockLevel.Out

/**
 * Whether this product card survives the active dashboard [filters].
 *
 * Stock is a product-level fact — quantity totals across lots and the threshold is
 * per-product — so [PantryDashboardFilter.groupPredicate] is judged once against the
 * whole group. Expiry and location belong to a single lot, so every active
 * [PantryDashboardFilter.lotPredicate] category has to be satisfied by the *same*
 * lot: a freezer lot 60 days out plus a pantry lot due tomorrow is not "cold and
 * expiring soon".
 *
 * Filters that share a [PantryDashboardFilter.category] — the mutually-exclusive
 * location and stock-status groups — OR-join: a lot can only be in one location, and
 * a product in one stock state, so selecting Fridge + Freezer (or Running Low + Out)
 * widens the result to the union rather than emptying it. Distinct categories, and
 * every solo (category-less) filter such as Expiring, AND together. So Expiring +
 * Fridge + Freezer yields products with a cold lot that is expiring soon.
 *
 * With no active filter that has a predicate, every group matches.
 */
internal fun ProductGroup.matchesFilters(filters: Set<PantryDashboardFilter>): Boolean {
    val groupGroups = filters.orGroups { it.groupPredicate }
    if (!groupGroups.all { orGroup -> orGroup.any { predicate -> predicate(this) } }) return false

    val lotGroups = filters.orGroups { it.lotPredicate }
    if (lotGroups.isEmpty()) return true
    return lots.any { lot ->
        lotGroups.all { orGroup -> orGroup.any { predicate -> predicate(lot) } }
    }
}

/**
 * The active [selector] predicates bucketed into OR-groups: filters sharing a
 * [PantryDashboardFilter.category] land in one bucket, and a category-less filter
 * keys on itself so it buckets alone.
 */
private fun <T> Set<PantryDashboardFilter>.orGroups(
    selector: (PantryDashboardFilter) -> ((T) -> Boolean)?,
): Collection<List<(T) -> Boolean>> =
    mapNotNull { filter -> selector(filter)?.let { predicate -> (filter.category ?: filter) to predicate } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .values

/**
 * Narrows [groups] to the product cards to show for the active dashboard [filters].
 * A surviving card keeps every one of its lots and its true aggregates — filtering
 * decides which products are listed, never what a listed product contains.
 *
 * See [matchesFilters] for how a group is matched.
 */
internal fun applyPantryFilters(
    groups: List<ProductGroup>,
    filters: Set<PantryDashboardFilter>,
): List<ProductGroup> = groups.filter { it.matchesFilters(filters) }
