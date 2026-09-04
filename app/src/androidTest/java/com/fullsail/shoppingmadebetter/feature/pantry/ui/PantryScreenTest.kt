package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.pantry.domain.AdjustmentReason
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ApplyInventoryAdjustment
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ApplyInventoryAdjustmentUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.DeleteInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.EstimateSource
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetSkipRemoveConfirmationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.SetSkipRemoveConfirmationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryExpiry
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryExpiryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLocationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThreshold
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThresholdUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.groupInventoryByProduct
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** A successful inventory fetch of [items], grouped the way the real use case returns it. */
private fun inventoryOf(vararg items: InventoryItem) =
    GetInventoryUseCase.Output.Success(groupInventoryByProduct(items.toList()))

/**
 * Compose UI tests for [PantryScreen] — the inventory list, its row/cart
 * interactions, and the "add to list" bottom sheet.
 */
class PantryScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeGetInventoryUseCase(
        var output: GetInventoryUseCase.Output = inventoryOf(),
    ) : GetInventoryUseCase {
        override suspend fun execute(input: Unit) = output
    }

    private class FakeGetShoppingTripsUseCase(
        var output: GetShoppingTripsUseCase.Output = GetShoppingTripsUseCase.Output.Success(emptyList()),
    ) : GetShoppingTripsUseCase {
        override suspend fun execute(input: Unit) = output
    }

    private class FakeInsertItemUseCase(
        var output: InsertItemUseCase.Output = InsertItemUseCase.Output.Success("sli-1"),
    ) : InsertItemUseCase {
        override suspend fun execute(input: InsertItem) = output
    }

    private class FakeDeleteItemsUseCase(
        var output: DeleteItemsUseCase.Output = DeleteItemsUseCase.Output.Success,
    ) : DeleteItemsUseCase {
        var lastId: String? = null
        override suspend fun execute(input: String): DeleteItemsUseCase.Output {
            lastId = input
            return output
        }
    }

    private class FakeDeleteInventoryItemUseCase(
        var output: DeleteInventoryItemUseCase.Output = DeleteInventoryItemUseCase.Output.Success,
    ) : DeleteInventoryItemUseCase {
        var lastId: String? = null
        override suspend fun execute(input: String): DeleteInventoryItemUseCase.Output {
            lastId = input
            return output
        }
    }

    private class FakeGetSkipRemoveConfirmationUseCase(
        var value: Boolean = false,
    ) : GetSkipRemoveConfirmationUseCase {
        override suspend fun execute(input: Unit): Boolean = value
    }

    private class FakeSetSkipRemoveConfirmationUseCase : SetSkipRemoveConfirmationUseCase {
        var lastValue: Boolean? = null
        override suspend fun execute(input: Boolean) {
            lastValue = input
        }
    }

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

    private class FakeUpdateInventoryLocationUseCase(
        var output: UpdateInventoryLocationUseCase.Output = UpdateInventoryLocationUseCase.Output.Success,
    ) : UpdateInventoryLocationUseCase {
        var lastInput: UpdateInventoryLocation? = null
        override suspend fun execute(input: UpdateInventoryLocation): UpdateInventoryLocationUseCase.Output {
            lastInput = input
            return output
        }
    }

    private class FakeUpdateInventoryExpiryUseCase(
        var output: UpdateInventoryExpiryUseCase.Output = UpdateInventoryExpiryUseCase.Output.Success,
    ) : UpdateInventoryExpiryUseCase {
        var lastInput: UpdateInventoryExpiry? = null
        override suspend fun execute(input: UpdateInventoryExpiry): UpdateInventoryExpiryUseCase.Output {
            lastInput = input
            return output
        }
    }

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

    private val milk = InventoryItem(
        id = "i1",
        productId = "p1",
        name = "2% Milk",
        brand = "Great Value",
        description = "Reduced-fat milk",
        size = "1 gal",
        imageUrl = "",
        quantity = 2,
        expiresInDays = null,
    )

    // Expires within the threshold, so it survives the "Expiring" filter.
    private val expiringYogurt = milk.copy(
        id = "i2",
        productId = "p2",
        name = "Yogurt",
        expiresInDays = 2,
    )

    // No expiration date, so the "Expiring" filter hides it.
    private val cannedBeans = milk.copy(
        id = "i3",
        productId = "p3",
        name = "Canned Beans",
        expiresInDays = null,
    )

    private val weeklyTrip = ShoppingTrip(
        shoppingListId = "l1",
        listName = "Weekly",
        storeId = "s1",
        storeName = "ALDI",
        itemCount = 3,
        totalCost = 9.99,
    )

    private fun string(resId: Int, vararg args: Any) =
        composeTestRule.activity.getString(resId, *args)

    private fun quantityString(resId: Int, quantity: Int, vararg args: Any) =
        composeTestRule.activity.resources.getQuantityString(resId, quantity, *args)

    /** Matches a node whose click action carries [label] (e.g. a lot row's "View lot details"). */
    private fun hasClickLabel(label: String) = SemanticsMatcher("clickLabel = $label") { node ->
        node.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }

    /** Expands (or collapses) [productName]'s card by tapping its header. */
    private fun toggleCard(productName: String) {
        composeTestRule.onNodeWithText(productName).performClick()
    }

    /** Content description of a dashboard card, used to find and tap it. */
    private fun cardDescription(filter: PantryDashboardFilter, count: Int) =
        quantityString(
            R.plurals.pantry_dashboard_card_desc,
            count,
            string(filter.labelRes),
            count,
        )

    /** Builds the screen; callers can pre-configure the fakes and observe [onProductClick]. */
    private fun setScreen(
        inventory: GetInventoryUseCase = FakeGetInventoryUseCase(inventoryOf(milk)),
        trips: GetShoppingTripsUseCase = FakeGetShoppingTripsUseCase(
            GetShoppingTripsUseCase.Output.Success(listOf(weeklyTrip))
        ),
        insert: InsertItemUseCase = FakeInsertItemUseCase(),
        delete: DeleteItemsUseCase = FakeDeleteItemsUseCase(),
        deleteInventory: DeleteInventoryItemUseCase = FakeDeleteInventoryItemUseCase(),
        getSkip: GetSkipRemoveConfirmationUseCase = FakeGetSkipRemoveConfirmationUseCase(),
        setSkip: SetSkipRemoveConfirmationUseCase = FakeSetSkipRemoveConfirmationUseCase(),
        applyAdjustment: ApplyInventoryAdjustmentUseCase = FakeApplyInventoryAdjustmentUseCase(),
        updateLocation: UpdateInventoryLocationUseCase = FakeUpdateInventoryLocationUseCase(),
        updateExpiry: UpdateInventoryExpiryUseCase = FakeUpdateInventoryExpiryUseCase(),
        updateThreshold: UpdateInventoryLowStockThresholdUseCase =
            FakeUpdateInventoryLowStockThresholdUseCase(),
        onProductClick: (String) -> Unit = {},
    ) {
        val viewModel = PantryViewModel(
            inventory, trips, insert, delete, deleteInventory, getSkip, setSkip, applyAdjustment,
            updateLocation, updateExpiry, updateThreshold,
        )
        composeTestRule.setContent {
            ShoppingMadeBetterTheme {
                PantryScreen(onProductClick = onProductClick, viewModel = viewModel)
            }
        }
    }

    @Test
    fun rendersTheProductCard() {
        setScreen()

        composeTestRule.onNodeWithText("2% Milk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Great Value").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 gal").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(
                quantityString(R.plurals.pantry_card_total_quantity_desc, 2, 2)
            )
            .assertIsDisplayed()
    }

    @Test
    fun theExpiryChipStaysForLotsThatExpireFarOut() {
        setScreen(
            inventory = FakeGetInventoryUseCase(inventoryOf(milk.copy(expiresInDays = 30)))
        )
        toggleCard("2% Milk")

        // Well past the "expiring soon" threshold, but the lot's chip is still there
        // and still opens the editor, so a mistyped date can be corrected.
        composeTestRule
            .onNodeWithContentDescription(
                quantityString(R.plurals.pantry_detail_expires_in_days, 30, 30)
            )
            .performClick()

        composeTestRule
            .onNodeWithText(string(R.string.pantry_expiry_edit_label))
            .assertIsDisplayed()
    }

    @Test
    fun tappingTheHeaderExpandsAndCollapsesTheLots() {
        setScreen()
        val lotQuantityDesc = quantityString(R.plurals.pantry_card_quantity_desc, 2, 2)

        // Collapsed: only the header's total chip shows, no per-lot quantity chip.
        composeTestRule.onNodeWithContentDescription(lotQuantityDesc).assertDoesNotExist()

        toggleCard("2% Milk")
        composeTestRule.onNodeWithContentDescription(lotQuantityDesc).assertIsDisplayed()

        toggleCard("2% Milk")
        composeTestRule.onNodeWithContentDescription(lotQuantityDesc).assertDoesNotExist()
    }

    @Test
    fun multipleCardsCanBeExpandedAtOnce() {
        setScreen(
            inventory = FakeGetInventoryUseCase(inventoryOf(expiringYogurt, cannedBeans))
        )

        toggleCard("Yogurt")
        toggleCard("Canned Beans")

        // Both cards' lot rows are showing at the same time (no accordion).
        composeTestRule
            .onAllNodes(hasClickLabel(string(R.string.pantry_card_lot_details)))
            .assertCountEquals(2)
    }

    @Test
    fun repeatPurchasesShowOneCardWithAllLots() {
        val secondLot = milk.copy(id = "i9", quantity = 3, expiresInDays = 5)
        setScreen(inventory = FakeGetInventoryUseCase(inventoryOf(milk, secondLot)))

        // One card for the product, with the header aggregating both lots.
        composeTestRule.onAllNodesWithText("2% Milk").assertCountEquals(1)
        composeTestRule
            .onNodeWithContentDescription(
                quantityString(R.plurals.pantry_card_total_quantity_desc, 5, 5)
            )
            .assertIsDisplayed()

        // Expanding lists each lot with its own quantity.
        toggleCard("2% Milk")
        composeTestRule
            .onNodeWithContentDescription(quantityString(R.plurals.pantry_card_quantity_desc, 2, 2))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(quantityString(R.plurals.pantry_card_quantity_desc, 3, 3))
            .assertIsDisplayed()
    }

    @Test
    fun tappingALotRowReportsTheProductId() {
        // The detail screen is per-product, not per-lot: every lot of a product opens it.
        var clickedId: String? = null
        setScreen(onProductClick = { clickedId = it })

        toggleCard("2% Milk")
        composeTestRule
            .onNode(hasClickLabel(string(R.string.pantry_card_lot_details)))
            .performClick()

        assertEquals("p1", clickedId)
    }

    @Test
    fun lotQuantityEditsCommitAgainstThatLot() {
        val applyAdjustment = FakeApplyInventoryAdjustmentUseCase(
            ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 4, appliedDelta = 1)
        )
        val secondLot = milk.copy(id = "i9", quantity = 3, expiresInDays = 5)
        setScreen(
            inventory = FakeGetInventoryUseCase(inventoryOf(milk, secondLot)),
            applyAdjustment = applyAdjustment,
        )
        toggleCard("2% Milk")

        // Open the 3-quantity lot's stepper, bump it once, and dismiss to commit.
        composeTestRule
            .onNodeWithContentDescription(quantityString(R.plurals.pantry_card_quantity_desc, 3, 3))
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_quantity_increase))
            .performClick()
        Espresso.pressBack()
        composeTestRule.waitForIdle()

        assertEquals(
            ApplyInventoryAdjustment(id = "i9", delta = 1, reason = AdjustmentReason.Manual),
            applyAdjustment.lastInput,
        )
    }

    @Test
    fun onlyAnEstimatedLotShowsTheEstChip() {
        val estimatedMilk = milk.copy(lastAdjustmentReason = AdjustmentReason.Auto, estimateSource = EstimateSource.History)
        setScreen(
            inventory = FakeGetInventoryUseCase(inventoryOf(estimatedMilk, expiringYogurt)),
        )
        toggleCard("2% Milk")
        toggleCard("Yogurt")

        // One est. chip across both expanded cards: the yogurt lot is not estimated.
        composeTestRule
            .onAllNodesWithContentDescription(string(R.string.pantry_estimate_desc_history))
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithText(string(R.string.pantry_estimate_chip))
            .assertCountEquals(1)
    }

    @Test
    fun confirmingAnEstimateCommitsAZeroDeltaConfirmedAdjustment() {
        val applyAdjustment = FakeApplyInventoryAdjustmentUseCase(
            ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 2, appliedDelta = 0)
        )
        val estimatedMilk = milk.copy(lastAdjustmentReason = AdjustmentReason.Auto, estimateSource = EstimateSource.ShelfLife)
        setScreen(
            inventory = FakeGetInventoryUseCase(inventoryOf(estimatedMilk)),
            applyAdjustment = applyAdjustment,
        )
        toggleCard("2% Milk")

        // Tapping the est. chip reveals the inline check; Yes confirms as-is.
        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_estimate_desc_shelf_life))
            .performClick()
        composeTestRule
            .onNodeWithText(string(R.string.pantry_estimate_confirm_question, 2))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pantry_estimate_confirm_yes)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            ApplyInventoryAdjustment(id = "i1", delta = 0, reason = AdjustmentReason.Confirmed),
            applyAdjustment.lastInput,
        )
        // The confirmed lot no longer reads as estimated.
        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_estimate_desc_shelf_life))
            .assertDoesNotExist()
    }

    @Test
    fun fixingAnEstimateCommitsTheCorrectedCountAsConfirmed() {
        val applyAdjustment = FakeApplyInventoryAdjustmentUseCase(
            ApplyInventoryAdjustmentUseCase.Output.Success(newQuantity = 3, appliedDelta = 1)
        )
        val estimatedMilk = milk.copy(lastAdjustmentReason = AdjustmentReason.Auto, estimateSource = EstimateSource.History)
        setScreen(
            inventory = FakeGetInventoryUseCase(inventoryOf(estimatedMilk)),
            applyAdjustment = applyAdjustment,
        )
        toggleCard("2% Milk")

        // Fix opens the stepper; bump once and dismiss to commit as 'confirmed'.
        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_estimate_desc_history))
            .performClick()
        composeTestRule.onNodeWithText(string(R.string.pantry_estimate_confirm_fix)).performClick()
        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_quantity_increase))
            .performClick()
        Espresso.pressBack()
        composeTestRule.waitForIdle()

        assertEquals(
            ApplyInventoryAdjustment(id = "i1", delta = 1, reason = AdjustmentReason.Confirmed),
            applyAdjustment.lastInput,
        )
    }

    @Test
    fun theTotalQuantityChipEditsOnlyTheProductThreshold() {
        val updateThreshold = FakeUpdateInventoryLowStockThresholdUseCase()
        setScreen(updateThreshold = updateThreshold)

        composeTestRule
            .onNodeWithContentDescription(
                quantityString(R.plurals.pantry_card_total_quantity_desc, 2, 2)
            )
            .performClick()

        // The popup offers only the low-stock threshold — no quantity stepper.
        composeTestRule.onNodeWithText(string(R.string.pantry_low_stock_label)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.pantry_quantity_edit_label))
            .assertDoesNotExist()

        // Raising from Off sets 1; dismissing commits it against the product.
        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_low_stock_increase))
            .performClick()
        Espresso.pressBack()
        composeTestRule.waitForIdle()

        assertEquals(
            UpdateInventoryLowStockThreshold(productId = "p1", threshold = 1),
            updateThreshold.lastInput,
        )
    }

    @Test
    fun tappingTheCartIconOpensTheAddToListSheet() {
        setScreen()

        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_add_to_list))
            .performClick()

        composeTestRule
            .onNodeWithText(string(R.string.add_to_list_title, "2% Milk"))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Weekly").assertIsDisplayed()
    }

    @Test
    fun errorStateShowsRetry() {
        setScreen(
            inventory = FakeGetInventoryUseCase(
                GetInventoryUseCase.Output.Failure(RuntimeException("boom"))
            )
        )

        composeTestRule.onNodeWithText(string(R.string.pantry_error)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pantry_retry)).assertIsDisplayed()
    }

    @Test
    fun tappingUndoOnTheAddSnackbarRemovesTheJustAddedItem() {
        val delete = FakeDeleteItemsUseCase()
        setScreen(
            insert = FakeInsertItemUseCase(InsertItemUseCase.Output.Success("sli-1")),
            delete = delete,
        )

        // Add "2% Milk" to the "Weekly" list via the sheet.
        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_add_to_list))
            .performClick()
        composeTestRule.onNodeWithText("Weekly").performClick()

        // The confirmation snackbar shows an Undo action; tap it.
        composeTestRule
            .onNodeWithText(string(R.string.added_to_list, "2% Milk", "Weekly"))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.add_to_list_undo)).performClick()

        // Undo removes exactly the row that was inserted, and confirms removal.
        composeTestRule
            .onNodeWithText(string(R.string.removed_from_list, "2% Milk"))
            .assertIsDisplayed()
        assertEquals("sli-1", delete.lastId)
    }

    @Test
    fun removingALotConfirmsThenDeletesIt() {
        val deleteInventory = FakeDeleteInventoryItemUseCase()
        setScreen(deleteInventory = deleteInventory)
        toggleCard("2% Milk")

        // Tapping a lot row's remove action opens a confirmation dialog.
        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_card_remove_lot))
            .performClick()
        composeTestRule
            .onNodeWithText(string(R.string.pantry_remove_confirm_title))
            .assertIsDisplayed()

        // Confirming deletes exactly this item and reports the removal.
        composeTestRule
            .onNodeWithText(string(R.string.pantry_remove_confirm_action))
            .performClick()
        composeTestRule
            .onNodeWithText(string(R.string.pantry_removed, "2% Milk"))
            .assertIsDisplayed()
        assertEquals("i1", deleteInventory.lastId)
    }

    @Test
    fun cancellingRemoveKeepsTheItem() {
        val deleteInventory = FakeDeleteInventoryItemUseCase()
        setScreen(deleteInventory = deleteInventory)
        toggleCard("2% Milk")

        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_card_remove_lot))
            .performClick()
        composeTestRule
            .onNodeWithText(string(R.string.pantry_remove_cancel))
            .performClick()

        // Dialog dismissed, nothing deleted, item still shown.
        composeTestRule
            .onNodeWithText(string(R.string.pantry_remove_confirm_title))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("2% Milk").assertIsDisplayed()
        assertEquals(null, deleteInventory.lastId)
    }

    @Test
    fun checkingDontAskAgainDeletesAndPersistsThePreference() {
        val deleteInventory = FakeDeleteInventoryItemUseCase()
        val setSkip = FakeSetSkipRemoveConfirmationUseCase()
        setScreen(deleteInventory = deleteInventory, setSkip = setSkip)
        toggleCard("2% Milk")

        // Open the dialog and toggle the "Don't ask again" checkbox.
        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_card_remove_lot))
            .performClick()
        composeTestRule
            .onNodeWithText(string(R.string.pantry_remove_dont_ask_again))
            .performClick()

        // Confirming deletes the item and persists the suppression choice.
        composeTestRule
            .onNodeWithText(string(R.string.pantry_remove_confirm_action))
            .performClick()
        composeTestRule
            .onNodeWithText(string(R.string.pantry_removed, "2% Milk"))
            .assertIsDisplayed()
        assertEquals("i1", deleteInventory.lastId)
        assertEquals(true, setSkip.lastValue)
    }

    @Test
    fun removeSkipsTheDialogWhenThePreferenceIsSet() {
        val deleteInventory = FakeDeleteInventoryItemUseCase()
        setScreen(
            deleteInventory = deleteInventory,
            getSkip = FakeGetSkipRemoveConfirmationUseCase(value = true),
        )
        toggleCard("2% Milk")

        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_card_remove_lot))
            .performClick()

        // No confirmation dialog appears; the item is removed directly.
        composeTestRule
            .onNodeWithText(string(R.string.pantry_remove_confirm_title))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(string(R.string.pantry_removed, "2% Milk"))
            .assertIsDisplayed()
        assertEquals("i1", deleteInventory.lastId)
    }

    @Test
    fun tappingTheExpiringCardTogglesTheFilter() {
        setScreen(
            inventory = FakeGetInventoryUseCase(inventoryOf(expiringYogurt, cannedBeans))
        )
        // Both items show before any filter is applied.
        composeTestRule.onNodeWithText("Yogurt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Canned Beans").assertIsDisplayed()

        // Turn the filter on: only the soon-to-expire item remains. The card counts
        // one item — the yogurt; the beans have no expiration date at all.
        val expiringCard = cardDescription(PantryDashboardFilter.Expiring, count = 1)
        composeTestRule.onNodeWithContentDescription(expiringCard).performClick()
        composeTestRule.onNodeWithText("Yogurt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Canned Beans").assertDoesNotExist()

        // Turn it off again: the full list comes back.
        composeTestRule.onNodeWithContentDescription(expiringCard).performClick()
        composeTestRule.onNodeWithText("Yogurt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Canned Beans").assertIsDisplayed()
    }

    @Test
    fun theRunningLowCardCountsProductsNotLots() {
        // Two loaves in separate lots against a threshold of one: two on hand, so the
        // product is not low even though every lot sits at the threshold.
        val bread = milk.copy(
            id = "b1",
            productId = "bread",
            name = "Sourdough",
            quantity = 1,
            expiresInDays = 2,
            lowStockThreshold = 1,
        )
        setScreen(
            inventory = FakeGetInventoryUseCase(
                inventoryOf(bread, bread.copy(id = "b2", expiresInDays = 5))
            )
        )
        composeTestRule.onNodeWithText("Sourdough").assertIsDisplayed()

        // The card counts no low products, and turning it on filters the bread away.
        composeTestRule
            .onNodeWithContentDescription(cardDescription(PantryDashboardFilter.RunningLow, 0))
            .performClick()
        composeTestRule.onNodeWithText("Sourdough").assertDoesNotExist()
    }

    @Test
    fun theRunningLowCardCountsALowProductOnceAcrossItsLots() {
        // The same two lots against a threshold of three: two on hand is low, and the
        // product counts once however many lots it is spread over.
        val bread = milk.copy(
            id = "b1",
            productId = "bread",
            name = "Sourdough",
            quantity = 1,
            expiresInDays = 2,
            lowStockThreshold = 3,
        )
        setScreen(
            inventory = FakeGetInventoryUseCase(
                inventoryOf(bread, bread.copy(id = "b2", expiresInDays = 5))
            )
        )

        composeTestRule
            .onNodeWithContentDescription(cardDescription(PantryDashboardFilter.RunningLow, 1))
            .performClick()
        composeTestRule.onAllNodesWithText("Sourdough").assertCountEquals(1)
    }
}
