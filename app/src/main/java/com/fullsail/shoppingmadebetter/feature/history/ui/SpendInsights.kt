package com.fullsail.shoppingmadebetter.feature.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.history.domain.CheapestStore
import com.fullsail.shoppingmadebetter.feature.history.domain.MonthlySpend
import com.fullsail.shoppingmadebetter.feature.history.domain.SpendSummary
import com.fullsail.shoppingmadebetter.feature.history.domain.StoreSpend
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The spend cards above the History list: this month's total against last month's,
 * where the money went, and which store would have been cheapest.
 *
 * Each card hides itself when it has nothing to say, so a first month shows a total
 * with no delta rather than a fabricated comparison.
 */
@Composable
internal fun SpendInsightsSection(summary: SpendSummary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MonthSpendCard(thisMonth = summary.thisMonth, lastMonth = summary.lastMonth)
        if (summary.byStore.isNotEmpty()) {
            StoreBreakdownCard(stores = summary.byStore)
        }
        summary.cheapest?.let { CheapestStoreCard(cheapest = it) }
    }
}

/** The hero: this month's total, its trip count, and the change since last month. */
@Composable
private fun MonthSpendCard(thisMonth: MonthlySpend, lastMonth: MonthlySpend?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.history_insights_this_month),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatPrice(thisMonth.total),
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.history_insights_trips,
                        thisMonth.tripCount,
                        thisMonth.tripCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // No previous month means no honest comparison to draw.
            if (lastMonth != null) {
                DeltaPill(thisMonth = thisMonth.total, lastMonth = lastMonth.total)
            }
        }
    }
}

/** Spending less is the good direction, so down is the positive colour. */
@Composable
private fun DeltaPill(thisMonth: Double, lastMonth: Double) {
    val difference = thisMonth - lastMonth
    val spentLess = difference < 0
    val amount = formatPrice(abs(difference))
    val label = stringResource(
        if (spentLess) R.string.history_insights_delta_down else R.string.history_insights_delta_up,
        amount,
    )
    val description = stringResource(
        if (spentLess) {
            R.string.history_insights_delta_down_desc
        } else {
            R.string.history_insights_delta_up_desc
        },
        amount,
    )

    Column(horizontalAlignment = Alignment.End) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (spentLess) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
            contentColor = if (spentLess) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Text(
            text = stringResource(R.string.history_insights_delta_caption),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** This month split by store, biggest first, each with a bar for its share. */
@Composable
private fun StoreBreakdownCard(stores: List<StoreSpend>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.history_insights_by_store),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            stores.forEach { store -> StoreSpendRow(store) }
        }
    }
}

@Composable
private fun StoreSpendRow(store: StoreSpend) {
    val name = store.storeName ?: stringResource(R.string.history_unknown_store)
    val percent = (store.share * 100).roundToInt()
    val description = stringResource(
        R.string.history_insights_store_desc,
        name,
        formatPrice(store.total),
        percent,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(STORE_NAME_WIDTH),
        )
        ShareBar(share = store.share, modifier = Modifier.weight(1f))
        Text(
            text = formatPrice(store.total),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** A proportional bar over a track; no chart library for three rows. */
@Composable
private fun ShareBar(share: Double, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(BAR_HEIGHT)
            .clip(RoundedCornerShape(BAR_HEIGHT / 2))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(share.coerceIn(0.0, 1.0).toFloat())
                .height(BAR_HEIGHT)
                .clip(RoundedCornerShape(BAR_HEIGHT / 2))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/** The savings pitch: the store that would have cost least for what was bought. */
@Composable
private fun CheapestStoreCard(cheapest: CheapestStore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shopping_cart),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.history_insights_cheapest_title),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = stringResource(
                        R.string.history_insights_cheapest_body,
                        cheapest.storeName,
                        formatPrice(cheapest.saving),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Keeps the bars left-aligned across rows whatever the store names are. */
private val STORE_NAME_WIDTH = 96.dp
private val BAR_HEIGHT = 8.dp

internal fun previewSummary(
    thisMonthTotal: Double = 142.87,
    tripCount: Int = 6,
    lastMonth: MonthlySpend? = MonthlySpend(LocalDate(2026, 7, 1), 161.27, 7),
    byStore: List<StoreSpend> = listOf(
        StoreSpend("s-2", "ALDI", 71.20, 0.498),
        StoreSpend("s-3", "Publix", 48.10, 0.337),
        StoreSpend("s-1", "Whole Foods", 23.57, 0.165),
    ),
    cheapest: CheapestStore? = CheapestStore("s-2", "ALDI", 130.57, 12.30),
) = SpendSummary(
    thisMonth = MonthlySpend(LocalDate(2026, 8, 1), thisMonthTotal, tripCount),
    lastMonth = lastMonth,
    byStore = byStore,
    cheapest = cheapest,
)

@Composable
private fun SpendInsightsPreviewHost(summary: SpendSummary) {
    ShoppingMadeBetterTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SpendInsightsSection(summary = summary, modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview(showBackground = true, name = "Spent less than last month")
@Composable
private fun SpendInsightsPreview() {
    SpendInsightsPreviewHost(previewSummary())
}

@Preview(showBackground = true, name = "Spent more than last month")
@Composable
private fun SpendInsightsUpPreview() {
    SpendInsightsPreviewHost(
        previewSummary(lastMonth = MonthlySpend(LocalDate(2026, 7, 1), 96.40, 4)),
    )
}

@Preview(showBackground = true, name = "First month, no delta")
@Composable
private fun SpendInsightsNoDeltaPreview() {
    SpendInsightsPreviewHost(previewSummary(lastMonth = null, cheapest = null))
}

@Preview(showBackground = true, name = "One store, no saving to name")
@Composable
private fun SpendInsightsSingleStorePreview() {
    SpendInsightsPreviewHost(
        previewSummary(
            byStore = listOf(StoreSpend("s-2", "ALDI", 142.87, 1.0)),
            cheapest = null,
        ),
    )
}

@Preview(showBackground = true, name = "Deleted store")
@Composable
private fun SpendInsightsUnknownStorePreview() {
    SpendInsightsPreviewHost(
        previewSummary(
            byStore = listOf(
                StoreSpend("s-2", "ALDI", 100.00, 0.7),
                StoreSpend(null, null, 42.87, 0.3),
            ),
        ),
    )
}
