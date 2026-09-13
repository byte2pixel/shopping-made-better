package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class SortListUpdatedUseCaseImpl  @Inject constructor(
    private val repository: ShoppingListRepository,
) : SortListUpdatedUseCase {
    override suspend fun execute(input : Unit): SortListUpdatedUseCase.Output {
        return try {
            repository.sortListByCreated()
            SortListUpdatedUseCase.Output.Success

        } catch (e: Exception) {
            Log.e(TAG, "Failed to sort shopping list: ${e.message}", e)
            SortListUpdatedUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "sortByCreatedUseCase"
    }
}