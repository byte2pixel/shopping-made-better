package com.fullsail.shoppingmadebetter.feature.pantry.data

import kotlinx.serialization.Serializable

@Serializable
data class InventoryItemDto(
    val id: String,
    val name: String,
    val brand: String,
    val description: String,
    val size: String,
    val quantity: Int,
    val imageUrl: String)
