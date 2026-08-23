package com.fullsail.shoppingmadebetter.feature.history.domain

import com.fullsail.shoppingmadebetter.feature.history.data.HistoryRepository
import com.fullsail.shoppingmadebetter.feature.history.data.PurchaseHistoryRowDto
import kotlinx.datetime.LocalDate

/** Fake repository: returns [rows] for both reads, or throws [error]. */
internal class FakeHistoryRepository(
    private val rows: List<PurchaseHistoryRowDto> = emptyList(),
    private val error: Throwable? = null,
) : HistoryRepository {
    override suspend fun getPurchaseHistory(): List<PurchaseHistoryRowDto> =
        error?.let { throw it } ?: rows

    override suspend fun getPurchase(purchaseId: String): List<PurchaseHistoryRowDto> =
        error?.let { throw it } ?: rows.filter { it.purchaseId == purchaseId }
}

/** A view row with sensible defaults; override only what a test is about. */
internal fun row(
    purchaseId: String,
    id: String = "line-$purchaseId",
    purchasedOn: LocalDate = LocalDate(2026, 8, 12),
    purchasedAtEpoch: Long = 1_786_504_429L,
    storeName: String? = "ALDI",
    totalAmount: Double? = 10.0,
    productId: String = "product-$id",
    productName: String = "Product $id",
    brand: String = "Some Brand",
    size: String = "1 ea",
    imageUrl: String = "",
    quantity: Double = 1.0,
    pricePaid: Double = 2.50,
) = PurchaseHistoryRowDto(
    id = id,
    purchaseId = purchaseId,
    purchasedOn = purchasedOn,
    purchasedAtEpoch = purchasedAtEpoch,
    storeName = storeName,
    totalAmount = totalAmount,
    productId = productId,
    productName = productName,
    brand = brand,
    size = size,
    imageUrl = imageUrl,
    quantity = quantity,
    pricePaid = pricePaid,
)
