package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import javax.inject.Inject

class GetInventoryUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository
) : GetInventoryUseCase {
    override suspend fun execute(input: Unit): GetInventoryUseCase.Output = try {
        val inventoryItems = pantryRepository.getInventoryItems().map { it.toDomain() }
        GetInventoryUseCase.Output.Success(inventoryItems)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch inventory items: ${e.message}", e)
        GetInventoryUseCase.Output.Failure(e)
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
        const val TAG = "GetInventoryUseCase"
    }
}
