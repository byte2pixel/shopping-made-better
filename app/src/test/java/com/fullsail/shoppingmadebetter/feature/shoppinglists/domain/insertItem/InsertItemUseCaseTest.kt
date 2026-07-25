package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem

import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ProductSearchDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListItemsDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingTripDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.StoreProductPricingDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class InsertItemUseCaseTest {

    private class FakeRepo(
        private val error: Throwable? = null,
    ) : ShoppingListRepository {
        var added: InsertItem? = null
        override suspend fun getTrips(): List<ShoppingTripDto> {
            TODO("Not yet implemented")
        }

        override suspend fun getStores(productName: String): List<StoreProductPricingDto> {
            TODO("Not yet implemented")
        }

        override suspend fun getProduct(searchName: String): List<ProductSearchDto> {
            TODO("Not yet implemented")
        }

        override suspend fun addItem(item: InsertItem) {
            error?.let { throw it }
            added = item
        }

        override suspend fun addList(list: ShoppingList) {
            TODO("Not yet implemented")
        }

        override suspend fun getItems(list: String): List<ShoppingListItemsDto> {
            TODO("Not yet implemented")
        }

        override suspend fun deleteItem(itemId: String) {
            TODO("Not yet implemented")
        }

        override suspend fun removeList(listId: String) {
            TODO("Not yet implemented")
        }
    }

    private val sampleItem = InsertItem(
        shoppingListId = "l1",
        productId = "p1",
        quantity = 1,
        note = "",
        isChecked = false,
        addInventory = true,
    )

    @Test
    fun `returns Success and forwards the item on a clean insert`() = runTest {
        val repo = FakeRepo()

        val out = InsertItemUseCaseImpl(repo).execute(sampleItem)

        assertTrue(out is InsertItemUseCase.Output.Success)
        assertEquals(sampleItem, repo.added)
    }

    @Test
    fun `returns Failure when the repository throws`() = runTest {
        val boom = IOException("insert failed")

        val out = InsertItemUseCaseImpl(FakeRepo(error = boom)).execute(sampleItem)

        assertTrue(out is InsertItemUseCase.Output.Failure)
        assertSame(boom, (out as InsertItemUseCase.Output.Failure).error)
    }
}
