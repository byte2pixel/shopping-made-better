package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.AddInventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.AddInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.DeleteInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearch
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearchUseCase
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** The sheet as [AddToPantrySheetState.Visible]; fails the test if it is hidden. */
private val AddToPantrySheetViewModel.visibleSheet: AddToPantrySheetState.Visible
    get() = addToPantrySheet.value as AddToPantrySheetState.Visible

/** The one product the catalog fake returns. */
private val milkProduct = ProductSearch(productId = "p1", productName = "Milk")

/**
 * Unit tests for [AddToPantrySheetViewModel]. Each collaborator is a hand-written fake, and
 * [MainDispatcherRule] backs `viewModelScope` with an unconfined test dispatcher so
 * launched work runs eagerly to its first suspension point.
 */
class AddToPantrySheetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * Fake catalog search: returns a settable [output] and records every query it was
     * asked for, so a test can prove the debounce collapsed the keystrokes.
     */
    private class FakeProductSearchUseCase(
        var output: ProductSearchUseCase.Output = ProductSearchUseCase.Output.Success(emptyList()),
    ) : ProductSearchUseCase {
        val queries = mutableListOf<String>()
        override suspend fun execute(input: String): ProductSearchUseCase.Output {
            queries += input
            return output
        }
    }

    /** Fake pantry-add use case: records its input and returns a settable [output]. */
    private class FakeAddInventoryItemUseCase(
        var output: AddInventoryItemUseCase.Output = AddInventoryItemUseCase.Output.Success("lot-9"),
    ) : AddInventoryItemUseCase {
        var lastInput: AddInventoryItem? = null
        override suspend fun execute(input: AddInventoryItem): AddInventoryItemUseCase.Output {
            lastInput = input
            return output
        }
    }

    /** Fake pantry-delete use case: records the id it received and returns [output]. */
    private class FakeDeleteInventoryItemUseCase(
        var output: DeleteInventoryItemUseCase.Output = DeleteInventoryItemUseCase.Output.Success,
    ) : DeleteInventoryItemUseCase {
        var lastId: String? = null
        override suspend fun execute(input: String): DeleteInventoryItemUseCase.Output {
            lastId = input
            return output
        }
    }

    private fun buildViewModel(
        search: FakeProductSearchUseCase = FakeProductSearchUseCase(),
        addToPantry: FakeAddInventoryItemUseCase = FakeAddInventoryItemUseCase(),
        deleteInventory: FakeDeleteInventoryItemUseCase = FakeDeleteInventoryItemUseCase(),
    ) = AddToPantrySheetViewModel(search, addToPantry, deleteInventory)

    @Test
    fun `onAddToPantryQuery searches after the pause and shows what came back`() = runTest {
        val search = FakeProductSearchUseCase(
            ProductSearchUseCase.Output.Success(listOf(milkProduct))
        )
        val viewModel = buildViewModel(search = search)
        viewModel.onAddToPantryClicked()

        viewModel.onAddToPantryQuery("milk")
        // Nothing has gone out yet: the pause has not elapsed.
        assertEquals(emptyList<String>(), search.queries)
        assertTrue(viewModel.visibleSheet.searching)

        advanceUntilIdle()

        assertEquals(listOf("milk"), search.queries)
        assertEquals(listOf(milkProduct), viewModel.visibleSheet.results)
        assertFalse(viewModel.visibleSheet.searching)
    }

    @Test
    fun `onAddToPantryQuery sends only the last of a burst of keystrokes`() = runTest {
        val search = FakeProductSearchUseCase()
        val viewModel = buildViewModel(search = search)
        viewModel.onAddToPantryClicked()

        viewModel.onAddToPantryQuery("mi")
        advanceTimeBy(100)
        viewModel.onAddToPantryQuery("mil")
        advanceTimeBy(100)
        viewModel.onAddToPantryQuery("milk")
        advanceUntilIdle()

        assertEquals(listOf("milk"), search.queries)
    }

    @Test
    fun `onAddToPantryQuery does not search a single letter`() = runTest {
        val search = FakeProductSearchUseCase(
            ProductSearchUseCase.Output.Success(listOf(milkProduct))
        )
        val viewModel = buildViewModel(search = search)
        viewModel.onAddToPantryClicked()
        viewModel.onAddToPantryQuery("milk")
        advanceUntilIdle()

        viewModel.onAddToPantryQuery("m")
        advanceUntilIdle()

        // One letter matches most of the catalog, so it is not worth a round trip — and
        // the previous results go with it rather than sitting under a query that never ran.
        assertEquals(listOf("milk"), search.queries)
        assertEquals(emptyList<ProductSearch>(), viewModel.visibleSheet.results)
    }

    @Test
    fun `onAddToPantryQuery escapes LIKE wildcards in the query`() = runTest {
        val search = FakeProductSearchUseCase()
        val viewModel = buildViewModel(search = search)
        viewModel.onAddToPantryClicked()

        viewModel.onAddToPantryQuery("100% whole_grain")
        advanceUntilIdle()

        // Unescaped, "100%" would match every product whose title starts with 100.
        assertEquals(listOf("100\\% whole\\_grain"), search.queries)
    }

    @Test
    fun `a failed search says so rather than claiming there are no matches`() = runTest {
        val search = FakeProductSearchUseCase(
            ProductSearchUseCase.Output.Failure(IOException("no network"))
        )
        val viewModel = buildViewModel(search = search)
        viewModel.onAddToPantryClicked()

        viewModel.onAddToPantryQuery("milk")
        advanceUntilIdle()

        assertTrue(viewModel.visibleSheet.searchFailed)
        assertFalse(viewModel.visibleSheet.searching)
    }

    @Test
    fun `a new search keeps the previous results on screen while it runs`() = runTest {
        val search = FakeProductSearchUseCase(
            ProductSearchUseCase.Output.Success(listOf(milkProduct))
        )
        val viewModel = buildViewModel(search = search)
        viewModel.onAddToPantryClicked()
        viewModel.onAddToPantryQuery("mi")
        advanceUntilIdle()
        assertEquals(listOf(milkProduct), viewModel.visibleSheet.results)

        viewModel.onAddToPantryQuery("mil")

        // Mid-type the rows are the previous answer, not nothing: blanking them collapses
        // the sheet to the height of a spinner and bounces it back on the next keystroke.
        assertTrue(viewModel.visibleSheet.searching)
        assertEquals(listOf(milkProduct), viewModel.visibleSheet.results)
    }

    @Test
    fun `a failed search stays on screen while the next one runs`() = runTest {
        val search = FakeProductSearchUseCase(
            ProductSearchUseCase.Output.Failure(IOException("no network"))
        )
        val viewModel = buildViewModel(search = search)
        viewModel.onAddToPantryClicked()
        viewModel.onAddToPantryQuery("mi")
        advanceUntilIdle()
        assertTrue(viewModel.visibleSheet.searchFailed)

        viewModel.onAddToPantryQuery("mil")

        // Same reasoning as the rows: the message goes when its replacement arrives.
        assertTrue(viewModel.visibleSheet.searchFailed)
    }

    @Test
    fun `no matches is only claimed once a search has come back`() = runTest {
        val search = FakeProductSearchUseCase(
            ProductSearchUseCase.Output.Success(emptyList())
        )
        val viewModel = buildViewModel(search = search)
        viewModel.onAddToPantryClicked()

        viewModel.onAddToPantryQuery("zzz")
        assertFalse(viewModel.visibleSheet.hasSearched)

        advanceUntilIdle()
        assertTrue(viewModel.visibleSheet.hasSearched)
    }

    @Test
    fun `backing down to one letter forgets that a search happened`() = runTest {
        val search = FakeProductSearchUseCase(
            ProductSearchUseCase.Output.Success(emptyList())
        )
        val viewModel = buildViewModel(search = search)
        viewModel.onAddToPantryClicked()
        viewModel.onAddToPantryQuery("zz")
        advanceUntilIdle()
        assertTrue(viewModel.visibleSheet.hasSearched)

        viewModel.onAddToPantryQuery("z")

        // Too short to search, so the sheet has nothing to report either way.
        assertFalse(viewModel.visibleSheet.hasSearched)
        assertEquals(emptyList<ProductSearch>(), viewModel.visibleSheet.results)
    }

    @Test
    fun `onAddToPantryQuantity floors at one and caps at ninety-nine`() = runTest {
        val viewModel = buildViewModel()
        viewModel.onAddToPantryClicked()

        viewModel.onAddToPantryQuantity(0)
        assertEquals(1, viewModel.visibleSheet.quantity)

        viewModel.onAddToPantryQuantity(100)
        assertEquals(99, viewModel.visibleSheet.quantity)
    }

    @Test
    fun `onAddToPantryProductCleared goes back to the results and resets the choices`() = runTest {
        val viewModel = buildViewModel()
        viewModel.onAddToPantryClicked()
        viewModel.onAddToPantryProductSelected(milkProduct)
        viewModel.onAddToPantryQuantity(4)
        viewModel.onAddToPantryLocation(PantryLocation.Fridge)

        viewModel.onAddToPantryProductCleared()

        assertNull(viewModel.visibleSheet.selected)
        assertEquals(1, viewModel.visibleSheet.quantity)
        assertNull(viewModel.visibleSheet.location)
    }

    @Test
    fun `onAddToPantryConfirm adds the picked product, closes the sheet and emits Added`() = runTest {
        val addToPantry = FakeAddInventoryItemUseCase()
        val viewModel = buildViewModel(addToPantry = addToPantry)
        viewModel.onAddToPantryClicked()
        viewModel.onAddToPantryProductSelected(milkProduct)
        viewModel.onAddToPantryQuantity(3)
        viewModel.onAddToPantryLocation(PantryLocation.Freezer)

        viewModel.onAddToPantryConfirm()

        assertEquals(
            AddInventoryItem("p1", quantity = 3, location = PantryLocation.Freezer),
            addToPantry.lastInput,
        )
        assertEquals(AddToPantrySheetState.Hidden, viewModel.addToPantrySheet.value)
        // The event carries the lot id so the screen can reload and Undo can target it.
        assertEquals(AddToPantrySheetEvent.Added("Milk", "lot-9"), viewModel.events.first())
    }

    @Test
    fun `onAddToPantryConfirm keeps the location null when none was picked`() = runTest {
        val addToPantry = FakeAddInventoryItemUseCase()
        val viewModel = buildViewModel(addToPantry = addToPantry)
        viewModel.onAddToPantryClicked()
        viewModel.onAddToPantryProductSelected(milkProduct)

        viewModel.onAddToPantryConfirm()

        // Null is the signal that the product's category, not the user, decides.
        assertNull(addToPantry.lastInput?.location)
        assertEquals(1, addToPantry.lastInput?.quantity)
    }

    @Test
    fun `onAddToPantryConfirm reports a failed add`() = runTest {
        val addToPantry = FakeAddInventoryItemUseCase(
            AddInventoryItemUseCase.Output.Failure(IOException("boom"))
        )
        val viewModel = buildViewModel(addToPantry = addToPantry)
        viewModel.onAddToPantryClicked()
        viewModel.onAddToPantryProductSelected(milkProduct)

        viewModel.onAddToPantryConfirm()

        assertEquals(AddToPantrySheetEvent.AddFailed("Milk"), viewModel.events.first())
    }

    @Test
    fun `onAddToPantryConfirm does nothing until a product is picked`() = runTest {
        val addToPantry = FakeAddInventoryItemUseCase()
        val viewModel = buildViewModel(addToPantry = addToPantry)
        viewModel.onAddToPantryClicked()

        viewModel.onAddToPantryConfirm()

        assertNull(addToPantry.lastInput)
        assertTrue(viewModel.addToPantrySheet.value is AddToPantrySheetState.Visible)
    }

    @Test
    fun `undoAddToPantry deletes the new lot and emits Undone`() = runTest {
        val deleteInventory = FakeDeleteInventoryItemUseCase()
        val viewModel = buildViewModel(deleteInventory = deleteInventory)

        viewModel.undoAddToPantry("lot-9", "Milk")

        assertEquals("lot-9", deleteInventory.lastId)
        assertEquals(AddToPantrySheetEvent.Undone("Milk"), viewModel.events.first())
    }

    @Test
    fun `undoAddToPantry reports a failed delete`() = runTest {
        val viewModel = buildViewModel(
            deleteInventory = FakeDeleteInventoryItemUseCase(
                DeleteInventoryItemUseCase.Output.Failure(IOException("boom"))
            ),
        )

        viewModel.undoAddToPantry("lot-9", "Milk")

        assertEquals(AddToPantrySheetEvent.UndoFailed("Milk"), viewModel.events.first())
    }

    @Test
    fun `dismiss hides the sheet and drops the pending search`() = runTest {
        val search = FakeProductSearchUseCase()
        val viewModel = buildViewModel(search = search)
        viewModel.onAddToPantryClicked()
        viewModel.onAddToPantryQuery("milk")

        viewModel.dismiss()
        advanceUntilIdle()

        assertEquals(AddToPantrySheetState.Hidden, viewModel.addToPantrySheet.value)
        assertEquals(emptyList<String>(), search.queries)
    }
}
