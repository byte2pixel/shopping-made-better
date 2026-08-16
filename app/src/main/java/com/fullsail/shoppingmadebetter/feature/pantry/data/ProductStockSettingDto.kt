package com.fullsail.shoppingmadebetter.feature.pantry.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Upsert payload for `user_product_stock_settings`. `user_id` is omitted — the column
 * defaults to `auth.uid()` in the database, and RLS scopes the row to the current user.
 */
@Serializable
data class ProductStockSettingDto(
    @SerialName("product_id") val productId: String,
    @SerialName("low_stock_threshold") val lowStockThreshold: Int,
)
