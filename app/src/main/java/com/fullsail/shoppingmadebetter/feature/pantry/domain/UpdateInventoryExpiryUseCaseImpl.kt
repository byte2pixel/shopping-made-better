package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import javax.inject.Inject

class UpdateInventoryExpiryUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val clock: Clock,
) : UpdateInventoryExpiryUseCase {
    override suspend fun execute(input: UpdateInventoryExpiry): UpdateInventoryExpiryUseCase.Output = try {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val expiresAt = today.plus(input.expiresInDays, DateTimeUnit.DAY)
        pantryRepository.updateExpiry(input.id, expiresAt)
        UpdateInventoryExpiryUseCase.Output.Success
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update expiry for inventory item ${input.id}: ${e.message}", e)
        UpdateInventoryExpiryUseCase.Output.Failure(e)
    }

    private companion object {
        const val TAG = "UpdateInventoryExpiry"
    }
}
