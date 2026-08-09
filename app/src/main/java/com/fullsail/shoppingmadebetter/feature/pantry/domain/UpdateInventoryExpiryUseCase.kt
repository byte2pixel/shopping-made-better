package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/**
 * The inventory row [id] to update and its new shelf life as [expiresInDays] from today
 * (0 = due today, negative = already overdue). The use case converts this to an absolute
 * date callers never deal with dates.
 */
data class UpdateInventoryExpiry(val id: String, val expiresInDays: Int)

/** Sets an inventory item's expiry, expressed as a number of days from today. */
interface UpdateInventoryExpiryUseCase :
    UseCase<UpdateInventoryExpiry, UpdateInventoryExpiryUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}
