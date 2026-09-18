package com.fullsail.shoppingmadebetter.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.shoppinglists.domain.shoppingTrip.ShoppingTrip
import com.fullsail.shoppingmadebetter.feature.stores.domain.Store

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
 *
 * "New list…" is offered once the lists have settled, including when there are none
 * and when the load failed, so the sheet is never a dead end. A new list needs a store
 * because the list-of-lists view inner-joins stores and would hide a storeless one;
 * [defaultStoreId] preselects one when the caller knows which store fits (a past
 * trip's), and [onCreateList] fires with the trimmed name and the chosen store.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToShoppingListSheet(
    title: String,
    lists: ShoppingListPickerState,
    stores: List<Store>,
    defaultStoreId: String?,
    onDismiss: () -> Unit,
    onListChosen: (ShoppingTrip) -> Unit,
    onCreateList: (name: String, storeId: String) -> Unit,
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
            if (lists != ShoppingListPickerState.Loading) {
                CreateListSection(
                    stores = stores,
                    defaultStoreId = defaultStoreId,
                    showDivider = lists is ShoppingListPickerState.Loaded,
                    onCreateList = onCreateList,
                )
            }
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

/**
 * One pickable list: its name and how many items are already on it. A list a housemate
 * created also carries the owner chip, since anyone in the household can add to it.
 */
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = trip.listName, style = MaterialTheme.typography.bodyLarge)
            if (!trip.isOwn) {
                OwnerChip(
                    text = stringResource(
                        R.string.list_created_by,
                        trip.createdBy ?: stringResource(R.string.owner_chip_household),
                    ),
                )
            }
        }
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

/**
 * The "New list…" row and, once tapped, the name field, the store dropdown and Create.
 * It stays collapsed until asked for so the sheet still reads as a list of lists.
 * [showDivider] separates it from the rows above when there are any. A failed store
 * load leaves Create disabled with a note rather than hiding the form, so the rest of
 * the sheet still works.
 */
@Composable
private fun CreateListSection(
    stores: List<Store>,
    defaultStoreId: String?,
    showDivider: Boolean,
    onCreateList: (name: String, storeId: String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    // Re-keyed on the stores because they arrive after the sheet opens; until then
    // there is nothing to preselect.
    var storeId by rememberSaveable(defaultStoreId, stores) {
        mutableStateOf(
            stores.firstOrNull { it.id == defaultStoreId }?.id ?: stores.firstOrNull()?.id,
        )
    }

    if (showDivider) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    Text(
        text = stringResource(R.string.add_to_list_create),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 12.dp),
    )

    if (!expanded) return

    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(R.string.add_to_list_name_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (stores.isEmpty()) {
        Text(
            text = stringResource(R.string.add_to_list_store_error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    } else {
        StorePicker(
            stores = stores,
            selectedId = storeId,
            onSelect = { storeId = it },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    Button(
        onClick = { storeId?.let { onCreateList(name.trim(), it) } },
        enabled = name.isNotBlank() && storeId != null,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(text = stringResource(R.string.add_to_list_create_action))
    }
}

/** Read-only field over the store list; [selectedId] is the one currently chosen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorePicker(
    stores: List<Store>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = stores.firstOrNull { it.id == selectedId }?.name.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.add_to_list_store)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            stores.forEach { store ->
                DropdownMenuItem(
                    text = { Text(store.name) },
                    onClick = {
                        expanded = false
                        onSelect(store.id)
                    },
                )
            }
        }
    }
}
