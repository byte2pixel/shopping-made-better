package com.fullsail.shoppingmadebetter.domain.usecase

import com.fullsail.shoppingmadebetter.domain.model.Store

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
