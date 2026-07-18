package com.fullsail.shoppingmadebetter.feature.onboarding.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPreferenceScreen(
    selectedCategories: Set<String>,
    onCategoryToggled: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("🥩 Meat", "🥦 Produce", "🥛 Dairy", "🍞 Bakery", "🥫 Pantry Staples", "❄️ Frozen Foods")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "What categories do you shop for most frequently?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategories.contains(category)
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategoryToggled(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryPreferenceScreenPreview() {
    ShoppingMadeBetterTheme {
        CategoryPreferenceScreen(
            selectedCategories = setOf("🥦 Produce", "🥛 Dairy"),
            onCategoryToggled = {}
        )
    }
}