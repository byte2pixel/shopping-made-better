package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.pantry.domain.DeleteInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetSkipRemoveConfirmationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.SetSkipRemoveConfirmationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryExpiry
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryExpiryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLocationUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryQuantity
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryQuantityUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.DeleteItemsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItem
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.insertItem.InsertItemUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for [PantryScreen] — the inventory list, its row/cart
 * interactions, and the "add to list" bottom sheet.
 */
class PantryScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeGetInventoryUseCase(
        var output: GetInventoryUseCase.Output = GetInventoryUseCase.Output.Success(emptyList()),
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

    private class FakeUpdateInventoryQuantityUseCase(
        var output: UpdateInventoryQuantityUseCase.Output = UpdateInventoryQuantityUseCase.Output.Success,
    ) : UpdateInventoryQuantityUseCase {
        var lastInput: UpdateInventoryQuantity? = null
        override suspend fun execute(input: UpdateInventoryQuantity): UpdateInventoryQuantityUseCase.Output {
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

    /** Content description of the "Expiring" dashboard card, used to tap it. */
    private fun expiringCardDescription(count: Int) =
        composeTestRule.activity.resources.getQuantityString(
            R.plurals.pantry_dashboard_card_desc,
            count,
            string(R.string.pantry_dashboard_expiring),
            count,
        )

    /** Builds the screen; callers can pre-configure the fakes and observe [onItemClick]. */
    private fun setScreen(
        inventory: GetInventoryUseCase = FakeGetInventoryUseCase(
            GetInventoryUseCase.Output.Success(listOf(milk))
        ),
        trips: GetShoppingTripsUseCase = FakeGetShoppingTripsUseCase(
            GetShoppingTripsUseCase.Output.Success(listOf(weeklyTrip))
        ),
        insert: InsertItemUseCase = FakeInsertItemUseCase(),
        delete: DeleteItemsUseCase = FakeDeleteItemsUseCase(),
        deleteInventory: DeleteInventoryItemUseCase = FakeDeleteInventoryItemUseCase(),
        getSkip: GetSkipRemoveConfirmationUseCase = FakeGetSkipRemoveConfirmationUseCase(),
        setSkip: SetSkipRemoveConfirmationUseCase = FakeSetSkipRemoveConfirmationUseCase(),
        updateQuantity: UpdateInventoryQuantityUseCase = FakeUpdateInventoryQuantityUseCase(),
        updateLocation: UpdateInventoryLocationUseCase = FakeUpdateInventoryLocationUseCase(),
        updateExpiry: UpdateInventoryExpiryUseCase = FakeUpdateInventoryExpiryUseCase(),
        onItemClick: (String) -> Unit = {},
    ) {
        val viewModel = PantryViewModel(
            inventory, trips, insert, delete, deleteInventory, getSkip, setSkip, updateQuantity,
            updateLocation, updateExpiry,
        )
        composeTestRule.setContent {
            ShoppingMadeBetterTheme {
                PantryScreen(onItemClick = onItemClick, viewModel = viewModel)
            }
        }
    }

    @Test
    fun rendersTheInventoryCard() {
        setScreen()

        composeTestRule.onNodeWithText("2% Milk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Great Value").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 gal").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(
                composeTestRule.activity.resources.getQuantityString(
                    R.plurals.pantry_card_quantity_desc, 2, 2,
                )
            )
            .assertIsDisplayed()
    }

    @Test
    fun tappingARowReportsTheItemId() {
        var clickedId: String? = null
        setScreen(onItemClick = { clickedId = it })

        composeTestRule.onNodeWithText("2% Milk").performClick()

        assertEquals("i1", clickedId)
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
    fun removingAnItemConfirmsThenDeletesIt() {
        val deleteInventory = FakeDeleteInventoryItemUseCase()
        setScreen(deleteInventory = deleteInventory)

        // Tapping the card's remove action opens a confirmation dialog.
        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_remove_from_pantry))
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

        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_remove_from_pantry))
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

        // Open the dialog and toggle the "Don't ask again" checkbox.
        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_remove_from_pantry))
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

        composeTestRule
            .onNodeWithContentDescription(string(R.string.pantry_remove_from_pantry))
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
            inventory = FakeGetInventoryUseCase(
                GetInventoryUseCase.Output.Success(listOf(expiringYogurt, cannedBeans))
            )
        )
        // Both items show before any filter is applied.
        composeTestRule.onNodeWithText("Yogurt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Canned Beans").assertIsDisplayed()

        // Turn the filter on: only the soon-to-expire item remains.
        val expiringCard = expiringCardDescription(count = 3)
        composeTestRule.onNodeWithContentDescription(expiringCard).performClick()
        composeTestRule.onNodeWithText("Yogurt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Canned Beans").assertDoesNotExist()

        // Turn it off again: the full list comes back.
        composeTestRule.onNodeWithContentDescription(expiringCard).performClick()
        composeTestRule.onNodeWithText("Yogurt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Canned Beans").assertIsDisplayed()
    }
}
