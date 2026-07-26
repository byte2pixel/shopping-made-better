package com.fullsail.shoppingmadebetter.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for [NavigationViewModel]'s screen-title handling. */
class NavigationViewModelTest {

    @Test
    fun `screen title starts null`() {
        val viewModel = NavigationViewModel()

        assertNull(viewModel.screenTitle.value)
    }

    @Test
    fun `setScreenTitle exposes the title`() {
        val viewModel = NavigationViewModel()

        viewModel.setScreenTitle("2% Milk")

        assertEquals("2% Milk", viewModel.screenTitle.value)
    }

    @Test
    fun `onDestinationChanged clears a previously set title`() {
        val viewModel = NavigationViewModel()
        viewModel.setScreenTitle("2% Milk")

        // Navigating anywhere drops the previous screen's custom title.
        viewModel.onDestinationChanged(routeName = "Pantry", tab = TopLevelDestination.PANTRY, showChrome = true)

        assertNull(viewModel.screenTitle.value)
    }

    @Test
    fun `a screen can set its title after the destination change that cleared it`() {
        val viewModel = NavigationViewModel()

        // The listener fires first (tab = null for a detail route), then the screen loads.
        viewModel.onDestinationChanged(routeName = "PantryItemDetail", tab = null, showChrome = true)
        viewModel.setScreenTitle("2% Milk")

        assertNull(viewModel.currentTab.value)
        assertEquals("2% Milk", viewModel.screenTitle.value)
    }
}