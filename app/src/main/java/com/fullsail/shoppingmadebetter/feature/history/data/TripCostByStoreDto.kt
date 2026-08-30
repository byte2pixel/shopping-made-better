package com.fullsail.shoppingmadebetter.feature.history.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * One row of `purchase_trip_cost_by_store`: what one trip's basket would cost at one
 * store at today's prices.
 */
@Serializable
data class TripCostByStoreDto(
    val purchaseId: String,
    val purchasedOn: LocalDate,
    val storeId: String,
    val storeName: String,
    /** The basket at this store's current prices, over the items it prices. */
    val costHere: Double = 0.0,
    /** What was actually paid for those same items; the comparison's baseline. */
    val paidForSameItems: Double = 0.0,
    /** Items this store prices, against the trip's total. Unequal means the
     *  comparison is over part of the basket and must be dropped. */
    val itemsPriced: Int = 0,
    val itemsTotal: Int = 0,
)
