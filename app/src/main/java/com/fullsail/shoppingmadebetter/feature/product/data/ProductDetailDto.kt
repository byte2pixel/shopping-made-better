package com.fullsail.shoppingmadebetter.feature.product.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * One row of the `product_details` view: the catalog record plus the household's pantry
 * position for it. [quantity] is the total on hand across every household lot (0 when
 * nobody holds it), [expiryDate] is the soonest-expiring lot, if any, and
 * [lowStockThreshold] is the current user's own setting.
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
