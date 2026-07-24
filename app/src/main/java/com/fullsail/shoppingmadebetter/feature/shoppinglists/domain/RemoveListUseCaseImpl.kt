package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class RemoveListUseCaseImpl @Inject constructor(
    private val repository: ShoppingListRepository,
) : RemoveListUseCase {
    override suspend fun execute(listId: String): RemoveListUseCase.Output {
        return try {
            repository.removeList(listId)
            RemoveListUseCase.Output.Success

        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove list: ${e.message}", e)
            RemoveListUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "RemoveListUseCase"
    }
}