package com.fullsail.shoppingmadebetter.feature.pantry.domain

import com.fullsail.shoppingmadebetter.core.domain.UseCase

/** Removes an inventory item from the user's pantry by its row id. */
interface DeleteInventoryItemUseCase : UseCase<String, DeleteInventoryItemUseCase.Output> {
    sealed interface Output {
        data object Success : Output
        data class Failure(val error: Throwable) : Output
    }
}