package com.fullsail.shoppingmadebetter.feature.stores.data

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Supabase-backed [StoreRepository]. Queries the `stores` table through
 * [Postgrest] and decodes the rows into [StoreDto]s. Any failure propagates to
 * the caller.
 */
class StoreRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : StoreRepository {

    override suspend fun getStores(): List<StoreDto> = withContext(Dispatchers.IO) {
        postgrest.from("stores").select().decodeList<StoreDto>()
    }
}
