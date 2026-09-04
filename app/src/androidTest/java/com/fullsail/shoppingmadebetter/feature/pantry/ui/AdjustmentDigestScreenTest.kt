package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.pantry.domain.AdjustmentDigestEntry
import com.fullsail.shoppingmadebetter.feature.pantry.domain.EstimateSource
import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetAdjustmentDigestUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UndoInventoryAdjustment
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UndoInventoryAdjustmentUseCase
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Compose UI tests for [AdjustmentDigestScreen] — the rows, their markers, and both undos. */
class AdjustmentDigestScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeGetAdjustmentDigestUseCase(
        private val output: GetAdjustmentDigestUseCase.Output =
            GetAdjustmentDigestUseCase.Output.Success(emptyList()),
    ) : GetAdjustmentDigestUseCase {
        override suspend fun execute(input: Unit) = output
    }

    private class FakeUndoInventoryAdjustmentUseCase : UndoInventoryAdjustmentUseCase {
        val undone = mutableListOf<String>()
        override suspend fun execute(
            input: UndoInventoryAdjustment,
        ): UndoInventoryAdjustmentUseCase.Output {
            undone += input.adjustmentId
            return UndoInventoryAdjustmentUseCase.Output.Success(newQuantity = 3, appliedDelta = 1)
        }
    }

    private val rice = AdjustmentDigestEntry(
        adjustmentId = "a1",
        lotId = "lot1",
        productId = "p1",
        productName = "Jasmine Rice",
        imageUrl = "",
        delta = -2,
        quantityNow = 1,
        productQuantity = 2,
        lowStockThreshold = 3,
        source = EstimateSource.History,
        daysAgo = 0,
    )

    /** No threshold and plenty on hand, so this row carries no stock marker. */
    private val wings = AdjustmentDigestEntry(
        adjustmentId = "a2",
        lotId = "lot2",
        productId = "p2",
        productName = "Jerk Chicken Wings",
        imageUrl = "",
        delta = -1,
        quantityNow = 4,
        productQuantity = 4,
        source = EstimateSource.ShelfLife,
        daysAgo = 2,
    )

    private val peanutButter = rice.copy(
        adjustmentId = "a3",
        lotId = "lot3",
        productId = "p3",
        productName = "Peanut Butter",
        delta = -1,
        quantityNow = 0,
        productQuantity = 0,
        source = null,
        daysAgo = 1,
    )

    private fun string(resId: Int, vararg args: Any) =
        composeTestRule.activity.getString(resId, *args)

    private fun quantityString(resId: Int, quantity: Int, vararg args: Any) =
        composeTestRule.activity.resources.getQuantityString(resId, quantity, *args)

    private fun setScreen(
        digest: GetAdjustmentDigestUseCase = FakeGetAdjustmentDigestUseCase(),
        undo: UndoInventoryAdjustmentUseCase = FakeUndoInventoryAdjustmentUseCase(),
    ) {
        val viewModel = AdjustmentDigestViewModel(digest, undo)
        composeTestRule.setContent {
            ShoppingMadeBetterTheme {
                AdjustmentDigestScreen(viewModel = viewModel)
            }
        }
    }

    private fun digestOf(vararg entries: AdjustmentDigestEntry) = FakeGetAdjustmentDigestUseCase(
        GetAdjustmentDigestUseCase.Output.Success(entries.toList())
    )

    @Test
    fun aRowShowsTheProductChangeAndWhy() {
        setScreen(digest = digestOf(rice))

        composeTestRule.onNodeWithText("Jasmine Rice").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.pantry_digest_change, -2, 1))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.pantry_digest_why_history))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pantry_digest_today)).assertIsDisplayed()
    }

    @Test
    fun theWhyLineFollowsTheEstimateSource() {
        setScreen(digest = digestOf(wings))

        composeTestRule
            .onNodeWithText(string(R.string.pantry_digest_why_shelf_life))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(quantityString(R.plurals.pantry_digest_days_ago, 2, 2))
            .assertIsDisplayed()
    }

    @Test
    fun theStockMarkerIsLowOutOrAbsent() {
        setScreen(digest = digestOf(rice, wings, peanutButter))

        // Rice: 2 on hand against a threshold of 3. Peanut Butter: none left. Wings: neither.
        composeTestRule
            .onAllNodesWithText(string(R.string.pantry_dashboard_running_low))
            .assertCountEquals(1)
        composeTestRule
            .onAllNodesWithText(string(R.string.pantry_dashboard_out))
            .assertCountEquals(1)
    }

    @Test
    fun undoOnARowReversesThatAdjustment() {
        val undo = FakeUndoInventoryAdjustmentUseCase()
        setScreen(digest = digestOf(rice, wings), undo = undo)

        composeTestRule
            .onAllNodesWithText(string(R.string.pantry_estimate_undo))[0]
            .performClick()

        composeTestRule.waitForIdle()
        assertEquals(listOf("a1"), undo.undone)
        composeTestRule.onNodeWithText("Jasmine Rice").assertDoesNotExist()
        composeTestRule.onNodeWithText("Jerk Chicken Wings").assertIsDisplayed()
    }

    @Test
    fun undoAllReversesEveryRow() {
        val undo = FakeUndoInventoryAdjustmentUseCase()
        setScreen(digest = digestOf(rice, wings), undo = undo)

        composeTestRule
            .onNodeWithText(string(R.string.pantry_digest_undo_all, 2))
            .performClick()

        composeTestRule.waitForIdle()
        assertEquals(listOf("a1", "a2"), undo.undone)
        composeTestRule.onNodeWithText(string(R.string.pantry_digest_empty)).assertIsDisplayed()
    }

    @Test
    fun theHeaderCountsTheAdjustments() {
        setScreen(digest = digestOf(rice, wings, peanutButter))

        composeTestRule
            .onNodeWithText(quantityString(R.plurals.pantry_digest_header, 3, 3))
            .assertIsDisplayed()
    }

    @Test
    fun anEmptyDigestSaysThereIsNothingToReview() {
        setScreen(digest = digestOf())

        composeTestRule.onNodeWithText(string(R.string.pantry_digest_empty)).assertIsDisplayed()
    }

    @Test
    fun aFailedLoadOffersRetry() {
        setScreen(
            digest = FakeGetAdjustmentDigestUseCase(
                GetAdjustmentDigestUseCase.Output.Failure(IOException("boom"))
            ),
        )

        composeTestRule.onNodeWithText(string(R.string.pantry_error)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.pantry_retry)).assertIsDisplayed()
    }
}
