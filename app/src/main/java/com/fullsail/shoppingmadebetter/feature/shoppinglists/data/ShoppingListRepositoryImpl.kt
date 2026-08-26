package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import io.github.jan.supabase.postgrest.Postgrest
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

    override suspend fun getTrips(): List<ShoppingTripDto> = withContext(Dispatchers.IO) {
        postgrest.from("shopping_trip_summaries").select().decodeList<ShoppingTripDto>()
    }
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


    override suspend fun removeList(listId: String) {
        postgrest.from("shopping_lists").delete{
            filter{
                eq("id", listId)
            }

        }
    }

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

    override suspend fun completeShoppingTrip(listId: String) {
        postgrest.rpc("complete_shopping_trip", buildJsonObject { put("p_list_id", listId) })

        Log.d("CompleteTrip", "List: $listId marked purchased")
    }

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

}
