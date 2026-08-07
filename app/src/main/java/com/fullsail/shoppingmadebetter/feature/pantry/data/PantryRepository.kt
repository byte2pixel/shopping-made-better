package com.fullsail.shoppingmadebetter.feature.pantry.data

interface PantryRepository {
    suspend fun getInventoryItems(): List<InventoryItemDto>

    suspend fun getInventoryItem(id: String): InventoryItemDto?

    /** Deletes the inventory row [id]. RLS scopes the delete to the current user. */
    suspend fun deleteInventoryItem(id: String)
}
