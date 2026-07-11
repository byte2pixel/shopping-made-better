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
}
