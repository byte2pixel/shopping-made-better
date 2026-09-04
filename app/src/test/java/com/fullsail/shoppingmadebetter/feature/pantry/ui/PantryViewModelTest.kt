package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.core.ui.ShoppingListPickerState
import com.fullsail.shoppingmadebetter.feature.pantry.domain.AdjustmentReason
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ApplyInventoryAdjustment
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ApplyInventoryAdjustmentUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.DeleteInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.EstimateSource
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetPantryEstimateAlertsUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetPantryEstimateAlertsUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetSkipRemoveConfirmationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.domain.SetSkipRemoveConfirmationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UndoInventoryAdjustment
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UndoInventoryAdjustmentUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryExpiry
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryExpiryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLocationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThreshold
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThresholdUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.groupInventoryByProduct
import com.fullsail.shoppingmadebetter.feature.profile.domain.GetAutoAdjustEnabledUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** A successful inventory fetch of [items], grouped the way the real use case returns it. */
private fun inventoryOf(vararg items: InventoryItem) =
    GetInventoryUseCase.Output.Success(groupInventoryByProduct(items.toList()))

/** Every lot across the state's product groups, flattened in display order. */
private val PantryUiState.Success.lots: List<InventoryItem>
    get() = productGroups.flatMap { it.lots }

/**
 * Unit tests for [PantryViewModel]. Each collaborator is a hand-written fake, and
 * [MainDispatcherRule] backs `viewModelScope` with an unconfined test dispatcher
 * so launched work runs eagerly to its first suspension point â€” making the state
 * observable synchronously after each call.
 */
class PantryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Fake inventory use case: returns a settable [output]. */
    private class FakeGetInventoryUseCase(
        var output: GetInventoryUseCase.Output = inventoryOf(),
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

    /** Fake adjustment use case: records its input and returns a settable [output]. */
    private class FakeApplyInventoryAdjustmentUseCase(
        var output: ApplyInventoryAdjustmentUseCase.Output =
            ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 0, appliedDelta = 0),
    ) : ApplyInventoryAdjustmentUseCase {
        var lastInput: ApplyInventoryAdjustment? = null
        override suspend fun execute(
            input: ApplyInventoryAdjustment,
        ): ApplyInventoryAdjustmentUseCase.Output {
            lastInput = input
            return output
        }
    }

    /** Fake undo use case: records the last input and returns a settable [output]. */
    private class FakeUndoInventoryAdjustmentUseCase(
        var output: UndoInventoryAdjustmentUseCase.Output =
            UndoInventoryAdjustmentUseCase.Output.Success(newQuantity = 3, appliedDelta = 2),
    ) : UndoInventoryAdjustmentUseCase {
        var lastInput: UndoInventoryAdjustment? = null
        override suspend fun execute(
            input: UndoInventoryAdjustment,
        ): UndoInventoryAdjustmentUseCase.Output {
            lastInput = input
            return output
        }
    }

    /** Fake auto-adjust flag: returns a settable [output]. */
    private class FakeGetAutoAdjustEnabledUseCase(
        var output: GetAutoAdjustEnabledUseCase.Output =
            GetAutoAdjustEnabledUseCase.Output.Success(enabled = true),
    ) : GetAutoAdjustEnabledUseCase {
        override suspend fun execute(input: Unit) = output
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

    /** Fake expiry-update use case: records its input and returns a settable [output]. */
    private class FakeUpdateInventoryExpiryUseCase(
        var output: UpdateInventoryExpiryUseCase.Output = UpdateInventoryExpiryUseCase.Output.Success,
    ) : UpdateInventoryExpiryUseCase {
        var lastInput: UpdateInventoryExpiry? = null
        override suspend fun execute(input: UpdateInventoryExpiry): UpdateInventoryExpiryUseCase.Output {
            lastInput = input
            return output
        }
    }

    /** Fake threshold-update use case: records its input and returns a settable [output]. */
    private class FakeUpdateInventoryLowStockThresholdUseCase(
        var output: UpdateInventoryLowStockThresholdUseCase.Output =
            UpdateInventoryLowStockThresholdUseCase.Output.Success,
    ) : UpdateInventoryLowStockThresholdUseCase {
        var lastInput: UpdateInventoryLowStockThreshold? = null
        override suspend fun execute(
            input: UpdateInventoryLowStockThreshold,
        ): UpdateInventoryLowStockThresholdUseCase.Output {
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
        applyAdjustment: FakeApplyInventoryAdjustmentUseCase = FakeApplyInventoryAdjustmentUseCase(),
        updateLocation: FakeUpdateInventoryLocationUseCase = FakeUpdateInventoryLocationUseCase(),
        updateExpiry: FakeUpdateInventoryExpiryUseCase = FakeUpdateInventoryExpiryUseCase(),
        updateThreshold: FakeUpdateInventoryLowStockThresholdUseCase =
            FakeUpdateInventoryLowStockThresholdUseCase(),
        alerts: GetPantryEstimateAlertsUseCase = GetPantryEstimateAlertsUseCaseImpl(),
        undoAdjustment: FakeUndoInventoryAdjustmentUseCase = FakeUndoInventoryAdjustmentUseCase(),
        autoAdjust: FakeGetAutoAdjustEnabledUseCase = FakeGetAutoAdjustEnabledUseCase(),
    ) = PantryViewModel(
        inventory, trips, insert, delete, deleteInventory, getSkip, setSkip, applyAdjustment,
        updateLocation, updateExpiry, updateThreshold, alerts, undoAdjustment, autoAdjust,
    )

    @Test
    fun `initial load exposes Success with the inventory items`() = runTest {
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(sampleItem))
        )

        val state = viewModel.uiState.value
        assertTrue(state is PantryUiState.Success)
        assertEquals(listOf(sampleItem), (state as PantryUiState.Success).lots)
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
        val inventory = FakeGetInventoryUseCase(inventoryOf())
        val viewModel = buildViewModel(inventory = inventory)
        assertEquals(emptyList<InventoryItem>(), (viewModel.uiState.value as PantryUiState.Success).lots)

        inventory.output = inventoryOf(sampleItem)
        viewModel.loadInventory()

        val state = viewModel.uiState.value
        assertTrue(state is PantryUiState.Success)
        assertEquals(listOf(sampleItem), (state as PantryUiState.Success).lots)
    }

    @Test
    fun `a failed refresh keeps the items on screen and emits RefreshFailed`() = runTest {
        val inventory = FakeGetInventoryUseCase(inventoryOf(sampleItem))
        val viewModel = buildViewModel(inventory = inventory)

        inventory.output = GetInventoryUseCase.Output.Failure(IOException("no network"))
        viewModel.loadInventory()

        val state = viewModel.uiState.value
        assertTrue(state is PantryUiState.Success)
        assertEquals(listOf(sampleItem), (state as PantryUiState.Success).lots)
        assertEquals(PantryEvent.RefreshFailed, viewModel.events.first())
    }

    @Test
    fun `a failed first load errors without emitting RefreshFailed`() = runTest {
        // The error state already says it; a snackbar over it would be saying it twice.
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(
                GetInventoryUseCase.Output.Failure(IOException("no network"))
            )
        )

        assertTrue(viewModel.uiState.value is PantryUiState.Error)
        assertNull(withTimeoutOrNull(1_000) { viewModel.events.first() })
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
        assertTrue(sheet.lists is ShoppingListPickerState.Loaded)
        assertEquals(
            listOf(sampleTrip),
            (sheet.lists as ShoppingListPickerState.Loaded).trips,
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
        assertTrue(sheet.lists is ShoppingListPickerState.Empty)
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
        assertTrue(sheet.lists is ShoppingListPickerState.Error)
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
        assertTrue(loading.lists is ShoppingListPickerState.Loading)

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
        assertTrue(sheet.lists is ShoppingListPickerState.Loaded)
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
            inventory = FakeGetInventoryUseCase(inventoryOf(sampleItem, otherItem)),
            deleteInventory = FakeDeleteInventoryItemUseCase(DeleteInventoryItemUseCase.Output.Success),
        )
        viewModel.onRemoveClicked(sampleItem)

        viewModel.confirmRemove(dontAskAgain = false)

        // State stays Success (never flips to Loading) and only the removed item is gone.
        val state = viewModel.uiState.value
        assertTrue(state is PantryUiState.Success)
        assertEquals(listOf(otherItem), (state as PantryUiState.Success).lots)
    }

    @Test
    fun `confirmRemove keeps the product group while other lots remain`() = runTest {
        // Two lots of the same product: removing one lot must not take the card with it.
        val lotA = sampleItem.copy(id = "a")
        val lotB = sampleItem.copy(id = "b")
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(lotA, lotB)),
        )
        viewModel.onRemoveClicked(lotA)

        viewModel.confirmRemove(dontAskAgain = false)

        val groups = (viewModel.uiState.value as PantryUiState.Success).productGroups
        assertEquals(listOf(lotB), groups.single().lots)
    }

    @Test
    fun `confirmRemove drops the whole product group with its last lot`() = runTest {
        val otherProduct = sampleItem.copy(id = "i2", productId = "p2", name = "Bread")
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(sampleItem, otherProduct)),
        )
        viewModel.onRemoveClicked(sampleItem)

        viewModel.confirmRemove(dontAskAgain = false)

        val groups = (viewModel.uiState.value as PantryUiState.Success).productGroups
        assertEquals(listOf("p2"), groups.map { it.productId })
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
    fun `onQuantityChanged updates the item in place and persists a manual adjustment`() = runTest {
        val update = FakeApplyInventoryAdjustmentUseCase(
            ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 5, appliedDelta = 3)
        )
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(sampleItem)),
            applyAdjustment = update,
        )

        viewModel.onQuantityChanged(sampleItem, newQuantity = 5)

        // The list reflects the new quantity immediately; the delta persists as 'manual'.
        val items = (viewModel.uiState.value as PantryUiState.Success).lots
        assertEquals(5, items.single().quantity)
        assertEquals(
            ApplyInventoryAdjustment(id = "i1", delta = 3, reason = AdjustmentReason.Manual),
            update.lastInput,
        )
    }

    @Test
    fun `onQuantityChanged reconciles to the quantity the backend applied`() = runTest {
        // The RPC floors at zero, so the applied quantity can differ from the request.
        val update = FakeApplyInventoryAdjustmentUseCase(
            ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 0, appliedDelta = -2)
        )
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(sampleItem)),
            applyAdjustment = update,
        )

        viewModel.onQuantityChanged(sampleItem, newQuantity = 0)

        val items = (viewModel.uiState.value as PantryUiState.Success).lots
        assertEquals(0, items.single().quantity)
    }

    @Test
    fun `onQuantityChanged reverts and emits UpdateFailed when the save fails`() = runTest {
        val update = FakeApplyInventoryAdjustmentUseCase(
            ApplyInventoryAdjustmentUseCase.Output.Failure(IOException("boom"))
        )
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(sampleItem)),
            applyAdjustment = update,
        )

        viewModel.onQuantityChanged(sampleItem, newQuantity = 5)

        // The optimistic change is rolled back to the original quantity.
        val items = (viewModel.uiState.value as PantryUiState.Success).lots
        assertEquals(sampleItem.quantity, items.single().quantity)
        val event = viewModel.events.first()
        assertTrue(event is PantryEvent.UpdateFailed)
        assertEquals("Milk", (event as PantryEvent.UpdateFailed).itemName)
    }

    @Test
    fun `onQuantityChanged patches only the target lot within its product group`() = runTest {
        val lotA = sampleItem.copy(id = "a", quantity = 2)
        val lotB = sampleItem.copy(id = "b", quantity = 3)
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(lotA, lotB)),
            applyAdjustment = FakeApplyInventoryAdjustmentUseCase(
                ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 7, appliedDelta = 4)
            ),
        )

        viewModel.onQuantityChanged(lotB, newQuantity = 7)

        // The sibling lot is untouched and the group's aggregate follows the edit.
        val group = (viewModel.uiState.value as PantryUiState.Success).productGroups.single()
        assertEquals(2, group.lots.first { it.id == "a" }.quantity)
        assertEquals(7, group.lots.first { it.id == "b" }.quantity)
        assertEquals(9, group.totalQuantity)
    }

    @Test
    fun `onQuantityChanged does nothing when the quantity is unchanged`() = runTest {
        val update = FakeApplyInventoryAdjustmentUseCase()
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(sampleItem)),
            applyAdjustment = update,
        )

        viewModel.onQuantityChanged(sampleItem, newQuantity = sampleItem.quantity)

        assertNull(update.lastInput)
    }

    @Test
    fun `onQuantityChanged clears the estimated marker`() = runTest {
        val estimatedItem = sampleItem.copy(lastAdjustmentReason = AdjustmentReason.Auto, estimateSource = EstimateSource.History)
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(estimatedItem)),
            applyAdjustment = FakeApplyInventoryAdjustmentUseCase(
                ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 5, appliedDelta = 3)
            ),
        )

        viewModel.onQuantityChanged(estimatedItem, newQuantity = 5)

        val items = (viewModel.uiState.value as PantryUiState.Success).lots
        assertTrue(items.none { it.estimated })
    }

    @Test
    fun `onConfirmEstimate persists a zero-delta confirmed adjustment and clears the marker`() = runTest {
        val estimatedItem = sampleItem.copy(lastAdjustmentReason = AdjustmentReason.Auto, estimateSource = EstimateSource.History)
        val update = FakeApplyInventoryAdjustmentUseCase(
            ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 2, appliedDelta = 0)
        )
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(estimatedItem)),
            applyAdjustment = update,
        )

        viewModel.onConfirmEstimate(estimatedItem)

        assertEquals(
            ApplyInventoryAdjustment(id = "i1", delta = 0, reason = AdjustmentReason.Confirmed),
            update.lastInput,
        )
        val lot = (viewModel.uiState.value as PantryUiState.Success).lots.single()
        assertEquals(estimatedItem.quantity, lot.quantity)
        assertTrue(!lot.estimated)
    }

    @Test
    fun `onCorrectEstimate persists the delta as confirmed and updates the quantity`() = runTest {
        val estimatedItem = sampleItem.copy(lastAdjustmentReason = AdjustmentReason.Auto, estimateSource = EstimateSource.ShelfLife)
        val update = FakeApplyInventoryAdjustmentUseCase(
            ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 4, appliedDelta = 2)
        )
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(estimatedItem)),
            applyAdjustment = update,
        )

        viewModel.onCorrectEstimate(estimatedItem, newQuantity = 4)

        assertEquals(
            ApplyInventoryAdjustment(id = "i1", delta = 2, reason = AdjustmentReason.Confirmed),
            update.lastInput,
        )
        val lot = (viewModel.uiState.value as PantryUiState.Success).lots.single()
        assertEquals(4, lot.quantity)
        assertTrue(!lot.estimated)
    }

    @Test
    fun `loadInventory hides estimates and alerts when auto-adjust is off`() = runTest {
        val estimatedItem = sampleItem.copy(quantity = 0, lastAdjustmentReason = AdjustmentReason.Auto, lastAdjustmentId = "a1")
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(estimatedItem)),
            autoAdjust = FakeGetAutoAdjustEnabledUseCase(
                GetAutoAdjustEnabledUseCase.Output.Success(enabled = false)
            ),
        )

        val lot = (viewModel.uiState.value as PantryUiState.Success).lots.single()
        assertNull(lot.lastAdjustmentReason)
        assertFalse(lot.estimated)
        assertFalse(lot.canUndo)
        assertNull(viewModel.zeroStockAlert.value)
    }

    @Test
    fun `loadInventory keeps estimates when auto-adjust is on or its read fails`() = runTest {
        val estimatedItem = sampleItem.copy(quantity = 0, lastAdjustmentReason = AdjustmentReason.Auto, lastAdjustmentId = "a1")
        val on = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(estimatedItem)),
            autoAdjust = FakeGetAutoAdjustEnabledUseCase(
                GetAutoAdjustEnabledUseCase.Output.Success(enabled = true)
            ),
        )
        val unknown = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(estimatedItem)),
            autoAdjust = FakeGetAutoAdjustEnabledUseCase(
                GetAutoAdjustEnabledUseCase.Output.Failure(IOException("boom"))
            ),
        )

        assertTrue((on.uiState.value as PantryUiState.Success).lots.single().estimated)
        assertEquals(estimatedItem, on.zeroStockAlert.value)
        assertTrue((unknown.uiState.value as PantryUiState.Success).lots.single().estimated)
        assertEquals(estimatedItem, unknown.zeroStockAlert.value)
    }

    @Test
    fun `onUndoEstimate reverses the latest auto adjustment and reconciles the quantity`() = runTest {
        val estimatedItem = sampleItem.copy(quantity = 1, lastAdjustmentReason = AdjustmentReason.Auto, lastAdjustmentId = "a1")
        val undo = FakeUndoInventoryAdjustmentUseCase(
            UndoInventoryAdjustmentUseCase.Output.Success(newQuantity = 3, appliedDelta = 2)
        )
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(estimatedItem)),
            undoAdjustment = undo,
        )

        viewModel.onUndoEstimate(estimatedItem)

        assertEquals(UndoInventoryAdjustment(adjustmentId = "a1"), undo.lastInput)
        val lot = (viewModel.uiState.value as PantryUiState.Success).lots.single()
        assertEquals(3, lot.quantity)
        assertEquals(AdjustmentReason.Undo, lot.lastAdjustmentReason)
        assertFalse(lot.estimated)
        assertFalse(lot.canUndo)
    }

    @Test
    fun `onUndoEstimate restores the lot and emits UpdateFailed when the undo fails`() = runTest {
        val estimatedItem = sampleItem.copy(quantity = 1, lastAdjustmentReason = AdjustmentReason.Auto, lastAdjustmentId = "a1")
        val undo = FakeUndoInventoryAdjustmentUseCase(
            UndoInventoryAdjustmentUseCase.Output.Failure(IOException("boom"))
        )
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(estimatedItem)),
            undoAdjustment = undo,
        )

        viewModel.onUndoEstimate(estimatedItem)

        assertEquals(estimatedItem, (viewModel.uiState.value as PantryUiState.Success).lots.single())
        assertEquals(PantryEvent.UpdateFailed("Milk"), viewModel.events.first())
    }

    @Test
    fun `onUndoEstimate ignores a lot whose latest adjustment is not auto`() = runTest {
        val dismissedItem = sampleItem.copy(lastAdjustmentReason = AdjustmentReason.Dismissed, lastAdjustmentId = "a1")
        val undo = FakeUndoInventoryAdjustmentUseCase()
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(dismissedItem)),
            undoAdjustment = undo,
        )

        viewModel.onUndoEstimate(dismissedItem)

        assertNull(undo.lastInput)
        assertEquals(dismissedItem, (viewModel.uiState.value as PantryUiState.Success).lots.single())
    }

    @Test
    fun `onConfirmEstimate reverts the marker and emits UpdateFailed when the save fails`() = runTest {
        val estimatedItem = sampleItem.copy(lastAdjustmentReason = AdjustmentReason.Auto, estimateSource = EstimateSource.History)
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(estimatedItem)),
            applyAdjustment = FakeApplyInventoryAdjustmentUseCase(
                ApplyInventoryAdjustmentUseCase.Output.Failure(IOException("boom"))
            ),
        )

        viewModel.onConfirmEstimate(estimatedItem)

        // The lot still reads as estimated so the affordance survives the failure.
        val lot = (viewModel.uiState.value as PantryUiState.Success).lots.single()
        assertTrue(lot.estimated)
        assertTrue(viewModel.events.first() is PantryEvent.UpdateFailed)
    }

    @Test
    fun `onLowStockThresholdChanged updates the item in place and persists the threshold`() = runTest {
        val update = FakeUpdateInventoryLowStockThresholdUseCase()
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(sampleItem)),
            updateThreshold = update,
        )

        viewModel.onLowStockThresholdChanged(sampleItem, newThreshold = 3)

        val items = (viewModel.uiState.value as PantryUiState.Success).lots
        assertEquals(3, items.single().lowStockThreshold)
        assertEquals(UpdateInventoryLowStockThreshold(productId = "p1", threshold = 3), update.lastInput)
    }

    @Test
    fun `onLowStockThresholdChanged updates every pantry entry of the same product`() = runTest {
        // Two separate pantry rows for the same product (p1); the threshold is per-product,
        // so setting it on one must update both.
        val entryA = sampleItem.copy(id = "a")
        val entryB = sampleItem.copy(id = "b")
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(entryA, entryB)),
        )

        viewModel.onLowStockThresholdChanged(entryA, newThreshold = 4)

        val items = (viewModel.uiState.value as PantryUiState.Success).lots
        assertTrue(items.all { it.lowStockThreshold == 4 })
    }

    @Test
    fun `onLowStockThresholdChanged reverts and emits UpdateFailed when the save fails`() = runTest {
        val update = FakeUpdateInventoryLowStockThresholdUseCase(
            UpdateInventoryLowStockThresholdUseCase.Output.Failure(IOException("boom"))
        )
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(sampleItem)),
            updateThreshold = update,
        )

        viewModel.onLowStockThresholdChanged(sampleItem, newThreshold = 3)

        val items = (viewModel.uiState.value as PantryUiState.Success).lots
        assertEquals(sampleItem.lowStockThreshold, items.single().lowStockThreshold)
        val event = viewModel.events.first()
        assertTrue(event is PantryEvent.UpdateFailed)
    }

    @Test
    fun `onLowStockThresholdChanged does nothing when the threshold is unchanged`() = runTest {
        val update = FakeUpdateInventoryLowStockThresholdUseCase()
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(sampleItem)),
            updateThreshold = update,
        )

        viewModel.onLowStockThresholdChanged(sampleItem, newThreshold = sampleItem.lowStockThreshold)

        assertNull(update.lastInput)
    }

    @Test
    fun `onLocationChanged updates the item in place and persists the new location`() = runTest {
        val update = FakeUpdateInventoryLocationUseCase()
        val fridgeItem = sampleItem.copy(location = PantryLocation.Fridge)
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(fridgeItem)),
            updateLocation = update,
        )

        viewModel.onLocationChanged(fridgeItem, newLocation = PantryLocation.Freezer)

        val items = (viewModel.uiState.value as PantryUiState.Success).lots
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
            inventory = FakeGetInventoryUseCase(inventoryOf(fridgeItem)),
            updateLocation = update,
        )

        viewModel.onLocationChanged(fridgeItem, newLocation = PantryLocation.Freezer)

        val items = (viewModel.uiState.value as PantryUiState.Success).lots
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
            inventory = FakeGetInventoryUseCase(inventoryOf(fridgeItem)),
            updateLocation = update,
        )

        viewModel.onLocationChanged(fridgeItem, newLocation = PantryLocation.Fridge)

        assertNull(update.lastInput)
    }

    @Test
    fun `onExpiryChanged updates the item in place and persists the new day offset`() = runTest {
        val update = FakeUpdateInventoryExpiryUseCase()
        val soonItem = sampleItem.copy(expiresInDays = 4)
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(soonItem)),
            updateExpiry = update,
        )

        viewModel.onExpiryChanged(soonItem, newExpiresInDays = 7)

        val items = (viewModel.uiState.value as PantryUiState.Success).lots
        assertEquals(7, items.single().expiresInDays)
        assertEquals(UpdateInventoryExpiry(id = "i1", expiresInDays = 7), update.lastInput)
    }

    @Test
    fun `onExpiryChanged reverts and emits UpdateFailed when the save fails`() = runTest {
        val update = FakeUpdateInventoryExpiryUseCase(
            UpdateInventoryExpiryUseCase.Output.Failure(IOException("boom"))
        )
        val soonItem = sampleItem.copy(expiresInDays = 4)
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(soonItem)),
            updateExpiry = update,
        )

        viewModel.onExpiryChanged(soonItem, newExpiresInDays = 7)

        val items = (viewModel.uiState.value as PantryUiState.Success).lots
        assertEquals(4, items.single().expiresInDays)
        val event = viewModel.events.first()
        assertTrue(event is PantryEvent.UpdateFailed)
        assertEquals("Milk", (event as PantryEvent.UpdateFailed).itemName)
    }

    @Test
    fun `onExpiryChanged does nothing when the day offset is unchanged`() = runTest {
        val update = FakeUpdateInventoryExpiryUseCase()
        val soonItem = sampleItem.copy(expiresInDays = 4)
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(soonItem)),
            updateExpiry = update,
        )

        viewModel.onExpiryChanged(soonItem, newExpiresInDays = 4)

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

    // --- Zero-stock gate ---

    private val emptyEstimatedItem = sampleItem.copy(
        quantity = 0,
        lastAdjustmentReason = AdjustmentReason.Auto,
        estimateSource = EstimateSource.History,
    )

    @Test
    fun `zeroStockAlert surfaces an auto-adjusted lot at zero once inventory loads`() = runTest {
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(emptyEstimatedItem)),
        )

        assertEquals("i1", viewModel.zeroStockAlert.value?.id)
    }

    @Test
    fun `zeroStockAlert ignores an auto-adjusted lot that still has stock`() = runTest {
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(emptyEstimatedItem.copy(quantity = 2))),
        )

        assertNull(viewModel.zeroStockAlert.value)
    }

    @Test
    fun `zeroStockAlert ignores a lot whose latest adjustment was dismissed`() = runTest {
        val dismissed = emptyEstimatedItem.copy(lastAdjustmentReason = AdjustmentReason.Dismissed)
        val viewModel = buildViewModel(inventory = FakeGetInventoryUseCase(inventoryOf(dismissed)))

        assertNull(viewModel.zeroStockAlert.value)
    }

    @Test
    fun `zeroStockAlert is null while the first load has not succeeded`() = runTest {
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(GetInventoryUseCase.Output.Failure(IOException("boom"))),
        )

        assertEquals(PantryUiState.Error, viewModel.uiState.value)
        assertNull(viewModel.zeroStockAlert.value)
    }

    @Test
    fun `onZeroStockOut confirms the lot at zero, opens the add-to-list sheet and clears the alert`() =
        runTest {
            val update = FakeApplyInventoryAdjustmentUseCase()
            val viewModel = buildViewModel(
                inventory = FakeGetInventoryUseCase(inventoryOf(emptyEstimatedItem)),
                trips = FakeGetShoppingTripsUseCase(
                    GetShoppingTripsUseCase.Output.Success(listOf(sampleTrip))
                ),
                applyAdjustment = update,
            )

            viewModel.onZeroStockOut(emptyEstimatedItem)

            assertEquals(
                ApplyInventoryAdjustment(id = "i1", delta = 0, reason = AdjustmentReason.Confirmed),
                update.lastInput,
            )
            val sheet = viewModel.addToListSheet.value
            assertTrue(sheet is AddToListSheetState.Visible && sheet.item.id == "i1")
            assertNull(viewModel.zeroStockAlert.value)
            val lot = (viewModel.uiState.value as PantryUiState.Success).lots.single()
            assertEquals(AdjustmentReason.Confirmed, lot.lastAdjustmentReason)

            viewModel.dismissAddToListSheet()

            assertNull(viewModel.zeroStockAlert.value)
        }

    @Test
    fun `onZeroStockStillHave persists the count as confirmed and clears the alert`() = runTest {
        val update = FakeApplyInventoryAdjustmentUseCase(
            ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 3, appliedDelta = 3)
        )
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(emptyEstimatedItem)),
            applyAdjustment = update,
        )

        viewModel.onZeroStockStillHave(emptyEstimatedItem, count = 3)

        assertEquals(
            ApplyInventoryAdjustment(id = "i1", delta = 3, reason = AdjustmentReason.Confirmed),
            update.lastInput,
        )
        val lot = (viewModel.uiState.value as PantryUiState.Success).lots.single()
        assertEquals(3, lot.quantity)
        assertFalse(lot.estimated)
        assertNull(viewModel.zeroStockAlert.value)
    }

    @Test
    fun `onZeroStockDismissed persists a zero-delta dismissed adjustment and keeps the lot estimated`() =
        runTest {
            val update = FakeApplyInventoryAdjustmentUseCase()
            val viewModel = buildViewModel(
                inventory = FakeGetInventoryUseCase(inventoryOf(emptyEstimatedItem)),
                applyAdjustment = update,
            )

            viewModel.onZeroStockDismissed(emptyEstimatedItem)

            assertEquals(
                ApplyInventoryAdjustment(id = "i1", delta = 0, reason = AdjustmentReason.Dismissed),
                update.lastInput,
            )
            val lot = (viewModel.uiState.value as PantryUiState.Success).lots.single()
            assertEquals(AdjustmentReason.Dismissed, lot.lastAdjustmentReason)
            assertTrue(lot.estimated)
            assertNull(viewModel.zeroStockAlert.value)
        }

    @Test
    fun `onZeroStockDismissed does not re-prompt when the save fails`() = runTest {
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(emptyEstimatedItem)),
            applyAdjustment = FakeApplyInventoryAdjustmentUseCase(
                ApplyInventoryAdjustmentUseCase.Output.Failure(IOException("boom"))
            ),
        )

        viewModel.onZeroStockDismissed(emptyEstimatedItem)

        val lot = (viewModel.uiState.value as PantryUiState.Success).lots.single()
        assertEquals(AdjustmentReason.Auto, lot.lastAdjustmentReason)
        assertTrue(viewModel.events.first() is PantryEvent.UpdateFailed)
        assertNull(viewModel.zeroStockAlert.value)
    }

    @Test
    fun `zeroStockAlert moves to the next zero lot once the first is handled`() = runTest {
        val first = emptyEstimatedItem.copy(id = "a", productId = "pa", expiresInDays = 1)
        val second = emptyEstimatedItem.copy(id = "b", productId = "pb", expiresInDays = 5)
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(second, first)),
        )
        assertEquals("a", viewModel.zeroStockAlert.value?.id)

        viewModel.onZeroStockDismissed(first)
        assertEquals("b", viewModel.zeroStockAlert.value?.id)

        viewModel.onZeroStockDismissed(second)
        assertNull(viewModel.zeroStockAlert.value)
    }

    @Test
    fun `zeroStockAlert is hidden while the add-to-list sheet is open`() = runTest {
        val other = sampleItem.copy(id = "i2", productId = "p2")
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(emptyEstimatedItem, other)),
            trips = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(sampleTrip))
            ),
        )

        viewModel.onAddToListClicked(other)
        assertNull(viewModel.zeroStockAlert.value)

        viewModel.dismissAddToListSheet()
        assertEquals("i1", viewModel.zeroStockAlert.value?.id)
    }

    @Test
    fun `zeroStockAlert is hidden while the remove dialog is open`() = runTest {
        val other = sampleItem.copy(id = "i2", productId = "p2")
        val viewModel = buildViewModel(
            inventory = FakeGetInventoryUseCase(inventoryOf(emptyEstimatedItem, other)),
        )

        viewModel.onRemoveClicked(other)
        assertNull(viewModel.zeroStockAlert.value)

        viewModel.dismissRemove()
        assertEquals("i1", viewModel.zeroStockAlert.value?.id)
    }

    @Test
    fun `loadInventory refreshes the alert from the new inventory`() = runTest {
        val inventory = FakeGetInventoryUseCase(inventoryOf(sampleItem))
        val viewModel = buildViewModel(inventory = inventory)
        assertNull(viewModel.zeroStockAlert.value)

        inventory.output = inventoryOf(emptyEstimatedItem)
        viewModel.loadInventory()
        assertEquals("i1", viewModel.zeroStockAlert.value?.id)

        inventory.output = inventoryOf(emptyEstimatedItem.copy(quantity = 2))
        viewModel.loadInventory()
        assertNull(viewModel.zeroStockAlert.value)
    }
}
