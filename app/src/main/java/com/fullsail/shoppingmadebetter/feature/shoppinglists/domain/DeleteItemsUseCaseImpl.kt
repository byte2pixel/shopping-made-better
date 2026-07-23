package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class DeleteItemsUseCaseImpl @Inject constructor(
    private val repository: ShoppingListRepository,
) : DeleteItemsUseCase {
    override suspend fun execute(input : String): DeleteItemsUseCase.Output {
        return try {
            repository.deleteItem(input)
            DeleteItemsUseCase.Output.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove item: ${e.message}", e)
            DeleteItemsUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "DeleteItemUseCase"
    }
}
