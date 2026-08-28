package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.stores.domain.Store
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * The filter row above the History list: one chip per store the user can narrow to.
 *
 * Stateless, like the pantry's dashboard row — the selection and the toggle are
 * both hoisted, so the row renders the same in a preview as under the ViewModel.
 *
 * Chips rather than the pantry's 96.dp dashboard cards: those carry a count and
 * double as a summary of the pantry, while these only pick a value. Multi-select,
 * and picking a second store widens the list to both — a trip happened at one
 * store, so OR is the only join that means anything here.
 *
 * @param stores the stores to offer, in the order they should appear. Empty renders
 *   nothing at all, which is what a failed store load falls back to.
 * @param selectedStoreIds ids of the currently active chips.
 * @param onToggleStore invoked with a store's id when its chip is tapped.
 */
@Composable
internal fun HistoryFilterRow(
    stores: List<Store>,
    selectedStoreIds: Set<String>,
    onToggleStore: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (stores.isEmpty()) return

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(stores, key = { it.id }) { store ->
            val isSelected = store.id in selectedStoreIds
            val description = stringResource(
                if (isSelected) {
                    R.string.history_filter_store_selected_desc
                } else {
                    R.string.history_filter_store_desc
                },
                store.name,
            )

            FilterChip(
                selected = isSelected,
                onClick = { onToggleStore(store.id) },
                label = { Text(text = store.name) },
                // The chip's own label already reads the store name, so replacing the
                // node's semantics says "Whole Foods, filter by store" once rather
                // than the name twice.
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = description
                    selected = isSelected
                },
            )
        }
    }
}

/** Stores for previews across the History screens; the tab only ever sees a few. */
internal val previewStores = listOf(
    Store("s-1", "Whole Foods", "1 Main St", "Orlando", "FL", "32801", null),
    Store("s-2", "ALDI", "2 Oak Ave", "Orlando", "FL", "32802", null),
    Store("s-3", "Publix", "3 Pine Rd", "Orlando", "FL", "32803", null),
)

@Preview(showBackground = true, name = "No filter")
@Composable
private fun HistoryFilterRowPreview() {
    ShoppingMadeBetterTheme {
        HistoryFilterRow(
            stores = previewStores,
            selectedStoreIds = emptySet(),
            onToggleStore = {},
        )
    }
}

@Preview(showBackground = true, name = "Two stores selected")
@Composable
private fun HistoryFilterRowSelectedPreview() {
    ShoppingMadeBetterTheme {
        HistoryFilterRow(
            stores = previewStores,
            selectedStoreIds = setOf("s-2", "s-3"),
            onToggleStore = {},
        )
    }
}
