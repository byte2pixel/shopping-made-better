package com.fullsail.shoppingmadebetter.feature.pantry.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Row returned by the `apply_inventory_adjustment` RPC. [delta] is the effective change
 * after flooring at zero, so it can be smaller in magnitude than the delta requested.
 */
@Serializable
data class InventoryAdjustmentResultDto(
    @SerialName("inventory_item_id") val inventoryItemId: String,
    val delta: Double,
    @SerialName("new_quantity") val newQuantity: Double,
)
