package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class RemoveListUseCaseImpl @Inject constructor(
    private val repository: ShoppingListRepository,
) : RemoveListUseCase {
    override suspend fun execute(input: String): ShoppingListUseCase.Output {
        return try {
            repository.deleteList(input)
            ShoppingListUseCase.Output.Success

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add item to shopping list: ${e.message}", e)
            ShoppingListUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "RemoveJUseCase"
    }
}