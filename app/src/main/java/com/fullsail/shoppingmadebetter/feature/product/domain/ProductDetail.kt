package com.fullsail.shoppingmadebetter.feature.product.domain

/**
 * A product as the detail screen shows it: the catalog record, plus how the current
 * user stands on it in the pantry.
 * @param quantityOnHand total across every lot of this product, 0 when none is held.
 * @param expiresInDays days until the soonest-expiring lot (negative = overdue,
 *   0 = today), `null` when nothing is held or no lot carries a date.
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
    val quantityOnHand: Int,
    val expiresInDays: Int?,
    val lowStockThreshold: Int? = null,
)
