package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import ItemDetailsDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem

interface ShoppingListRepository {
    // gets all shopping lists belonging to the user
    suspend fun getTrips(): List<ShoppingTripDto>
    //Takes a products name and returns all stores that have it in a list
    suspend fun getStores(productName : String): List<StoreProductPricingDto>
    //Gets products with a similar name to searchName and then returns them in a list
    suspend fun getProduct(searchName : String): List<ProductSearchDto>
    //Add item to a specific list
    suspend fun addItem(item: InsertItem): InsertItemResultDto
    //Create a shopping list
    suspend fun addList(list: ShoppingList) : ShoppingList
    //Get a list of shopping list items from
    suspend fun getItems(list: String) : List<ShoppingListItemsDto>
    //delete  an item from its item id
    suspend fun deleteItem(itemId : String)
    //Delete a list from its given listId
    suspend fun removeList(listId : String)
    //Rename a list from its list id and the new name
    suspend fun renameList(listId : String, newName : String)
    //Given a list id it will complete the shopping trip marks all as purchased and adds to pantry
    suspend fun completeShoppingTrip(listId : String)
    //Toggles checkbox value in the database given the item id and true or false value
    suspend fun toggleCheckBox(id : String, value: Boolean)
    //Update the quantity of an items id and then set its new quantity
    suspend fun updateQuantity(id: String, newQuantity: Int)
    /** Flags every item on [listId] as checked in one request. */
    suspend fun checkAllItems(listId : String)
    //updates sort orderby its created date
    suspend fun sortListByCreated()
    //updates sort order by updated date
    suspend fun sortListByUpdated()
    //Manually swap the sort order of 2 lists
    suspend fun setSortOrder(list1: String, sortOrder: Int, list2: String, sortOrder2 : Int)
    //get information about the item from an itemId
    suspend fun getItemDetails(id : String) : ItemDetailsDto
    //Get information about the store including its address from the store ID
    suspend fun getStoreAddress(id : String) : StoreAddressInformationDto
}
