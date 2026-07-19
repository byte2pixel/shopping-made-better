package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip

data class ShoppingTrip(
    val shoppingListId: String,
    val listName: String,
    val storeId: String,
    val storeName: String,
    val itemCount: Int,
    val totalCost: Double,
)