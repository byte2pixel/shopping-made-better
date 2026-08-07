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
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onFilterSelected = viewModel::onFilterSelected,
        onSelectMeal = viewModel::selectMealPlan,
        onRetry = viewModel::loadMeals,
        modifier = modifier
    )
}

@Composable
private fun MealsContent(
    uiState: MealsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (String) -> Unit,
    onSelectMeal: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Root container without local Scaffold/TopAppBar
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (uiState) {
            is MealsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is MealsUiState.Error -> {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is MealsUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Fixed Uniform Metrics Banner
                    MetricsBanner(
                        canMake = uiState.canMakeCount,
                        almostThere = uiState.almostThereCount,
                        expiring = uiState.expiringCount,
                        recommended = uiState.recommendedCount
                    )

                    // Local Search Bar (SCRUM-134)
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search meals or ingredients...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Filter Pills
                    FilterChipsRow(
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = onFilterSelected
                    )

                    // Recipe List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
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

@Composable
private fun EmptyMealsState(
    searchQuery: String,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (searchQuery.isNotEmpty()) "No meals found for \"$searchQuery\"" else "No meals available",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try adjusting your search query or switching filters.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onClearFilters) {
            Text("Clear Search & Filters")
        }
    }
}

@Composable
private fun ErrorMealsState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E5A44))
        ) {
            Text("Retry")
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
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MetricCard("Can Make", canMake, modifier = Modifier.weight(1f))
            MetricCard("Almost There", almostThere, modifier = Modifier.weight(1f))
            MetricCard("Expiring", expiring, modifier = Modifier.weight(1f))
            MetricCard("Recommended", recommended, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3E6B54)),
        modifier = modifier.height(68.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
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
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = meal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = meal.matchPercentage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { /* Options / Back */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "More"
                    )
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
                    Meal(
                        id = "1",
                        title = "Chicken Alfredo",
                        matchPercentage = "95% Match",
                        itemCount = 4,
                        totalPrice = "$34.19",
                        category = "Recommended",
                        ingredients = emptyList()
                    )
                ),
                selectedFilter = "All",
                searchQuery = "",
                canMakeCount = 12,
                almostThereCount = 3,
                expiringCount = 5,
                recommendedCount = 5
            ),
            onSearchQueryChanged = {},
            onFilterSelected = {},
            onSelectMeal = {},
            onRetry = {}
        )
    }
}