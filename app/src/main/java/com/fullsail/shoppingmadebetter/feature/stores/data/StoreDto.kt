package com.fullsail.shoppingmadebetter.feature.stores.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire model for a row of the `public.stores` table, decoded from the Postgrest
 * response. Column names are snake_case in Postgres, so [SerialName] maps the
 * ones that differ from their Kotlin property names.
 */
@Serializable
data class StoreDto(
    val id: String,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    @SerialName("postal_code") val postalCode: String,
    val phone: String? = null,
)
