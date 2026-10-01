package com.fullsail.shoppingmadebetter.feature.product.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThreshold
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThresholdUseCase
import com.fullsail.shoppingmadebetter.feature.product.domain.GetProductDetailUseCase
import com.fullsail.shoppingmadebetter.feature.product.domain.ProductDetail
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for [ProductDetailScreen].
 */
class ProductDetailScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Fake use case. An optional [gate] lets a test hold [execute] suspended so
     * the screen stays in its Loading state for as long as needed to inspect it.
     */
    private class FakeGetProductDetailUseCase(
        var output: GetProductDetailUseCase.Output,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : GetProductDetailUseCase {
        override suspend fun execute(input: String): GetProductDetailUseCase.Output {
            gate?.await()
            return output
        }
    }

    /** No-op threshold-update use case; the detail screen tests don't exercise saves. */
    private class FakeUpdateThresholdUseCase : UpdateInventoryLowStockThresholdUseCase {
        override suspend fun execute(
            input: UpdateInventoryLowStockThreshold,
        ): UpdateInventoryLowStockThresholdUseCase.Output =
            UpdateInventoryLowStockThresholdUseCase.Output.Success
    }

    private fun lot(
        id: String,
        expiresInDays: Int?,
        location: PantryLocation,
        addedBy: String? = null,
    ) = InventoryItem(
        id = id,
        productId = "p1",
        name = "2% Milk",
        brand = "Great Value",
        description = "Reduced-fat milk",
        size = "1 gal",
        imageUrl = "",
        quantity = 1,
        expiresInDays = expiresInDays,
        location = location,
        addedBy = addedBy,
        isOwn = addedBy == null,
    )

    /** Two lots: the user's own, expired, in the fridge; a housemate's, 8 days out, in the pantry. */
    private val milk = ProductDetail(
        id = "p1",
        name = "2% Milk",
        brand = "Great Value",
        description = "Reduced-fat milk",
        size = "1 gal",
        imageUrl = "",
        lots = listOf(
            lot("l1", expiresInDays = -2, location = PantryLocation.Fridge),
            lot("l2", expiresInDays = 8, location = PantryLocation.Pantry, addedBy = "Demo Roommate"),
        ),
    )

    /** Convenience: look up a string resource the way the screen does. */
    private fun string(resId: Int, vararg args: Any) =
        composeTestRule.activity.getString(resId, *args)

    private fun quantityString(resId: Int, quantity: Int, vararg args: Any) =
        composeTestRule.activity.resources.getQuantityString(resId, quantity, *args)

    /** The "Qty 1 · Where" line of a lot row. */
    private fun lotLine(locationRes: Int) =
        string(R.string.pantry_card_quantity, 1) + " · " + string(locationRes)

    /** The last title the screen reported for the top app bar, or null if none yet. */
    private var reportedTitle: String? = null

    /** Renders the screen wired to [useCase], inside the app theme. */
    private fun setScreen(useCase: GetProductDetailUseCase) {
        val viewModel = ProductDetailViewModel(useCase, FakeUpdateThresholdUseCase())
        composeTestRule.setContent {
            ShoppingMadeBetterTheme {
                ProductDetailScreen(
                    productId = "p1",
                    onTitleChange = { reportedTitle = it },
                    viewModel = viewModel,
                )
            }
        }
    }

    @Test
    fun showsSpinnerWhileLoading() {
        // A gate that we never complete keeps the load suspended -> Loading state.
        setScreen(
            FakeGetProductDetailUseCase(
                GetProductDetailUseCase.Output.Success(milk),
                gate = CompletableDeferred(),
            )
        )

        composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("2% Milk").assertDoesNotExist()
        // No title is reported until the product loads.
        assertNull(reportedTitle)
    }

    @Test
    fun showsProductDetailsOnSuccess() {
        setScreen(FakeGetProductDetailUseCase(GetProductDetailUseCase.Output.Success(milk)))

        composeTestRule.waitForIdle()
        assertEquals("2% Milk", reportedTitle)
        composeTestRule.onNodeWithText("Great Value · 1 gal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reduced-fat milk").assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pantry_detail_lots, 2)).assertIsDisplayed()
    }

    @Test
    fun eachLotShowsQuantityLocationAndExpiry() {
        setScreen(FakeGetProductDetailUseCase(GetProductDetailUseCase.Output.Success(milk)))

        composeTestRule.onNodeWithText(lotLine(R.string.pantry_dashboard_fridge)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pantry_expiry_expired)).assertIsDisplayed()
        composeTestRule.onNodeWithText(lotLine(R.string.pantry_dashboard_pantry)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(quantityString(R.plurals.pantry_expiry_days, 8, 8))
            .assertIsDisplayed()
    }

    @Test
    fun aHousematesLotSaysWhoAddedIt() {
        setScreen(FakeGetProductDetailUseCase(GetProductDetailUseCase.Output.Success(milk)))

        composeTestRule
            .onNodeWithText(string(R.string.pantry_lot_added_by, "Demo Roommate"))
            .assertIsDisplayed()
    }

    @Test
    fun aProductNoLongerInThePantryStillRenders() {
        // The History case: bought on a past trip, none on hand.
        setScreen(
            FakeGetProductDetailUseCase(
                GetProductDetailUseCase.Output.Success(milk.copy(lots = emptyList())),
            )
        )

        composeTestRule.waitForIdle()
        assertEquals("2% Milk", reportedTitle)
        composeTestRule.onNodeWithText("Great Value · 1 gal").assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pantry_detail_lots, 0)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pantry_detail_no_lots)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pantry_card_quantity, 1), substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun showsNotFoundMessage() {
        setScreen(FakeGetProductDetailUseCase(GetProductDetailUseCase.Output.NotFound))

        composeTestRule.onNodeWithText(string(R.string.pantry_detail_not_found)).assertIsDisplayed()
    }

    @Test
    fun errorStateRetriesAndThenShowsTheProduct() {
        // Start failing so the screen lands on the Error state...
        val useCase = FakeGetProductDetailUseCase(
            GetProductDetailUseCase.Output.Failure(RuntimeException("boom"))
        )
        setScreen(useCase)

        composeTestRule.onNodeWithText(string(R.string.pantry_error)).assertIsDisplayed()

        useCase.output = GetProductDetailUseCase.Output.Success(milk)
        composeTestRule.onNodeWithText(string(R.string.pantry_retry)).performClick()

        composeTestRule.onNodeWithText("Great Value · 1 gal").assertIsDisplayed()
        composeTestRule.waitForIdle()
        assertEquals("2% Milk", reportedTitle)
    }
}
