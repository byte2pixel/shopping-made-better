package com.fullsail.shoppingmadebetter.feature.pantry.domain

/**
 * Every pantry lot of one product, collapsed into a single card's worth of data.
 *
 * The pantry view returns one row per `inventory_items` row, re-buying a product
 * creates several rows for it. Create [lots]: each keeps its own quantity,
 * expiry and location, and per-lot edits still address [InventoryItem.id]. The derived
 * totals below are what a product-level card header shows.
 *
 * [lots] is ordered by soonest expiry, undated last, and is never empty.
 */
data class ProductGroup(
    val productId: String,
    val name: String,
    val brand: String,
    val description: String,
    val size: String,
    val imageUrl: String,
    val lots: List<InventoryItem>,
) {
    /** How many are on hand across every lot. Drives the header and the stock filters. */
    val totalQuantity: Int = lots.sumOf { it.quantity }

    /** The soonest expiry among the dated lots, or `null` when no lot is dated. */
    val earliestExpiresInDays: Int? = lots.mapNotNull { it.expiresInDays }.minOrNull()

    /** The distinct places this product is stored; more than one means it's split up. */
    val locations: Set<PantryLocation> = lots.mapTo(LinkedHashSet()) { it.location }

    /** The one location holding this product, or `null` when its lots are mixed. */
    val singleLocation: PantryLocation? = locations.singleOrNull()

    /**
     * The low-stock threshold for this product (`null` when unset). It is a per-product
     * setting, so every lot carries the same value, can just use the first.
     */
    val lowStockThreshold: Int? = lots.first().lowStockThreshold
}

/** Soonest expiry first; lots with no expiry date sort last. */
private val byExpiry: Comparator<Int?> = nullsLast(naturalOrder())

/**
 * Collapses [items] into one [ProductGroup] per [InventoryItem.productId].
 *
 * Lots inside a group are ordered by soonest expiry (undated last), and the groups
 * themselves by [ProductGroup.earliestExpiresInDays] the same way, so the list keeps
 * the expiry-ascending order the pantry list is built around. The product's display
 * fields are taken from its first lot.
 */
fun groupInventoryByProduct(items: List<InventoryItem>): List<ProductGroup> =
    items.groupBy { it.productId }
        .map { (productId, lots) ->
            val sortedLots = lots.sortedWith(compareBy(byExpiry) { it.expiresInDays })
            val first = sortedLots.first()
            ProductGroup(
                productId = productId,
                name = first.name,
                brand = first.brand,
                description = first.description,
                size = first.size,
                imageUrl = first.imageUrl,
                lots = sortedLots,
            )
        }
        .sortedWith(compareBy(byExpiry) { it.earliestExpiresInDays })
