package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryDatePreset
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryFilter
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for [HistoryFilterPanel] — the expand/collapse behaviour and what
 * a collapsed panel still says about an active filter. Pure Compose state, so this is
 * the only place it can be covered.
 */
class HistoryFilterPanelTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(resId: Int, vararg args: Any) =
        composeTestRule.activity.getString(resId, *args)

    private fun plural(resId: Int, count: Int, vararg args: Any) =
        composeTestRule.activity.resources.getQuantityString(resId, count, *args)

    /** The header, addressed by the title it always shows. */
    private fun header() = composeTestRule.onNodeWithText(string(R.string.history_filters_title))

    private fun searchField() =
        composeTestRule.onNodeWithText(string(R.string.history_search_label))

    private val activeFilter = HistoryFilter(
        storeIds = setOf("s-2"),
        from = LocalDate(2026, 7, 30),
        to = LocalDate(2026, 8, 28),
        search = "oats",
    )

    private fun setPanel(
        filter: HistoryFilter = HistoryFilter(),
        selectedPreset: HistoryDatePreset? = null,
    ) {
        composeTestRule.setContent {
            var expanded by remember { mutableStateOf(false) }
            ShoppingMadeBetterTheme {
                HistoryFilterPanel(
                    stores = previewStores,
                    filter = filter,
                    selectedPreset = selectedPreset,
                    searchInput = filter.search,
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    onSearchChange = {},
                    onToggleStore = {},
                    onSelectPreset = {},
                    onCustomRange = { _, _ -> },
                    onClearFilters = {},
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun thePanelStartsCollapsed() {
        setPanel()

        header().assertIsDisplayed()
        searchField().assertDoesNotExist()
    }

    @Test
    fun tappingTheHeaderRevealsTheControls() {
        setPanel()

        header().performClick()
        composeTestRule.waitForIdle()

        searchField().assertIsDisplayed()
        // A store chip, so the whole body opened rather than just the field.
        composeTestRule
            .onNodeWithContentDescription(string(R.string.history_filter_store_desc, "ALDI"))
            .assertIsDisplayed()
    }

    @Test
    fun tappingTheHeaderAgainHidesTheControls() {
        setPanel()

        header().performClick()
        composeTestRule.waitForIdle()
        header().performClick()
        composeTestRule.waitForIdle()

        searchField().assertDoesNotExist()
    }

    @Test
    fun aCleanPanelShowsNoBadgeAndNoSummary() {
        setPanel()

        composeTestRule
            .onNodeWithContentDescription(plural(R.plurals.history_filters_active, 1, 1))
            .assertDoesNotExist()
        composeTestRule.onNode(hasText("ALDI", substring = true)).assertDoesNotExist()
    }

    @Test
    fun aCollapsedPanelCountsAndNamesTheActiveFilters() {
        setPanel(filter = activeFilter, selectedPreset = HistoryDatePreset.Last30Days)

        // One store + one range + one search.
        composeTestRule
            .onNodeWithContentDescription(plural(R.plurals.history_filters_active, 3, 3))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("ALDI · Last 30 days · \"oats\"")
            .assertIsDisplayed()
    }

    @Test
    fun expandingHidesTheSummaryTheChipsNowShow() {
        setPanel(filter = activeFilter, selectedPreset = HistoryDatePreset.Last30Days)

        header().performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("ALDI · Last 30 days · \"oats\"")
            .assertDoesNotExist()
        // The badge stays: it is the count, not a restatement of the chips.
        composeTestRule
            .onNodeWithContentDescription(plural(R.plurals.history_filters_active, 3, 3))
            .assertIsDisplayed()
    }
}
