package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.feature.history.data.PurchaseHistoryRowDto

/**
 * Collapses the flat `purchase_history_detail` rows into one [PurchaseTrip] per
 * purchase, newest first. Shared by the history list and the trip detail screen,
 * which read the same view.
 */
internal fun List<PurchaseHistoryRowDto>.toTrips(): List<PurchaseTrip> =
    groupBy { it.purchaseId }
        .map { (purchaseId, rows) ->
            // Every row of a purchase repeats its trip's header columns.
            val header = rows.first()
            PurchaseTrip(
                id = purchaseId,
                purchasedOn = header.purchasedOn,
                purchasedAtEpoch = header.purchasedAtEpoch,
                storeName = header.storeName,
                recordedTotal = header.totalAmount,
                items = rows.map { it.toLineItem() },
            )
        }
        .sortedByDescending { it.purchasedAtEpoch }

private fun PurchaseHistoryRowDto.toLineItem() = PurchaseLineItem(
    id = id,
    productId = productId,
    productName = productName,
    brand = brand,
    size = size,
    imageUrl = imageUrl,
    quantity = quantity,
    pricePaid = pricePaid,
    addedToInventory = addedToInventory,
)
