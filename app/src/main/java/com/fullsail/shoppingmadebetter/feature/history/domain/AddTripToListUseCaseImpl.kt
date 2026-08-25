package com.fullsail.shoppingmadebetter.feature.history.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.history.data.HistoryRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import javax.inject.Inject
import kotlin.math.ceil

class AddTripToListUseCaseImpl @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val insertItemUseCase: InsertItemUseCase,
) : AddTripToListUseCase {

    /**
     * Re-reads the trip so the copy is made from current data rather than whatever
     * the screen was showing, keeps the selected lines, and inserts one shopping-list
     * item each. A selected product that is no longer on the trip — the
     * `purchase_history_detail` view inner-joins `products`, so a delisted one drops
     * out — is counted as skipped instead of failing the add.
     */
    override suspend fun execute(input: AddTripToList): AddTripToListUseCase.Output {
        val lines = try {
            historyRepository.getPurchase(input.purchaseId)
                .toTrips()
                .firstOrNull()
                ?.items
                ?.filter { it.productId in input.productIds }
                .orEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read trip ${input.purchaseId}: ${e.message}", e)
            return AddTripToListUseCase.Output.Failure(e)
        }

        // One list entry per product: a product bought on two lines of the same trip
        // was bought once in the user's eyes, so its quantities add up.
        val quantityByProduct = lines
            .groupBy { it.productId }
            .mapValues { (_, sameProduct) -> sameProduct.sumOf { it.quantity } }

        // Selected products the trip no longer has: dropped from the catalog since.
        val skipped = input.productIds.size - quantityByProduct.size

        var added = 0
        var failed = 0
        var lastError: Throwable? = null
        for ((productId, quantity) in quantityByProduct) {
            val out = insertItemUseCase.execute(
                InsertItem(
                    shoppingListId = input.shoppingListId,
                    productId = productId,
                    quantity = quantity.toListQuantity(),
                    note = "",
                    isChecked = false,
                    addInventory = true,
                )
            )
            when (out) {
                is InsertItemUseCase.Output.Success -> added++
                is InsertItemUseCase.Output.Failure -> {
                    failed++
                    lastError = out.error
                }
            }
        }

        return when {
            failed > 0 && added == 0 -> AddTripToListUseCase.Output.Failure(
                lastError ?: IllegalStateException("Failed to add any items"),
            )

            failed > 0 -> AddTripToListUseCase.Output.PartialFailure(added, failed, skipped)
            else -> AddTripToListUseCase.Output.Success(added, skipped)
        }
    }

    private companion object {
        const val TAG = "AddTripToListUseCase"
    }
}

/**
 * A purchased quantity as a shopping-list quantity. History stores a Double (a trip can
 * record 0.75 lb of produce) while `shopping_list_items.quantity` is a whole number, so
 * round up — buying part of something again still means putting one on the list.
 */
private fun Double.toListQuantity(): Int = ceil(this).toInt().coerceAtLeast(1)
