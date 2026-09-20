package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreAddressInformationDto(
    @SerialName("id") val storeId: String,
    @SerialName("name") val name: String,
    @SerialName("address") val address: String,
    @SerialName("city") val city: String,
    @SerialName("state") val state: String,
    @SerialName("postal_code") val postalCode: String,
    @SerialName("phone") val phone: String,
) {
}