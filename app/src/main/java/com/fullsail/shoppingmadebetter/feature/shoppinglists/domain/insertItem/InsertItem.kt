package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem

data class InsertItem (
    val shoppingListId: String,
    val productId: String,
    val quantity: Int,
    val note: String,
    val isChecked: Boolean,
    val addInventory: Boolean
){
}