package com.fullsail.shoppingmadebetter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * Pantry tab. Currently hosts a temporary "View Stores (dev)" button that opens
 * the Supabase example screen ([StoresScreen]) — the dev entry point for
 * SCRUM-79. The button will be removed once the real pantry UI lands.
 */
@Composable
fun PantryScreen(
    onOpenStores: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.tab_pantry),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onOpenStores) {
            Text(text = stringResource(R.string.stores_dev_button))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PantryScreenPreview() {
    ShoppingMadeBetterTheme {
        PantryScreen(onOpenStores = {})
    }
}
