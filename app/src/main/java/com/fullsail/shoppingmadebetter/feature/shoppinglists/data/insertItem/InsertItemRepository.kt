package com.fullsail.shoppingmadebetter.feature.shoppinglists.data.insertItem

import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem

interface InsertItemRepository {

    suspend fun addItem(item: InsertItem)
}