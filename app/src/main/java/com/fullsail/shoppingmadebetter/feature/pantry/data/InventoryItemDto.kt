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
)
