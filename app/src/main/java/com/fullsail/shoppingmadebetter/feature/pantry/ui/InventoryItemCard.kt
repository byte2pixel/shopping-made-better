package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.LabelChip
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
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
 */
@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onClick: () -> Unit,
    onAddToList: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemThumbnail()
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
            InventoryIndicators(item = item, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/**
 * The at-a-glance indicator chips shown beneath an item's details: how many are
 * on hand, and how much shelf life is left. Expandable for other info.
 */
@Composable
private fun InventoryIndicators(item: InventoryItem, modifier: Modifier = Modifier) {
    val bucket = expiryBucket(item.expiresInDays)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuantityChip(quantity = item.quantity)
        if (bucket != null) {
            ExpiryChip(bucket = bucket, expiresInDays = item.expiresInDays!!)
        }
    }
}

@Composable
private fun QuantityChip(quantity: Int, modifier: Modifier = Modifier) {
    LabelChip(
        label = stringResource(R.string.pantry_card_quantity, quantity),
        accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        iconRes = R.drawable.ic_add_circle,
        contentDescription = pluralStringResource(
            R.plurals.pantry_card_quantity_desc,
            quantity,
            quantity,
        ),
        modifier = modifier,
    )
}

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

/**
 * Leading placeholder for the item's image. Real product thumbnails are a
 * todo for now every card shows a generic pantry glyph.
 */
@Composable
private fun ItemThumbnail(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pantry),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
    }
}

private fun previewItem(expiresInDays: Int?) = InventoryItem(
    id = "1",
    productId = "p1",
    name = "2% Milk",
    brand = "Great Value",
    description = "Reduced-fat milk, one gallon.",
    size = "1 gal",
    imageUrl = "",
    quantity = 2,
    expiresInDays = expiresInDays,
)

@Preview(showBackground = true, name = "Soon (yellow)")
@Composable
private fun InventoryItemCardPreview() {
    ShoppingMadeBetterTheme {
        InventoryItemCard(
            item = previewItem(expiresInDays = 4),
            onClick = {},
            onAddToList = {},
            onRemove = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Expired")
@Composable
private fun InventoryItemCardExpiredPreview() {
    ShoppingMadeBetterTheme {
        InventoryItemCard(
            item = previewItem(expiresInDays = -2),
            onClick = {},
            onAddToList = {},
            onRemove = {},
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
            modifier = Modifier.padding(16.dp),
        )
    }
}
