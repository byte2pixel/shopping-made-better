package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain

import ItemDetailsDto
import android.util.Log
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import javax.inject.Inject

class GetItemDetailsUseCaseImpl  @Inject constructor(
    private val repository: ShoppingListRepository,
) : GetItemDetailsUseCase {
    override suspend fun execute(input: String): GetItemDetailsUseCase.Output {
        return try {
            GetItemDetailsUseCase.Output.Success(repository.getItemDetails(input).toDomain())

        } catch (e: Exception) {
            Log.e(TAG, "Failed to grab product information: ${e.message}", e)
            GetItemDetailsUseCase.Output.Failure(e)
        }
    }

    private fun ItemDetailsDto.toDomain() = ItemDetails(
       id = id,
        title = title,
        brand = brand,
        description = description,
        sizing = sizing,
        image = image,
        source = source,
        shelfLife= shelfLife,
        lifeCategory = lifeCategory


    )



    private companion object {
        const val TAG = "getItemDetails"
    }
}
