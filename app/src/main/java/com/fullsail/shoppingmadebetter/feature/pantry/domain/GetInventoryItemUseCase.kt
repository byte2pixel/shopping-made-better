package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

interface GetInventoryItemUseCase : UseCase<String, GetInventoryItemUseCase.Output> {
    sealed interface Output {
        data class Success(val inventoryItem: InventoryItem) : Output
        data object NotFound : Output
        data class Failure(val error: Throwable) : Output
    }
}