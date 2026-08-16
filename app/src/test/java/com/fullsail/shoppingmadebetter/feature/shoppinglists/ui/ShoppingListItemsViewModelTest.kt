package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.GetShoppingListItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.CompleteShoppingTripUseCase
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [ShoppingListItemsViewModel.markAllPurchased]. Hand-written
 * fakes drive each use case; the [MainDispatcherRule] lets `viewModelScope`
 * work run on the JVM.
 */
class ShoppingListItemsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeGetItems(
        var output: GetShoppingListItemsUseCase.Output,
    ) : GetShoppingListItemsUseCase {
        var requested: String? = null
        override suspend fun execute(input: String): GetShoppingListItemsUseCase.Output {
            requested = input
            return output
        }
    }

    private class FakeDelete : DeleteItemsUseCase {
        override suspend fun execute(input: String): DeleteItemsUseCase.Output =
            DeleteItemsUseCase.Output.Success
    }

    private class FakeComplete(
        var output: CompleteShoppingTripUseCase.Output,
    ) : CompleteShoppingTripUseCase {
        var requested: String? = null
        override suspend fun execute(input: String): CompleteShoppingTripUseCase.Output {
            requested = input
            return output
        }
    }

    @Test
    fun `markAllPurchased success refreshes to the now-empty list`() = runTest {
        val getItems = FakeGetItems(GetShoppingListItemsUseCase.Output.Success(emptyList()))
        val complete = FakeComplete(CompleteShoppingTripUseCase.Output.Success)
        val viewModel = ShoppingListItemsViewModel(getItems, FakeDelete(), complete)

        viewModel.markAllPurchased("l1")

        val state = viewModel.uiState.value
        assertTrue(state is ShoppingListItemsState.Success)
        assertTrue((state as ShoppingListItemsState.Success).items.isEmpty())
        assertEquals("l1", complete.requested)
        assertEquals("l1", getItems.requested)
    }

    @Test
    fun `markAllPurchased exposes Error and skips the refresh when completion fails`() = runTest {
        val getItems = FakeGetItems(GetShoppingListItemsUseCase.Output.Success(emptyList()))
        val complete = FakeComplete(CompleteShoppingTripUseCase.Output.Failure(RuntimeException("boom")))
        val viewModel = ShoppingListItemsViewModel(getItems, FakeDelete(), complete)

        viewModel.markAllPurchased("l1")

        assertTrue(viewModel.uiState.value is ShoppingListItemsState.Error)
        assertEquals("l1", complete.requested)
        assertNull(getItems.requested)
    }
}