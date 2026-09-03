package com.fullsail.shoppingmadebetter.feature.pantry.domain

import kotlinx.datetime.LocalDate

data class InventoryItem(
    val id: String,
    val productId: String,
    val name: String,
    val brand: String,
    val description: String,
    val size: String,
    val imageUrl: String,
    val quantity: Int,
    val expiresInDays: Int?,
    val location: PantryLocation = PantryLocation.Pantry,
    val lowStockThreshold: Int? = null,
    /** True when the quantity is an unconfirmed auto-adjustment estimate. */
    val estimated: Boolean = false,
    val estimateSource: EstimateSource? = null,
)
