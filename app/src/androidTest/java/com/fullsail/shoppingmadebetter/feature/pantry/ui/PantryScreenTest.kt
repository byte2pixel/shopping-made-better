package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
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
        var output: InsertItemUseCase.Output = InsertItemUseCase.Output.Success,
    ) : InsertItemUseCase {
        override suspend fun execute(input: InsertItem) = output
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
        onItemClick: (String) -> Unit = {},
    ) {
        val viewModel = PantryViewModel(inventory, trips, insert)
        composeTestRule.setContent {
            ShoppingMadeBetterTheme {
                PantryScreen(onItemClick = onItemClick, viewModel = viewModel)
            }
        }
    }

    @Test
    fun rendersTheInventoryRow() {
        setScreen()

        composeTestRule.onNodeWithText("2% Milk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Great Value").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 1 gal").assertIsDisplayed()
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
