package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import javax.inject.Inject

class GetInventoryItemUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository
) : GetInventoryItemUseCase {
    override suspend fun execute(input: String): GetInventoryItemUseCase.Output = try {
        when (val dto = pantryRepository.getInventoryItem(input)) {
            null -> GetInventoryItemUseCase.Output.NotFound
            else -> GetInventoryItemUseCase.Output.Success(dto.toDomain())
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch inventory item $input: ${e.message}", e)
        GetInventoryItemUseCase.Output.Failure(e)
    }

    private fun InventoryItemDto.toDomain() = InventoryItem(
        id = id,
        productId = productId,
        name = name,
        brand = brand,
        description = description,
        size = size,
        imageUrl = imageUrl,
        quantity = quantity,
    )

    private companion object {
        const val TAG = "GetInventoryItemUseCase"
    }
}
