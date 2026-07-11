package com.fullsail.shoppingmadebetter.feature.shoppinglists.data.productSearch

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProductSearchRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : ProductSearchRepository
{
//Searches products table for products that contain the searchName inside of it
    override suspend fun getProduct(searchName : String): List<ProductSearchDto> = withContext(Dispatchers.IO) {

        postgrest.from("products").select{
            filter{
                ilike("title", "%$searchName%")
            }
            limit(10)
        }.decodeList<ProductSearchDto>()
    }
}