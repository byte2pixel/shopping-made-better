package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class UpdateQuantityUseCaseImpl @Inject constructor(
    private val repository: ShoppingListRepository,
) : UpdateQuantityUseCase {

    override suspend fun execute(input: QuantityUpdate): UpdateQuantityUseCase.Output {
        return try{
            repository.updateQuantity(input.id, input.quantity)
            UpdateQuantityUseCase.Output.Success
        } catch (e : Exception) {
            Log.e(TAG, "Failed to requantity List: ${e.message}", e)
            UpdateQuantityUseCase.Output.Failure(e)
        }
    }

    private companion object {
        const val TAG = "QuantityUpdate"
    }
}