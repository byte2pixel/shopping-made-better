package com.fullsail.shoppingmadebetter.feature.product.data

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : ProductRepository {
    override suspend fun getProductDetail(productId: String): ProductDetailDto? =
        withContext(Dispatchers.IO) {
            postgrest.from("product_details").select { filter { eq("id", productId) } }
                .decodeSingleOrNull<ProductDetailDto>()
        }
}
