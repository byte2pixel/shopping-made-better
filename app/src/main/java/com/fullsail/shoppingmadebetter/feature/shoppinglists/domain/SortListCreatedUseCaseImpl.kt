package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class SortListCreatedUseCaseImpl  @Inject constructor(
    private val repository: ShoppingListRepository,
) : SortListCreatedUseCase {
    override suspend fun execute(input : Unit): SortListCreatedUseCase.Output {
        return try {
           repository.sortListByCreated()
            SortListCreatedUseCase.Output.Success

        } catch (e: Exception) {
            Log.e(TAG, "Failed to sort shopping list: ${e.message}", e)
            SortListCreatedUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "sortByCreatedUseCase"
    }
}