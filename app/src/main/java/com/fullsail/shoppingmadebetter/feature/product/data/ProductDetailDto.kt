package com.fullsail.shoppingmadebetter.feature.product.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * One row of the `product_details` view: the catalog record plus the current user's
 * pantry position for it. [quantity] is the total on hand across every lot (0 when the
 * product isn't held) and [expiryDate] is the soonest-expiring lot, if any.
 */
@Serializable
data class ProductDetailDto(
    val id: String,
    val name: String,
    val brand: String,
    val description: String,
    val size: String,
    val imageUrl: String,
    val quantity: Int,
    val expiryDate: LocalDate?,
    val lowStockThreshold: Int? = null,
)
