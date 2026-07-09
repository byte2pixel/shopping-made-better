package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface GetInventoryUseCase : UseCase<Unit, GetInventoryUseCase.Output> {
    sealed interface Output {
        data class Success(val inventoryItems: List<InventoryItem>) : Output
        data class Failure(val error: Throwable) : Output
    }
}
