package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.history.domain.GetPurchaseHistoryUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.GetSpendSummaryUseCase
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTripSummary
import com.fullsail.shoppingmadebetter.feature.stores.domain.GetStoresUseCase
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Compose UI tests for [HistoryScreen]: the four states its trip list can be in, the
 * card tap, and the warning a failed background refresh raises over loaded trips.
 *
 * The real [HistoryViewModel] and a real Pager drive the screen, so these cover the
 * paging wiring as well as the rendering.
 */
class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Fake history page reader. [gate], when set, holds the call suspended so a test
     * can look at the screen mid-load.
     */
    private class FakeGetPurchaseHistoryUseCase(
        var output: GetPurchaseHistoryUseCase.Output =
            GetPurchaseHistoryUseCase.Output.Success(emptyList(), endReached = true),
        var gate: CompletableDeferred<Unit>? = null,
    ) : GetPurchaseHistoryUseCase {
        override suspend fun execute(
            input: GetPurchaseHistoryUseCase.Input,
        ): GetPurchaseHistoryUseCase.Output {
            gate?.await()
            return output
        }
    }

    /** The chips are collapsed in every test here, so an empty list is enough. */
    private class FakeGetStoresUseCase : GetStoresUseCase {
        override suspend fun execute(input: Unit) =
            GetStoresUseCase.Output.Success(emptyList())
    }

    /**
     * Fails by default, which leaves the insights section absent. A first-load failure
     * raises no warning, so it cannot interfere with the assertions below.
     */
    private class FakeGetSpendSummaryUseCase(
        var output: GetSpendSummaryUseCase.Output =
            GetSpendSummaryUseCase.Output.Failure(IOException("not under test")),
    ) : GetSpendSummaryUseCase {
        override suspend fun execute(input: Unit) = output
    }

    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-28T12:00:00Z")
    }

    private val aldiTrip = PurchaseTripSummary(
        id = "trip-1",
        purchasedOn = LocalDate(2026, 8, 19),
        purchasedAtEpoch = 1_787_109_229L,
        storeName = "ALDI",
        recordedTotal = 42.32,
        lineTotal = 42.32,
        itemCount = 4,
    )

    private var clickedTripId: String? = null

    private fun string(resId: Int, vararg args: Any) =
        composeTestRule.activity.getString(resId, *args)

    private fun plural(resId: Int, count: Int, vararg args: Any) =
        composeTestRule.activity.resources.getQuantityString(resId, count, *args)

    /** Matches the trip card, which names its action rather than the node's text. */
    private fun hasClickLabel(label: String) = SemanticsMatcher("clickLabel = $label") { node ->
        node.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }

    private fun tripCard() = composeTestRule.onNode(hasClickLabel(string(R.string.history_trip_open)))

    /** Builds the screen over a real ViewModel; [onTab] lets a test leave and return. */
    private fun setScreen(
        history: GetPurchaseHistoryUseCase,
        spend: GetSpendSummaryUseCase = FakeGetSpendSummaryUseCase(),
        onTab: () -> Boolean = { true },
    ): HistoryViewModel {
        val viewModel = HistoryViewModel(
            history,
            FakeGetStoresUseCase(),
            spend,
            SavedStateHandle(),
            fixedClock,
        )
        composeTestRule.setContent {
            ShoppingMadeBetterTheme {
                if (onTab()) {
                    HistoryScreen(
                        onTripClick = { clickedTripId = it },
                        viewModel = viewModel,
                    )
                }
            }
        }
        return viewModel
    }

    /** Waits for [text] to appear; paging delivers after the composition settles. */
    private fun awaitText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun aLoadInFlightShowsAProgressIndicator() {
        val gate = CompletableDeferred<Unit>()
        setScreen(
            history = FakeGetPurchaseHistoryUseCase(
                GetPurchaseHistoryUseCase.Output.Success(listOf(aldiTrip), endReached = true),
                gate = gate,
            ),
        )

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
        // Nothing has arrived, so none of the settled states are on screen.
        composeTestRule.onNodeWithText(string(R.string.history_empty)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.history_error)).assertDoesNotExist()

        gate.complete(Unit)
    }

    @Test
    fun noPurchasesYetExplainsItself() {
        setScreen(
            history = FakeGetPurchaseHistoryUseCase(
                GetPurchaseHistoryUseCase.Output.Success(emptyList(), endReached = true),
            ),
        )

        awaitText(string(R.string.history_empty))
        composeTestRule.onNodeWithText(string(R.string.history_empty)).assertIsDisplayed()
    }

    @Test
    fun aFailedFirstLoadShowsTheErrorAndRetry() {
        setScreen(
            history = FakeGetPurchaseHistoryUseCase(
                GetPurchaseHistoryUseCase.Output.Failure(IOException("no network")),
            ),
        )

        awaitText(string(R.string.history_error))
        composeTestRule.onNodeWithText(string(R.string.history_error)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.history_retry)).assertIsDisplayed()
    }

    @Test
    fun loadedTripsRenderAsCards() {
        setScreen(
            history = FakeGetPurchaseHistoryUseCase(
                GetPurchaseHistoryUseCase.Output.Success(listOf(aldiTrip), endReached = true),
            ),
        )

        awaitText("ALDI")
        composeTestRule.onNodeWithText(formatTripDate(aldiTrip.purchasedOn)).assertIsDisplayed()
        composeTestRule.onNodeWithText(formatPrice(aldiTrip.total)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(plural(R.plurals.history_trip_item_count, 4, 4))
            .assertIsDisplayed()
    }

    @Test
    fun tappingATripCardReportsItsId() {
        setScreen(
            history = FakeGetPurchaseHistoryUseCase(
                GetPurchaseHistoryUseCase.Output.Success(listOf(aldiTrip), endReached = true),
            ),
        )
        awaitText("ALDI")

        tripCard().performClick()

        assertEquals("trip-1", clickedTripId)
    }

    @Test
    fun aFailedRefreshKeepsTheTripsAndWarns() {
        val history = FakeGetPurchaseHistoryUseCase(
            GetPurchaseHistoryUseCase.Output.Success(listOf(aldiTrip), endReached = true),
        )
        var onTab by mutableStateOf(true)
        setScreen(history = history, onTab = { onTab })
        awaitText("ALDI")

        // Leave the tab and come back to a failing server: the pager's cached pages are
        // replayed, so the refresh on entry fails over trips already on screen.
        onTab = false
        composeTestRule.waitForIdle()
        history.output = GetPurchaseHistoryUseCase.Output.Failure(IOException("no network"))
        onTab = true

        awaitText(string(R.string.history_refresh_failed))
        composeTestRule.onNodeWithText("ALDI").assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.history_error)).assertDoesNotExist()
    }
}
