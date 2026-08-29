package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.history.domain.AddTripToList
import com.fullsail.shoppingmadebetter.feature.history.domain.AddTripToListUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseTripUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.GetTripCostComparisonUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseLineItem
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTrip
import com.fullsail.shoppingmadebetter.feature.history.domain.StoreBasketCost
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.GetShoppingTripsUseCase
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for [PurchaseTripDetailScreen]'s line items, which carry two
 * actions at once: the row opens the product, the leading strip ticks the item for
 * "buy again". The edge cases below pin down that the strip — not the row — owns
 * every pixel beside the checkbox, including the band above and below it.
 */
class PurchaseTripDetailScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeGetPurchaseTripUseCase(
        private val output: GetPurchaseTripUseCase.Output,
    ) : GetPurchaseTripUseCase {
        override suspend fun execute(input: String): GetPurchaseTripUseCase.Output = output
    }

    /** The picker is never opened by these tests; an empty list is enough. */
    private class FakeGetShoppingTripsUseCase : GetShoppingTripsUseCase {
        override suspend fun execute(input: Unit): GetShoppingTripsUseCase.Output =
            GetShoppingTripsUseCase.Output.Success(emptyList())
    }

    private class FakeAddTripToListUseCase : AddTripToListUseCase {
        override suspend fun execute(input: AddTripToList): AddTripToListUseCase.Output =
            AddTripToListUseCase.Output.Success(added = 0, skipped = 0)
    }

    private class FakeGetTripCostComparisonUseCase(
        private val stores: List<StoreBasketCost> = emptyList(),
    ) : GetTripCostComparisonUseCase {
        override suspend fun execute(input: String): GetTripCostComparisonUseCase.Output =
            GetTripCostComparisonUseCase.Output.Success(stores)
    }

    private val milk = PurchaseLineItem(
        id = "li1",
        productId = "p1",
        productName = "2% Milk",
        brand = "Great Value",
        size = "1 gal",
        imageUrl = "",
        quantity = 2.0,
        pricePaid = 3.99,
    )

    private val trip = PurchaseTrip(
        id = "ph1",
        purchasedOn = LocalDate(2026, 8, 20),
        purchasedAtEpoch = 1_787_000_000L,
        storeName = "Corner Market",
        recordedTotal = 7.98,
        items = listOf(milk),
    )

    /** Convenience: look up a string resource the way the screen does. */
    private fun string(resId: Int, vararg args: Any) =
        composeTestRule.activity.getString(resId, *args)

    private fun hasClickLabel(label: String) = SemanticsMatcher("clickLabel = $label") { node ->
        node.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }

    /** The whole line-item row: the node carrying the product's text and a click action. */
    private fun row() = composeTestRule.onNode(hasClickAction() and hasText("2% Milk"))

    /** The leading toggle strip, addressed by the checkbox's own label. */
    private fun toggleStrip() = composeTestRule.onNodeWithContentDescription(
        string(R.string.history_buy_again_select, "2% Milk"),
    )

    private var clickedProductId: String? = null

    private fun setScreen(storeCosts: List<StoreBasketCost> = emptyList()) {
        val viewModel = PurchaseTripDetailViewModel(
            FakeGetPurchaseTripUseCase(GetPurchaseTripUseCase.Output.Success(trip)),
            FakeGetShoppingTripsUseCase(),
            FakeAddTripToListUseCase(),
            FakeGetTripCostComparisonUseCase(storeCosts),
        )
        composeTestRule.setContent {
            ShoppingMadeBetterTheme {
                PurchaseTripDetailScreen(
                    purchaseId = "ph1",
                    onProductClick = { clickedProductId = it },
                    viewModel = viewModel,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun tappingTheRowOpensTheProductWithoutChangingTheSelection() {
        setScreen()

        // Every item starts ticked, so a stray toggle would show up as "off".
        toggleStrip().assertIsOn()
        row().performClick()

        assertEquals("p1", clickedProductId)
        toggleStrip().assertIsOn()
    }

    @Test
    fun theRowAnnouncesWhatTappingItDoes() {
        setScreen()

        row().assert(hasClickLabel(string(R.string.history_line_item_open)))
    }

    @Test
    fun tappingTheCheckboxTogglesWithoutOpeningTheProduct() {
        setScreen()

        toggleStrip().performClick()

        toggleStrip().assertIsOff()
        assertNull(clickedProductId)
    }

    @Test
    fun tappingTheBandAboveAndBelowTheCheckboxStillToggles() {
        setScreen()

        // Aim inside the leading strip but at the very top of the row: the pixels a
        // slightly-high tap at the checkbox lands on. They must tick, not navigate.
        row().performTouchInput { click(Offset(24.dp.toPx(), 1f)) }
        toggleStrip().assertIsOff()
        assertNull(clickedProductId)

        // ...and the same at the very bottom of the row.
        row().performTouchInput { click(Offset(24.dp.toPx(), height - 1f)) }
        toggleStrip().assertIsOn()
        assertNull(clickedProductId)
    }

    @Test
    fun theComparisonIsAbsentWhenNoStoreCanPriceTheBasket() {
        setScreen()

        composeTestRule
            .onNodeWithText(string(R.string.history_detail_cost_elsewhere))
            .assertDoesNotExist()
    }

    @Test
    fun theComparisonNamesEveryStoreAndHowItComparesToWhatWasPaid() {
        setScreen(
            listOf(
                StoreBasketCost("s-2", "Publix", 6.50, 1.48),
                StoreBasketCost("s-1", "Whole Foods", 9.00, -1.02),
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.history_detail_cost_elsewhere))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Publix").assertIsDisplayed()
        // The difference is stated unsigned, with the wording carrying the direction.
        composeTestRule
            .onNodeWithText(string(R.string.history_detail_cost_cheaper, "$1.48"))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.history_detail_cost_dearer, "$1.02"))
            .assertIsDisplayed()
    }

    @Test
    fun aStoreMatchingWhatWasPaidSaysSoRatherThanShowingZero() {
        setScreen(listOf(StoreBasketCost("s-2", "Publix", 7.98, 0.0)))

        composeTestRule
            .onNodeWithText(string(R.string.history_detail_cost_same))
            .assertIsDisplayed()
    }
}
