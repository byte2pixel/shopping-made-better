package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for [PantryItemDetailScreen].
 */
class PantryItemDetailScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Fake use case. An optional [gate] lets a test hold [execute] suspended so
     * the screen stays in its Loading state for as long as needed to inspect it.
     */
    private class FakeGetInventoryItemUseCase(
        var output: GetInventoryItemUseCase.Output,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : GetInventoryItemUseCase {
        override suspend fun execute(input: String): GetInventoryItemUseCase.Output {
            gate?.await()
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
    )

    /** Convenience: look up a string resource the way the screen does. */
    private fun string(resId: Int, vararg args: Any) =
        composeTestRule.activity.getString(resId, *args)

    /** Renders the screen wired to [useCase], inside the app theme. */
    private fun setScreen(useCase: GetInventoryItemUseCase) {
        val viewModel = PantryItemDetailViewModel(useCase)
        composeTestRule.setContent {
            ShoppingMadeBetterTheme {
                PantryItemDetailScreen(itemId = "i1", viewModel = viewModel)
            }
        }
    }

    @Test
    fun showsSpinnerWhileLoading() {
        // A gate that we never complete keeps the load suspended -> Loading state.
        setScreen(
            FakeGetInventoryItemUseCase(
                GetInventoryItemUseCase.Output.Success(milk),
                gate = CompletableDeferred(),
            )
        )
        
        composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("2% Milk").assertDoesNotExist()
    }

    @Test
    fun showsItemDetailsOnSuccess() {
        setScreen(FakeGetInventoryItemUseCase(GetInventoryItemUseCase.Output.Success(milk)))

        // The title, plus each labeled DetailField (label + value are separate nodes).
        composeTestRule.onNodeWithText("2% Milk").assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pantry_detail_brand)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Great Value").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 gal").assertIsDisplayed()
        // Stock renders the quantity as text.
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun showsNotFoundMessage() {
        setScreen(FakeGetInventoryItemUseCase(GetInventoryItemUseCase.Output.NotFound))

        composeTestRule.onNodeWithText(string(R.string.pantry_detail_not_found)).assertIsDisplayed()
    }

    @Test
    fun errorStateRetriesAndThenShowsTheItem() {
        // Start failing so the screen lands on the Error state...
        val useCase = FakeGetInventoryItemUseCase(
            GetInventoryItemUseCase.Output.Failure(RuntimeException("boom"))
        )
        setScreen(useCase)

        composeTestRule.onNodeWithText(string(R.string.pantry_error)).assertIsDisplayed()

        useCase.output = GetInventoryItemUseCase.Output.Success(milk)
        composeTestRule.onNodeWithText(string(R.string.pantry_retry)).performClick()

        composeTestRule.onNodeWithText("2% Milk").assertIsDisplayed()
    }
}