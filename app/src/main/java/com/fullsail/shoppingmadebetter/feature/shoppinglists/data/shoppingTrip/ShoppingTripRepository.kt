package com.fullsail.shoppingmadebetter.feature.shoppinglists.data.shoppingTrip

interface ShoppingTripRepository {
    suspend fun getTrips(): List<ShoppingTripDto>
}