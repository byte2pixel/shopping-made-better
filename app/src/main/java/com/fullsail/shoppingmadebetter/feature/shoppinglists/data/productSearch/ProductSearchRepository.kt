package com.fullsail.shoppingmadebetter.feature.shoppinglists.data.productSearch

interface ProductSearchRepository
{
    suspend fun getProduct(searchName : String): List<ProductSearchDto>
}