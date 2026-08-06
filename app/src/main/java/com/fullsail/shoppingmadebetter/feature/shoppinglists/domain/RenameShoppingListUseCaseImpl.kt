package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class RenameShoppingListUseCaseImpl @Inject constructor(
    private val repository: ShoppingListRepository,
) : RenameShoppingListUseCase {

    override suspend fun execute(input: RenameList): RenameShoppingListUseCase.Output {
        return try{
            repository.renameList(input.listId, input.newName)
            RenameShoppingListUseCase.Output.Success
        } catch (e : Exception) {
            Log.e(TAG, "Failed to rename List: ${e.message}", e)
            RenameShoppingListUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "RenameListUseCase"
    }
}