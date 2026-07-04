package com.fullsail.shoppingmadebetter.data

import com.fullsail.shoppingmadebetter.data.dto.StoreDto

/**
 * Data-access contract for the `stores` table. Implementations talk to Supabase;
 * callers (use cases) depend on this interface so they can be tested with a fake.
 */
interface StoreRepository {
    /**
     * Fetches every store. Throws if the request fails (network, decoding, etc.) —
     * error translation is the caller's responsibility.
     */
    suspend fun getStores(): List<StoreDto>
}
