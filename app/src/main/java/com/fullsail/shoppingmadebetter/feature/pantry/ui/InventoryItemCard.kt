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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.fullsail.shoppingmadebetter.ui.theme.expiryUrgentAccentDark
import com.fullsail.shoppingmadebetter.ui.theme.expiryUrgentAccentLight

/**
 * Severity of an item's remaining shelf life, used to color the expiry chip.
 * See [expiryBucket] for the day thresholds each bucket covers.
 */
enum class ExpiryBucket { Expired, Urgent, Soon }

/**
 * Maps days-until-expiry to a chip severity, or `null` when no expiry chip
 * should show. Mirrors [EXPIRING_SOON_DAYS]
 *
 * - `<= 0`     -> [ExpiryBucket.Expired] (red)  — already past its date or due today
 * - `1..2`     -> [ExpiryBucket.Urgent] (orange)
 * - `3..5`     -> [ExpiryBucket.Soon] (yellow)
 * - `6+` / `null` -> `null` — no chip (plenty of time, or no known date)
 */
internal fun expiryBucket(expiresInDays: Int?): ExpiryBucket? = when {
    expiresInDays == null -> null
    expiresInDays <= 0 -> ExpiryBucket.Expired
    expiresInDays <= 2 -> ExpiryBucket.Urgent
    expiresInDays <= EXPIRING_SOON_DAYS -> ExpiryBucket.Soon
    else -> null
}

/**
 * A single pantry inventory item rendered as a card.
 * @param onClick opens the item's detail screen.
 * @param onAddToList opens the "add to shopping list" flow for this item.
 * @param onRemove requests removal of this item from the pantry.
 * @param onQuantityChange requests persisting a new on-hand quantity for this item.
 */
@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onClick: () -> Unit,
    onAddToList: () -> Unit,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit,
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
    modifier: Modifier = Modifier,
) {
    val bucket = expiryBucket(item.expiresInDays)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuantityChip(quantity = item.quantity, onQuantityChange = onQuantityChange)
        LocationChip(location = item.location)
        if (bucket != null) {
            ExpiryChip(bucket = bucket, expiresInDays = item.expiresInDays!!)
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

@Composable
private fun LocationChip(location: PantryLocation, modifier: Modifier = Modifier) {
    val label = stringResource(location.labelRes())
    LabelChip(
        label = label,
        accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        iconRes = location.iconRes(),
        contentDescription = stringResource(R.string.pantry_card_location_desc, label),
        modifier = modifier,
    )
}

/**
 * The quantity chip. Tapping it opens an anchored popup with a [Stepper]; the new
 * value is committed via [onQuantityChange] when the popup closes (nothing is sent
 * when the value is unchanged). Quantity is floored at 1 — removing the item is the
 * job of the card's separate remove action.
 */
@Composable
private fun QuantityChip(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var draft by remember { mutableIntStateOf(quantity) }

    Box(modifier = modifier) {
        LabelChip(
            label = stringResource(R.string.pantry_card_quantity, quantity),
            accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Quantity can't be lowered below this from the chip; use the remove action instead. */
private const val MIN_QUANTITY = 1

@Composable
private fun ExpiryChip(bucket: ExpiryBucket, expiresInDays: Int, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val accent: Color = when (bucket) {
        ExpiryBucket.Expired -> MaterialTheme.colorScheme.error
        ExpiryBucket.Urgent -> if (dark) expiryUrgentAccentDark else expiryUrgentAccentLight
        ExpiryBucket.Soon -> if (dark) expirySoonAccentDark else expirySoonAccentLight
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

    LabelChip(
        label = label,
        accentColor = accent,
        iconRes = R.drawable.ic_expiring,
        contentDescription = description,
        modifier = modifier,
    )
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
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "No chip")
@Composable
private fun InventoryItemCardNoChipPreview() {
    ShoppingMadeBetterTheme {
        InventoryItemCard(
            item = previewItem(expiresInDays = null),
            onClick = {},
            onAddToList = {},
            onRemove = {},
            onQuantityChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
