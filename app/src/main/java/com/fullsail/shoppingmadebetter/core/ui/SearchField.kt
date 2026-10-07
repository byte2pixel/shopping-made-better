package com.fullsail.shoppingmadebetter.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/** Footprint of the trailing slot, the size of an [IconButton], held whether or not it is used. */
private val TRAILING_SLOT = 48.dp

/**
 * A single-line search box with a clear button that appears once there is text.
 * Callers own the value and supply their own [label] and [clearContentDescription].
 *
 * While [busy] the trailing slot shows a spinner in place of the clear button, so a search
 * in flight is visible and the clear waits for it. The slot keeps its size either way, so
 * the field does not resize as the spinner and button come and go.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    clearContentDescription: String,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(text = label) },
        singleLine = true,
        trailingIcon = {
            Box(
                modifier = Modifier.size(TRAILING_SLOT),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    busy -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )

                    // Only worth offering once there is something to clear.
                    value.isNotEmpty() -> IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = clearContentDescription,
                        )
                    }
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
