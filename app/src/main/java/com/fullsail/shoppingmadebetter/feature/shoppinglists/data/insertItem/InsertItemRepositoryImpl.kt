package com.fullsail.shoppingmadebetter.feature.shoppinglists.data.insertItem

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import io.github.jan.supabase.postgrest.Postgrest
import jakarta.inject.Inject

class InsertItemRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : InsertItemRepository {

    override suspend fun addItem(item: InsertItem){
        //Convert item into Dto and then insert it into shopping_list_items
        postgrest.from("shopping_list_items").insert(InsertItemDto(
            item.shoppingListId, item.productId, item.quantity,
            item.note, item.isChecked, item.addInventory)
        )
        Log.d("InsertItem", "Insert completed successfully")
    }
}