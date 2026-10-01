package com.fullsail.shoppingmadebetter.feature.product.domain

import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem

/**
 * A product as the detail screen shows it: the catalog record, plus the household's
 * pantry lots of it.
 * @param lots every lot of this product in the pantry, soonest expiry first (undated
 *   last); empty when nobody holds it.
 * @param lowStockThreshold the user's "warn me when running low" level for this
 *   product, `null` when unset. Stored per user + product, so it outlives the lots.
 */
data class ProductDetail(
    val id: String,
    val name: String,
    val brand: String,
    val description: String,
    val size: String,
    val imageUrl: String,
    val lots: List<InventoryItem>,
    val lowStockThreshold: Int? = null,
) {
    /** Total across every lot, 0 when none is held. */
    val quantityOnHand: Int
        get() = lots.sumOf { it.quantity }

    /** Days until the soonest-expiring lot (negative = overdue, 0 = today), `null` when no lot carries a date. */
    val expiresInDays: Int?
        get() = lots.mapNotNull { it.expiresInDays }.minOrNull()
}
