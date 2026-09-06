package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R

/**
 * Entry point to this week's digest: one line saying how many lots the job touched.
 * The caller hides it at [lotCount] 0, so a user with nothing to review never sees it.
 */
@Composable
internal fun AdjustmentDigestCard(
    lotCount: Int,
    onReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val action = stringResource(R.string.pantry_digest_card_action)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = action) { onReview() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pantry_digest_card_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.pantry_digest_card_title,
                        lotCount,
                        lotCount,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
