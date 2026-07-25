package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [PantryViewModel]. Each collaborator is a hand-written fake, and
 * [MainDispatcherRule] backs `viewModelScope` with an unconfined test dispatcher
 * so launched work runs eagerly to its first suspension point — making the state
 * observable synchronously after each call.
 */
class PantryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Fake inventory use case: returns a settable [output]. */
    private class FakeGetInventoryUseCase(
        var output: GetInventoryUseCase.Output = GetInventoryUseCase.Output.Success(emptyList()),
    ) : GetInventoryUseCase {
        override suspend fun execute(input: Unit): GetInventoryUseCase.Output = output
    }

    /**
     * Fake shopping-trips use case: returns a settable [output]. An optional
     * [gate] lets a test hold the call suspended to exercise stale-result guards.
     */
    private class FakeGetShoppingTripsUseCase(
        var output: GetShoppingTripsUseCase.Output = GetShoppingTripsUseCase.Output.Success(emptyList()),
        private val gate: CompletableDeferred<Unit>? = null,
    ) : GetShoppingTripsUseCase {
        override suspend fun execute(input: Unit): GetShoppingTripsUseCase.Output {
            gate?.await()
            return output
        }
    }

    /** Fake insert use case: records the item it received and returns [output]. */
    private class FakeInsertItemUseCase(
        var output: InsertItemUseCase.Output = InsertItemUseCase.Output.Success,
    ) : InsertItemUseCase {
        var lastItem: InsertItem? = null
        override suspend fun execute(input: InsertItem): InsertItemUseCase.Output {
            lastItem = input
            return output
        }
    }

    private val sampleItem = InventoryItem(
        id = "i1",
        productId = "p1",
        name = "Milk",
        brand = "Dairy Co",
        description = "2% milk",
        size = "1 gal",
        imageUrl = "http://img/milk.png",
        quantity = 2,
        expiresInDays = null,
    )

    private val sampleTrip = ShoppingTrip(
        shoppingListId = "l1",
        listName = "Weekly",
        storeId = "s1",
        storeName = "ALDI",
        itemCount = 3,
        totalCost = 9.99,
    )

    private fun buildViewModel(
        inventory: FakeGetInventoryUseCase = FakeGetInventoryUseCase(),
        trips: FakeGetShoppingTripsUseCase = FakeGetShoppingTripsUseCase(),
        insert: FakeInsertItemUseCase = FakeInsertItemUseCase(),
    ) = PantryViewModel(inventory, trips, insert)

    @Test
    fun `initial load exposes Success with the inventory items`() = runTest {
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(
                GetInventoryUseCase.Output.Success(listOf(sampleItem))
            )
        )

        val state = viewModel.uiState.value
        assertTrue(state is PantryUiState.Success)
        assertEquals(listOf(sampleItem), (state as PantryUiState.Success).inventoryItems)
    }

    @Test
    fun `initial load exposes Error when the inventory fetch fails`() = runTest {
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(
                GetInventoryUseCase.Output.Failure(IOException("boom"))
            )
        )

        assertTrue(viewModel.uiState.value is PantryUiState.Error)
    }

    @Test
    fun `loadInventory refreshes the state on demand`() = runTest {
        val inventory = FakeGetInventoryUseCase(GetInventoryUseCase.Output.Success(emptyList()))
        val viewModel = buildViewModel(inventory = inventory)
        assertEquals(emptyList<InventoryItem>(), (viewModel.uiState.value as PantryUiState.Success).inventoryItems)

        inventory.output = GetInventoryUseCase.Output.Success(listOf(sampleItem))
        viewModel.loadInventory()

        val state = viewModel.uiState.value
        assertTrue(state is PantryUiState.Success)
        assertEquals(listOf(sampleItem), (state as PantryUiState.Success).inventoryItems)
    }

    @Test
    fun `onAddToListClicked shows the sheet with the loaded lists`() = runTest {
        val viewModel = buildViewModel(
            trips = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(sampleTrip))
            )
        )

        viewModel.onAddToListClicked(sampleItem)

        val sheet = viewModel.addToListSheet.value
        assertTrue(sheet is AddToListSheetState.Visible)
        sheet as AddToListSheetState.Visible
        assertEquals(sampleItem, sheet.item)
        assertTrue(sheet.lists is AddToListSheetState.Lists.Loaded)
        assertEquals(
            listOf(sampleTrip),
            (sheet.lists as AddToListSheetState.Lists.Loaded).trips,
        )
    }

    @Test
    fun `onAddToListClicked shows Empty when the user has no lists`() = runTest {
        val viewModel = buildViewModel(
            trips = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(emptyList())
            )
        )

        viewModel.onAddToListClicked(sampleItem)

        val sheet = viewModel.addToListSheet.value as AddToListSheetState.Visible
        assertTrue(sheet.lists is AddToListSheetState.Lists.Empty)
    }

    @Test
    fun `onAddToListClicked shows Error when loading the lists fails`() = runTest {
        val viewModel = buildViewModel(
            trips = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Failure(IOException("boom"))
            )
        )

        viewModel.onAddToListClicked(sampleItem)

        val sheet = viewModel.addToListSheet.value as AddToListSheetState.Visible
        assertTrue(sheet.lists is AddToListSheetState.Lists.Error)
    }

    @Test
    fun `onAddToListClicked ignores a stale list result once the sheet is dismissed`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val viewModel = buildViewModel(
            trips = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(sampleTrip)),
                gate = gate,
            )
        )

        viewModel.onAddToListClicked(sampleItem)
        // The trips call is parked on the gate; the sheet is up but still loading.
        val loading = viewModel.addToListSheet.value as AddToListSheetState.Visible
        assertTrue(loading.lists is AddToListSheetState.Lists.Loading)

        viewModel.dismissAddToListSheet()
        gate.complete(Unit) // Resume the load; its result should be discarded.

        assertEquals(AddToListSheetState.Hidden, viewModel.addToListSheet.value)
    }

    @Test
    fun `onAddToListClicked ignores a stale list result when the sheet moves to another item`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val secondItem = sampleItem.copy(id = "i2", productId = "p2", name = "Bread")
        val viewModel = buildViewModel(
            trips = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(sampleTrip)),
                gate = gate,
            )
        )

        viewModel.onAddToListClicked(sampleItem) // load parks on the gate
        viewModel.onAddToListClicked(secondItem) // sheet now shows a different item
        gate.complete(Unit) // both loads resume; only the current item's result applies

        val sheet = viewModel.addToListSheet.value as AddToListSheetState.Visible
        assertEquals(secondItem, sheet.item)
        assertTrue(sheet.lists is AddToListSheetState.Lists.Loaded)
    }

    @Test
    fun `onListChosen inserts the item and emits ItemAdded on success`() = runTest {
        val insert = FakeInsertItemUseCase(InsertItemUseCase.Output.Success)
        val viewModel = buildViewModel(
            trips = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(sampleTrip))
            ),
            insert = insert,
        )
        viewModel.onAddToListClicked(sampleItem)

        viewModel.onListChosen(sampleTrip)

        // The sheet closes immediately.
        assertEquals(AddToListSheetState.Hidden, viewModel.addToListSheet.value)
        // The insert carries the chosen list and the sheet's item.
        val inserted = insert.lastItem!!
        assertEquals("l1", inserted.shoppingListId)
        assertEquals("p1", inserted.productId)
        assertEquals(1, inserted.quantity)
        assertTrue(inserted.addInventory)
        // A success event is surfaced.
        val event = viewModel.events.first()
        assertTrue(event is PantryEvent.ItemAdded)
        event as PantryEvent.ItemAdded
        assertEquals("Milk", event.itemName)
        assertEquals("Weekly", event.listName)
    }

    @Test
    fun `onListChosen emits AddFailed when the insert fails`() = runTest {
        val insert = FakeInsertItemUseCase(InsertItemUseCase.Output.Failure(IOException("boom")))
        val viewModel = buildViewModel(
            trips = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(sampleTrip))
            ),
            insert = insert,
        )
        viewModel.onAddToListClicked(sampleItem)

        viewModel.onListChosen(sampleTrip)

        val event = viewModel.events.first()
        assertTrue(event is PantryEvent.AddFailed)
        assertEquals("Milk", (event as PantryEvent.AddFailed).itemName)
    }

    @Test
    fun `onListChosen does nothing when the sheet is hidden`() = runTest {
        val insert = FakeInsertItemUseCase()
        val viewModel = buildViewModel(insert = insert)

        viewModel.onListChosen(sampleTrip)

        assertNull(insert.lastItem)
        assertEquals(AddToListSheetState.Hidden, viewModel.addToListSheet.value)
    }

    @Test
    fun `dismissAddToListSheet hides the sheet`() = runTest {
        val viewModel = buildViewModel(
            trips = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(sampleTrip))
            )
        )
        viewModel.onAddToListClicked(sampleItem)
        assertTrue(viewModel.addToListSheet.value is AddToListSheetState.Visible)

        viewModel.dismissAddToListSheet()

        assertEquals(AddToListSheetState.Hidden, viewModel.addToListSheet.value)
    }
}