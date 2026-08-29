package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryDatePreset
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryFilter
import com.fullsail.shoppingmadebetter.feature.history.domain.hasCustomRange
import com.fullsail.shoppingmadebetter.feature.history.domain.isActive
import com.fullsail.shoppingmadebetter.feature.history.domain.localDateFromUtcMillis
import com.fullsail.shoppingmadebetter.feature.history.domain.toUtcMillis
import com.fullsail.shoppingmadebetter.feature.stores.domain.Store
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlinx.datetime.LocalDate

/**
 * The filter row above the History list: one row of store chips over one row of
 * date chips.
 *
 * Two rows rather than one so neither filter kind can push the other off-screen,
 * and so the dates still render when the store load failed.
 *
 * Stateless but for the picker dialog's visibility — the selection and every
 * toggle are hoisted, so the row renders the same in a preview as under the
 * ViewModel.
 *
 * Chips rather than the pantry's 96.dp dashboard cards: those carry a count and
 * double as a summary of the pantry, while these only pick a value. Stores are
 * multi-select, and picking a second widens the list to both — a trip happened at
 * one store, so OR is the only join that means anything there. Dates are
 * single-select for the mirror-image reason: a trip falls in one range.
 *
 * @param stores the stores to offer, in the order they should appear. Empty hides
 *   the store row, which is what a failed store load falls back to.
 * @param filter the active filter, read for the store selection and the dates.
 * @param searchInput the search field's text. Deliberately not read off [filter]:
 *   that one lags a keystroke by the debounce, and binding the field to it would
 *   make typing stutter.
 * @param onSearchChange invoked on every keystroke, and with "" by the clear icon.
 * @param selectedPreset which date chip reads as selected; null when the range was
 *   picked by hand or there is none.
 * @param onToggleStore invoked with a store's id when its chip is tapped.
 * @param onSelectPreset invoked when a date preset chip is tapped, including the
 *   already-selected one — clearing is the caller's rule, not this row's.
 * @param onCustomRange invoked with the range confirmed in the picker.
 * @param onClearFilters invoked when the Clear chip is tapped.
 */
@Composable
internal fun HistoryFilterRow(
    stores: List<Store>,
    filter: HistoryFilter,
    selectedPreset: HistoryDatePreset?,
    searchInput: String,
    onSearchChange: (String) -> Unit,
    onToggleStore: (String) -> Unit,
    onSelectPreset: (HistoryDatePreset) -> Unit,
    onCustomRange: (LocalDate, LocalDate) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRangePicker by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = searchInput,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text(text = stringResource(R.string.history_search_label)) },
            singleLine = true,
            trailingIcon = {
                // Only worth offering once there is something to clear.
                if (searchInput.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.history_search_clear),
                        )
                    }
                }
            },
        )

        if (stores.isNotEmpty()) {
            ChipRow {
                items(stores, key = { it.id }) { store ->
                    val isSelected = store.id in filter.storeIds
                    FilterChoiceChip(
                        label = store.name,
                        description = stringResource(
                            if (isSelected) {
                                R.string.history_filter_store_selected_desc
                            } else {
                                R.string.history_filter_store_desc
                            },
                            store.name,
                        ),
                        isSelected = isSelected,
                        onClick = { onToggleStore(store.id) },
                    )
                }
            }
        }

        ChipRow {
            items(HistoryDatePreset.entries, key = { it.name }) { preset ->
                DateChip(
                    label = stringResource(preset.labelRes()),
                    isSelected = preset == selectedPreset,
                    onClick = { onSelectPreset(preset) },
                )
            }

            item(key = CUSTOM_CHIP_KEY) {
                // Only a hand-picked range belongs to this chip. A preset's dates
                // are shown by the preset's own chip, and repeating them here would
                // read as two ranges being filtered on at once.
                val isCustom = filter.hasCustomRange(selectedPreset)
                DateChip(
                    // Reads back the range it picked once there is one: no preset
                    // chip matches a hand-picked range, so this is the only place
                    // those dates are visible.
                    label = customRangeLabel(filter.takeIf { isCustom }),
                    isSelected = isCustom,
                    onClick = { showRangePicker = true },
                )
            }

            if (filter.isActive) {
                item(key = CLEAR_CHIP_KEY) {
                    AssistChip(
                        onClick = onClearFilters,
                        label = { Text(text = stringResource(R.string.history_filter_clear)) },
                    )
                }
            }
        }
    }

    if (showRangePicker) {
        HistoryDateRangeDialog(
            from = filter.from,
            to = filter.to,
            onDismiss = { showRangePicker = false },
            onConfirm = { from, to ->
                showRangePicker = false
                onCustomRange(from, to)
            },
        )
    }
}

/** The custom chip's label: the hand-picked range, or the invitation to pick one. */
@Composable
private fun customRangeLabel(filter: HistoryFilter?): String {
    val from = filter?.from
    val to = filter?.to
    return if (from != null && to != null) {
        stringResource(
            R.string.history_filter_date_range,
            formatFilterDate(from),
            formatFilterDate(to),
        )
    } else {
        stringResource(R.string.history_filter_date_custom)
    }
}

/**
 * The M3 range picker in a dialog; Apply stays off until both ends are picked.
 *
 * The title and headline are ours rather than [DateRangePickerDefaults]': that
 * title carries no top padding, so it sits against the dialog's edge, and that
 * headline is a row of three texts that wraps the end date onto its own lines once
 * two full dates have to share the width.
 */
@Composable
private fun HistoryDateRangeDialog(
    from: LocalDate?,
    to: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    // The picker works in UTC start-of-day millis, so the range goes in and comes
    // back out through the converters rather than any local time zone.
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = from?.toUtcMillis(),
        initialSelectedEndDateMillis = to?.toUtcMillis(),
    )
    val start = state.selectedStartDateMillis
    val end = state.selectedEndDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (start != null && end != null) {
                        onConfirm(localDateFromUtcMillis(start), localDateFromUtcMillis(end))
                    }
                },
                // A half-picked range has nothing to send.
                enabled = start != null && end != null,
            ) {
                Text(text = stringResource(R.string.history_filter_date_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.history_filter_date_cancel))
            }
        },
    ) {
        val colors = DatePickerDefaults.colors()
        DateRangePicker(
            state = state,
            title = {
                Text(
                    text = stringResource(
                        if (state.displayMode == DisplayMode.Input) {
                            R.string.history_filter_date_title_input
                        } else {
                            R.string.history_filter_date_title
                        },
                    ),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp),
                    color = colors.titleContentColor,
                    style = MaterialTheme.typography.labelLarge,
                )
            },
            headline = {
                Text(
                    text = rangeHeadline(state.selectedStartDateMillis, state.selectedEndDateMillis),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                    color = colors.headlineContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Shrinks to fit rather than wrapping — two long dates side by
                    // side are what broke the default headline onto three lines.
                    autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 24.sp),
                )
            },
        )
    }
}

/** The picker's headline: the range so far, with a placeholder for each end not yet picked. */
@Composable
private fun rangeHeadline(startMillis: Long?, endMillis: Long?): String {
    val start = startMillis?.let { formatTripDate(localDateFromUtcMillis(it)) }
        ?: stringResource(R.string.history_filter_date_start)
    val end = endMillis?.let { formatTripDate(localDateFromUtcMillis(it)) }
        ?: stringResource(R.string.history_filter_date_end)
    return stringResource(R.string.history_filter_date_range, start, end)
}

/** One horizontally scrolling row of filter chips. */
@Composable
private fun ChipRow(content: LazyListScope.() -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** A date chip, which names itself in its own description. */
@Composable
private fun DateChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChoiceChip(
        label = label,
        description = stringResource(
            if (isSelected) {
                R.string.history_filter_date_selected_desc
            } else {
                R.string.history_filter_date_desc
            },
            label,
        ),
        isSelected = isSelected,
        onClick = onClick,
    )
}

/**
 * A selectable filter chip.
 *
 * The chip's own label already reads its name, so replacing the node's semantics
 * says "ALDI, filter by store" once rather than the name twice.
 */
@Composable
private fun FilterChoiceChip(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text = label) },
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = description
            selected = isSelected
        },
    )
}

/** The chip label for each preset. */
@StringRes
private fun HistoryDatePreset.labelRes(): Int = when (this) {
    HistoryDatePreset.Last30Days -> R.string.history_filter_date_30d
    HistoryDatePreset.Last3Months -> R.string.history_filter_date_3mo
    HistoryDatePreset.ThisYear -> R.string.history_filter_date_year
}

private const val CUSTOM_CHIP_KEY = "history-filter-custom-range"
private const val CLEAR_CHIP_KEY = "history-filter-clear"

/** Stores for previews across the History screens; the tab only ever sees a few. */
internal val previewStores = listOf(
    Store("s-1", "Whole Foods", "1 Main St", "Orlando", "FL", "32801", null),
    Store("s-2", "ALDI", "2 Oak Ave", "Orlando", "FL", "32802", null),
    Store("s-3", "Publix", "3 Pine Rd", "Orlando", "FL", "32803", null),
)

@Composable
private fun HistoryFilterRowPreviewHost(
    filter: HistoryFilter = HistoryFilter(),
    selectedPreset: HistoryDatePreset? = null,
    searchInput: String = "",
) {
    ShoppingMadeBetterTheme {
        HistoryFilterRow(
            stores = previewStores,
            filter = filter,
            selectedPreset = selectedPreset,
            searchInput = searchInput,
            onSearchChange = {},
            onToggleStore = {},
            onSelectPreset = {},
            onCustomRange = { _, _ -> },
            onClearFilters = {},
        )
    }
}

@Preview(showBackground = true, name = "No filter")
@Composable
private fun HistoryFilterRowPreview() {
    HistoryFilterRowPreviewHost()
}

@Preview(showBackground = true, name = "Two stores selected")
@Composable
private fun HistoryFilterRowSelectedPreview() {
    HistoryFilterRowPreviewHost(HistoryFilter(storeIds = setOf("s-2", "s-3")))
}

@Preview(showBackground = true, name = "Date preset selected")
@Composable
private fun HistoryFilterRowPresetPreview() {
    HistoryFilterRowPreviewHost(
        filter = HistoryFilter(from = LocalDate(2026, 7, 30), to = LocalDate(2026, 8, 28)),
        selectedPreset = HistoryDatePreset.Last30Days,
    )
}

@Preview(showBackground = true, name = "Custom range")
@Composable
private fun HistoryFilterRowCustomRangePreview() {
    HistoryFilterRowPreviewHost(
        HistoryFilter(from = LocalDate(2026, 8, 19), to = LocalDate(2026, 8, 28)),
    )
}

@Preview(showBackground = true, name = "Searching")
@Composable
private fun HistoryFilterRowSearchPreview() {
    HistoryFilterRowPreviewHost(
        filter = HistoryFilter(search = "oats"),
        searchInput = "oats",
    )
}
