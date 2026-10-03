package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.core.ui.ShoppingListPickerState
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingList
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.ShoppingListUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.feature.stores.domain.GetStoresUseCase
import com.fullsail.shoppingmadebetter.feature.stores.domain.Store
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.time.Instant

/** The one store the fakes offer, so a new list always has somewhere to live. */
private val sampleStore = Store(
    id = "s1",
    name = "ALDI",
    address = "1 Main St",
    city = "Orlando",
    state = "FL",
    postalCode = "32801",
    phone = null,
)

/**
 * Unit tests for [AddToListSheetViewModel]. Each collaborator is a hand-written fake, and
 * [MainDispatcherRule] backs `viewModelScope` with an unconfined test dispatcher so
 * launched work runs eagerly to its first suspension point.
 */
class AddToListSheetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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

    /** Fake list-create use case: records the list it was asked for and returns [output]. */
    private class FakeShoppingListUseCase(
        var output: ShoppingListUseCase.Output =
            ShoppingListUseCase.Output.Success(ShoppingList("new-id", "s1", "Weekend", false)),
    ) : ShoppingListUseCase {
        var lastInput: ShoppingList? = null
        override suspend fun execute(input: ShoppingList): ShoppingListUseCase.Output {
            lastInput = input
            return output
        }
    }

    /** Fake stores use case: returns a settable [output]. */
    private class FakeGetStoresUseCase(
        var output: GetStoresUseCase.Output = GetStoresUseCase.Output.Success(listOf(sampleStore)),
    ) : GetStoresUseCase {
        override suspend fun execute(input: Unit): GetStoresUseCase.Output = output
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
        sortOrder = 0,
        createdAt = Instant.parse("2026-09-01T12:00:00Z"),
        updatedAt = Instant.parse("2026-09-01T12:00:00Z"),
    )

    private fun buildViewModel(
        trips: FakeGetShoppingTripsUseCase = FakeGetShoppingTripsUseCase(),
        insert: FakeInsertItemUseCase = FakeInsertItemUseCase(),
        delete: FakeDeleteItemsUseCase = FakeDeleteItemsUseCase(),
        createList: FakeShoppingListUseCase = FakeShoppingListUseCase(),
        stores: FakeGetStoresUseCase = FakeGetStoresUseCase(),
    ) = AddToListSheetViewModel(trips, insert, delete, createList, stores)

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

        viewModel.dismiss()
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
    fun `onAddToListClicked carries the stores a new list could be created at`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onAddToListClicked(sampleItem)

        val sheet = viewModel.addToListSheet.value as AddToListSheetState.Visible
        assertEquals(listOf(sampleStore), sheet.stores)
    }

    @Test
    fun `onAddToListClicked leaves the stores empty when they fail to load`() = runTest {
        val viewModel = buildViewModel(
            stores = FakeGetStoresUseCase(GetStoresUseCase.Output.Failure(IOException("boom")))
        )

        viewModel.onAddToListClicked(sampleItem)

        val sheet = viewModel.addToListSheet.value as AddToListSheetState.Visible
        assertTrue(sheet.stores.isEmpty())
        // The picker itself still loaded, so existing lists stay pickable.
        assertTrue(sheet.lists is ShoppingListPickerState.Empty)
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
        assertTrue(event is AddToListSheetEvent.ItemAdded)
        event as AddToListSheetEvent.ItemAdded
        assertEquals("Milk", event.itemName)
        assertEquals("Weekly", event.listName)
        assertEquals("sli-9", event.insertedItemId)
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
        assertTrue(event is AddToListSheetEvent.AddFailed)
        assertEquals("Milk", (event as AddToListSheetEvent.AddFailed).itemName)
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
    fun `onCreateList creates the list then adds the item to it`() = runTest {
        val createList = FakeShoppingListUseCase(
            ShoppingListUseCase.Output.Success(ShoppingList("new-id", "s1", "Weekend", false))
        )
        val insert = FakeInsertItemUseCase(InsertItemUseCase.Output.Success("sli-3"))
        val viewModel = buildViewModel(insert = insert, createList = createList)
        viewModel.onAddToListClicked(sampleItem)

        viewModel.onCreateList("  Weekend  ", "s1")

        assertEquals(AddToListSheetState.Hidden, viewModel.addToListSheet.value)
        // The list is created at the chosen store, under the trimmed name, unshared.
        val created = createList.lastInput!!
        assertNull(created.shoppingListId)
        assertEquals("s1", created.storeId)
        assertEquals("Weekend", created.name)
        assertFalse(created.shared)
        // The item lands on the list that was just created.
        assertEquals("new-id", insert.lastItem!!.shoppingListId)
        val event = viewModel.events.first()
        assertTrue(event is AddToListSheetEvent.ItemAdded)
        event as AddToListSheetEvent.ItemAdded
        assertEquals("Milk", event.itemName)
        assertEquals("Weekend", event.listName)
        assertEquals("sli-3", event.insertedItemId)
    }

    @Test
    fun `onCreateList emits AddFailed when the list cannot be created`() = runTest {
        val insert = FakeInsertItemUseCase()
        val viewModel = buildViewModel(
            insert = insert,
            createList = FakeShoppingListUseCase(
                ShoppingListUseCase.Output.Failure(IOException("boom"))
            ),
        )
        viewModel.onAddToListClicked(sampleItem)

        viewModel.onCreateList("Weekend", "s1")

        // Nothing was added, since there is no list to add to.
        assertNull(insert.lastItem)
        val event = viewModel.events.first()
        assertTrue(event is AddToListSheetEvent.AddFailed)
        assertEquals("Milk", (event as AddToListSheetEvent.AddFailed).itemName)
    }

    @Test
    fun `onCreateList does nothing when the sheet is hidden`() = runTest {
        val createList = FakeShoppingListUseCase()
        val viewModel = buildViewModel(createList = createList)

        viewModel.onCreateList("Weekend", "s1")

        assertNull(createList.lastInput)
    }

    @Test
    fun `undoAdd deletes the inserted item and emits ItemRemoved on success`() = runTest {
        val delete = FakeDeleteItemsUseCase(DeleteItemsUseCase.Output.Success)
        val viewModel = buildViewModel(delete = delete)

        viewModel.undoAdd(insertedItemId = "sli-9", itemName = "Milk")

        assertEquals("sli-9", delete.lastId)
        val event = viewModel.events.first()
        assertTrue(event is AddToListSheetEvent.ItemRemoved)
        assertEquals("Milk", (event as AddToListSheetEvent.ItemRemoved).itemName)
    }

    @Test
    fun `undoAdd emits UndoFailed when the delete fails`() = runTest {
        val delete = FakeDeleteItemsUseCase(DeleteItemsUseCase.Output.Failure(IOException("boom")))
        val viewModel = buildViewModel(delete = delete)

        viewModel.undoAdd(insertedItemId = "sli-9", itemName = "Milk")

        val event = viewModel.events.first()
        assertTrue(event is AddToListSheetEvent.UndoFailed)
        assertEquals("Milk", (event as AddToListSheetEvent.UndoFailed).itemName)
    }

    @Test
    fun `dismiss hides the sheet`() = runTest {
        val viewModel = buildViewModel(
            trips = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(sampleTrip))
            )
        )
        viewModel.onAddToListClicked(sampleItem)
        assertTrue(viewModel.addToListSheet.value is AddToListSheetState.Visible)

        viewModel.dismiss()

        assertEquals(AddToListSheetState.Hidden, viewModel.addToListSheet.value)
    }
}
