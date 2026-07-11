package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.insertItem.InsertItemDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.insertItem.InsertItemRepository
import javax.inject.Inject

class InsertItemUseCaseImpl @Inject constructor(
    private val repository: InsertItemRepository,

    ) : InsertItemUseCase {
        //Inserts item into shopping_list_items and logs exception if fails
    override suspend fun execute(item: InsertItem) {
        try {
            InsertItemUseCase.Output.Success(repository.addItem(item))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch trips: ${e.message}", e)
            InsertItemUseCase.Output.Failure(e)
        }
    }

    private fun InsertItemDto.toDomain() = InsertItem(
        productId = productId,
        shoppingListId = shoppingListId,
        quantity =  quantity,
        note = note,
        isChecked = isChecked,
        addInventory = addInventory

    )




    private companion object { const val TAG = "InsertItemUseCase" }

    }
