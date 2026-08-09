package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class ShoppingListUseCaseImpl  @Inject constructor(
    private val repository: ShoppingListRepository,
) : ShoppingListUseCase {
      override suspend fun execute(input: ShoppingList): ShoppingListUseCase.Output {
          return try {
            ShoppingListUseCase.Output.Success(repository.addList(input))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add item to shopping list: ${e.message}", e)
            ShoppingListUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "ShoppingListUseCase"
    }
}
