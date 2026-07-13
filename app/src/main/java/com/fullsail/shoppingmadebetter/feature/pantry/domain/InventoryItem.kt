package com.fullsail.shoppingmadebetter.feature.pantry.domain

data class InventoryItem(
    val id: String,
    val name: String,
    val brand: String,
    val description: String,
    val size: String,
    val imageUrl: String,
    val quantity: Int
)
