package com.fullsail.shoppingmadebetter.feature.pantry.data

interface PantryRepository {
    suspend fun getInventoryItems(): List<InventoryItemDto>

    suspend fun getInventoryItem(id: String): InventoryItemDto?

    /** Deletes the inventory row [id]. RLS scopes the delete to the current user. */
    suspend fun deleteInventoryItem(id: String)

    /** Sets the [quantity] on inventory row [id]. RLS scopes the update to the current user. */
    suspend fun updateQuantity(id: String, quantity: Int)

    /**
     * Sets the [location] on inventory row [id] (raw db value: 'pantry'|'fridge'|'freezer').
     * RLS scopes the update to the current user.
     */
    suspend fun updateLocation(id: String, location: String)
}
