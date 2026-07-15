package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingTripDto
import javax.inject.Inject

class GetShoppingTripsUseCaseImpl @Inject constructor(
    private val repository: ShoppingListRepository,
) : GetShoppingTripsUseCase {
    //Checks the prices of each list used to get the name of the store and the total
    override suspend fun execute(input: Unit): GetShoppingTripsUseCase.Output =
        try {
            GetShoppingTripsUseCase.Output.Success(repository.getTrips().map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch trips: ${e.message}", e)
            GetShoppingTripsUseCase.Output.Failure(e)
        }

    private fun ShoppingTripDto.toDomain() = ShoppingTrip(
        shoppingListId = shoppingListId,
        storeId = storeId,
        storeName = storeName,
        itemCount = itemCount,
        totalCost = totalCost,
    )

    private companion object { const val TAG = "GetShoppingTripsUseCase" }
}
