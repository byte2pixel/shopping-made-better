package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip

import kotlin.time.Instant

data class ShoppingTrip(
    val shoppingListId: String,
    val listName: String,
    val storeId: String,
    val storeName: String,
    val itemCount: Int,
    val totalCost: Double,
    val sortOrder : Int,
    val createdAt : Instant,
    val updatedAt : Instant,
    /** Display name of whoever created the list; null when it is out of reach. */
    val createdBy: String? = null,
    /** False when a housemate created the list, which hides the owner-only controls. */
    val isOwn: Boolean = true,
)