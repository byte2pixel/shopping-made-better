package com.fullsail.shoppingmadebetter.feature.pantry.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.AdjustmentDigestEntryDto
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant
import javax.inject.Inject

class GetAdjustmentDigestUseCaseImpl @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val clock: Clock,
) : GetAdjustmentDigestUseCase {
    override suspend fun execute(input: Unit): GetAdjustmentDigestUseCase.Output = try {
        val timeZone = TimeZone.currentSystemDefault()
        val today = clock.todayIn(timeZone)
        val entries = pantryRepository.getAdjustmentDigest().map { it.toDomain(today, timeZone) }
        GetAdjustmentDigestUseCase.Output.Success(entries)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch the adjustment digest: ${e.message}", e)
        GetAdjustmentDigestUseCase.Output.Failure(e)
    }

    private fun AdjustmentDigestEntryDto.toDomain(today: LocalDate, timeZone: TimeZone) =
        AdjustmentDigestEntry(
            adjustmentId = adjustmentId,
            lotId = inventoryItemId,
            productId = productId,
            productName = productName,
            imageUrl = imageUrl,
            delta = delta,
            quantityNow = quantityNow,
            productQuantity = productQuantity,
            lowStockThreshold = lowStockThreshold,
            source = EstimateSource.fromDbValue(estimateSource),
            daysAgo = Instant.fromEpochSeconds(createdAtEpoch)
                .toLocalDateTime(timeZone).date.daysUntil(today),
        )

    private companion object {
        const val TAG = "GetAdjustmentDigest"
    }
}
