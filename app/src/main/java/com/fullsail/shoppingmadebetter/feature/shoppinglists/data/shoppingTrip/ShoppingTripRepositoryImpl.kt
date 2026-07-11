package com.fullsail.shoppingmadebetter.feature.shoppinglists.data.shoppingTrip

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ShoppingTripRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : ShoppingTripRepository
{

    override suspend fun getTrips(): List<ShoppingTripDto> = withContext(Dispatchers.IO) {
        postgrest.from("shopping_trip_summaries").select().decodeList<ShoppingTripDto>()
    }
}