package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.InventoryItemDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import javax.inject.Inject

class GetInventoryItemUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val clock: Clock,
) : GetInventoryItemUseCase {
    override suspend fun execute(input: String): GetInventoryItemUseCase.Output = try {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        when (val dto = pantryRepository.getInventoryItem(input)) {
            null -> GetInventoryItemUseCase.Output.NotFound
            else -> GetInventoryItemUseCase.Output.Success(dto.toDomain(today))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch inventory item $input: ${e.message}", e)
        GetInventoryItemUseCase.Output.Failure(e)
    }

    private fun InventoryItemDto.toDomain(today: LocalDate) = InventoryItem(
        id = id,
        productId = productId,
        name = name,
        brand = brand,
        description = description,
        size = size,
        imageUrl = imageUrl,
        quantity = quantity,
        expiresInDays = expiryDate?.let { today.daysUntil(it) },
    )

    private companion object {
        const val TAG = "GetInventoryItemUseCase"
    }
}
