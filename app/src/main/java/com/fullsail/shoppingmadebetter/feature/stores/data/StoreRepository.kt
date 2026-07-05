package com.fullsail.shoppingmadebetter.feature.stores.data

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
