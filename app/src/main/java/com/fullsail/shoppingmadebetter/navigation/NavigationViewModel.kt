package com.fullsail.shoppingmadebetter.navigation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.auth.data.AuthRepository
import com.fullsail.shoppingmadebetter.feature.auth.domain.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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

    /** Leave the login/splash gate and enter the tabbed app, clearing the gate from the back stack. */
    data object EnterApp : NavEvent

    /** Leave the splash gate for the login screen (no cached session), clearing splash from the back stack. */
    data object ToLogin : NavEvent

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
class NavigationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    // BUFFERED so intents emitted before the UI starts collecting are not dropped.
    private val _events = Channel<NavEvent>(Channel.BUFFERED)
    val events: Flow<NavEvent> = _events.receiveAsFlow()

    init {
        // Cold-start gate: wait for the cached session to settle (past Initializing),
        // then leave the splash for the app or the login screen. One-shot — later
        // sign-in/sign-out is driven by the screens themselves, so a new sign-up's
        // session doesn't yank the user off the onboarding flow.
        viewModelScope.launch {
            val settled = authRepository.authState.first { it != AuthState.Initializing }
            Log.d(NAV_LOG_TAG, "Auth settled: $settled")
            _events.trySend(
                if (settled == AuthState.Authenticated) NavEvent.EnterApp else NavEvent.ToLogin
            )
        }
    }

    private val _currentTab = MutableStateFlow<TopLevelDestination?>(null)

    /** The selected top-level tab, or null when the current screen is not a top-level tab. */
    val currentTab: StateFlow<TopLevelDestination?> = _currentTab.asStateFlow()

    private val _screenTitle = MutableStateFlow<String?>(null)

    /**
     * A title supplied by the current screen for the top app bar, or null to use the
     * default (the tab label, or the app name on a non-tab screen that sets no title).
     *
     * Non-tab screens (e.g. the pantry item detail) call [setScreenTitle] once their
     * content is known to show something meaningful like the item's name 
     */
    val screenTitle: StateFlow<String?> = _screenTitle.asStateFlow()

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

    /**
     * Signs the user out and returns to the login screen.
     */
    fun logout() {
        viewModelScope.launch {
            Log.d(NAV_LOG_TAG, "Logging out")
            authRepository.signOut()
            _events.trySend(NavEvent.ToLogin)
        }
    }

    /** Up navigation, typically from the top bar's back arrow. */
    fun navigateUp() {
        Log.d(NAV_LOG_TAG, "Navigate up")
        _events.trySend(NavEvent.Up)
    }

    /**
     * Sets the top-app-bar [title] for the current (non-tab) screen. Call this once the
     * screen has the data it wants to show, like `onTitleChange(item.name)` when a detail
     * screen finishes loading. The title is reset on the next destination change, 
     */
    fun setScreenTitle(title: String) {
        Log.d(NAV_LOG_TAG, "Screen title set: $title")
        _screenTitle.value = title
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
        // Drop any custom title from the previous screen; the new screen re-sets its own.
        _screenTitle.value = null
    }
}
