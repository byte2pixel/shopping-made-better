package com.fullsail.shoppingmadebetter.feature.stores.domain

/**
 * A grocery store location, as used by the app's UI and use cases. This is the
 * clean domain model — decoupled from the Supabase wire format
 * ([com.fullsail.shoppingmadebetter.feature.stores.data.StoreDto]).
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
