package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.DeleteInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetSkipRemoveConfirmationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.domain.SetSkipRemoveConfirmationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLocationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryQuantity
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryQuantityUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCase
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
        var output: InsertItemUseCase.Output = InsertItemUseCase.Output.Success("sli-1"),
    ) : InsertItemUseCase {
        var lastItem: InsertItem? = null
        override suspend fun execute(input: InsertItem): InsertItemUseCase.Output {
            lastItem = input
            return output
        }
    }

    /** Fake delete use case: records the id it received and returns [output]. */
    private class FakeDeleteItemsUseCase(
        var output: DeleteItemsUseCase.Output = DeleteItemsUseCase.Output.Success,
    ) : DeleteItemsUseCase {
        var lastId: String? = null
        override suspend fun execute(input: String): DeleteItemsUseCase.Output {
            lastId = input
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

    /** Fake skip-confirmation read: returns a settable [value]. */
    private class FakeGetSkipRemoveConfirmationUseCase(
        var value: Boolean = false,
    ) : GetSkipRemoveConfirmationUseCase {
        override suspend fun execute(input: Unit): Boolean = value
    }

    /** Fake skip-confirmation write: records the last value it persisted. */
    private class FakeSetSkipRemoveConfirmationUseCase : SetSkipRemoveConfirmationUseCase {
        var lastValue: Boolean? = null
        override suspend fun execute(input: Boolean) {
            lastValue = input
        }
    }

    /** Fake quantity-update use case: records its input and returns a settable [output]. */
    private class FakeUpdateInventoryQuantityUseCase(
        var output: UpdateInventoryQuantityUseCase.Output = UpdateInventoryQuantityUseCase.Output.Success,
    ) : UpdateInventoryQuantityUseCase {
        var lastInput: UpdateInventoryQuantity? = null
        override suspend fun execute(input: UpdateInventoryQuantity): UpdateInventoryQuantityUseCase.Output {
            lastInput = input
            return output
        }
    }

    /** Fake location-update use case: records its input and returns a settable [output]. */
    private class FakeUpdateInventoryLocationUseCase(
        var output: UpdateInventoryLocationUseCase.Output = UpdateInventoryLocationUseCase.Output.Success,
    ) : UpdateInventoryLocationUseCase {
        var lastInput: UpdateInventoryLocation? = null
        override suspend fun execute(input: UpdateInventoryLocation): UpdateInventoryLocationUseCase.Output {
            lastInput = input
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
        delete: FakeDeleteItemsUseCase = FakeDeleteItemsUseCase(),
        deleteInventory: FakeDeleteInventoryItemUseCase = FakeDeleteInventoryItemUseCase(),
        getSkip: FakeGetSkipRemoveConfirmationUseCase = FakeGetSkipRemoveConfirmationUseCase(),
        setSkip: FakeSetSkipRemoveConfirmationUseCase = FakeSetSkipRemoveConfirmationUseCase(),
        updateQuantity: FakeUpdateInventoryQuantityUseCase = FakeUpdateInventoryQuantityUseCase(),
        updateLocation: FakeUpdateInventoryLocationUseCase = FakeUpdateInventoryLocationUseCase(),
    ) = PantryViewModel(
        inventory, trips, insert, delete, deleteInventory, getSkip, setSkip, updateQuantity,
        updateLocation,
    )

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
        val insert = FakeInsertItemUseCase(InsertItemUseCase.Output.Success("sli-9"))
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
        // A success event is surfaced, with the new item id so undo can use it.
        val event = viewModel.events.first()
        assertTrue(event is PantryEvent.ItemAdded)
        event as PantryEvent.ItemAdded
        assertEquals("Milk", event.itemName)
        assertEquals("Weekly", event.listName)
        assertEquals("sli-9", event.insertedItemId)
    }

    @Test
    fun `undoAdd deletes the inserted item and emits ItemRemoved on success`() = runTest {
        val delete = FakeDeleteItemsUseCase(DeleteItemsUseCase.Output.Success)
        val viewModel = buildViewModel(delete = delete)

        viewModel.undoAdd(insertedItemId = "sli-9", itemName = "Milk")

        assertEquals("sli-9", delete.lastId)
        val event = viewModel.events.first()
        assertTrue(event is PantryEvent.ItemRemoved)
        assertEquals("Milk", (event as PantryEvent.ItemRemoved).itemName)
    }

    @Test
    fun `undoAdd emits UndoFailed when the delete fails`() = runTest {
        val delete = FakeDeleteItemsUseCase(DeleteItemsUseCase.Output.Failure(IOException("boom")))
        val viewModel = buildViewModel(delete = delete)

        viewModel.undoAdd(insertedItemId = "sli-9", itemName = "Milk")

        val event = viewModel.events.first()
        assertTrue(event is PantryEvent.UndoFailed)
        assertEquals("Milk", (event as PantryEvent.UndoFailed).itemName)
    }

    @Test
    fun `onRemoveClicked opens the confirmation dialog for the item`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onRemoveClicked(sampleItem)

        assertEquals(sampleItem, viewModel.removeConfirm.value)
    }

    @Test
    fun `dismissRemove closes the dialog without deleting`() = runTest {
        val deleteInventory = FakeDeleteInventoryItemUseCase()
        val viewModel = buildViewModel(deleteInventory = deleteInventory)
        viewModel.onRemoveClicked(sampleItem)

        viewModel.dismissRemove()

        assertNull(viewModel.removeConfirm.value)
        assertNull(deleteInventory.lastId)
    }

    @Test
    fun `confirmRemove deletes the item and emits RemovedFromPantry on success`() = runTest {
        val deleteInventory = FakeDeleteInventoryItemUseCase(DeleteInventoryItemUseCase.Output.Success)
        val viewModel = buildViewModel(deleteInventory = deleteInventory)
        viewModel.onRemoveClicked(sampleItem)

        viewModel.confirmRemove(dontAskAgain = false)

        // The dialog closes and exactly the chosen item's id is deleted.
        assertNull(viewModel.removeConfirm.value)
        assertEquals("i1", deleteInventory.lastId)
        val event = viewModel.events.first()
        assertTrue(event is PantryEvent.RemovedFromPantry)
        assertEquals("Milk", (event as PantryEvent.RemovedFromPantry).itemName)
    }

    @Test
    fun `confirmRemove drops the item from the list in place without a loading flash`() = runTest {
        val otherItem = sampleItem.copy(id = "i2", productId = "p2", name = "Bread")
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(
                GetInventoryUseCase.Output.Success(listOf(sampleItem, otherItem))
            ),
            deleteInventory = FakeDeleteInventoryItemUseCase(DeleteInventoryItemUseCase.Output.Success),
        )
        viewModel.onRemoveClicked(sampleItem)

        viewModel.confirmRemove(dontAskAgain = false)

        // State stays Success (never flips to Loading) and only the removed item is gone.
        val state = viewModel.uiState.value
        assertTrue(state is PantryUiState.Success)
        assertEquals(listOf(otherItem), (state as PantryUiState.Success).inventoryItems)
    }

    @Test
    fun `confirmRemove emits RemoveFailed when the delete fails`() = runTest {
        val deleteInventory = FakeDeleteInventoryItemUseCase(
            DeleteInventoryItemUseCase.Output.Failure(IOException("boom"))
        )
        val viewModel = buildViewModel(deleteInventory = deleteInventory)
        viewModel.onRemoveClicked(sampleItem)

        viewModel.confirmRemove(dontAskAgain = false)

        val event = viewModel.events.first()
        assertTrue(event is PantryEvent.RemoveFailed)
        assertEquals("Milk", (event as PantryEvent.RemoveFailed).itemName)
    }

    @Test
    fun `confirmRemove does nothing when no item is pending`() = runTest {
        val deleteInventory = FakeDeleteInventoryItemUseCase()
        val viewModel = buildViewModel(deleteInventory = deleteInventory)

        viewModel.confirmRemove(dontAskAgain = false)

        assertNull(deleteInventory.lastId)
    }

    @Test
    fun `onRemoveClicked removes directly without a dialog when skip is set`() = runTest {
        val deleteInventory = FakeDeleteInventoryItemUseCase()
        val viewModel = buildViewModel(
            deleteInventory = deleteInventory,
            getSkip = FakeGetSkipRemoveConfirmationUseCase(value = true),
        )

        viewModel.onRemoveClicked(sampleItem)

        // No dialog is shown; the item is deleted directly and removal is reported.
        assertNull(viewModel.removeConfirm.value)
        assertEquals("i1", deleteInventory.lastId)
        assertTrue(viewModel.events.first() is PantryEvent.RemovedFromPantry)
    }

    @Test
    fun `confirmRemove persists the skip preference when dontAskAgain is set`() = runTest {
        val deleteInventory = FakeDeleteInventoryItemUseCase()
        val setSkip = FakeSetSkipRemoveConfirmationUseCase()
        val viewModel = buildViewModel(deleteInventory = deleteInventory, setSkip = setSkip)
        viewModel.onRemoveClicked(sampleItem)

        viewModel.confirmRemove(dontAskAgain = true)

        // The choice is persisted and the item is still deleted.
        assertEquals(true, setSkip.lastValue)
        assertEquals("i1", deleteInventory.lastId)
    }

    @Test
    fun `confirmRemove does not persist the skip preference when dontAskAgain is unset`() = runTest {
        val setSkip = FakeSetSkipRemoveConfirmationUseCase()
        val viewModel = buildViewModel(setSkip = setSkip)
        viewModel.onRemoveClicked(sampleItem)

        viewModel.confirmRemove(dontAskAgain = false)

        assertNull(setSkip.lastValue)
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
    fun `onQuantityChanged updates the item in place and persists the new quantity`() = runTest {
        val update = FakeUpdateInventoryQuantityUseCase()
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(
                GetInventoryUseCase.Output.Success(listOf(sampleItem))
            ),
            updateQuantity = update,
        )

        viewModel.onQuantityChanged(sampleItem, newQuantity = 5)

        // The list reflects the new quantity immediately and the id/value are persisted.
        val items = (viewModel.uiState.value as PantryUiState.Success).inventoryItems
        assertEquals(5, items.single().quantity)
        assertEquals(UpdateInventoryQuantity(id = "i1", quantity = 5), update.lastInput)
    }

    @Test
    fun `onQuantityChanged reverts and emits UpdateFailed when the save fails`() = runTest {
        val update = FakeUpdateInventoryQuantityUseCase(
            UpdateInventoryQuantityUseCase.Output.Failure(IOException("boom"))
        )
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(
                GetInventoryUseCase.Output.Success(listOf(sampleItem))
            ),
            updateQuantity = update,
        )

        viewModel.onQuantityChanged(sampleItem, newQuantity = 5)

        // The optimistic change is rolled back to the original quantity.
        val items = (viewModel.uiState.value as PantryUiState.Success).inventoryItems
        assertEquals(sampleItem.quantity, items.single().quantity)
        val event = viewModel.events.first()
        assertTrue(event is PantryEvent.UpdateFailed)
        assertEquals("Milk", (event as PantryEvent.UpdateFailed).itemName)
    }

    @Test
    fun `onQuantityChanged does nothing when the quantity is unchanged`() = runTest {
        val update = FakeUpdateInventoryQuantityUseCase()
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(
                GetInventoryUseCase.Output.Success(listOf(sampleItem))
            ),
            updateQuantity = update,
        )

        viewModel.onQuantityChanged(sampleItem, newQuantity = sampleItem.quantity)

        assertNull(update.lastInput)
    }

    @Test
    fun `onLocationChanged updates the item in place and persists the new location`() = runTest {
        val update = FakeUpdateInventoryLocationUseCase()
        val fridgeItem = sampleItem.copy(location = PantryLocation.Fridge)
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(
                GetInventoryUseCase.Output.Success(listOf(fridgeItem))
            ),
            updateLocation = update,
        )

        viewModel.onLocationChanged(fridgeItem, newLocation = PantryLocation.Freezer)

        val items = (viewModel.uiState.value as PantryUiState.Success).inventoryItems
        assertEquals(PantryLocation.Freezer, items.single().location)
        assertEquals(
            UpdateInventoryLocation(id = "i1", location = PantryLocation.Freezer),
            update.lastInput,
        )
    }

    @Test
    fun `onLocationChanged reverts and emits UpdateFailed when the save fails`() = runTest {
        val update = FakeUpdateInventoryLocationUseCase(
            UpdateInventoryLocationUseCase.Output.Failure(IOException("boom"))
        )
        val fridgeItem = sampleItem.copy(location = PantryLocation.Fridge)
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(
                GetInventoryUseCase.Output.Success(listOf(fridgeItem))
            ),
            updateLocation = update,
        )

        viewModel.onLocationChanged(fridgeItem, newLocation = PantryLocation.Freezer)

        val items = (viewModel.uiState.value as PantryUiState.Success).inventoryItems
        assertEquals(PantryLocation.Fridge, items.single().location)
        val event = viewModel.events.first()
        assertTrue(event is PantryEvent.UpdateFailed)
        assertEquals("Milk", (event as PantryEvent.UpdateFailed).itemName)
    }

    @Test
    fun `onLocationChanged does nothing when the location is unchanged`() = runTest {
        val update = FakeUpdateInventoryLocationUseCase()
        val fridgeItem = sampleItem.copy(location = PantryLocation.Fridge)
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(
                GetInventoryUseCase.Output.Success(listOf(fridgeItem))
            ),
            updateLocation = update,
        )

        viewModel.onLocationChanged(fridgeItem, newLocation = PantryLocation.Fridge)

        assertNull(update.lastInput)
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