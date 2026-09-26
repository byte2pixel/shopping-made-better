package com.fullsail.shoppingmadebetter.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * A single-line search box with a clear button that appears once there is text.
 * Callers own the value and supply their own [label] and [clearContentDescription].
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    clearContentDescription: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(text = label) },
        singleLine = true,
        trailingIcon = {
            // Only worth offering once there is something to clear.
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = clearContentDescription,
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchFieldPreview() {
    ShoppingMadeBetterTheme {
        SearchField(
            value = "oats",
            onValueChange = {},
            label = "Search",
            clearContentDescription = "Clear search",
            modifier = Modifier.padding(16.dp),
        )
    }
}
