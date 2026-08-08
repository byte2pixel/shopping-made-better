package com.fullsail.shoppingmadebetter.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * A stateless `−  (value)  +` control. Callers own the value and decide what each
 * button does. Control can drive quantity, days, or any other integer field.
 *
 * @param valueLabel the current value, already formatted for display (e.g. "2" or "4 days").
 * @param decrementContentDescription spoken label for the `−` button.
 * @param incrementContentDescription spoken label for the `+` button.
 * @param decrementEnabled when false, the `−` button is disabled (e.g. at a floor).
 * @param incrementEnabled when false, the `+` button is disabled (e.g. at a ceiling).
 */
@Composable
fun Stepper(
    valueLabel: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementContentDescription: String,
    incrementContentDescription: String,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDecrement, enabled = decrementEnabled) {
            Icon(
                painter = painterResource(R.drawable.ic_remove),
                contentDescription = decrementContentDescription,
            )
        }
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 56.dp),
        )
        IconButton(onClick = onIncrement, enabled = incrementEnabled) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = incrementContentDescription,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepperPreview() {
    ShoppingMadeBetterTheme {
        Stepper(
            valueLabel = "2",
            onDecrement = {},
            onIncrement = {},
            decrementContentDescription = "Decrease",
            incrementContentDescription = "Increase",
        )
    }
}

@Preview(showBackground = true, name = "Decrement disabled")
@Composable
private fun StepperFlooredPreview() {
    ShoppingMadeBetterTheme {
        Stepper(
            valueLabel = "1",
            onDecrement = {},
            onIncrement = {},
            decrementContentDescription = "Decrease",
            incrementContentDescription = "Increase",
            decrementEnabled = false,
        )
    }
}
