package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip

import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.InsertItemResultDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ProductSearchDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListItemsDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingTripDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.StoreProductPricingDto
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CompleteShoppingTripUseCaseImplTest {

    private class FakeRepo(
        private val error: Throwable? = null,
    ) : ShoppingListRepository {
        var completedListId: String? = null

        override suspend fun completeShoppingTrip(listId: String) {
            error?.let { throw it }
            completedListId = listId
        }

        override suspend fun getTrips(): List<ShoppingTripDto> {
            TODO("Not yet implemented")
        }

        override suspend fun getStores(productName: String): List<StoreProductPricingDto> {
            TODO("Not yet implemented")
        }

        override suspend fun getProduct(searchName: String): List<ProductSearchDto> {
            TODO("Not yet implemented")
        }

        override suspend fun addItem(item: InsertItem): InsertItemResultDto {
            TODO("Not yet implemented")
        }

        override suspend fun addList(list: ShoppingList): ShoppingList {
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

        override suspend fun renameList(listId: String, newName: String) {
            TODO("Not yet implemented")
        }
    }

    @Test
    fun `returns Success and forwards the list id to the repository`() = runTest {
        val repo = FakeRepo()

        val out = CompleteShoppingTripUseCaseImpl(repo).execute("list-7")

        assertTrue(out is CompleteShoppingTripUseCase.Output.Success)
        assertEquals("list-7", repo.completedListId)
    }

    @Test
    fun `returns Failure when the repository throws`() = runTest {
        val boom = IOException("rpc failed")

        val out = CompleteShoppingTripUseCaseImpl(FakeRepo(error = boom)).execute("list-7")

        assertTrue(out is CompleteShoppingTripUseCase.Output.Failure)
        assertSame(boom, (out as CompleteShoppingTripUseCase.Output.Failure).error)
    }
}