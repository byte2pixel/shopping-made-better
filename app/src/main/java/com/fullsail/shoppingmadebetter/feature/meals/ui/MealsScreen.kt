package com.fullsail.shoppingmadebetter.feature.meals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.meals.domain.Meal
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

@Composable
fun MealsScreen(
    viewModel: MealsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    MealsContent(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onSelectMeal = viewModel::selectMealPlan,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealsContent(
    uiState: MealsUiState,
    onFilterSelected: (String) -> Unit,
    onSelectMeal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meals") },
                navigationIcon = {
                    IconButton(onClick = { /* Open drawer */ }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Menu"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is MealsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MealsUiState.Error -> Text(text = uiState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                is MealsUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MetricsBanner(
                            canMake = uiState.canMakeCount,
                            almostThere = uiState.almostThereCount,
                            expiring = uiState.expiringCount,
                            recommended = uiState.recommendedCount
                        )

                        FilterChipsRow(
                            selectedFilter = uiState.selectedFilter,
                            onFilterSelected = onFilterSelected
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.meals) { meal ->
                                MealRecipeCard(
                                    meal = meal,
                                    isSelected = meal.id == uiState.selectedMealId,
                                    onDetailsClick = { onSelectMeal(meal.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsBanner(
    canMake: Int,
    almostThere: Int,
    expiring: Int,
    recommended: Int
) {
    Surface(
        color = Color(0xFF2E5A44),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCard("Can Make", canMake)
            MetricCard("Almost There", almostThere)
            MetricCard("Expiring", expiring)
            MetricCard("Recommended", recommended)
        }
    }
}

@Composable
private fun MetricCard(label: String, count: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3E6B54)),
        modifier = Modifier.width(80.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White)
            Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("All", "Can Make", "Expiring", "Almost There", "Recommended")
    LazyRow(
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) }
            )
        }
    }
}

@Composable
private fun MealRecipeCard(
    meal: Meal,
    isSelected: Boolean,
    onDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = meal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = meal.matchPercentage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { /* Options */ }) {
                    Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "More")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Recipe Image", color = Color.DarkGray)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { /* Add to List */ }) {
                    Text("Add to List")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDetailsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E5A44))
                ) {
                    Text("Details")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MealsScreenPreview() {
    ShoppingMadeBetterTheme {
        MealsContent(
            uiState = MealsUiState.Success(
                meals = listOf(
                    Meal("1", "Chicken Alfredo", "95% Match", 4, "$34.19", "Recommended")
                ),
                selectedFilter = "All",
                canMakeCount = 12,
                almostThereCount = 3,
                expiringCount = 5,
                recommendedCount = 5
            ),
            onFilterSelected = {},
            onSelectMeal = {}
        )
    }
}