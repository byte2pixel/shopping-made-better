package com.fullsail.shoppingmadebetter.feature.history.ui

import com.fullsail.shoppingmadebetter.core.ui.ShoppingListPickerState
import com.fullsail.shoppingmadebetter.feature.history.domain.AddTripToList
import com.fullsail.shoppingmadebetter.feature.history.domain.AddTripToListUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseTripUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.GetTripCostComparisonUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseLineItem
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTrip
import com.fullsail.shoppingmadebetter.feature.history.domain.StoreBasketCost
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

private fun lineItem(productId: String, id: String = "line-$productId") = PurchaseLineItem(
    id = id,
    productId = productId,
    productName = "Product $productId",
    brand = "Brand",
    size = "1 ea",
    imageUrl = "",
    quantity = 1.0,
    pricePaid = 2.50,
    addedToInventory = false,
)

private fun trip(vararg productIds: String) = PurchaseTrip(
    id = "trip-1",
    purchasedOn = LocalDate(2026, 8, 12),
    purchasedAtEpoch = 1_786_504_429L,
    storeName = "ALDI",
    recordedTotal = 10.0,
    items = productIds.map { lineItem(it) },
)

private fun shoppingTrip(id: String = "list-1", name: String = "Weekly") = ShoppingTrip(
    shoppingListId = id,
    listName = name,
    storeId = "store-1",
    storeName = "ALDI",
    itemCount = 3,
    totalCost = 12.0,
)

/**
 * Unit tests for [PurchaseTripDetailViewModel]. Collaborators are hand-written fakes and
 * [MainDispatcherRule] backs `viewModelScope` with an unconfined test dispatcher, so
 * launched work runs eagerly and the state is observable right after each call.
 */
class PurchaseTripDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeGetPurchaseTripUseCase(
        var output: GetPurchaseTripUseCase.Output = GetPurchaseTripUseCase.Output.NotFound,
    ) : GetPurchaseTripUseCase {
        override suspend fun execute(input: String) = output
    }

    private class FakeGetShoppingTripsUseCase(
        var output: GetShoppingTripsUseCase.Output = GetShoppingTripsUseCase.Output.Success(emptyList()),
        private val gate: CompletableDeferred<Unit>? = null,
    ) : GetShoppingTripsUseCase {
        override suspend fun execute(input: Unit): GetShoppingTripsUseCase.Output {
            gate?.await()
            return output
        }
    }

    /** Fake add use case: records its input and returns a settable [output]. */
    private class FakeAddTripToListUseCase(
        var output: AddTripToListUseCase.Output = AddTripToListUseCase.Output.Success(2, 0),
    ) : AddTripToListUseCase {
        var lastInput: AddTripToList? = null
        override suspend fun execute(input: AddTripToList): AddTripToListUseCase.Output {
            lastInput = input
            return output
        }
    }

    private class FakeGetTripCostComparisonUseCase(
        var output: GetTripCostComparisonUseCase.Output =
            GetTripCostComparisonUseCase.Output.Success(emptyList()),
    ) : GetTripCostComparisonUseCase {
        override suspend fun execute(input: String) = output
    }

    private fun viewModel(
        getTrip: FakeGetPurchaseTripUseCase = FakeGetPurchaseTripUseCase(),
        getLists: FakeGetShoppingTripsUseCase = FakeGetShoppingTripsUseCase(),
        addToList: FakeAddTripToListUseCase = FakeAddTripToListUseCase(),
        compare: FakeGetTripCostComparisonUseCase = FakeGetTripCostComparisonUseCase(),
    ) = PurchaseTripDetailViewModel(getTrip, getLists, addToList, compare)

    @Test
    fun `load selects every line item by default`() {
        val getTrip = FakeGetPurchaseTripUseCase(
            GetPurchaseTripUseCase.Output.Success(trip("milk", "eggs", "bread")),
        )
        val viewModel = viewModel(getTrip = getTrip)

        viewModel.load("trip-1")

        assertTrue(viewModel.uiState.value is PurchaseTripDetailUiState.Success)
        assertEquals(setOf("milk", "eggs", "bread"), viewModel.selectedProductIds.value)
    }

    @Test
    fun `load leaves nothing selected when the trip is missing`() {
        val viewModel = viewModel(getTrip = FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.NotFound))

        viewModel.load("trip-1")

        assertEquals(PurchaseTripDetailUiState.NotFound, viewModel.uiState.value)
        assertEquals(emptySet<String>(), viewModel.selectedProductIds.value)
    }

    @Test
    fun `onItemToggled unticks then re-ticks an item`() {
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(
                GetPurchaseTripUseCase.Output.Success(trip("milk", "eggs")),
            ),
        )
        viewModel.load("trip-1")

        viewModel.onItemToggled("eggs")
        assertEquals(setOf("milk"), viewModel.selectedProductIds.value)

        viewModel.onItemToggled("eggs")
        assertEquals(setOf("milk", "eggs"), viewModel.selectedProductIds.value)
    }

    @Test
    fun `onBuyAgainClicked loads the user's lists into the sheet`() {
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(
                GetPurchaseTripUseCase.Output.Success(trip("milk", "eggs")),
            ),
            getLists = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(shoppingTrip())),
            ),
        )
        viewModel.load("trip-1")

        viewModel.onBuyAgainClicked()

        val sheet = viewModel.buyAgainSheet.value as BuyAgainSheetState.Visible
        assertEquals(2, sheet.selectedCount)
        assertTrue(sheet.lists is ShoppingListPickerState.Loaded)
        assertEquals(listOf("Weekly"), (sheet.lists as ShoppingListPickerState.Loaded).trips.map { it.listName })
    }

    @Test
    fun `onBuyAgainClicked shows the empty state when there are no lists`() {
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.Success(trip("milk"))),
            getLists = FakeGetShoppingTripsUseCase(GetShoppingTripsUseCase.Output.Success(emptyList())),
        )
        viewModel.load("trip-1")

        viewModel.onBuyAgainClicked()

        val sheet = viewModel.buyAgainSheet.value as BuyAgainSheetState.Visible
        assertTrue(sheet.lists is ShoppingListPickerState.Empty)
    }

    @Test
    fun `onBuyAgainClicked shows the error state when the lists fail to load`() {
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.Success(trip("milk"))),
            getLists = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Failure(IOException("network down")),
            ),
        )
        viewModel.load("trip-1")

        viewModel.onBuyAgainClicked()

        val sheet = viewModel.buyAgainSheet.value as BuyAgainSheetState.Visible
        assertTrue(sheet.lists is ShoppingListPickerState.Error)
    }

    @Test
    fun `onBuyAgainClicked does nothing with no items selected`() {
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.Success(trip("milk"))),
        )
        viewModel.load("trip-1")
        viewModel.onItemToggled("milk")

        viewModel.onBuyAgainClicked()

        assertEquals(BuyAgainSheetState.Hidden, viewModel.buyAgainSheet.value)
    }

    @Test
    fun `a dismissed sheet does not reopen when the lists arrive`() {
        val gate = CompletableDeferred<Unit>()
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.Success(trip("milk"))),
            getLists = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(shoppingTrip())),
                gate = gate,
            ),
        )
        viewModel.load("trip-1")
        viewModel.onBuyAgainClicked()

        viewModel.dismissBuyAgainSheet()
        gate.complete(Unit)

        assertEquals(BuyAgainSheetState.Hidden, viewModel.buyAgainSheet.value)
    }

    @Test
    fun `onListChosen adds the selected items and reports success`() = runTest {
        val addToList = FakeAddTripToListUseCase(AddTripToListUseCase.Output.Success(added = 1, skipped = 0))
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(
                GetPurchaseTripUseCase.Output.Success(trip("milk", "eggs")),
            ),
            getLists = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(shoppingTrip())),
            ),
            addToList = addToList,
        )
        viewModel.load("trip-1")
        viewModel.onItemToggled("eggs")
        viewModel.onBuyAgainClicked()

        viewModel.onListChosen(shoppingTrip())

        assertEquals(BuyAgainSheetState.Hidden, viewModel.buyAgainSheet.value)
        assertEquals(
            AddTripToList(purchaseId = "trip-1", shoppingListId = "list-1", productIds = setOf("milk")),
            addToList.lastInput,
        )
        assertEquals(
            TripDetailEvent.ItemsAdded(added = 1, listName = "Weekly", skipped = 0),
            viewModel.events.first(),
        )
    }

    @Test
    fun `onListChosen reports skipped products`() = runTest {
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.Success(trip("milk"))),
            getLists = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(shoppingTrip())),
            ),
            addToList = FakeAddTripToListUseCase(
                AddTripToListUseCase.Output.Success(added = 1, skipped = 2),
            ),
        )
        viewModel.load("trip-1")
        viewModel.onBuyAgainClicked()

        viewModel.onListChosen(shoppingTrip())

        assertEquals(
            TripDetailEvent.ItemsAdded(added = 1, listName = "Weekly", skipped = 2),
            viewModel.events.first(),
        )
    }

    @Test
    fun `onListChosen reports a partial failure`() = runTest {
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.Success(trip("milk", "eggs"))),
            getLists = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(shoppingTrip())),
            ),
            addToList = FakeAddTripToListUseCase(
                AddTripToListUseCase.Output.PartialFailure(added = 1, failed = 1, skipped = 0),
            ),
        )
        viewModel.load("trip-1")
        viewModel.onBuyAgainClicked()

        viewModel.onListChosen(shoppingTrip())

        assertEquals(
            TripDetailEvent.AddPartiallyFailed(added = 1, failed = 1),
            viewModel.events.first(),
        )
    }

    @Test
    fun `onListChosen reports an outright failure`() = runTest {
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.Success(trip("milk"))),
            getLists = FakeGetShoppingTripsUseCase(
                GetShoppingTripsUseCase.Output.Success(listOf(shoppingTrip())),
            ),
            addToList = FakeAddTripToListUseCase(
                AddTripToListUseCase.Output.Failure(IOException("network down")),
            ),
        )
        viewModel.load("trip-1")
        viewModel.onBuyAgainClicked()

        viewModel.onListChosen(shoppingTrip())

        assertEquals(TripDetailEvent.AddFailed, viewModel.events.first())
    }

    @Test
    fun `onListChosen does nothing when the sheet is not open`() {
        val addToList = FakeAddTripToListUseCase()
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.Success(trip("milk"))),
            addToList = addToList,
        )
        viewModel.load("trip-1")

        viewModel.onListChosen(shoppingTrip())

        assertNull(addToList.lastInput)
    }

    // ---- cost comparison ----------------------------------------------------

    @Test
    fun `loading a trip also prices it at other stores`() {
        val compare = FakeGetTripCostComparisonUseCase(
            GetTripCostComparisonUseCase.Output.Success(
                listOf(StoreBasketCost("s-2", "Publix", 40.0, 2.0)),
            ),
        )
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.Success(trip("milk"))),
            compare = compare,
        )

        viewModel.load("trip-1")

        assertEquals(listOf("Publix"), viewModel.storeCosts.value.map { it.storeName })
    }

    @Test
    fun `a failed comparison leaves the trip readable`() {
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.Success(trip("milk"))),
            compare = FakeGetTripCostComparisonUseCase(
                GetTripCostComparisonUseCase.Output.Failure(IOException("no network")),
            ),
        )

        viewModel.load("trip-1")

        assertTrue(viewModel.storeCosts.value.isEmpty())
        assertTrue(viewModel.uiState.value is PurchaseTripDetailUiState.Success)
    }

    @Test
    fun `loading a second trip drops the first one's comparison`() {
        val compare = FakeGetTripCostComparisonUseCase(
            GetTripCostComparisonUseCase.Output.Success(
                listOf(StoreBasketCost("s-2", "Publix", 40.0, 2.0)),
            ),
        )
        val viewModel = viewModel(
            getTrip = FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.Success(trip("milk"))),
            compare = compare,
        )
        viewModel.load("trip-1")

        compare.output = GetTripCostComparisonUseCase.Output.Success(emptyList())
        viewModel.load("trip-2")

        assertTrue(viewModel.storeCosts.value.isEmpty())
    }
}
