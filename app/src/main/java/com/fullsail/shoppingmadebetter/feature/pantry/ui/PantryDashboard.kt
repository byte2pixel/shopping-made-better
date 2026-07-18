package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * The categories surfaced by the pantry mini dashboard. Each renders as a square
 * card showing an icon, a count, and a short label. Tapping a card toggles it as
 * a filter over the pantry list.
 */
enum class PantryDashboardFilter(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
) {
    Expiring(R.drawable.ic_expiring, R.string.pantry_dashboard_expiring),
    RunningLow(R.drawable.ic_running_low, R.string.pantry_dashboard_running_low),
    Out(R.drawable.ic_out_of_stock, R.string.pantry_dashboard_out),
    Freezer(R.drawable.ic_freezer, R.string.pantry_dashboard_freezer),
    Fridge(R.drawable.ic_fridge, R.string.pantry_dashboard_fridge),
    Pantry(R.drawable.ic_pantry, R.string.pantry_dashboard_pantry),
}

/** One card's view data: which filter it represents and its count. */
data class PantryDashboardCard(
    val filter: PantryDashboardFilter,
    val count: Int,
)

/**
 * Horizontally scrollable row of dashboard cards shown above the pantry list.
 *
 * @param cards the cards to display, in order.
 * @param selected the set of currently active (highlighted) filters.
 * @param onToggle invoked with a filter when its card is tapped.
 */
@Composable
fun PantryDashboard(
    cards: List<PantryDashboardCard>,
    selected: Set<PantryDashboardFilter>,
    onToggle: (PantryDashboardFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cards, key = { it.filter }) { card ->
            DashboardCard(
                card = card,
                isSelected = card.filter in selected,
                onClick = { onToggle(card.filter) },
            )
        }
    }
}

@Composable
private fun DashboardCard(
    card: PantryDashboardCard,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(card.filter.labelRes)
    val description = pluralStringResource(
        R.plurals.pantry_dashboard_card_desc,
        card.count,
        label,
        card.count,
    )

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = if (isSelected) 8.dp else 4.dp,
        modifier = Modifier
            .size(96.dp)
            .toggleable(
                value = isSelected,
                role = Role.Switch,
                onValueChange = { onClick() },
            )
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(card.filter.iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = card.count.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/**
 * Placeholder card data for the mini dashboard. Replace with real,
 * data-driven counts when the backing data lands (future task).
 */
internal fun placeholderDashboardCards(): List<PantryDashboardCard> = listOf(
    PantryDashboardCard(PantryDashboardFilter.Expiring, count = 3),
    PantryDashboardCard(PantryDashboardFilter.RunningLow, count = 5),
    PantryDashboardCard(PantryDashboardFilter.Out, count = 2),
    PantryDashboardCard(PantryDashboardFilter.Freezer, count = 8),
    PantryDashboardCard(PantryDashboardFilter.Fridge, count = 12),
    PantryDashboardCard(PantryDashboardFilter.Pantry, count = 24),
)

@Preview(showBackground = true)
@Composable
private fun PantryDashboardPreview() {
    var selected by rememberSaveable { mutableStateOf(setOf(PantryDashboardFilter.Expiring)) }
    ShoppingMadeBetterTheme {
        PantryDashboard(
            cards = remember { placeholderDashboardCards() },
            selected = selected,
            onToggle = { filter ->
                selected = if (filter in selected) selected - filter else selected + filter
            },
        )
    }
}
