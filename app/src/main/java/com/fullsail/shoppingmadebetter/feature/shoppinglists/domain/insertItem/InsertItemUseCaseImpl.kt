package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class InsertItemUseCaseImpl @Inject constructor(
    private val repository: ShoppingListRepository,
) : InsertItemUseCase {
    // Inserts the item into shopping_list_items; returns Success, or Failure(error) if it throws.
    override suspend fun execute(input: InsertItem): InsertItemUseCase.Output {
        return try {
            repository.addItem(input)
            InsertItemUseCase.Output.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add item to shopping list: ${e.message}", e)
            InsertItemUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "InsertItemUseCase"
    }
}
