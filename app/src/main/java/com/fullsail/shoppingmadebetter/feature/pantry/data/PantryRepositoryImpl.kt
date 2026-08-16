package com.fullsail.shoppingmadebetter.feature.pantry.data

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
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

    override suspend fun updateLocation(id: String, location: String) = withContext(Dispatchers.IO) {
        postgrest.from("inventory_items").update({ set("location", location) }) {
            filter { eq("id", id) }
        }
        Unit
    }

    override suspend fun updateExpiry(id: String, expiresAt: LocalDate) = withContext(Dispatchers.IO) {
        postgrest.from("inventory_items").update({ set("expires_at", expiresAt) }) {
            filter { eq("id", id) }
        }
        Unit
    }

    override suspend fun updateLowStockThreshold(productId: String, threshold: Int?) =
        withContext(Dispatchers.IO) {
            if (threshold == null) {
                postgrest.from("user_product_stock_settings").delete {
                    filter { eq("product_id", productId) }
                }
            } else {
                postgrest.from("user_product_stock_settings").upsert(
                    ProductStockSettingDto(productId = productId, lowStockThreshold = threshold),
                ) {
                    onConflict = "user_id,product_id"
                }
            }
            Unit
        }
}
