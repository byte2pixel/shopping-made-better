package com.fullsail.shoppingmadebetter.feature.product.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.product.data.ProductDetailDto
import com.fullsail.shoppingmadebetter.feature.product.data.ProductRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import javax.inject.Inject
import kotlin.time.Clock

class GetProductDetailUseCaseImpl @Inject constructor(
    private val productRepository: ProductRepository,
    private val clock: Clock,
) : GetProductDetailUseCase {
    override suspend fun execute(input: String): GetProductDetailUseCase.Output = try {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        when (val dto = productRepository.getProductDetail(input)) {
            null -> GetProductDetailUseCase.Output.NotFound
            else -> GetProductDetailUseCase.Output.Success(dto.toDomain(today))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch product $input: ${e.message}", e)
        GetProductDetailUseCase.Output.Failure(e)
    }

    private fun ProductDetailDto.toDomain(today: LocalDate) = ProductDetail(
        id = id,
        name = name,
        brand = brand,
        description = description,
        size = size,
        imageUrl = imageUrl,
        quantityOnHand = quantity,
        expiresInDays = expiryDate?.let { today.daysUntil(it) },
        lowStockThreshold = lowStockThreshold,
    )

    private companion object {
        const val TAG = "GetProductDetailUseCase"
    }
}
