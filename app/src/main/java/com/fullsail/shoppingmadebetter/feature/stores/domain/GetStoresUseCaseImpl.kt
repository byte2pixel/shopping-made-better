package com.fullsail.shoppingmadebetter.feature.stores.domain

import com.fullsail.shoppingmadebetter.feature.stores.data.StoreDto
import com.fullsail.shoppingmadebetter.feature.stores.data.StoreRepository
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
}
