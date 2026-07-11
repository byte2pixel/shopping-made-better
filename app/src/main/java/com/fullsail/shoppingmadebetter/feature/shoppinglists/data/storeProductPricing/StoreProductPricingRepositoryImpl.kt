package com.fullsail.shoppingmadebetter.feature.shoppinglists.data.storeProductPricing

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StoreProductPricingRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,

) : StoreProductPricingRepository {
//Gets the store, price, brand, name, etc via the inputted product name I originally wanted this to use
    // productId but I couldn't find an easy way to accomplish it so name should suffice
    override suspend fun getStores(productName : String): List<StoreProductPricingDto> = withContext(Dispatchers.IO) {

     postgrest
            .from("create_product_pricing_comparison")
            .select {
                filter {
                    eq("product_title", productName)
                }
            }
            .decodeList<StoreProductPricingDto>()
    }


}