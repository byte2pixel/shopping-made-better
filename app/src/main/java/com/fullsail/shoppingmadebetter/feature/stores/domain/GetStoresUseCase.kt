package com.fullsail.shoppingmadebetter.feature.stores.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/**
 * Fetches all stores. Takes no input ([Unit]); returns a sealed [Output] so
 * callers handle success and failure exhaustively without try/catch.
 */
interface GetStoresUseCase : UseCase<Unit, GetStoresUseCase.Output> {
    sealed interface Output {
        data class Success(val stores: List<Store>) : Output
        data class Failure(val error: Throwable) : Output
    }
}
