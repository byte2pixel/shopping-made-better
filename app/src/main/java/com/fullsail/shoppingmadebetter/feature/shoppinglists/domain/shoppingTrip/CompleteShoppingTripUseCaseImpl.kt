package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class CompleteShoppingTripUseCaseImpl @Inject constructor(
    private val repository: ShoppingListRepository,
) : CompleteShoppingTripUseCase {
    override suspend fun execute(input: String): CompleteShoppingTripUseCase.Output =
        try {
            repository.completeShoppingTrip(input)
            CompleteShoppingTripUseCase.Output.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete trip: ${e.message}", e)
            CompleteShoppingTripUseCase.Output.Failure(e)
        }

    private companion object { const val TAG = "CompleteShoppingTripUseCase" }
}