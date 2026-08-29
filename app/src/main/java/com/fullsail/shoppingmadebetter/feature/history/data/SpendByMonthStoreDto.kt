package com.fullsail.shoppingmadebetter.feature.history.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** One row of `purchase_spend_by_month_store`: one month's spend at one store. */
@Serializable
data class SpendByMonthStoreDto(
    /** First day of the month the trips fall in. */
    val monthStart: LocalDate,
    /** `null` when the store the trips were made at has since been deleted. */
    val storeId: String? = null,
    val storeName: String? = null,
    /** Recorded totals where present, summed lines otherwise. */
    val total: Double = 0.0,
    val tripCount: Int = 0,
)
