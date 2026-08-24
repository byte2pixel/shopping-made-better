package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class isCheckedUseCaseImpl @Inject constructor(
    private val repository: ShoppingListRepository,
) : isCheckedUseCase {
    override suspend fun execute(input : isChecked ): isCheckedUseCase.Output {
        return try {
            repository.toggleCheckBox(input.id,input.value)
            isCheckedUseCase.Output.Success

        } catch (e: Exception) {
            Log.e(TAG, "Failed to check product: ${e.message}", e)
            isCheckedUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "isCheckedUseCase"
    }
}