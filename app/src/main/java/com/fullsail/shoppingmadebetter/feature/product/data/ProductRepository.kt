package com.fullsail.shoppingmadebetter.feature.product.data

interface ProductRepository {
    /**
     * The catalog record for [productId] with the current user's pantry position folded
     * in, or `null` when no such product exists. RLS scopes the pantry columns to the
     * current user; a product the user doesn't hold still returns a row.
     */
    suspend fun getProductDetail(productId: String): ProductDetailDto?
}
