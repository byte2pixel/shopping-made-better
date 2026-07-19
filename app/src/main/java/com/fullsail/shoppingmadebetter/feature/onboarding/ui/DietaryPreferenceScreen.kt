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
fun DietaryPreferenceScreen(
    selectedDiets: Set<String>,
    onDietToggled: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val diets = listOf("Gluten-Free", "Ketogenic", "Vegetarian", "Vegan", "Paleo", "Dairy-Free")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Do you follow any of these dietary patterns?",
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
            items(diets) { diet ->
                val isSelected = selectedDiets.contains(diet)
                FilterChip(
                    selected = isSelected,
                    onClick = { onDietToggled(diet) },
                    label = { Text(diet) },
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
fun DietaryPreferenceScreenPreview() {
    ShoppingMadeBetterTheme {
        DietaryPreferenceScreen(
            selectedDiets = setOf("Vegan", "Gluten-Free"),
            onDietToggled = {}
        )
    }
}