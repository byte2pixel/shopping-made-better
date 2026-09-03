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

class GetInventoryUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val clock: Clock,
) : GetInventoryUseCase {
    override suspend fun execute(input: Unit): GetInventoryUseCase.Output = try {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val inventoryItems = pantryRepository.getInventoryItems().map { it.toDomain(today) }
        GetInventoryUseCase.Output.Success(groupInventoryByProduct(inventoryItems))
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch inventory items: ${e.message}", e)
        GetInventoryUseCase.Output.Failure(e)
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
        location = PantryLocation.fromDbValue(location),
        lowStockThreshold = lowStockThreshold,
        estimated = lastAdjustmentReason == AdjustmentReason.Auto.toDbValue(),
        estimateSource = EstimateSource.fromDbValue(estimateSource),
    )

    private companion object {
        const val TAG = "GetInventoryUseCase"
    }
}
