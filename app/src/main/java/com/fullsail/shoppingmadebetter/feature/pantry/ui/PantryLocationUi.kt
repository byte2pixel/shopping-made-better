package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation

/** The drawable icon representing where an item is stored. */
@DrawableRes
internal fun PantryLocation.iconRes(): Int = when (this) {
    PantryLocation.Freezer -> R.drawable.ic_freezer
    PantryLocation.Fridge -> R.drawable.ic_fridge
    PantryLocation.Pantry -> R.drawable.ic_pantry
}

/** The display name for a storage location, shared with the pantry dashboard. */
@StringRes
internal fun PantryLocation.labelRes(): Int = when (this) {
    PantryLocation.Freezer -> R.string.pantry_dashboard_freezer
    PantryLocation.Fridge -> R.string.pantry_dashboard_fridge
    PantryLocation.Pantry -> R.string.pantry_dashboard_pantry
}
