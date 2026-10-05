package com.fullsail.shoppingmadebetter.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for [ContentSizedBottomSheet]: content that changes height while the
 * sheet is animating, the way a list that loads after the sheet opens does.
 */
@OptIn(ExperimentalMaterial3Api::class)
class ContentSizedBottomSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var visible by mutableStateOf(false)
    private var grown by mutableStateOf(false)
    private lateinit var sheetState: SheetState
    private lateinit var scope: CoroutineScope

    /** A sheet whose content is [SHORT] until [grown], with [FOOTER] as its last line. */
    private fun setSheet() {
        composeTestRule.setContent {
            ShoppingMadeBetterTheme {
                scope = rememberCoroutineScope()
                if (visible) {
                    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    sheetState = state
                    ContentSizedBottomSheet(
                        onDismissRequest = { visible = false },
                        sheetState = state,
                    ) {
                        Box(Modifier.height(if (grown) TALL else SHORT))
                        Text(FOOTER)
                    }
                }
            }
        }
    }

    /** Shows the short sheet with the clock paused, so the caller steps its open animation. */
    private fun openPaused() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnUiThread {
            grown = false
            visible = true
        }
    }

    private fun resume() {
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
    }

    private fun close() {
        composeTestRule.runOnUiThread { visible = false }
        composeTestRule.waitForIdle()
    }

    /** False until the first frame has composed the sheet. */
    private fun isExpanded() = composeTestRule.runOnUiThread<Boolean> {
        ::sheetState.isInitialized && sheetState.currentValue == SheetValue.Expanded
    }

    /** How many frames the open animation takes to report the sheet expanded. */
    private fun framesToOpen(): Int {
        openPaused()
        var frames = 0
        do {
            assertTrue("the sheet never finished opening", frames < MAX_OPEN_FRAMES)
            composeTestRule.mainClock.advanceTimeByFrame()
            frames++
        } while (!isExpanded())
        resume()
        close()
        return frames
    }

    @Test
    fun contentThatGrowsAsTheOpenAnimationEndsIsNotClipped() {
        setSheet()
        val framesToOpen = framesToOpen()

        // The stock sheet drops a height change that lands on the frame its open animation
        // ends, one before it reports expanded. The frames around it guard the timing.
        for (frames in framesToOpen - 3..framesToOpen) {
            openPaused()
            repeat(frames) { composeTestRule.mainClock.advanceTimeByFrame() }
            composeTestRule.runOnUiThread { grown = true }
            resume()

            composeTestRule.onNodeWithText(FOOTER).assertIsDisplayed()
            assertTrue("grown after $frames frames", isExpanded())
            close()
        }
    }

    @Test
    fun contentThatGrowsWhileTheSheetHidesDoesNotReopenIt() {
        setSheet()
        openPaused()
        resume()
        assertTrue(isExpanded())

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnUiThread { scope.launch { sheetState.hide() } }
        repeat(3) { composeTestRule.mainClock.advanceTimeByFrame() }
        composeTestRule.runOnUiThread { grown = true }
        resume()

        assertFalse(composeTestRule.runOnUiThread<Boolean> { sheetState.isVisible })
    }

    private companion object {
        val SHORT = 72.dp
        val TALL = 220.dp
        const val FOOTER = "footer"
        const val MAX_OPEN_FRAMES = 120
    }
}
