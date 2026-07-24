package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    override suspend fun addItem(item: InsertItem){
        //Convert item into Dto and then insert it into shopping_list_items
           postgrest.from("shopping_list_items").insert(InsertItemDto(
            item.shoppingListId, item.productId, item.quantity,
            item.note, item.isChecked, item.addInventory)
        )
        Log.d("InsertItem", "Insert completed successfully")
    }

    override suspend fun addList(list: ShoppingList) {
            postgrest.from("shopping_lists").insert(ShoppingListDto(
             list.storeId, list.name, list.shared)){ select()}.decodeSingle<ShoppingListDto>()
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

}
