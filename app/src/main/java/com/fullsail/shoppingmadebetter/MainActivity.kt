package com.fullsail.shoppingmadebetter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fullsail.shoppingmadebetter.feature.auth.ui.LoginScreen
import com.fullsail.shoppingmadebetter.feature.auth.ui.SignUpScreen
import com.fullsail.shoppingmadebetter.feature.pantry.ui.PantryItemDetailScreen
import com.fullsail.shoppingmadebetter.feature.pantry.ui.PantryScreen
import com.fullsail.shoppingmadebetter.feature.shoppinglists.ui.ShoppingListItemComparisonScreen
import com.fullsail.shoppingmadebetter.feature.shoppinglists.ui.ShoppingListsScreen
import com.fullsail.shoppingmadebetter.feature.stores.ui.StoresScreen
import com.fullsail.shoppingmadebetter.navigation.Dest
import com.fullsail.shoppingmadebetter.navigation.NavEvent
import com.fullsail.shoppingmadebetter.navigation.NavigationViewModel
import com.fullsail.shoppingmadebetter.navigation.TopLevelDestination
import com.fullsail.shoppingmadebetter.ui.screens.CartScreen
import com.fullsail.shoppingmadebetter.ui.screens.HistoryScreen
import com.fullsail.shoppingmadebetter.ui.screens.MealsScreen
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import dagger.hilt.android.AndroidEntryPoint
import com.fullsail.shoppingmadebetter.feature.onboarding.ui.OnboardingScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShoppingMadeBetterTheme {
                ShoppingMadeBetterApp()
            }
        }
    }
}

/**
 * Root of the app UI: a [Scaffold] with the top app bar (menu / back) and the bottom
 * navigation bar (the five top-level tabs) wrapped around a type-safe [NavHost].
 *
 * All navigation is driven through the [NavigationViewModel]: the UI collects the
 * ViewModel's [NavEvent]s and applies them to the NavController, and reports every
 * destination change back to the ViewModel (which owns the current-tab state and logging).
 */
@Composable
fun ShoppingMadeBetterApp(
    navigationViewModel: NavigationViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val currentTab by navigationViewModel.currentTab.collectAsState()
    val showAppChrome by navigationViewModel.showAppChrome.collectAsState()

    // Apply one-shot navigation commands from the ViewModel to the NavController.
    LaunchedEffect(navController, navigationViewModel) {
        navigationViewModel.events.collect { event ->
            when (event) {
                is NavEvent.ToTab -> navController.navigate(event.destination.route) {
                    // Single instance per tab; save/restore each tab's own back stack. The home
                    // tab (ShoppingLists) is the anchor every tab pops back to, so system-back
                    // from any tab returns there and back from the home tab exits the app.
                    popUpTo(Dest.ShoppingLists) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }

                NavEvent.EnterApp -> navController.navigate(Dest.ShoppingLists) {
                    // Drop the login gate so system-back exits the app instead of returning to it.
                    popUpTo(Dest.Login) { inclusive = true }
                    launchSingleTop = true
                }

                NavEvent.Up -> navController.navigateUp()
            }
        }
    }

    // Keep the ViewModel's current-tab / chrome state in sync with the real back stack.
    DisposableEffect(navController, navigationViewModel) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            // hasRoute matches by the route's serializer, so it stays correct even if a
            // destination later gains a custom @SerialName.
            val tab = TopLevelDestination.entries.firstOrNull { tab ->
                destination.hasRoute(tab.route::class)
            }
            val showChrome =
                !destination.hasRoute(Dest.Login::class) && !destination.hasRoute(Dest.SignUp::class)
            navigationViewModel.onDestinationChanged(destination.route, tab, showChrome)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    // Top-level tabs show the menu button; any deeper (non-tab) screen shows a back arrow.
    val canNavigateBack = currentTab == null

    Scaffold(
        topBar = {
            if (showAppChrome) {
                AppTopBar(
                    title = currentTab?.let { stringResource(it.label) }
                        ?: stringResource(R.string.app_name),
                    canNavigateBack = canNavigateBack,
                    onMenuClick = { /* TODO: open navigation drawer (future ticket) */ },
                    onBackClick = navigationViewModel::navigateUp,
                )
            }
        },
        bottomBar = {
            if (showAppChrome) {
                AppBottomBar(
                    currentTab = currentTab,
                    onTabSelected = navigationViewModel::onTabSelected,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Login,
            modifier = Modifier.padding(innerPadding),
        ) {

            composable<Dest.ShoppingLists> {
                ShoppingListsScreen(onItemComparison = {
                    navController.navigate(Dest.ShoppingListItemComparison)
                })
            }
            composable<Dest.ShoppingListItemComparison> {
                ShoppingListItemComparisonScreen(onItemComparison = {
                    navController.popBackStack<Dest.ShoppingLists>(false)
                })
            }

            composable<Dest.Login> {
                LoginScreen(
                    onSignedIn = navigationViewModel::onAuthenticated,
                    onNavigateToSignUp = {
                        navController.navigate(Dest.SignUp) { launchSingleTop = true }
                    },
                )
            }
            composable<Dest.SignUp> {
                SignUpScreen(
                    onSignedUp = navigationViewModel::onAuthenticated,
                    onNavigateToSignIn = {
                        // Return to the sign-in gate without stacking Login entries.
                        navController.navigate(Dest.Login) {
                            popUpTo(Dest.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable<Dest.Cart> { CartScreen() }
            composable<Dest.Pantry> {
                PantryScreen(onItemClick = { id -> navController.navigate(Dest.PantryItemDetail(id)) })
            }
            composable<Dest.PantryItemDetail> { entry ->
                PantryItemDetailScreen(itemId = entry.toRoute<Dest.PantryItemDetail>().id)
            }
            composable<Dest.History> { HistoryScreen() }
            composable<Dest.Meals> { MealsScreen() }
            // Dev-only example wired to the Supabase store use case (SCRUM-79).
            composable<Dest.Stores> { StoresScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    canNavigateBack: Boolean,
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.nav_back),
                    )
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = stringResource(R.string.nav_menu),
                    )
                }
            }
        },
    )
}

@Composable
private fun AppBottomBar(
    currentTab: TopLevelDestination?,
    onTabSelected: (TopLevelDestination) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { tab ->
            val label = stringResource(tab.label)
            NavigationBarItem(
                selected = tab == currentTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(painter = painterResource(tab.icon), contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}
