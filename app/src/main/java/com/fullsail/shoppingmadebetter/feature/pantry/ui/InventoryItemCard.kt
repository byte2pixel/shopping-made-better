package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.LabelChip
import com.fullsail.shoppingmadebetter.core.ui.ProductImage
import com.fullsail.shoppingmadebetter.core.ui.Stepper
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import com.fullsail.shoppingmadebetter.ui.theme.expirySoonAccentDark
import com.fullsail.shoppingmadebetter.ui.theme.expirySoonAccentLight
import com.fullsail.shoppingmadebetter.ui.theme.warningAccentDark
import com.fullsail.shoppingmadebetter.ui.theme.warningAccentLight

/**
 * Severity of an item's remaining shelf life, used to color the expiry chip.
 * See [expiryBucket] for the day thresholds each bucket covers.
 */
enum class ExpiryBucket { Expired, Urgent, Soon, Later }

/**
 * Maps days-until-expiry to a chip severity. Mirrors [EXPIRING_SOON_DAYS]
 *
 * - `<= 0` -> [ExpiryBucket.Expired] (red)  — already past its date or due today
 * - `1..2` -> [ExpiryBucket.Urgent] (orange)
 * - `3..5` -> [ExpiryBucket.Soon] (yellow)
 * - `6+`   -> [ExpiryBucket.Later] (grey) — plenty of time, but still adjustable
 */
internal fun expiryBucket(expiresInDays: Int): ExpiryBucket = when {
    expiresInDays <= 0 -> ExpiryBucket.Expired
    expiresInDays <= 2 -> ExpiryBucket.Urgent
    expiresInDays <= EXPIRING_SOON_DAYS -> ExpiryBucket.Soon
    else -> ExpiryBucket.Later
}

/**
 * On-hand severity of an item, used to color the quantity chip and drive the
 * dashboard's "Out"/"Running Low" filters. See [stockLevel] for the thresholds.
 */
enum class StockLevel { Out, Low, Ok }

/**
 * Maps an item's on-hand [quantity] and its per-item [lowStockThreshold] to a severity.
 *
 * - `<= 0`                    -> [StockLevel.Out] (red) — none on hand
 * - `1..lowStockThreshold`    -> [StockLevel.Low] (orange) — running low
 * - otherwise / no threshold  -> [StockLevel.Ok] (grey) — plenty on hand
 *
 * Items without a [lowStockThreshold] are never [StockLevel.Low].
 */
internal fun stockLevel(quantity: Int, lowStockThreshold: Int?): StockLevel = when {
    quantity <= 0 -> StockLevel.Out
    lowStockThreshold != null && quantity <= lowStockThreshold -> StockLevel.Low
    else -> StockLevel.Ok
}

/**
 * A single pantry inventory item rendered as a card.
 * @param onClick opens the item's detail screen.
 * @param onAddToList opens the "add to shopping list" flow for this item.
 * @param onRemove requests removal of this item from the pantry.
 * @param onQuantityChange requests persisting a new on-hand quantity for this item.
 * @param onLocationChange requests persisting a new storage location for this item.
 * @param onExpiryChange requests persisting a new shelf life (days from today) for this item.
 */
@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onClick: () -> Unit,
    onAddToList: () -> Unit,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onLocationChange: (PantryLocation) -> Unit,
    onExpiryChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductImage(imageUrl = item.imageUrl, contentDescription = item.name)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = item.brand,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.size,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onAddToList) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shopping_cart),
                        contentDescription = stringResource(R.string.pantry_add_to_list),
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.pantry_remove_from_pantry),
                    )
                }
            }
            InventoryIndicators(
                item = item,
                onQuantityChange = onQuantityChange,
                onLocationChange = onLocationChange,
                onExpiryChange = onExpiryChange,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * The at-a-glance indicator chips shown beneath an item's details: how many are
 * on hand, and how much shelf life is left. Expandable for other info.
 */
@Composable
private fun InventoryIndicators(
    item: InventoryItem,
    onQuantityChange: (Int) -> Unit,
    onLocationChange: (PantryLocation) -> Unit,
    onExpiryChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuantityChip(
            quantity = item.quantity,
            lowStockThreshold = item.lowStockThreshold,
            onQuantityChange = onQuantityChange,
        )
        LocationChip(location = item.location, onLocationChange = onLocationChange)
        item.expiresInDays?.let { days ->
            ExpiryChip(
                bucket = expiryBucket(days),
                expiresInDays = days,
                onExpiryChange = onExpiryChange,
            )
        }
    }
}

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

/** The three storage locations, in the order they're offered in the location picker. */
private val locationChoices = listOf(
    PantryLocation.Pantry,
    PantryLocation.Fridge,
    PantryLocation.Freezer,
)

/**
 * The location chip. Tapping it opens an anchored popup listing the storage locations.
 * Picking one commits it via [onLocationChange] (nothing is sent when it's unchanged).
 */
@Composable
private fun LocationChip(
    location: PantryLocation,
    onLocationChange: (PantryLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = stringResource(location.labelRes())

    Box(modifier = modifier) {
        LabelChip(
            label = label,
            accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconRes = location.iconRes(),
            contentDescription = stringResource(R.string.pantry_card_location_desc, label),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
        ) {
            locationChoices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(stringResource(choice.labelRes())) },
                    leadingIcon = {
                        Icon(painter = painterResource(choice.iconRes()), contentDescription = null)
                    },
                    trailingIcon = {
                        RadioButton(selected = choice == location, onClick = null)
                    },
                    onClick = {
                        expanded = false
                        onLocationChange(choice)
                    },
                )
            }
        }
    }
}

/**
 * The quantity chip. Tapping it opens an anchored popup with a [Stepper]; the new
 * value is committed via [onQuantityChange] when the popup closes (nothing is sent
 * when the value is unchanged). Quantity floors at 0 — that's "out of stock"
 */
@Composable
private fun QuantityChip(
    quantity: Int,
    lowStockThreshold: Int?,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var draft by remember { mutableIntStateOf(quantity) }

    val dark = isSystemInDarkTheme()
    val accent: Color = when (stockLevel(quantity, lowStockThreshold)) {
        StockLevel.Out -> MaterialTheme.colorScheme.error
        StockLevel.Low -> if (dark) warningAccentDark else warningAccentLight
        StockLevel.Ok -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = modifier) {
        LabelChip(
            label = stringResource(R.string.pantry_card_quantity, quantity),
            accentColor = accent,
            iconRes = R.drawable.ic_add,
            contentDescription = pluralStringResource(
                R.plurals.pantry_card_quantity_desc,
                quantity,
                quantity,
            ),
            onClick = {
                draft = quantity
                expanded = true
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                onQuantityChange(draft)
            },
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.pantry_quantity_edit_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Stepper(
                    valueLabel = draft.toString(),
                    onDecrement = { if (draft > MIN_QUANTITY) draft-- },
                    onIncrement = { draft++ },
                    decrementContentDescription = stringResource(R.string.pantry_quantity_decrease),
                    incrementContentDescription = stringResource(R.string.pantry_quantity_increase),
                    decrementEnabled = draft > MIN_QUANTITY,
                )
            }
        }
    }
}

/** Quantity floors here from the chip: 0 means out of stock (still wanted). */
private const val MIN_QUANTITY = 0

/** Days added by each expiry quick-add button, in the order shown. */
private val expiryQuickAddDays = listOf(1, 3, 7)

/**
 * The expiry chip. Tapping it opens an anchored popup with a day [Stepper] plus quick-add
 * buttons; the new shelf life (days from today) is committed via [onExpiryChange] when the
 * popup closes (nothing is sent when unchanged). Decrement floors at "today" (0 days).
 */
@Composable
private fun ExpiryChip(
    bucket: ExpiryBucket,
    expiresInDays: Int,
    onExpiryChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val accent: Color = when (bucket) {
        ExpiryBucket.Expired -> MaterialTheme.colorScheme.error
        ExpiryBucket.Urgent -> if (dark) warningAccentDark else warningAccentLight
        ExpiryBucket.Soon -> if (dark) expirySoonAccentDark else expirySoonAccentLight
        ExpiryBucket.Later -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val label = when {
        expiresInDays < 0 -> stringResource(R.string.pantry_expiry_expired)
        expiresInDays == 0 -> stringResource(R.string.pantry_expiry_today)
        else -> stringResource(R.string.pantry_expiry_in_days_short, expiresInDays)
    }
    val description = when {
        expiresInDays < 0 -> stringResource(R.string.pantry_detail_expired)
        expiresInDays == 0 -> stringResource(R.string.pantry_detail_expires_today)
        else -> pluralStringResource(
            R.plurals.pantry_detail_expires_in_days,
            expiresInDays,
            expiresInDays,
        )
    }

    var expanded by remember { mutableStateOf(false) }
    var draft by remember { mutableIntStateOf(expiresInDays) }

    Box(modifier = modifier) {
        LabelChip(
            label = label,
            accentColor = accent,
            iconRes = R.drawable.ic_expiring,
            contentDescription = description,
            onClick = {
                draft = expiresInDays
                expanded = true
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                onExpiryChange(draft)
            },
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.pantry_expiry_edit_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Stepper(
                    valueLabel = expiryDraftLabel(draft),
                    onDecrement = { if (draft > 0) draft-- },
                    onIncrement = { draft++ },
                    decrementContentDescription = stringResource(R.string.pantry_expiry_days_decrease),
                    incrementContentDescription = stringResource(R.string.pantry_expiry_days_increase),
                    decrementEnabled = draft > 0,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    expiryQuickAddDays.forEach { days ->
                        TextButton(onClick = { draft += days }) {
                            Text(text = stringResource(R.string.pantry_expiry_add_days, days))
                        }
                    }
                }
            }
        }
    }
}

/** The stepper's value label for [days] until expiry: "Today" at 0, otherwise a day count. */
@Composable
private fun expiryDraftLabel(days: Int): String = if (days == 0) {
    stringResource(R.string.pantry_expiry_today)
} else {
    pluralStringResource(R.plurals.pantry_expiry_days, days, days)
}

private fun previewItem(
    expiresInDays: Int?,
    location: PantryLocation = PantryLocation.Pantry,
) = InventoryItem(
    id = "1",
    productId = "p1",
    name = "2% Milk",
    brand = "Great Value",
    description = "Reduced-fat milk, one gallon.",
    size = "1 gal",
    imageUrl = "",
    quantity = 2,
    expiresInDays = expiresInDays,
    location = location,
)

@Preview(showBackground = true, name = "Soon (yellow)")
@Composable
private fun InventoryItemCardPreview() {
    ShoppingMadeBetterTheme {
        InventoryItemCard(
            item = previewItem(expiresInDays = 4, location = PantryLocation.Fridge),
            onClick = {},
            onAddToList = {},
            onRemove = {},
            onQuantityChange = {},
            onLocationChange = {},
            onExpiryChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Expired")
@Composable
private fun InventoryItemCardExpiredPreview() {
    ShoppingMadeBetterTheme {
        InventoryItemCard(
            item = previewItem(expiresInDays = -2, location = PantryLocation.Freezer),
            onClick = {},
            onAddToList = {},
            onRemove = {},
            onQuantityChange = {},
            onLocationChange = {},
            onExpiryChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Later (grey)")
@Composable
private fun InventoryItemCardLaterPreview() {
    ShoppingMadeBetterTheme {
        InventoryItemCard(
            item = previewItem(expiresInDays = 30, location = PantryLocation.Pantry),
            onClick = {},
            onAddToList = {},
            onRemove = {},
            onQuantityChange = {},
            onLocationChange = {},
            onExpiryChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "No expiry date")
@Composable
private fun InventoryItemCardNoChipPreview() {
    ShoppingMadeBetterTheme {
        InventoryItemCard(
            item = previewItem(expiresInDays = null),
            onClick = {},
            onAddToList = {},
            onRemove = {},
            onQuantityChange = {},
            onLocationChange = {},
            onExpiryChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
