package com.fullsail.shoppingmadebetter.feature.shoppinglists.data

import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem

/**
 * Base fake for [ShoppingListRepository] tests: every method throws by default, so a
 * test only overrides the one or two calls it is actually about and an unexpected call
 * fails loudly rather than passing silently.
 *
 * The point of the defaults is maintenance. Adding a method to [ShoppingListRepository]
 * used to break every hand-written fake at compile time — the whole unit test source set
 * stopped compiling more than once. Overriding here keeps that to a single edit.
 */
internal abstract class FakeShoppingListRepository : ShoppingListRepository {
    override suspend fun getTrips(): List<ShoppingTripDto> = notStubbed("getTrips")

    override suspend fun getStores(productName: String): List<StoreProductPricingDto> =
        notStubbed("getStores")

    override suspend fun getProduct(searchName: String): List<ProductSearchDto> =
        notStubbed("getProduct")

    override suspend fun addItem(item: InsertItem): InsertItemResultDto = notStubbed("addItem")

    override suspend fun addList(list: ShoppingList): ShoppingList = notStubbed("addList")

    override suspend fun getItems(list: String): List<ShoppingListItemsDto> = notStubbed("getItems")

    override suspend fun deleteItem(itemId: String): Unit = notStubbed("deleteItem")

    override suspend fun removeList(listId: String): Unit = notStubbed("removeList")

    override suspend fun renameList(listId: String, newName: String): Unit = notStubbed("renameList")

    override suspend fun completeShoppingTrip(listId: String): Unit =
        notStubbed("completeShoppingTrip")

    override suspend fun toggleCheckBox(id: String, value: Boolean): Unit =
        notStubbed("toggleCheckBox")

    private fun notStubbed(method: String): Nothing =
        throw NotImplementedError("$method was called but this fake does not stub it")
}
