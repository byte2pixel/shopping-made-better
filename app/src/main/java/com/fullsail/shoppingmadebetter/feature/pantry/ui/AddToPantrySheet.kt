package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.Stepper
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.productSearch.ProductSearch

/** Highest quantity the add-to-pantry stepper will climb to. */
internal const val MAX_PANTRY_QUANTITY = 99

/** Fixed footprint for the search spinner, so the field does not resize when it appears. */
private val SEARCH_INDICATOR_SLOT = 24.dp

/**
 * Height reserved for whatever is under the search field. The catalog returns up to ten
 * rows and the sheet must not resize as that count moves, so the area is a fixed size that
 * scrolls rather than one that grows: about four rows fit and the fifth peeks, which is the
 * cue that there is more.
 */
private val SEARCH_RESULTS_HEIGHT = 200.dp

/** Handle for the reserved area, so a test can prove its height does not move. */
internal const val SEARCH_RESULTS_TAG = "addToPantryResults"

/**
 * Slide-up sheet for putting a product in the pantry by hand — the only way stock gets
 * there without a shopping trip. It searches the catalog until a product is picked, then
 * swaps to that product's quantity and location, so only one decision is on screen at a
 * time.
 *
 * Leaving the location unpicked is the normal case: the database derives it from the
 * product's category, which is right far more often than a guess from the user.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddToPantrySheet(
    state: AddToPantrySheetState.Visible,
    onQueryChange: (String) -> Unit,
    onProductSelected: (ProductSearch) -> Unit,
    onProductCleared: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onLocationChange: (PantryLocation?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.pantry_add_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            val selected = state.selected
            if (selected == null) {
                SearchPhase(
                    state = state,
                    onQueryChange = onQueryChange,
                    onProductSelected = onProductSelected,
                )
            } else {
                DetailsPhase(
                    product = selected,
                    quantity = state.quantity,
                    location = state.location,
                    onProductCleared = onProductCleared,
                    onQuantityChange = onQuantityChange,
                    onLocationChange = onLocationChange,
                    onConfirm = onConfirm,
                )
            }
        }
    }
}

/**
 * The search field and its results. The catalog search returns names only, so each row is
 * the product's title and nothing else.
 *
 * A search in flight never takes anything off screen. The spinner lives in the field's
 * trailing slot, which holds the same space whether or not it is spinning, and the rows
 * below stay as the last search left them until the next one lands. Swapping the rows for
 * a spinner instead — which is what this did first — collapsed the sheet and bounced it
 * back on every keystroke.
 */
@Composable
private fun SearchPhase(
    state: AddToPantrySheetState.Visible,
    onQueryChange: (String) -> Unit,
    onProductSelected: (ProductSearch) -> Unit,
) {
    OutlinedTextField(
        value = state.query,
        onValueChange = onQueryChange,
        label = { Text(stringResource(R.string.pantry_add_search_hint)) },
        singleLine = true,
        trailingIcon = {
            Box(
                modifier = Modifier.size(SEARCH_INDICATOR_SLOT),
                contentAlignment = Alignment.Center,
            ) {
                if (state.searching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SEARCH_RESULTS_HEIGHT)
            .testTag(SEARCH_RESULTS_TAG),
    ) {
        when {
            state.searchFailed -> Text(
                text = stringResource(R.string.pantry_add_search_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            state.results.isNotEmpty() -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.results, key = { it.productId }) { product ->
                    Text(
                        text = product.productName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProductSelected(product) }
                            .padding(vertical = 12.dp),
                    )
                }
            }

            // Only once a search has actually come back. Before that an empty area is not a
            // claim that the catalog has no matches.
            state.hasSearched -> Text(
                text = stringResource(R.string.pantry_add_no_results),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            else -> Text(
                text = stringResource(R.string.pantry_add_search_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }
}

/** The picked product, how many of it, and where it goes. */
@Composable
private fun DetailsPhase(
    product: ProductSearch,
    quantity: Int,
    location: PantryLocation?,
    onProductCleared: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onLocationChange: (PantryLocation?) -> Unit,
    onConfirm: () -> Unit,
) {
    Text(text = product.productName, style = MaterialTheme.typography.bodyLarge)
    TextButton(onClick = onProductCleared, contentPadding = PaddingValues(0.dp)) {
        Text(text = stringResource(R.string.pantry_add_change_product))
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.pantry_add_quantity),
            style = MaterialTheme.typography.bodyMedium,
        )
        Stepper(
            valueLabel = quantity.toString(),
            onDecrement = { onQuantityChange(quantity - 1) },
            onIncrement = { onQuantityChange(quantity + 1) },
            decrementContentDescription = stringResource(R.string.pantry_add_quantity_decrease),
            incrementContentDescription = stringResource(R.string.pantry_add_quantity_increase),
            decrementEnabled = quantity > 1,
            incrementEnabled = quantity < MAX_PANTRY_QUANTITY,
        )
    }

    Text(
        text = stringResource(R.string.pantry_add_location),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PantryLocation.entries.forEach { option ->
            FilterChip(
                selected = location == option,
                // Tapping the chosen chip again clears it, which is how the user gets
                // back to letting the product decide.
                onClick = { onLocationChange(if (location == option) null else option) },
                label = { Text(stringResource(option.labelRes())) },
            )
        }
    }
    if (location == null) {
        Text(
            text = stringResource(R.string.pantry_add_location_auto),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Button(onClick = onConfirm, modifier = Modifier.padding(top = 16.dp)) {
        Text(text = stringResource(R.string.pantry_add_confirm))
    }
}
