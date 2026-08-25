package com.fullsail.shoppingmadebetter.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip

/**
 * The user's shopping lists as loaded for a picker. Shared by every screen that
 * offers "add to shopping list" — the pantry (one item) and history (a past trip's
 * items) — so they show the same loading, empty and error states.
 */
sealed interface ShoppingListPickerState {
    data object Loading : ShoppingListPickerState
    data class Loaded(val trips: List<ShoppingTrip>) : ShoppingListPickerState
    data object Empty : ShoppingListPickerState
    data object Error : ShoppingListPickerState
}

/**
 * Slide-up picker for choosing which shopping list to add to. [title] names what is
 * being added ("Add "2% Milk" to…"), [lists] supplies the rows, and [onListChosen]
 * fires with the tapped list. The caller owns the outcome — this sheet only picks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToShoppingListSheet(
    title: String,
    lists: ShoppingListPickerState,
    onDismiss: () -> Unit,
    onListChosen: (ShoppingTrip) -> Unit,
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            ShoppingListPickerBody(lists = lists, onListChosen = onListChosen)
        }
    }
}

@Composable
private fun ColumnScope.ShoppingListPickerBody(
    lists: ShoppingListPickerState,
    onListChosen: (ShoppingTrip) -> Unit,
) {
    when (lists) {
        ShoppingListPickerState.Loading -> CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
        )

        ShoppingListPickerState.Empty -> Text(
            text = stringResource(R.string.add_to_list_empty),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        ShoppingListPickerState.Error -> Text(
            text = stringResource(R.string.add_to_list_error),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        is ShoppingListPickerState.Loaded -> lists.trips.forEach { trip ->
            ShoppingListRow(trip = trip, onClick = { onListChosen(trip) })
        }
    }
}

/** One pickable list: its name and how many items are already on it. */
@Composable
private fun ShoppingListRow(trip: ShoppingTrip, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = trip.listName, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = pluralStringResource(
                R.plurals.add_to_list_item_count,
                trip.itemCount,
                trip.itemCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
