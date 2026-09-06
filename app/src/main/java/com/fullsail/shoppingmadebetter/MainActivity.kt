package com.fullsail.shoppingmadebetter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fullsail.shoppingmadebetter.feature.auth.ui.LoginScreen
import com.fullsail.shoppingmadebetter.feature.auth.ui.SignUpScreen
import com.fullsail.shoppingmadebetter.feature.pantry.ui.AdjustmentDigestScreen
import com.fullsail.shoppingmadebetter.feature.pantry.ui.PantryScreen
import com.fullsail.shoppingmadebetter.feature.product.ui.ProductDetailScreen
import com.fullsail.shoppingmadebetter.feature.shoppinglists.ui.ShoppingListItemComparisonScreen
import com.fullsail.shoppingmadebetter.feature.shoppinglists.ui.ShoppingListsScreen
import com.fullsail.shoppingmadebetter.feature.stores.ui.StoresScreen
import com.fullsail.shoppingmadebetter.navigation.Dest
import com.fullsail.shoppingmadebetter.navigation.NavEvent
import com.fullsail.shoppingmadebetter.navigation.NavigationViewModel
import com.fullsail.shoppingmadebetter.navigation.TopLevelDestination
import com.fullsail.shoppingmadebetter.ui.screens.CartScreen
import com.fullsail.shoppingmadebetter.feature.history.ui.HistoryScreen
import com.fullsail.shoppingmadebetter.feature.history.ui.PurchaseTripDetailScreen
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.fullsail.shoppingmadebetter.feature.onboarding.ui.OnboardingScreen
import com.fullsail.shoppingmadebetter.feature.profile.ui.ChangePasswordScreen
import com.fullsail.shoppingmadebetter.feature.profile.ui.ProfileScreen
import com.fullsail.shoppingmadebetter.feature.shoppinglists.ui.ShoppingListCartScreen
import com.fullsail.shoppingmadebetter.feature.shoppinglists.ui.ShoppingListItemsScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.fullsail.shoppingmadebetter.feature.onboarding.ui.OnboardingUiState
import com.fullsail.shoppingmadebetter.feature.onboarding.ui.OnboardingViewModel
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.saveable.rememberSaveable
import com.fullsail.shoppingmadebetter.feature.meals.ui.MealsScreen
import com.fullsail.shoppingmadebetter.feature.meals.ui.MealsViewModel
import com.fullsail.shoppingmadebetter.feature.shoppinglists.ui.InformationScreen

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
    val screenTitle by navigationViewModel.screenTitle.collectAsState()

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
                    // Drop the splash/login gate so system-back exits the app instead of returning to it.
                    // popUpTo(Splash) clears both the splash (auto-login) and any login screen above it.
                    popUpTo(Dest.Splash) { inclusive = true }
                    launchSingleTop = true
                }

                NavEvent.ToLogin -> navController.navigate(Dest.Login) {
                    // Clear the whole back stack so system-back from login exits the app.
                    // Fires both from the splash gate (cold start) and from the tab stack
                    // (logout), so pop the graph root rather than a specific destination.
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }

                NavEvent.Up -> navController.navigateUp()
            }
        }
    }

    /// Keep the ViewModel's current-tab / chrome state in sync with the real back stack.
    DisposableEffect(navController, navigationViewModel) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val tab = TopLevelDestination.entries.firstOrNull { tab ->
                destination.hasRoute(tab.route::class)
            }
            val showChrome =
                !destination.hasRoute(Dest.Splash::class) &&
                        !destination.hasRoute(Dest.Login::class) &&
                        !destination.hasRoute(Dest.SignUp::class) &&
                        !destination.hasRoute(Dest.Onboarding::class) &&
                        !destination.hasRoute(Dest.Profile::class) &&
                        !destination.hasRoute(Dest.ChangePassword::class)

            // Pass ALL 3 parameters: routeName, tab, showChrome
            navigationViewModel.onDestinationChanged(
                routeName = destination.route,
                tab = tab,
                showChrome = showChrome
            )
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    // Top-level tabs show the menu button; any deeper (non-tab) screen shows a back arrow.
    val canNavigateBack = currentTab == null

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Reachable via the top-bar menu on tab screens; no swipe-to-open elsewhere.
        gesturesEnabled = drawerState.isOpen || currentTab != null,
        drawerContent = {
            AppDrawer(
                onProfile = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Dest.Profile) { launchSingleTop = true }
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    navigationViewModel.logout()
                },
            )
        },
    ) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showAppChrome) {
                AppTopBar(
                    // A screen-supplied title wins (e.g. the pantry item's name); otherwise
                    // fall back to the tab label, then the app name on a titleless non-tab screen.
                    title = screenTitle
                        ?: currentTab?.let { stringResource(it.label) }
                        ?: stringResource(R.string.app_name),
                    canNavigateBack = canNavigateBack,
                    onMenuClick = { scope.launch { drawerState.open() } },
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
            startDestination = Dest.Splash,
            modifier = Modifier.padding(innerPadding),
        ) {

            composable<Dest.Splash> { SplashScreen() }

            composable<Dest.ShoppingLists> {
                ShoppingListsScreen(onItemComparison = {
                    navController.navigate(it)
                })
            }

            composable<Dest.ShoppingListCartScreen>{
                ShoppingListCartScreen( listId = it.toRoute<Dest.ShoppingListCartScreen>().listId, onInfoScreen = {navController.navigate(it)} )
            }
            composable<Dest.InformationScreen>{
                InformationScreen(productId = it.toRoute<Dest.InformationScreen>().productId)
            }
            composable<Dest.ShoppingListItemsScreen>{
                ShoppingListItemsScreen( listId = it.toRoute<Dest.ShoppingListItemsScreen>().listId , onItemComparison = {navController.navigate(it)})
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
                    onSignedUp = {

                        navController.navigate(Dest.Onboarding) {
                            popUpTo(Dest.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSignIn = {
                        navController.navigate(Dest.Login) {
                            popUpTo(Dest.Login) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable<Dest.Onboarding> {
                val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                val uiState by onboardingViewModel.uiState.collectAsState()


                val selectedDiets = rememberSaveable { mutableStateOf(emptySet<String>()) }
                val selectedCategories = rememberSaveable { mutableStateOf(emptySet<String>()) }
                val selectedGoal = rememberSaveable { mutableStateOf<String?>(null) }


                LaunchedEffect(uiState) {
                    when (val s = uiState) {
                        is OnboardingUiState.Success -> {
                            navController.navigate(Dest.ShoppingLists) {
                                popUpTo(Dest.Onboarding::class) { inclusive = true }
                            }
                        }
                        is OnboardingUiState.Error -> {
                            snackbarHostState.showSnackbar(s.message)
                        }
                        else -> {}
                    }
                }

                OnboardingScreen(
                    isSubmitting = uiState is OnboardingUiState.Submitting,
                    selectedDiets = selectedDiets.value,
                    selectedCategories = selectedCategories.value,
                    selectedGoal = selectedGoal.value,
                    onDietToggled = { diet ->
                        val current = selectedDiets.value
                        selectedDiets.value = if (current.contains(diet)) current - diet else current + diet
                    },
                    onCategoryToggled = { category ->
                        val current = selectedCategories.value
                        selectedCategories.value = if (current.contains(category)) current - category else current + category
                    },
                    onGoalSelected = { goal -> selectedGoal.value = goal },


                    onSubmit = {
                        onboardingViewModel.submitFinalPreferences(
                            dietary = selectedDiets.value.toList(),
                            categories = selectedCategories.value.toList(),
                            goal = selectedGoal.value ?: ""
                        )
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Dest.Profile> {

                ProfileScreen(
                    onNavigateToChangePassword = { navController.navigate(Dest.ChangePassword) },
                    onNavigateBack = { navController.popBackStack() },
                    onEditPreferences = { navController.navigate(Dest.Onboarding) }
                )
            }

            composable<Dest.ChangePassword> {
                ChangePasswordScreen(
                    viewModel = hiltViewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Dest.Cart> { CartScreen() }
            composable<Dest.Pantry> {
                PantryScreen(
                    onProductClick = { id -> navController.navigate(Dest.ProductDetail(id)) },
                    onReviewDigest = { navController.navigate(Dest.AdjustmentDigest) },
                )
            }
            composable<Dest.ProductDetail> { entry ->
                ProductDetailScreen(
                    productId = entry.toRoute<Dest.ProductDetail>().productId,
                    onTitleChange = navigationViewModel::setScreenTitle,
                )
            }
            composable<Dest.AdjustmentDigest> {
                AdjustmentDigestScreen(onTitleChange = navigationViewModel::setScreenTitle)
            }
            composable<Dest.History> {
                HistoryScreen(
                    onTripClick = { id -> navController.navigate(Dest.PurchaseTripDetail(id)) },
                )
            }
            composable<Dest.PurchaseTripDetail> { entry ->
                PurchaseTripDetailScreen(
                    purchaseId = entry.toRoute<Dest.PurchaseTripDetail>().purchaseId,
                    onTitleChange = navigationViewModel::setScreenTitle,
                    onProductClick = { id -> navController.navigate(Dest.ProductDetail(id)) },
                )
            }
            composable<Dest.Meals> {
                val mealsViewModel: MealsViewModel = hiltViewModel()
                MealsScreen(
                    viewModel = mealsViewModel,
                    onNavigateToDetails = { mealId: String ->
                        navController.navigate(
                            route = Dest.MealDetails(
                                mealId = mealId
                            )
                        )
                    }
                )
            }

            composable<Dest.MealDetails> { entry ->
                val args = entry.toRoute<Dest.MealDetails>()
                com.fullsail.shoppingmadebetter.feature.meals.ui.MealDetailsScreen(
                    mealId = args.mealId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Dest.Stores> { StoresScreen() }
        }
    }
    }
}

/**
 * The app's navigation drawer, opened from the top-bar menu on tab screens.
 * [onProfile] opens Profile & Settings; [onLogout] at the bottom signs the user out.
 */
@Composable
private fun AppDrawer(onProfile: () -> Unit, onLogout: () -> Unit) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.menu_profile)) },
                selected = false,
                onClick = onProfile,
            )
            // The weight pushes logout to the bottom.
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.menu_logout))
            }
        }
    }
}

/**
 * The cold-start gate shown while the cached Supabase session is being restored.
 * Once [NavigationViewModel] resolves the session it routes away from here, so this
 * is only ever visible for the brief initializing window. Mirrors the login screen's
 * green backdrop and logo for a seamless hand-off into either destination.
 */
@Composable
private fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = stringResource(R.string.auth_logo_desc),
            modifier = Modifier.size(200.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        CircularProgressIndicator()
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
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
