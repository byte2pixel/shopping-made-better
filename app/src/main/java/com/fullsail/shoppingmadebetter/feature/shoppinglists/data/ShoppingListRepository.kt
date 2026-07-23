package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem

interface ShoppingListRepository {
    suspend fun getTrips(): List<ShoppingTripDto>
    suspend fun getStores(productName : String): List<StoreProductPricingDto>
    suspend fun getProduct(searchName : String): List<ProductSearchDto>
    suspend fun addItem(item: InsertItem)
    suspend fun addList(list: ShoppingList)
    suspend fun removeList(listId : String)

}