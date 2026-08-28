package com.fullsail.shoppingmadebetter.navigation

import com.fullsail.shoppingmadebetter.feature.auth.data.AuthRepository
import com.fullsail.shoppingmadebetter.feature.auth.domain.AuthState
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Unit tests for [NavigationViewModel]'s cold-start routing and screen-title handling. */
class NavigationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * Fake auth repository whose [authState] is a settable flow. It starts
     * [AuthState.Initializing] (as on a real cold start) unless overridden, so the
     * ViewModel's one-shot gate stays parked until a test settles it.
     */
    private class FakeAuthRepository(
        initial: AuthState = AuthState.Initializing,
    ) : AuthRepository {
        val authStateFlow = MutableStateFlow(initial)
        override val authState: StateFlow<AuthState> = authStateFlow
        var signedOut = false
        override suspend fun signUp(email: String, password: String) = Unit
        override suspend fun signIn(email: String, password: String) = Unit
        override suspend fun signOut() { signedOut = true }
    }

    private fun buildViewModel(
        auth: FakeAuthRepository = FakeAuthRepository(),
    ) = NavigationViewModel(auth)

    @Test
    fun `a cached session routes into the app on startup`() = runTest {
        val viewModel = buildViewModel(FakeAuthRepository(AuthState.Authenticated))

        assertEquals(NavEvent.EnterApp, viewModel.events.first())
    }

    @Test
    fun `no cached session routes to login on startup`() = runTest {
        val viewModel = buildViewModel(FakeAuthRepository(AuthState.Unauthenticated))

        assertEquals(NavEvent.ToLogin, viewModel.events.first())
    }

    @Test
    fun `routing waits until the session settles past Initializing`() = runTest {
        val auth = FakeAuthRepository(AuthState.Initializing)
        val viewModel = buildViewModel(auth)

        // Session restored as signed-in only after the gate has started waiting.
        auth.authStateFlow.value = AuthState.Authenticated

        assertEquals(NavEvent.EnterApp, viewModel.events.first())
    }

    @Test
    fun `logout signs out and routes to login`() = runTest {
        val auth = FakeAuthRepository() // stays Initializing
        val viewModel = buildViewModel(auth)

        viewModel.logout()

        assertEquals(NavEvent.ToLogin, viewModel.events.first())
        assertTrue(auth.signedOut)
    }

    @Test
    fun `screen title starts null`() {
        val viewModel = buildViewModel()

        assertNull(viewModel.screenTitle.value)
    }

    @Test
    fun `setScreenTitle exposes the title`() {
        val viewModel = buildViewModel()

        viewModel.setScreenTitle("2% Milk")

        assertEquals("2% Milk", viewModel.screenTitle.value)
    }

    @Test
    fun `onDestinationChanged clears a previously set title`() {
        val viewModel = buildViewModel()
        viewModel.setScreenTitle("2% Milk")

        // Navigating anywhere drops the previous screen's custom title.
        viewModel.onDestinationChanged(routeName = "Pantry", tab = TopLevelDestination.PANTRY, showChrome = true)

        assertNull(viewModel.screenTitle.value)
    }

    @Test
    fun `a screen can set its title after the destination change that cleared it`() {
        val viewModel = buildViewModel()

        // The listener fires first (tab = null for a detail route), then the screen loads.
        viewModel.onDestinationChanged(routeName = "ProductDetail", tab = null, showChrome = true)
        viewModel.setScreenTitle("2% Milk")

        assertNull(viewModel.currentTab.value)
        assertEquals("2% Milk", viewModel.screenTitle.value)
    }
}