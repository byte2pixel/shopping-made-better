package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain


data class ShoppingList (
    val shoppingListId: String?,
    val storeId: String,
    val name: String,
    val shared: Boolean,
)


