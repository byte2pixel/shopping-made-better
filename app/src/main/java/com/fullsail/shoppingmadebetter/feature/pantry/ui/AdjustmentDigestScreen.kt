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
import androidx.compose.runtime.remember
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
import com.fullsail.shoppingmadebetter.core.ui.ProductImage
import com.fullsail.shoppingmadebetter.feature.pantry.domain.AdjustmentDigestEntry
import com.fullsail.shoppingmadebetter.feature.pantry.domain.EstimateSource

/**
 * This week's automatic adjustments, with per-row and bulk undo.
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProductImage(
            imageUrl = entry.imageUrl,
            contentDescription = entry.productName,
            size = 40.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
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
        TextButton(enabled = enabled, onClick = onUndo) {
            Text(text = stringResource(R.string.pantry_estimate_undo))
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
