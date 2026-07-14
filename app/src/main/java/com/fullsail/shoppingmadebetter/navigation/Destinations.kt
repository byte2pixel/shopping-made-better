package com.fullsail.shoppingmadebetter.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.fullsail.shoppingmadebetter.R
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations for the app.
 *
 * Each destination is a [Serializable] route object consumed by the type-safe
 * Navigation Compose APIs (`composable<Dest.Cart> { }`, `navController.navigate(Dest.Cart)`).
 */
sealed interface Dest {
    @Serializable
    data object Login : Dest

    @Serializable
    data object SignUp : Dest

    @Serializable
    data object ShoppingLists : Dest
    @Serializable
    data object ShoppingListItemComparison : Dest
    {

    }
    @Serializable
    data object Cart : Dest

    @Serializable
    data object Pantry : Dest

    /**
     * A detail screen for an item in the pantry.
     * @param id The ID of the item to show.
     */
    @Serializable
    data class PantryItemDetail(val id: String) : Dest

    @Serializable
    data object History : Dest

    @Serializable
    data object Meals : Dest

    /** Dev-only example screen listing stores from Supabase (SCRUM-79). */
    @Serializable
    data object Stores : Dest
}

/**
 * The five top-level destinations shown in the bottom navigation bar, in display order.
 */
enum class TopLevelDestination(
    val route: Dest,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    SHOPPING_LISTS(Dest.ShoppingLists, R.string.tab_shopping_lists, R.drawable.ic_shopping_lists),
    CART(Dest.Cart, R.string.tab_cart, R.drawable.ic_cart),
    PANTRY(Dest.Pantry, R.string.tab_pantry, R.drawable.ic_pantry),
    HISTORY(Dest.History, R.string.tab_history, R.drawable.ic_history),
    MEALS(Dest.Meals, R.string.tab_meals, R.drawable.ic_meals),
}
