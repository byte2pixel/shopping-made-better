package com.fullsail.shoppingmadebetter.navigation

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

const val NAV_LOG_TAG = "NavigationViewModel"

/**
 * A one-shot navigation command emitted by the [NavigationViewModel] and applied to the
 * [androidx.navigation.NavController] by the UI layer.
 *
 * The ViewModel never touches a NavController directly (which would risk leaking a
 * composition-scoped object across configuration changes and is hard to unit test).
 * Instead it emits these events; the UI collects them and performs the actual navigation.
 */
sealed interface NavEvent {
    /** Navigate to a bottom-bar tab, preserving/restoring each tab's back stack. */
    data class ToTab(val destination: TopLevelDestination) : NavEvent

    /** Leave the login/landing gate and enter the tabbed app, clearing login from the back stack. */
    data object EnterApp : NavEvent

    /** Up navigation (respects the navigation hierarchy). */
    data object Up : NavEvent
}

/**
 * Centralizes all navigation logic for the app.
 *
 * Responsibilities:
 *  - Expose navigation intents ([onTabSelected], [onAuthenticated], [navigateUp]) as a stream of
 *    [NavEvent]s that the UI applies to the NavController.
 *  - Track the currently-selected top-level tab ([currentTab]) and whether the app chrome
 *    (top/bottom bars) should be shown ([showAppChrome]) so the UI can render selection, title,
 *    and the login gate without owning that state itself.
 *  - Log every navigation transition under [NAV_LOG_TAG] for easy verification in Logcat.
 *
 * Keeping the NavController out of the ViewModel follows current Android guidance: the
 * NavController stays in the UI layer, and this ViewModel stays a plain, testable holder of
 * navigation intent + state.
 */
@HiltViewModel
class NavigationViewModel @Inject constructor() : ViewModel() {

    // BUFFERED so intents emitted before the UI starts collecting are not dropped.
    private val _events = Channel<NavEvent>(Channel.BUFFERED)
    val events: Flow<NavEvent> = _events.receiveAsFlow()

    private val _currentTab = MutableStateFlow<TopLevelDestination?>(null)

    /** The selected top-level tab, or null when the current screen is not a top-level tab. */
    val currentTab: StateFlow<TopLevelDestination?> = _currentTab.asStateFlow()

    // Starts false so the login/landing gate (the start destination) shows no bars on cold start.
    private val _showAppChrome = MutableStateFlow(false)

    /** Whether the top/bottom app bars should be shown (false while on the login/landing gate). */
    val showAppChrome: StateFlow<Boolean> = _showAppChrome.asStateFlow()

    /** Select a bottom-bar tab. */
    fun onTabSelected(tab: TopLevelDestination) {
        Log.d(NAV_LOG_TAG, "Tab selected: ${tab.name}")
        _events.trySend(NavEvent.ToTab(tab))
    }

    /** Called after a successful sign-in or sign-up: enter the tabbed app. */
    fun onAuthenticated() {
        Log.d(NAV_LOG_TAG, "Authenticated -> entering app")
        _events.trySend(NavEvent.EnterApp)
    }

    /** Up navigation, typically from the top bar's back arrow. */
    fun navigateUp() {
        Log.d(NAV_LOG_TAG, "Navigate up")
        _events.trySend(NavEvent.Up)
    }

    /**
     * Called by the UI whenever the NavController's destination changes, keeping [currentTab]
     * and [showAppChrome] in sync with the real back stack (the single source of truth for
     * "where we are").
     *
     * @param routeName the serialized route of the new destination, for logging.
     * @param tab the matching top-level tab, or null if the destination is not a tab.
     * @param showChrome whether this destination should display the top/bottom app bars.
     */
    fun onDestinationChanged(routeName: String?, tab: TopLevelDestination?, showChrome: Boolean) {
        Log.d(NAV_LOG_TAG, "Destination changed -> route=$routeName, tab=${tab?.name}, chrome=$showChrome")
        _currentTab.value = tab
        _showAppChrome.value = showChrome
    }
}
