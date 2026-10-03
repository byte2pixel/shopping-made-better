package com.fullsail.shoppingmadebetter.feature.product.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * One row of the `product_details` view: the catalog record plus the household's pantry
 * position for it. [lowStockThreshold] is the current user's own setting. [quantity]
 * (total on hand) and [expiryDate] (soonest lot) still come back from the view but the
 * app derives both from the lots instead.
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
