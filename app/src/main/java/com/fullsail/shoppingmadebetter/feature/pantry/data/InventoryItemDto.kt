package com.fullsail.shoppingmadebetter.feature.pantry.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class InventoryItemDto(
    val id: String,
    val productId: String,
    val name: String,
    val brand: String,
    val description: String,
    val size: String,
    val quantity: Int,
    val imageUrl: String,
    val expiryDate: LocalDate?,
    val location: String = "pantry",
    val lowStockThreshold: Int? = null,
    /** When the estimator last adjusted this lot, epoch seconds; `null` if it never has. */
    val lastAutoAdjustedAtEpoch: Long? = null,
    /** What the estimate was based on: `history`, `shelf_life` or `manual`. */
    val estimateSource: String? = null,
)
