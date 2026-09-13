package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import ItemDetailsDto
import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class SetSortOrderUseCaseImpl @Inject constructor(
    private val repository: ShoppingListRepository,
) : SetSortOrderUseCase {
    override suspend fun execute(input: SortOrder): SetSortOrderUseCase.Output {
        return try {
            repository.setSortOrder(input.listId, input.newSortOrder)
            SetSortOrderUseCase.Output.Success

        } catch (e: Exception) {
            Log.e(TAG, "Failed to grab product information: ${e.message}", e)
            SetSortOrderUseCase.Output.Failure(e)
        }
    }



    private companion object {
        const val TAG = "setsortorder"
    }
}