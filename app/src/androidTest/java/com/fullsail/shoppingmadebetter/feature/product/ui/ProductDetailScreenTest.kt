package com.fullsail.shoppingmadebetter.feature.product.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fullsail.shoppingmadebetter.R
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

    private val milk = ProductDetail(
        id = "p1",
        name = "2% Milk",
        brand = "Great Value",
        description = "Reduced-fat milk",
        size = "1 gal",
        imageUrl = "",
        quantityOnHand = 2,
        expiresInDays = null,
    )

    /** Convenience: look up a string resource the way the screen does. */
    private fun string(resId: Int, vararg args: Any) =
        composeTestRule.activity.getString(resId, *args)

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
        composeTestRule.onNodeWithText(string(R.string.pantry_detail_brand)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Great Value").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 gal").assertIsDisplayed()
        // Stock renders the on-hand total as text.
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun aProductNoLongerInThePantryStillRenders() {
        // The History case: bought on a past trip, none on hand, no expiry to show.
        setScreen(
            FakeGetProductDetailUseCase(
                GetProductDetailUseCase.Output.Success(
                    milk.copy(quantityOnHand = 0, expiresInDays = null),
                )
            )
        )

        composeTestRule.waitForIdle()
        assertEquals("2% Milk", reportedTitle)
        composeTestRule.onNodeWithText("Great Value").assertIsDisplayed()
        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.pantry_detail_expires_unknown))
            .assertIsDisplayed()
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

        composeTestRule.onNodeWithText("Great Value").assertIsDisplayed()
        composeTestRule.waitForIdle()
        assertEquals("2% Milk", reportedTitle)
    }
}
