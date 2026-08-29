package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.LabelChip
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseLineItem
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTrip
import com.fullsail.shoppingmadebetter.feature.history.domain.PurchaseTripSummary
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlinx.datetime.LocalDate

/**
 * One completed trip as a summary card: when, where, what it cost, and how many
 * items. Tapping it opens the trip's detail screen.
 * @param onClick opens this trip's details.
 */
@Composable
fun PurchaseTripCard(
    trip: PurchaseTripSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Card's onClick overload takes no click label, so name the action via semantics —
    // otherwise a screen reader offers a bare "double tap to activate".
    val openLabel = stringResource(R.string.history_trip_open)
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { onClick(label = openLabel, action = null) },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = formatTripDate(trip.purchasedOn),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = trip.storeName ?: stringResource(R.string.history_unknown_store),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatPrice(trip.total),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
                // Decorative: the card announces itself through its click label.
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(CHEVRON_RIGHT_ROTATION),
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LabelChip(
                    label = pluralStringResource(
                        R.plurals.history_trip_item_count,
                        trip.itemCount,
                        trip.itemCount,
                    ),
                    accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconRes = R.drawable.ic_shopping_cart,
                )
            }
        }
    }
}

private const val CHEVRON_RIGHT_ROTATION = -90f

/** A list card's trip; the detail screen's [previewTrip] carries items instead. */
internal fun previewTripSummary(
    id: String = "trip-1",
    storeName: String? = "ALDI",
    recordedTotal: Double? = 42.32,
    lineTotal: Double = 42.32,
    itemCount: Int = 4,
) = PurchaseTripSummary(
    id = id,
    purchasedOn = LocalDate(2026, 8, 19),
    purchasedAtEpoch = 1_787_109_229L,
    storeName = storeName,
    recordedTotal = recordedTotal,
    lineTotal = lineTotal,
    itemCount = itemCount,
)

internal fun previewLineItem(
    id: String,
    productName: String = "Havarti Cheese Slices",
    quantity: Double = 2.0,
    pricePaid: Double = 5.22,
    addedToInventory: Boolean = false,
) = PurchaseLineItem(
    id = id,
    productId = "p-$id",
    productName = productName,
    brand = "Cracker Barrel",
    size = "220 g",
    imageUrl = "",
    quantity = quantity,
    pricePaid = pricePaid,
    addedToInventory = addedToInventory,
)

internal fun previewTrip(
    id: String = "trip-1",
    storeName: String? = "ALDI",
    recordedTotal: Double? = 42.32,
    items: List<PurchaseLineItem> = listOf(
        previewLineItem("1"),
        // Two of the four marked, so previews show both states of the pantry chip.
        previewLineItem(
            "2",
            productName = "100% Whole Grains Minute Oats",
            pricePaid = 4.08,
            addedToInventory = true,
        ),
        previewLineItem(
            "3",
            productName = "Less Sodium Soy Sauce",
            pricePaid = 4.27,
            addedToInventory = true,
        ),
        previewLineItem("4", productName = "White Stilton With Mango & Ginger", pricePaid = 7.59),
    ),
) = PurchaseTrip(
    id = id,
    purchasedOn = LocalDate(2026, 8, 19),
    purchasedAtEpoch = 1_787_109_229L,
    storeName = storeName,
    recordedTotal = recordedTotal,
    items = items,
)

@Preview(showBackground = true, name = "Trip card")
@Composable
private fun PurchaseTripCardPreview() {
    ShoppingMadeBetterTheme {
        PurchaseTripCard(
            trip = previewTripSummary(),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Deleted store, total from lines")
@Composable
private fun PurchaseTripCardUnknownStorePreview() {
    ShoppingMadeBetterTheme {
        PurchaseTripCard(
            trip = previewTripSummary(
                storeName = null,
                recordedTotal = null,
                lineTotal = 7.83,
                itemCount = 1,
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
