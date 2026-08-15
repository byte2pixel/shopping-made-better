package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.Stepper

/** Highest low-stock threshold the stepper will climb to. */
private const val MAX_LOW_STOCK_THRESHOLD = 99

/**
 * A `−  (value)  +` control for an item's optional low-stock [threshold], shared by the
 * pantry card's quantity popup and the item detail screen. `null` shows as "Off" and means
 * the item never counts as low; lowering from 1 clears it back to Off, raising from Off sets 1.
 * Each change is reported via [onThresholdChange]; the caller decides when to persist it.
 */
@Composable
internal fun LowStockThresholdStepper(
    threshold: Int?,
    onThresholdChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val valueLabel = threshold?.toString() ?: stringResource(R.string.pantry_low_stock_off)
    Stepper(
        modifier = modifier,
        valueLabel = valueLabel,
        onDecrement = {
            when {
                threshold == null -> Unit
                threshold <= 1 -> onThresholdChange(null)
                else -> onThresholdChange(threshold - 1)
            }
        },
        onIncrement = {
            val next = (threshold ?: 0) + 1
            if (next <= MAX_LOW_STOCK_THRESHOLD) onThresholdChange(next)
        },
        decrementContentDescription = stringResource(R.string.pantry_low_stock_decrease),
        incrementContentDescription = stringResource(R.string.pantry_low_stock_increase),
        decrementEnabled = threshold != null,
        incrementEnabled = (threshold ?: 0) < MAX_LOW_STOCK_THRESHOLD,
    )
}
