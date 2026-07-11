package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem

interface InsertItemUseCase
{
    suspend fun execute(item: InsertItem)

    sealed interface Output {
        data class Success( val product: Unit) : Output
        data class Failure(val error: Throwable) : Output
    }
}
