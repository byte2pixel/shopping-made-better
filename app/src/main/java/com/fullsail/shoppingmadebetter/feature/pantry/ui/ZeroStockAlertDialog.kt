package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.Stepper
import com.fullsail.shoppingmadebetter.feature.pantry.domain.AdjustmentReason
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * Asks whether an auto-adjusted [lot] that reached zero is really out. "Still have some"
 * swaps in a stepper whose count goes to [onStillHave]; back and outside taps call [onDismiss].
 */
@Composable
internal fun ZeroStockAlertDialog(
    lot: InventoryItem,
    onOut: () -> Unit,
    onStillHave: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var correcting by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf(MIN_ON_HAND) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.pantry_zero_stock_title)) },
        text = {
            if (correcting) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = stringResource(R.string.pantry_zero_stock_how_many))
                    Stepper(
                        valueLabel = draft.toString(),
                        onDecrement = { if (draft > MIN_ON_HAND) draft-- },
                        onIncrement = { draft++ },
                        decrementContentDescription = stringResource(R.string.pantry_quantity_decrease),
                        incrementContentDescription = stringResource(R.string.pantry_quantity_increase),
                        decrementEnabled = draft > MIN_ON_HAND,
                    )
                }
            } else {
                Text(text = stringResource(R.string.pantry_zero_stock_message, lot.name))
            }
        },
        confirmButton = {
            if (correcting) {
                TextButton(onClick = { onStillHave(draft) }) {
                    Text(text = stringResource(R.string.pantry_zero_stock_save))
                }
            } else {
                TextButton(onClick = onOut) {
                    Text(text = stringResource(R.string.pantry_zero_stock_add_to_list))
                }
            }
        },
        dismissButton = {
            if (correcting) {
                TextButton(onClick = { correcting = false }) {
                    Text(text = stringResource(R.string.pantry_zero_stock_back))
                }
            } else {
                Row {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.pantry_zero_stock_not_now))
                    }
                    TextButton(onClick = { correcting = true }) {
                        Text(text = stringResource(R.string.pantry_zero_stock_still_have))
                    }
                }
            }
        },
    )
}

/** "Still have some" means at least one. */
private const val MIN_ON_HAND = 1

@Preview(showBackground = true)
@Composable
private fun ZeroStockAlertDialogPreview() {
    ShoppingMadeBetterTheme {
        ZeroStockAlertDialog(
            lot = InventoryItem(
                id = "1",
                productId = "p1",
                name = "Peanut Butter Chocolatey",
                brand = "Great Value",
                description = "",
                size = "16 oz",
                imageUrl = "",
                quantity = 0,
                expiresInDays = 120,
                lastAdjustmentReason = AdjustmentReason.Auto,
            ),
            onOut = {},
            onStillHave = {},
            onDismiss = {},
        )
    }
}
