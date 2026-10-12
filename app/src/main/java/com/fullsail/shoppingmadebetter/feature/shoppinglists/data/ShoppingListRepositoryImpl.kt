package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import ItemDetailsDto
import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class ShoppingListRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,

    ) : ShoppingListRepository {
        //Gets the store, price, brand, name, etc via the inputted product name I originally wanted this to use
        // productId, but I couldn't find an easy way to accomplish it so name should suffice
        override suspend fun getStores(productName : String): List<StoreProductPricingDto> = withContext(Dispatchers.IO) {

            postgrest
                .from("create_product_pricing_comparison")
                .select {
                    filter {
                        eq("product_title", productName)
                    }
                }
                .decodeList<StoreProductPricingDto>()
        }
//returns all shopping lists belonging to the user and returns them in ascending order.
    override suspend fun getTrips(): List<ShoppingTripDto> = withContext(Dispatchers.IO) {
        postgrest.from("shopping_trip_summaries").select()
        { order("sort_order", Order.ASCENDING)}
            .decodeList<ShoppingTripDto>()
    }//Returns a list of products
    override suspend fun getProduct(searchName : String): List<ProductSearchDto> = withContext(Dispatchers.IO) {

        postgrest.from("products").select{
            filter{
                ilike("title", "%$searchName%")
            }
            limit(10)
        }.decodeList<ProductSearchDto>()
    }
    override suspend fun addItem(item: InsertItem): InsertItemResultDto {
        // insert the item but also return the result back in case the id is needed.
        val inserted = postgrest.from("shopping_list_items").insert(InsertItemDto(
            item.shoppingListId, item.productId, item.quantity,
            item.note, item.isChecked, item.addInventory)
        ) { select() }.decodeSingle<InsertItemResultDto>()
        Log.d("InsertItem", "Insert completed successfully: ${inserted.id}")
        return inserted
    }
//Gets information about the list and then createes a shoppinglist off of it then returns the list
    override suspend fun addList(list: ShoppingList) : ShoppingList {
           val response = postgrest.from("shopping_lists").insert(ShoppingListDto(
             null,list.storeId, list.name, list.shared)){ select()}.decodeSingle<ShoppingListDto>()

            return ShoppingList(
                response.shoppingListId,
                response.storeId,
                response.name,
                response.shared
            )


         }
//Gets the items off of a lists ID
    override suspend fun getItems(list: String) : List<ShoppingListItemsDto> = withContext(Dispatchers.IO) {
        postgrest
            .from("shopping_list_item_named_view")
            .select {
                filter {
                    eq("shopping_id", list )
                }
            }
            .decodeList<ShoppingListItemsDto>()
    }
    //deletes items off of the item id
    override suspend fun deleteItem(itemId : String)
    {
        postgrest
            .from("shopping_list_items")
            .delete {
                filter {
                    eq("id", itemId )
                }
            }

        Log.d("DeleteItem", "Item: $itemId deleted")

    }

//Remove a list out of the database from the listId
    override suspend fun removeList(listId: String) {
        postgrest.from("shopping_lists").delete{
            filter{
                eq("id", listId)
            }

        }
    }
//Rename a list, based off of its list id and new name
    override suspend fun renameList(listId: String, newName: String) {
        postgrest.from("shopping_lists").update(
            {
                set("name",newName)
            }
        ){
            filter{
                eq("id", listId)
            }
        }
    }
//Updates a lists quantity based off the list id and the int of new quantity
    override suspend fun updateQuantity(id: String, newQuantity: Int) {
        postgrest.from("shopping_list_items").update(
            {
                set("quantity",newQuantity)
            }
        ){
            filter{
                eq("id", id)
            }
        }
    }
    //Get item details about an item from an items id and returns them
    override suspend fun getItemDetails(id : String) : ItemDetailsDto
    {
       return postgrest.from("products")
            .select {
                filter {
                    eq("id", id )
                }
            }
            .decodeSingle<ItemDetailsDto>()
    }
//Complete a shopping list  removing checked items and adding them to pantry from a lists id
    override suspend fun completeShoppingTrip(listId: String) {
        postgrest.rpc("complete_shopping_trip", buildJsonObject { put("p_list_id", listId) })

        Log.d("CompleteTrip", "List: $listId marked purchased")
    }
//Takes the item id and a state and then toggles the checkbox state
    override suspend fun toggleCheckBox(id: String, value: Boolean) {
        postgrest.from("shopping_list_items").update(
            {
                set("is_checked",value)
            }

        ){
            filter { eq("id",id) }
        }
    }

    /**
     * Flags every item on the list as checked in a single filtered UPDATE, rather than
     * one request per item. Used by "mark all as purchased", since complete_shopping_trip
     * only buys rows where is_checked = true.
     */
    override suspend fun checkAllItems(listId: String): Unit = withContext(Dispatchers.IO) {
        postgrest.from("shopping_list_items").update(
            {
                set("is_checked", true)
            }
        ) {
            filter { eq("shopping_list_id", listId) }
        }

        Log.d("CheckAllItems", "List: $listId items all checked")
    }
//Changes the sort_order to order by created
    override suspend fun sortListByCreated() {
       postgrest.rpc("sortlistbycreatedat")
    }
//Changes the sort_order to order by updated
    override suspend fun sortListByUpdated() {
        postgrest.rpc("sortlistbyupdatedat")
    }
//Changes the sort order of 2 lists and manually swaps them.
    override suspend fun setSortOrder(list1: String, sortOrder: Int, list2: String, sortOrder2 : Int) {
        postgrest.rpc("swap_list_order_rpc", buildJsonObject {
            put("list_id_1", list1)
            put("sort_order_1", sortOrder)
            put("list_id_2", list2)
            put("sort_order_2", sortOrder2)
        } )
    }
//Get information of a store from the stores ID
    override suspend fun getStoreAddress(id: String): StoreAddressInformationDto {
        return postgrest.from("stores").select() {
            filter{
                eq("id", id)
            }
        }.decodeSingle<StoreAddressInformationDto>()
    }

}
