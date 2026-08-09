package com.fullsail.shoppingmadebetter.feature.pantry.data

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PantryRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : PantryRepository {
    override suspend fun getInventoryItems(): List<InventoryItemDto> = withContext(Dispatchers.IO) {
        postgrest.from("pantry_items_by_expire").select().decodeList<InventoryItemDto>()
    }

    override suspend fun getInventoryItem(id: String): InventoryItemDto? =
        withContext(Dispatchers.IO) {
            postgrest.from("pantry_items_by_expire").select { filter { eq("id", id) } }
                .decodeSingleOrNull<InventoryItemDto>()
        }

    override suspend fun deleteInventoryItem(id: String) = withContext(Dispatchers.IO) {
        postgrest.from("inventory_items").delete { filter { eq("id", id) } }
        Unit
    }

    override suspend fun updateQuantity(id: String, quantity: Int) = withContext(Dispatchers.IO) {
        postgrest.from("inventory_items").update({ set("quantity", quantity) }) {
            filter { eq("id", id) }
        }
        Unit
    }
}
