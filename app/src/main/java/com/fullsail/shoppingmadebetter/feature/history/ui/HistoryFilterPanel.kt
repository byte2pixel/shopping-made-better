package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryDatePreset
import com.fullsail.shoppingmadebetter.feature.history.domain.HistoryFilter
import com.fullsail.shoppingmadebetter.feature.history.domain.activeCount
import com.fullsail.shoppingmadebetter.feature.history.domain.hasCustomRange
import com.fullsail.shoppingmadebetter.feature.history.domain.isActive
import com.fullsail.shoppingmadebetter.feature.history.domain.localDateFromUtcMillis
import com.fullsail.shoppingmadebetter.feature.history.domain.searchTerm
import com.fullsail.shoppingmadebetter.feature.history.domain.toUtcMillis
import com.fullsail.shoppingmadebetter.feature.stores.domain.Store
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlinx.datetime.LocalDate

/**
 * The collapsible filter panel above the History list: a header that always shows,
 * over a body of search field + store chips + date chips that only shows when
 * expanded.
 *
 * Collapsed is the default because the common visit is a glance at recent trips,
 * and the expanded controls cost about a third of the screen. Nothing is hidden by
 * collapsing: the header carries a count badge and names the active filters.
 *
 * Stateless but for the picker dialog's visibility — expansion is hoisted, so the
 * panel renders the same in a preview as under the ViewModel.
 *
 * Stores are multi-select and OR-join; dates are single-select, since a trip falls
 * in one range.
 *
 * @param stores the stores to offer. Empty hides the store row, which is what a
 *   failed store load falls back to.
 * @param filter the active filter, read for the store selection, dates and badge.
 * @param searchInput the search field's text. Not read off [filter], which lags a
 *   keystroke by the debounce.
 * @param expanded whether the body shows.
 * @param onExpandedChange invoked with the state the header was tapped towards.
 * @param onSearchChange invoked on every keystroke, and with "" by the clear icon.
 * @param selectedPreset which date chip reads as selected; null for a hand-picked
 *   range or none.
 * @param onToggleStore invoked with a store's id when its chip is tapped.
 * @param onSelectPreset invoked when a date preset chip is tapped, including the
 *   already-selected one — clearing is the caller's rule.
 * @param onCustomRange invoked with the range confirmed in the picker.
 * @param onClearFilters invoked when the Clear chip is tapped.
 */
@Composable
internal fun HistoryFilterPanel(
    stores: List<Store>,
    filter: HistoryFilter,
    selectedPreset: HistoryDatePreset?,
    searchInput: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleStore: (String) -> Unit,
    onSelectPreset: (HistoryDatePreset) -> Unit,
    onCustomRange: (LocalDate, LocalDate) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRangePicker by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        FilterPanelHeader(
            activeCount = filter.activeCount,
            // Only worth the line when collapsed; expanded, the chips say it better.
            summary = if (expanded) null else filterSummary(filter, stores, selectedPreset),
            expanded = expanded,
            onToggle = { onExpandedChange(!expanded) },
        )

        AnimatedVisibility(visible = expanded) {
            Column {
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
                        // Only a hand-picked range belongs to this chip. A preset's
                        // dates are shown by the preset's own chip.
                        val isCustom = filter.hasCustomRange(selectedPreset)
                        DateChip(
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

/**
 * The always-visible header: what the panel is, how much it is filtering, and a
 * chevron that turns over as it opens. The whole block is one tap target, so the
 * summary line expands the panel too.
 */
@Composable
private fun FilterPanelHeader(
    activeCount: Int,
    summary: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val toggleLabel = stringResource(
        if (expanded) R.string.history_filters_collapse else R.string.history_filters_expand,
    )
    val state = stringResource(
        if (expanded) R.string.history_filters_expanded else R.string.history_filters_collapsed,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = toggleLabel, role = Role.Button, onClick = onToggle)
            .semantics { stateDescription = state }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_filter_list),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.history_filters_title),
                style = MaterialTheme.typography.titleSmall,
            )
            if (activeCount > 0) {
                val countDescription = pluralStringResource(
                    R.plurals.history_filters_active,
                    activeCount,
                    activeCount,
                )
                // Replaces the bare number a screen reader would otherwise announce.
                Badge(modifier = Modifier.clearAndSetSemantics {
                    contentDescription = countDescription
                }) {
                    Text(text = activeCount.toString())
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) CHEVRON_UP_ROTATION else 0f),
            )
        }

        if (summary != null) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The active filters named in one line, or null when nothing is filtered. A store
 * whose name did not load is left unnamed rather than guessed at, so this can name
 * fewer things than the badge counts.
 */
@Composable
private fun filterSummary(
    filter: HistoryFilter,
    stores: List<Store>,
    selectedPreset: HistoryDatePreset?,
): String? {
    val from = filter.from
    val to = filter.to
    val parts = buildList {
        // Chip order, not set order, so the line reads like the row it replaces.
        stores.filter { it.id in filter.storeIds }.forEach { add(it.name) }
        when {
            selectedPreset != null -> add(stringResource(selectedPreset.labelRes()))
            from != null && to != null -> add(
                stringResource(
                    R.string.history_filter_date_range,
                    formatFilterDate(from),
                    formatFilterDate(to),
                ),
            )
            // Unreachable from the chips, which always set both, but a saved
            // one-sided range must not go unnamed.
            from != null -> add(stringResource(R.string.history_filter_date_from, formatFilterDate(from)))
            to != null -> add(stringResource(R.string.history_filter_date_to, formatFilterDate(to)))
        }
        // The committed term, not the in-flight one: this names what is filtering.
        filter.searchTerm()?.let {
            add(stringResource(R.string.history_filter_summary_search, filter.search.trim()))
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(SUMMARY_SEPARATOR)
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
 * The title and headline are ours rather than [DateRangePickerDefaults]': that title
 * carries no top padding, and that headline wraps the end date onto its own lines
 * once two full dates share the width.
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
 * A selectable filter chip. Replaces the node's semantics so it announces "ALDI,
 * filter by store" once rather than the name twice.
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

/** Points the chevron up once the panel is open. */
private const val CHEVRON_UP_ROTATION = 180f

/** Joins the named filters on the collapsed summary line. */
private const val SUMMARY_SEPARATOR = " · "

/** Stores for previews across the History screens; the tab only ever sees a few. */
internal val previewStores = listOf(
    Store("s-1", "Whole Foods", "1 Main St", "Orlando", "FL", "32801", null),
    Store("s-2", "ALDI", "2 Oak Ave", "Orlando", "FL", "32802", null),
    Store("s-3", "Publix", "3 Pine Rd", "Orlando", "FL", "32803", null),
)

@Composable
private fun HistoryFilterPanelPreviewHost(
    filter: HistoryFilter = HistoryFilter(),
    selectedPreset: HistoryDatePreset? = null,
    searchInput: String = "",
    expanded: Boolean = false,
) {
    var isExpanded by rememberSaveable { mutableStateOf(expanded) }
    ShoppingMadeBetterTheme {
        HistoryFilterPanel(
            stores = previewStores,
            filter = filter,
            selectedPreset = selectedPreset,
            searchInput = searchInput,
            expanded = isExpanded,
            onExpandedChange = { isExpanded = it },
            onSearchChange = {},
            onToggleStore = {},
            onSelectPreset = {},
            onCustomRange = { _, _ -> },
            onClearFilters = {},
        )
    }
}

@Preview(showBackground = true, name = "Collapsed, nothing filtered")
@Composable
private fun HistoryFilterPanelCollapsedPreview() {
    HistoryFilterPanelPreviewHost()
}

@Preview(showBackground = true, name = "Collapsed, filters active")
@Composable
private fun HistoryFilterPanelCollapsedActivePreview() {
    HistoryFilterPanelPreviewHost(
        filter = HistoryFilter(
            storeIds = setOf("s-2", "s-3"),
            from = LocalDate(2026, 7, 30),
            to = LocalDate(2026, 8, 28),
            search = "oats",
        ),
        selectedPreset = HistoryDatePreset.Last30Days,
        searchInput = "oats",
    )
}

@Preview(showBackground = true, name = "Collapsed, summary overflows")
@Composable
private fun HistoryFilterPanelCollapsedOverflowPreview() {
    HistoryFilterPanelPreviewHost(
        filter = HistoryFilter(
            storeIds = setOf("s-1", "s-2", "s-3"),
            from = LocalDate(2026, 8, 19),
            to = LocalDate(2026, 8, 28),
            search = "whole grains",
        ),
        searchInput = "whole grains",
    )
}

@Preview(showBackground = true, name = "Expanded, no filter")
@Composable
private fun HistoryFilterPanelExpandedPreview() {
    HistoryFilterPanelPreviewHost(expanded = true)
}

@Preview(showBackground = true, name = "Expanded, two stores selected")
@Composable
private fun HistoryFilterPanelExpandedSelectedPreview() {
    HistoryFilterPanelPreviewHost(
        filter = HistoryFilter(storeIds = setOf("s-2", "s-3")),
        expanded = true,
    )
}

@Preview(showBackground = true, name = "Expanded, date preset selected")
@Composable
private fun HistoryFilterPanelExpandedPresetPreview() {
    HistoryFilterPanelPreviewHost(
        filter = HistoryFilter(from = LocalDate(2026, 7, 30), to = LocalDate(2026, 8, 28)),
        selectedPreset = HistoryDatePreset.Last30Days,
        expanded = true,
    )
}

@Preview(showBackground = true, name = "Expanded, custom range")
@Composable
private fun HistoryFilterPanelExpandedCustomRangePreview() {
    HistoryFilterPanelPreviewHost(
        filter = HistoryFilter(from = LocalDate(2026, 8, 19), to = LocalDate(2026, 8, 28)),
        expanded = true,
    )
}

@Preview(showBackground = true, name = "Expanded, searching")
@Composable
private fun HistoryFilterPanelExpandedSearchPreview() {
    HistoryFilterPanelPreviewHost(
        filter = HistoryFilter(search = "oats"),
        searchInput = "oats",
        expanded = true,
    )
}
