package com.fullsail.shoppingmadebetter.feature.pantry.data

interface PantryRepository {
    suspend fun getInventoryItems(): List<InventoryItemDto>
}
