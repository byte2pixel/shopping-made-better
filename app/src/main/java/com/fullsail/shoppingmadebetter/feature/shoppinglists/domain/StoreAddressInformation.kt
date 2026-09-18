package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

data class StoreAddressInformation(
    val storeId: String,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val phone: String,
){
}