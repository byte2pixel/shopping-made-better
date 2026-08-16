package com.fullsail.shoppingmadebetter.feature.pantry.data

import kotlinx.datetime.LocalDate

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

    /** Sets the [expiresAt] date on inventory row [id]. RLS scopes it to the current user. */
    suspend fun updateExpiry(id: String, expiresAt: LocalDate)

    /**
     * Sets the current user's low-stock [threshold] for [productId]; `null` clears it (the
     * product never counts as low). Stored per user + product — not per inventory row — so it
     * survives removing and re-adding the product. RLS scopes it to the current user.
     */
    suspend fun updateLowStockThreshold(productId: String, threshold: Int?)
}
