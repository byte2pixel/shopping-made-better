package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain
data class ShoppingListItems (
    val id: String,
    val shoppingListId: String,
    val productId: String,
    val quantity: Int,
    val title: String,
    val checked : Boolean
)