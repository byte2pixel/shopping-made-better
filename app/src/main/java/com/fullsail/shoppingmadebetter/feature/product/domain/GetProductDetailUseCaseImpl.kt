package com.fullsail.shoppingmadebetter.feature.product.domain

import android.util.Log
import com.fullsail.shoppingmadebetter.feature.pantry.data.PantryRepository
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.byExpiry
import com.fullsail.shoppingmadebetter.feature.pantry.domain.toDomain
import com.fullsail.shoppingmadebetter.feature.product.data.ProductDetailDto
import com.fullsail.shoppingmadebetter.feature.product.data.ProductRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject
import kotlin.time.Clock

class GetProductDetailUseCaseImpl @Inject constructor(
    private val productRepository: ProductRepository,
    private val pantryRepository: PantryRepository,
    private val clock: Clock,
) : GetProductDetailUseCase {
    override suspend fun execute(input: String): GetProductDetailUseCase.Output = try {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        coroutineScope {
            val detail = async { productRepository.getProductDetail(input) }
            val lots = async { pantryRepository.getInventoryItems(input) }
            when (val dto = detail.await()) {
                null -> {
                    lots.cancel()
                    GetProductDetailUseCase.Output.NotFound
                }

                else -> GetProductDetailUseCase.Output.Success(
                    dto.toDomain(
                        lots = lots.await()
                            .map { it.toDomain(today) }
                            .sortedWith(compareBy(byExpiry) { it.expiresInDays }),
                    ),
                )
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch product $input: ${e.message}", e)
        GetProductDetailUseCase.Output.Failure(e)
    }

    private fun ProductDetailDto.toDomain(lots: List<InventoryItem>) = ProductDetail(
        id = id,
        name = name,
        brand = brand,
        description = description,
        size = size,
        imageUrl = imageUrl,
        lots = lots,
        lowStockThreshold = lowStockThreshold,
    )

    private companion object {
        const val TAG = "GetProductDetailUseCase"
    }
}
