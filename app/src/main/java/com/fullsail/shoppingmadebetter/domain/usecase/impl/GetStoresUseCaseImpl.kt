package com.fullsail.shoppingmadebetter.domain.usecase.impl

import android.util.Log
import com.fullsail.shoppingmadebetter.data.StoreRepository
import com.fullsail.shoppingmadebetter.data.dto.StoreDto
import com.fullsail.shoppingmadebetter.domain.model.Store
import com.fullsail.shoppingmadebetter.domain.usecase.GetStoresUseCase
import javax.inject.Inject

/**
 * Default [GetStoresUseCase]: reads from the [StoreRepository], maps the wire
 * DTOs to domain [Store]s, and translates any failure into [Output.Failure].
 */
class GetStoresUseCaseImpl @Inject constructor(
    private val storeRepository: StoreRepository,
) : GetStoresUseCase {

    override suspend fun execute(input: Unit): GetStoresUseCase.Output =
        try {
            val stores = storeRepository.getStores().map { it.toDomain() }
            GetStoresUseCase.Output.Success(stores)
        } catch (e: Exception) {
            // Log the real cause (network, serialization, permission, …) — the UI
            // only shows a generic error, so without this the failure is invisible.
            Log.e(TAG, "Failed to fetch stores: ${e.message}", e)
            GetStoresUseCase.Output.Failure(e)
        }

    private fun StoreDto.toDomain() = Store(
        id = id,
        name = name,
        address = address,
        city = city,
        state = state,
        postalCode = postalCode,
        phone = phone,
    )

    private companion object {
        const val TAG = "GetStoresUseCase"
    }
}
