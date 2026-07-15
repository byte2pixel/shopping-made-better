package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem

import com.fullsail.shoppingmadebetter.feature.shoppinglists.data.ShoppingListRepository
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
        override suspend fun addItem(item: InsertItem) {
            error?.let { throw it }
            added = item
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
