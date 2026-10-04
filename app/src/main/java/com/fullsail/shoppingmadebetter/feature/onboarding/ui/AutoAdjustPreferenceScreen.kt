package com.fullsail.shoppingmadebetter.feature.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/** Onboarding step asking whether the app may lower pantry quantities on its own. */
@Composable
fun AutoAdjustPreferenceScreen(
    selected: Boolean?,
    onSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_auto_adjust_question),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(R.string.profile_auto_adjust_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            OptionCard(
                label = stringResource(R.string.onboarding_auto_adjust_yes),
                isSelected = selected == true,
                onClick = { onSelected(true) },
            )
            OptionCard(
                label = stringResource(R.string.onboarding_auto_adjust_no),
                isSelected = selected == false,
                onClick = { onSelected(false) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AutoAdjustPreferenceScreenPreview() {
    ShoppingMadeBetterTheme {
        AutoAdjustPreferenceScreen(
            selected = true,
            onSelected = {}
        )
    }
}
