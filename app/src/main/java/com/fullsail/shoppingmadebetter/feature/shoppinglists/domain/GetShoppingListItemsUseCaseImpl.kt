package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListItemsDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject
import kotlin.String

class GetShoppingListItemsUseCaseImpl @Inject constructor(
    private val repository: ShoppingListRepository,
) : GetShoppingListItemsUseCase {
    //Checks the prices of each list used to get the name of the store and the total
    override suspend fun execute(input: String): GetShoppingListItemsUseCase.Output =
        try {
            GetShoppingListItemsUseCase.Output.Success(repository.getItems(input).map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch trips: ${e.message}", e)
            GetShoppingListItemsUseCase.Output.Failure(e)
        }

    private fun ShoppingListItemsDto.toDomain() = ShoppingListItems(
         id = id,
         shoppingListId = shoppingListId,
         productId = productId,
         quantity = quantity,
         title = title,
         checked = checked

    )

    private companion object { const val TAG = "GetListItemUseCase" }
}