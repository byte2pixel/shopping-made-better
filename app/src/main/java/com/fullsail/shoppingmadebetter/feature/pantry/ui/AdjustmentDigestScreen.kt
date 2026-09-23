package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.LabelChip
import com.fullsail.shoppingmadebetter.core.ui.OwnerChip
import com.fullsail.shoppingmadebetter.core.ui.ProductImage
import com.fullsail.shoppingmadebetter.feature.pantry.domain.AdjustmentDigestEntry
import com.fullsail.shoppingmadebetter.feature.pantry.domain.EstimateSource

/**
 * This week's automatic adjustments. Each row offers the same three answers as the lot's
 * estimate row on the pantry card (undo, confirm, fix), plus a bulk undo.
 *
 * @param onTitleChange supplies the top-bar title; the screen has no Scaffold of its own.
 */
@Composable
fun AdjustmentDigestScreen(
    modifier: Modifier = Modifier,
    onTitleChange: (String) -> Unit = {},
    viewModel: AdjustmentDigestViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val title = stringResource(R.string.pantry_digest_title)

    LaunchedEffect(title) { onTitleChange(title) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is AdjustmentDigestEvent.UndoFailed ->
                    resources.getString(R.string.pantry_digest_undo_failed, event.productName)

                is AdjustmentDigestEvent.UpdateFailed ->
                    resources.getString(R.string.pantry_update_failed, event.productName)

                is AdjustmentDigestEvent.UndoneAll ->
                    if (event.succeeded == event.attempted) {
                        resources.getQuantityString(
                            R.plurals.pantry_digest_undone,
                            event.succeeded,
                            event.succeeded,
                        )
                    } else {
                        resources.getString(
                            R.string.pantry_digest_undone_partial,
                            event.succeeded,
                            event.attempted,
                        )
                    }
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            AdjustmentDigestUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            AdjustmentDigestUiState.Error -> CenteredMessage(
                message = stringResource(R.string.pantry_error),
                actionLabel = stringResource(R.string.pantry_retry),
                onAction = viewModel::load,
            )

            is AdjustmentDigestUiState.Success ->
                if (state.entries.isEmpty()) {
                    CenteredMessage(message = stringResource(R.string.pantry_digest_empty))
                } else {
                    DigestList(
                        state = state,
                        onUndo = viewModel::onUndo,
                        onConfirm = viewModel::onConfirm,
                        onCorrect = viewModel::onCorrect,
                        onUndoAll = viewModel::onUndoAll,
                    )
                }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun DigestList(
    state: AdjustmentDigestUiState.Success,
    onUndo: (AdjustmentDigestEntry) -> Unit,
    onConfirm: (AdjustmentDigestEntry) -> Unit,
    onCorrect: (AdjustmentDigestEntry, Int) -> Unit,
    onUndoAll: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.pantry_digest_header,
                    state.entries.size,
                    state.entries.size,
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(enabled = !state.undoingAll, onClick = onUndoAll) {
                Text(text = stringResource(R.string.pantry_digest_undo_all, state.entries.size))
            }
        }
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.entries, key = { it.adjustmentId }) { entry ->
                AdjustmentDigestRow(
                    entry = entry,
                    enabled = !state.undoingAll,
                    onUndo = { onUndo(entry) },
                    onConfirm = { onConfirm(entry) },
                    onCorrect = { onCorrect(entry, it) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun AdjustmentDigestRow(
    entry: AdjustmentDigestEntry,
    enabled: Boolean,
    onUndo: () -> Unit,
    onConfirm: () -> Unit,
    onCorrect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProductImage(
                imageUrl = entry.imageUrl,
                contentDescription = entry.productName,
                size = 40.dp,
            )
            RowDetails(entry = entry, modifier = Modifier.weight(1f))
        }
        RowActions(
            quantity = entry.quantityNow,
            enabled = enabled,
            onUndo = onUndo,
            onConfirm = onConfirm,
            onCorrect = onCorrect,
        )
    }
}

@Composable
private fun RowDetails(entry: AdjustmentDigestEntry, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = entry.productName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            StockChip(entry = entry)
            if (!entry.isOwn) {
                OwnerChip(
                    text = stringResource(
                        R.string.pantry_lot_added_by,
                        entry.lotOwner ?: stringResource(R.string.owner_chip_household),
                    ),
                )
            }
        }
        Text(
            text = stringResource(
                R.string.pantry_digest_change,
                entry.delta,
                entry.quantityNow,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        entry.whyRes()?.let { why ->
            Text(
                text = stringResource(why),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = entry.dayLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Undo, Confirm and Fix, the same answers as the pantry card's estimate row. Fix opens the
 * quantity stepper and commits via [onCorrect] only when the count changed on close.
 */
@Composable
private fun RowActions(
    quantity: Int,
    enabled: Boolean,
    onUndo: () -> Unit,
    onConfirm: () -> Unit,
    onCorrect: (Int) -> Unit,
) {
    var fixExpanded by remember { mutableStateOf(false) }
    var draft by remember { mutableIntStateOf(quantity) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(enabled = enabled, onClick = onUndo) {
            Text(text = stringResource(R.string.pantry_estimate_undo))
        }
        TextButton(enabled = enabled, onClick = onConfirm) {
            Text(text = stringResource(R.string.pantry_digest_confirm))
        }
        Box {
            TextButton(
                enabled = enabled,
                onClick = {
                    draft = quantity
                    fixExpanded = true
                },
            ) {
                Text(text = stringResource(R.string.pantry_estimate_confirm_fix))
            }
            QuantityStepperPopup(
                expanded = fixExpanded,
                labelRes = R.string.pantry_quantity_edit_label,
                draft = draft,
                onDraftChange = { draft = it },
                onDismissRequest = {
                    fixExpanded = false
                    if (draft != quantity) onCorrect(draft)
                },
            )
        }
    }
}

/** Out or Low against the product's total, the same basis as the pantry card's chip. */
@Composable
private fun StockChip(entry: AdjustmentDigestEntry) {
    val level = stockLevel(entry.productQuantity, entry.lowStockThreshold)
    val label = when (level) {
        StockLevel.Out -> R.string.pantry_dashboard_out
        StockLevel.Low -> R.string.pantry_dashboard_running_low
        StockLevel.Ok -> return
    }
    LabelChip(label = stringResource(label), accentColor = stockAccent(level))
}

/** The estimate's basis; `null` for a manual rate, which has nothing to explain. */
private fun AdjustmentDigestEntry.whyRes(): Int? = when (source) {
    EstimateSource.History -> R.string.pantry_digest_why_history
    EstimateSource.ShelfLife -> R.string.pantry_digest_why_shelf_life
    EstimateSource.Manual, null -> null
}

@Composable
private fun AdjustmentDigestEntry.dayLabel(): String = when (daysAgo) {
    0 -> stringResource(R.string.pantry_digest_today)
    1 -> stringResource(R.string.pantry_digest_yesterday)
    else -> pluralStringResource(R.plurals.pantry_digest_days_ago, daysAgo, daysAgo)
}

@Composable
private fun CenteredMessage(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp),
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onAction) { Text(text = actionLabel) }
            }
        }
    }
}
