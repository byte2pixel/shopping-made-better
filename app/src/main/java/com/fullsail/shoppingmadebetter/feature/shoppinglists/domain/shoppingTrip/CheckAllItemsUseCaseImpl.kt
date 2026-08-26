package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class CheckAllItemsUseCaseImpl @Inject constructor(
    private val repository: ShoppingListRepository,
) : CheckAllItemsUseCase {
    override suspend fun execute(input: String): CheckAllItemsUseCase.Output =
        try {
            repository.checkAllItems(input)
            CheckAllItemsUseCase.Output.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check all items: ${e.message}", e)
            CheckAllItemsUseCase.Output.Failure(e)
        }

    private companion object { const val TAG = "CheckAllItemsUseCase" }
}
