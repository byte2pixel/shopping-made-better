package com.fullsail.shoppingmadebetter.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * Temporary placeholder screens for top-level tabs that don't have a real screen yet.
 * Each is replaced by its feature screen in a later ticket; only Cart is still a
 * placeholder.
 */
@Composable
private fun PlaceholderScreen(@StringRes titleRes: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.headlineMedium)
    }
}



@Composable
fun CartScreen(modifier: Modifier = Modifier) =
    PlaceholderScreen(titleRes = R.string.tab_cart, modifier = modifier)


@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    ShoppingMadeBetterTheme {
        PlaceholderScreen(titleRes = R.string.tab_shopping_lists)
    }
}
