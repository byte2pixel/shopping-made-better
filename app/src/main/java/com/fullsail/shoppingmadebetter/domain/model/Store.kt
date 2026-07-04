package com.fullsail.shoppingmadebetter.domain.model

/**
 * A grocery store location, as used by the app's UI and use cases. This is the
 * clean domain model — decoupled from the Supabase wire format
 * ([com.fullsail.shoppingmadebetter.data.dto.StoreDto]).
 */
data class Store(
    val id: String,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val phone: String?,
)
