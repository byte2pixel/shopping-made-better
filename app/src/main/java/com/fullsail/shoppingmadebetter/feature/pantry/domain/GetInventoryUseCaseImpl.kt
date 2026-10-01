package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.datetime.TimeZone
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

    private companion object {
        const val TAG = "GetInventoryUseCase"
    }
}
