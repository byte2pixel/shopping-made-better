package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The `shopping_list_items` row read back after an insert (`insert(dto) { select() }`).
 *
 * Supabase returns the whole inserted row, so we capture every column here in case
 * a future caller needs more than the id — the id alone drives the pantry Undo today.
 * Every field but [id] has a default so decoding tolerates a narrowed projection and
 * test fakes can construct this with just an id.
 *
 * [quantity] is `numeric(10,3)` in Postgres, so it must decode as a [Double]; [note]
 * is nullable in the schema.
 */
@Serializable
data class InsertItemResultDto(
    @SerialName("id") val id: String,
    @SerialName("shopping_list_id") val shoppingListId: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("quantity") val quantity: Double = 0.0,
    @SerialName("note") val note: String? = null,
    @SerialName("is_checked") val isChecked: Boolean = false,
    @SerialName("add_to_inventory") val addInventory: Boolean = true,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)