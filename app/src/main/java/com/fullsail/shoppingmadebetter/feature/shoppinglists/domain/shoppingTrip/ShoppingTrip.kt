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
)